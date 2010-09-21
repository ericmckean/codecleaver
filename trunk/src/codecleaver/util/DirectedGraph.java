/* Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package codecleaver.util;

import codecleaver.iterable.EmptyIterable;
import codecleaver.iterable.Sequence;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * A graph with directed edges between vertices. The graph is mutable. Vertices may not be null. The
 * graph can compute transitive reachability both to and from a vertex, as well as distance to and
 * from a vertex.
 *
 * @param <T> the type of the vertices in the graph.
 */
public class DirectedGraph<T> {
  private final Map<T, Vertex<T>> nodes = new HashMap<T, Vertex<T>>();

  /**
   * A vertex in a directed graph. Stores both incoming and outgoing edges. Mutable.
   */
  private static class Vertex<T> {
    public Vertex(T vertex) {
      this.vertex = vertex;
    }

    public final T vertex;
    public final HashSet<T> outEdges = new HashSet<T>();
    public final HashSet<T> inEdges = new HashSet<T>();
  }

  public DirectedGraph() {}

  /**
   * Adds an edge to the graph. Both the source and destination may not be null.
   */
  public void addEdge(T source, T destination) {
    Vertex<T> fromNode = nodeOfVertex(source);
    Vertex<T> toNode = nodeOfVertex(destination);
    addEdge(fromNode, toNode);
  }

  /**
   * Adds an edge to the graph from source to destination.
   */
  private void addEdge(Vertex<T> source, Vertex<T> destination) {
    source.outEdges.add(destination.vertex);
    destination.inEdges.add(source.vertex);
  }

  /**
   * Adds an edge, but only if both the source and destination are both not null, otherwise does
   * nothing.
   */
  public void addOptionalEdge(T source, T destination) {
    if (source == null || destination == null) {
      return;
    }

    addEdge(source, destination);
  }

  /**
   * Have any edges been added for this vertex.
   */
  public boolean containsVertex(T vertex) {
    return existingNodeOfVertex(vertex) != null;
  }

  /**
   * Get or create the node for a vertex.
   */
  private Vertex<T> nodeOfVertex(T vertex) {
    Vertex<T> result = existingNodeOfVertex(vertex);
    if (result == null) {
      result = new Vertex<T>(vertex);
      nodes.put(vertex, result);
    }
    return result;
  }

  /**
   * Get an existing node for a vertex or return null if the vertex has not been added to the graph.
   */
  private Vertex<T> existingNodeOfVertex(T vertex) {
    return nodes.get(vertex);
  }

  /**
   * Returns the set of vertices that can be reached directly from a vertex. Returns an empty set if
   * the vertex has no edges.
   */
  public Set<T> outEdgesOfVertex(T vertex) {
    Vertex<T> node = existingNodeOfVertex(vertex);
    return node == null ? ImmutableSet.<T>of() : node.outEdges;
  }

  /**
   * Returns the set of vertices that can reach a vertex directly. Returns an empty set if the
   * vertex has no edges.
   */
  public Set<T> inEdgesOfVertex(T vertex) {
    Vertex<T> node = existingNodeOfVertex(vertex);
    return node == null ? ImmutableSet.<T>of() : node.inEdges;
  }

  /**
   * Computes reachability and distances for a directed graph.
   */
  private static class Reachability<T> {
    private final HashMap<T, Integer> queued;
    private final ArrayList<T> todo;
    private final HashSet<T> terminals;
    private Integer terminalDistance;
    private final Func<? super T, ? extends Iterable<? extends T>> getEdges;

    private Reachability(Func<? super T, ? extends Iterable<? extends T>> getEdges,
        Iterable<? extends T> terminals) {
      this.queued = new HashMap<T, Integer>();
      this.todo = new ArrayList<T>();
      this.getEdges = getEdges;
      this.terminals = Sequence.createSet(terminals);
    }

    /**
     * Compute reachability and distance for the given roots.
     */
    private void compute(Iterable<? extends T> roots) {
      queueAll(roots, 0);
      while (!todo.isEmpty()) {
        T value = todo.remove(todo.size() - 1);
        Integer distance = queued.get(value) + 1;
        if (terminalDistance == null || distance <= terminalDistance) {
          queueAll(getEdges.apply(value), distance);
        }
      }
    }

    /**
     * Queue all unqueued values with distance of distance.
     */
    private void queueAll(Iterable<? extends T> values, Integer distance) {
      for (T value : values) {
        if (!queued.containsKey(value)) {
          queued.put(value, distance);
          if (terminalDistance != null && terminals.contains(value)) {
            terminalDistance = distance;
          }
          todo.add(value);
        }
      }
    }

    /**
     * Returns the set of reachable vertices for this computation.
     */
    private Set<T> canReach() {
      return queued.keySet();
    }

    /**
     * Returns the set of vertices reachable from any vertex in roots via the supplied getEdges.
     */
    public static <T> Set<T> canReach(Func<T, Set<T>> getEdges, Iterable<? extends T> roots) {
      Reachability<T> reachability = new Reachability<T>(getEdges, EmptyIterable.<T>value());
      reachability.compute(roots);
      return reachability.canReach();
    }

    /**
     * Returns the reachable vertices by distance.
     */
    private ArrayList<ArrayList<T>> distancesBy() {
      ArrayList<ArrayList<T>> result = new ArrayList<ArrayList<T>>();
      for (Entry<T, Integer> entry : queued.entrySet()) {
        int distance = entry.getValue();
        T value = entry.getKey();
        while (distance >= result.size()) {
          result.add(new ArrayList<T>());
        }
        result.get(distance).add(value);
      }
      return result;
    }

    /**
     * Returns the vertices reachable from roots by the supplied getEdges by distance. So
     * distancesBy(getEdges, roots, sinks).getAt(n) returns the vertices a distance of n from roots.
     * Limits results to distances less than or equal to the minimum distance from roots to sinks.
     */
    public static <T> ArrayList<ArrayList<T>> distancesBy(
        Func<T, Set<T>> getEdges, Iterable<? extends T> roots, Iterable<? extends T> sinks) {
      Reachability<T> reachability = new Reachability<T>(getEdges, sinks);
      reachability.compute(roots);
      return reachability.distancesBy();
    }
  }

  /**
   * Returns all vertices which are reachable from root.
   */
  public Set<T> reachableFrom(T root) {
    return reachableFrom(Sequence.singleton(root));
  }

  /**
   * Returns all vertices which are reachable from any vertex in roots.
   */
  public Set<T> reachableFrom(Iterable<? extends T> roots) {
    return Reachability.canReach(getOutEdgesOfVertex, roots);
  }

  /**
   * Returns all vertices which can reach a vertex in sinks.
   */
  public Set<T> canReach(Iterable<? extends T> sinks) {
    return Reachability.canReach(getInEdgesOfVertex, sinks);
  }

  /**
   * Returns the vertices reachable from roots by their minimum distance from a vertex in roots. So
   * distanceFrom(roots).getAt(n) yields all vertices a distance of n edges from roots.
   */
  public ArrayList<ArrayList<T>> distancesFrom(Iterable<? extends T> roots) {
    return distancesFrom(roots, EmptyIterable.<T>value());
  }

  /**
   * Returns the vertices that can reach sinks by their minimum distance to a vertex in sinks. So
   * distanceTo(sinks).getAt(n) yields all vertices which are a distance of n edges to sinks.
   */
  public ArrayList<ArrayList<T>> distancesTo(Iterable<? extends T> sinks) {
    return distancesTo(sinks, EmptyIterable.<T>value());
  }

  /**
   * Returns the vertices reachable from roots by their minimum distance from a vertex in roots. So
   * distanceFrom(roots).getAt(n) yields all vertices a distance of n edges from roots. Limits
   * results distances equal to or less than the minimum distance from roots to sinks.
   */
  public ArrayList<ArrayList<T>> distancesFrom(
      Iterable<? extends T> roots, Iterable<? extends T> sinks) {
    return Reachability.distancesBy(getOutEdgesOfVertex, roots, sinks);
  }

  /**
   * Returns the vertices that can reach sinks by their minimum distance to a vertex in sinks. So
   * distanceTo(sinks).getAt(n) yields all vertices which are a distance of n edges to sinks. Limits
   * results to distances equal to or less than the minimum distance from sinks to roots.
   */
  public ArrayList<ArrayList<T>> distancesTo(
      Iterable<? extends T> sinks, Iterable<? extends T> roots) {
    return Reachability.distancesBy(getInEdgesOfVertex, sinks, roots);
  }

  private final Func<T, Set<T>> getOutEdgesOfVertex =
      new Func<T, Set<T>>() {

        @Override public Set<T> apply(T arg) {
          return outEdgesOfVertex(arg);
        }
      };

  private final Func<T, Set<T>> getInEdgesOfVertex =
      new Func<T, Set<T>>() {

        @Override public Set<T> apply(T arg) {
          return inEdgesOfVertex(arg);
        }
      };
}
