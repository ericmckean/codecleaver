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

package codecleaver;


import codecleaver.iterable.FilteredIterable;
import codecleaver.iterable.Sequence;
import codecleaver.util.DirectedGraph;
import codecleaver.util.Func;
import codecleaver.util.Pair;

import com.google.common.collect.ImmutableSet;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

import static codecleaver.PredefinedSet.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * The mutable state of a CodeCleaver program. Includes open files, Ids, Infos, dependency graph,
 * predefined and user defined sets.
 */
public final class State {

  public final IdTable ids = new IdTable();
  private InfoTable infos = new InfoTable(ids);
  private DirectedGraph<Id> inheritanceGraph = new DirectedGraph<Id>();
  private DirectedGraph<Id> dependencyGraph = new DirectedGraph<Id>();
  private final ArrayList<String> definitionFileList = new ArrayList<String>();
  private final HashSet<String> definitionFileSet = new HashSet<String>();
  private final HashSet<String> symbolFiles = new HashSet<String>();

  private final HashMap<String, Supplier<HashSet<Id>>> sets =
      new HashMap<String, Supplier<HashSet<Id>>>();
  private final HashSet<String> predefinedSetNames = new HashSet<String>();

  private boolean predefinedSetsStale;

  public State() {}

  public void addDefinitionsOfFile(String fileName) {
    if (!this.containsFile(fileName)) {
      definitionFileList.add(fileName);
      definitionFileSet.add(fileName);
      clearInfos();
    }
  }

  public void removeDefinitionsOfFile(String fileName) {
    definitionFileList.remove(fileName);
    definitionFileSet.remove(fileName);
    clearInfos();
  }

  private void clearInfos() {
    setInfos(null);
    this.inheritanceGraph = null;
    this.dependencyGraph = null;
  }

  public Iterable<Pair<TypeId, String>> ensureInfos() throws StateException {
    Iterable<Pair<TypeId, String>> result = null;
    if (infos == null) {
      result = rebuildInfos();
    }
    if (predefinedSetsStale) {
      buildPredefinedSets();
    }
    
    return result;
  }

  private Iterable<Pair<TypeId, String>> rebuildInfos() throws StateException {
    Pair<InfoTable, Iterable<Pair<TypeId, String>>> results = buildInfos();
    InfoTable infos = results.first;
    DirectedGraph<Id> inheritanceGraph = buildInheritanceGraph(infos);
    DirectedGraph<Id> dependencyGraph = buildDependencyGraph(infos, inheritanceGraph);
    setInfos(infos);
    this.inheritanceGraph = inheritanceGraph;
    this.dependencyGraph = dependencyGraph;
    
    return results.second;
  }

  private DirectedGraph<Id> buildDependencyGraph(InfoTable infos, DirectedGraph<Id> inheritanceGraph) throws StateException {
    DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder(infos, inheritanceGraph);
    visitClassFiles(graphBuilder);
    return graphBuilder.getResult();
  }

  /**
   * Return a new inheritance graph from an InfoTable.
   *
   *  Inheritance graph contains: 
   *  class to super class 
   *  class to implemented interfaces 
   *  interface to super interfaces 
   *  interface to object non-static, non-init method to methods with same name/descriptor in any 
   *  inheritance of contained type
   */
  private static DirectedGraph<Id> buildInheritanceGraph(InfoTable infos) {
    DirectedGraph<Id> graph = new DirectedGraph<Id>();

    // add type inheritance hierarchy
    for (Info info : getInfosOfType(infos, IdType.Type)) {
      TypeInfo type = (TypeInfo) info;
      if (type.superId != null) {
        graph.addEdge(type.id, type.superId);
      }
      for (TypeId iface : type.interfaces) {
        graph.addEdge(type.id, iface);
      }
      if (type.isInterface()) {
        graph.addEdge(type.id, infos.ids.object);
      }
    }

    // add override methods
    for (Info info : getInfosOfType(infos, IdType.Method)) {
      MethodInfo method = (MethodInfo) info;
      if (!method.isStatic() && !method.isInitializer()) {
        Id containingType = method.id.getParent();
        for (Id inheritedType : graph.reachableFrom(containingType)) {
          if (inheritedType != containingType) {
            MethodInfo overriddenMethod =
                infos.lookupMethod((TypeId) inheritedType, method.getName(), method.desc);
            if (overriddenMethod != null) {
              graph.addEdge(method.id, overriddenMethod.id);
            }
          }
        }
      }
    }

    return graph;
  }

  private Pair<InfoTable, Iterable<Pair<TypeId, String>>> buildInfos() throws StateException {
    InfoTable infos = new InfoTable(ids);
    DefinitionBuilder definitionBuilder = new DefinitionBuilder(this.ids, infos);
    visitClassFiles(definitionBuilder);
    return new Pair<InfoTable, Iterable<Pair<TypeId, String>>>(infos, 
        definitionBuilder.getIgnoredTypes());
  }


  public static Iterable<String> jarsOfDirectory(String directoryName) {
    File dir = new File(directoryName);
    return Sequence.select(Sequence.toSequence(dir.listFiles(
        new FilenameFilter() {

          @Override public boolean accept(File dir, String name) {
            return name.endsWith(".jar");
          }
        })),
        new Func<File, String>() {

          @Override public String apply(File file) {
            return file.getPath();
          }
        });

  }

  private void visitClassFiles(IdClassVisitor visitor) throws StateException {
    for (String fileName : getFiles()) {
      IdClassReader.visitJarFile(ids, visitor, fileName);
    }
  }

  public Iterable<String> getFiles() {
    return definitionFileList;
  }

  public boolean containsFile(String fileName) {
    return definitionFileSet.contains(fileName);
  }

  public Iterable<String> getSymbolFiles() {
    return symbolFiles;
  }

  public boolean containsSymbolFile(String fileName) {
    return symbolFiles.contains(fileName);
  }


  public Iterable<Id> getFrom(Iterable<Id> values) {
    return getFrom(values, dependencyGraph);
  }

  public Iterable<Id> getTo(Iterable<Id> values) {
    return getTo(values, dependencyGraph);
  }

  public Iterable<Id> getTransitiveFrom(Iterable<Id> values) {
    return getTransitiveFrom(values, dependencyGraph);
  }

  public Iterable<Id> getTransitiveTo(Iterable<Id> values) {
    return getTransitiveTo(values, dependencyGraph);
  }

  public Iterable<Id> getOverrides(Iterable<Id> values) {
    return getFrom(values, inheritanceGraph);
  }

  public Iterable<Id> getOverridden(Iterable<Id> values) {
    return getTo(values, inheritanceGraph);
  }

  public Iterable<Id> getTransitiveOverrides(Iterable<Id> values) {
    return getTransitiveFrom(values, inheritanceGraph);
  }

  public Iterable<Id> getTransitiveOverridden(Iterable<Id> values) {
    return getTransitiveTo(values, inheritanceGraph);
  }

  private static Iterable<Id> getFrom(Iterable<Id> values, DirectedGraph<Id> graph) {
    HashSet<Id> result = new HashSet<Id>();
    for (Id value : values) {
      Sequence.addAll(result, graph.outEdgesOfVertex(value));
    }
    Sequence.removeAll(result, values);
    return result;
  }

  private static Iterable<Id> getTo(Iterable<Id> values, DirectedGraph<Id> graph) {
    HashSet<Id> result = new HashSet<Id>();
    for (Id value : values) {
      Sequence.addAll(result, graph.inEdgesOfVertex(value));
    }
    Sequence.removeAll(result, values);
    return result;
  }

  private static Iterable<Id> getTransitiveFrom(Iterable<Id> values, DirectedGraph<Id> graph) {
    HashSet<Id> result = new HashSet<Id>(graph.reachableFrom(values));
    Sequence.addAll(result, values);
    return result;
  }

  private static Iterable<Id> getTransitiveTo(Iterable<Id> values, DirectedGraph<Id> graph) {
    HashSet<Id> result = new HashSet<Id>(graph.canReach(values));
    Sequence.addAll(result, values);
    return result;
  }

  public Iterable<Id> getTransitiveExpand(Iterable<Id> values) {
    HashSet<Id> result = new HashSet<Id>();
    for (Id value : values) {
      Sequence.addAll(result, transitiveExpand(value));
    }
    return result;
  }

  public Iterable<Id> getExpand(Iterable<Id> values) {
    HashSet<Id> result = new HashSet<Id>();
    for (Id value : values) {
      Sequence.addAll(result, expand(value));
    }
    return result;
  }

  private Iterable<Id> expand(Id value) {
    return value.getChildren();
  }

  private Iterable<Id> transitiveExpand(Id value) {
    return getSet(value);
  }

  public ArrayList<ArrayList<Id>> getDistancesFrom(Iterable<Id> roots) {
    return dependencyGraph.distancesFrom(roots);
  }

  public ArrayList<ArrayList<Id>> getDistancesTo(Iterable<Id> sinks) {
    return dependencyGraph.distancesTo(sinks);
  }

  public ArrayList<ArrayList<Id>> getDistancesFrom(Iterable<Id> roots, Iterable<Id> sinks) {
    return dependencyGraph.distancesFrom(roots, sinks);
  }

  public ArrayList<ArrayList<Id>> getDistancesTo(Iterable<Id> sinks, Iterable<Id> roots) {
    return dependencyGraph.distancesTo(sinks, roots);
  }

  public Iterable<Id> getUnion(Iterable<Id> left, Iterable<Id> right) {
    HashSet<Id> result = Sequence.createSet(left);
    Sequence.addAll(result, right);
    return result;
  }

  public Iterable<Id> getIntersect(Iterable<Id> left, Iterable<Id> right) {
    HashSet<Id> result = Sequence.createSet(left);
    result.retainAll(Sequence.createCollection(right));
    return result;
  }

  public Iterable<Id> getMinus(Iterable<Id> left, Iterable<Id> right) {
    HashSet<Id> result = Sequence.createSet(left);
    Sequence.removeAll(result, right);
    return result;
  }

  public void buildPredefinedSets() {
    createPredefinedSetFromInfos(DEFINITIONS, getInfos().getAll());
    createPredefinedSet(ALL, ids.getAll());
    createPredefinedSet(EMPTY);

    // by IdType
    createPredefinedSet(PACKAGES, getIdsOfType(IdType.Package));
    createPredefinedSet(TYPES, getIdsOfType(IdType.Type));
    createPredefinedSet(METHODS, getIdsOfType(IdType.Method));
    createPredefinedSet(FIELDS, getIdsOfType(IdType.Field));

    // natives
    createPredefinedSetFromInfos(
        NATIVE_METHODS, new FilteredIterable<Info>(getInfosOfType(IdType.Method),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info method) {
                return method.isNative();
              }
            }));
    createPredefinedSetFromInfos(
        ABSTRACT_METHODS, new FilteredIterable<Info>(getInfosOfType(IdType.Method),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info method) {
                return method.isAbstract();
              }
            }));
    createPredefinedSetFromInfos(INTERFACES, new FilteredIterable<Info>(getInfosOfType(IdType.Type),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isInterface();
          }
        }));
    createPredefinedSetFromInfos(CLASSES, new FilteredIterable<Info>(getInfosOfType(IdType.Type),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return !type.isInterface();
          }
        }));
    createPredefinedSetFromInfos(PUBLICS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isPublic();
          }
        }));
    createPredefinedSetFromInfos(PROTECTEDS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isProtected();
          }
        }));
    createPredefinedSetFromInfos(PRIVATES, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isPrivate();
          }
        }));
    createPredefinedSetFromInfos(PACKAGE_PRIVATES, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isPackagePrivate();
          }
        }));
    createPredefinedSetFromInfos(STATICS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isStatic();
          }
        }));
    createPredefinedSetFromInfos(FINALS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info type) {
            return type.isFinal();
          }
        }));
    createPredefinedSetFromInfos(
        SYNCHRONIZED_METHODS, new FilteredIterable<Info>(getInfosOfType(IdType.Method),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isSynchronized();
              }
            }));
    createPredefinedSetFromInfos(
        VOLATILE_FIELDS, new FilteredIterable<Info>(getInfosOfType(IdType.Field),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isVolatile();
              }
            }));
    createPredefinedSetFromInfos(
        VARARGS_METHODS, new FilteredIterable<Info>(getInfosOfType(IdType.Method),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isVarArgs();
              }
            }));
    createPredefinedSetFromInfos(
        TRANSIENT_FIELDS, new FilteredIterable<Info>(getInfosOfType(IdType.Field),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isTransient();
              }
            }));
    createPredefinedSetFromInfos(
        ABSTRACT_CLASSES, new FilteredIterable<Info>(getInfosOfType(IdType.Type),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isAbstract() && !type.isInterface();
              }
            }));
    createPredefinedSetFromInfos(STRICTS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info info) {
            return info.isStrict();
          }
        }));
    createPredefinedSetFromInfos(SYNTHETICS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info info) {
            return info.isSynthetic();
          }
        }));
    createExternalsPredefinedSet();
    createPredefinedSetFromInfos(
        ANNOTATIONS, new FilteredIterable<Info>(getInfosOfType(IdType.Type),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info type) {
                return type.isAnnotation();
              }
            }));
    createPredefinedSetFromInfos(
        OVERRIDES, new FilteredIterable<Info>(getInfosOfType(IdType.Method),
            new Func<Info, Boolean>() {

              @Override public Boolean apply(Info method) {
                return inheritanceGraph.containsVertex(method.id);
              }
            }));
    createPredefinedSet(INITIALIZERS, new FilteredIterable<Id>(getIdsOfType(IdType.Method),
        new Func<Id, Boolean>() {

          @Override public Boolean apply(Id id) {
            return ((MethodId)id).isInitializer();
          }}));
    createPredefinedSet(STATIC_INITIALIZERS, new FilteredIterable<Id>(getIdsOfType(IdType.Method),
        new Func<Id, Boolean>() {

          @Override public Boolean apply(Id id) {
            return ((MethodId)id).isStaticInitializer();
          }}));
    
    this.predefinedSetsStale = false;
  }

  private Iterable<Id> getIdsOfType(final IdType type) {
    return new FilteredIterable<Id>(ids.getAll(),
        new Func<Id, Boolean>() {

          @Override public Boolean apply(Id id) {
            return id.type == type;
          }
        });
  }

  private Iterable<Info> getInfosOfType(final IdType type) {
    return getInfosOfType(this.infos, type);
  }

  private static Iterable<Info> getInfosOfType(final InfoTable infos, final IdType type) {
    return new FilteredIterable<Info>(infos.getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info t) {
            return t.getType() == type;
          }
        });
  }

  private void createExternalsPredefinedSet() {
    createPredefinedSetFromInfos(EXTERNALS, new FilteredIterable<Info>(getInfos().getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info info) {
            return hasExternalVisibility(info);
          }
        }));
  }

  public boolean isPredefinedSet(String name) {
    return predefinedSetNames.contains(name);
  }

  public boolean isValidNewSetName(String name) {
    return !hasSet(name) && isValidMutableSetName(name);
  }

  public boolean isValidMutableSetName(String name) {
    return !predefinedSetNames.contains(name) && !containsAnyIdChars(name)
        && !isValidSimplePackageName(name);
  }

  private boolean containsAnyIdChars(String name) {
    return (name.contains(".") || name.contains("/"));
  }

  private static boolean isValidSimplePackageName(String name) {
    return Sequence.all(name,
        new Func<Character, Boolean>() {

          @Override public Boolean apply(Character value) {
            return Character.isLowerCase(value);
          }
        });
  }

  public boolean hasSet(String name) {
    return sets.containsKey(name);
  }

  public HashSet<Id> getSet(String name) {
    return sets.get(name).get();
  }

  public Set<Id> getUnextensiblePackages() {
    if (hasSet(UNEXTENSIBLE_PACKAGES.toString())) {
      return getSet(UNEXTENSIBLE_PACKAGES.toString());
    }
    return ImmutableSet.of();
  }

  public Iterable<Id> getAnnotationSet(final TypeId annotationType) {
    return Info.idsOfInfos(new FilteredIterable<Info>(infos.getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info info) {
            return info.annotations.containsKey(annotationType);
          }
        }));
  }

  public Iterable<Id> getAnnotationSet(
      final TypeId annotationType, final Map<String, String> elements) {
    return Info.idsOfInfos(new FilteredIterable<Info>(infos.getAll(),
        new Func<Info, Boolean>() {

          @Override public Boolean apply(Info info) {
            final AnnotationInfo annotation = info.annotations.get(annotationType);
            return annotation != null && Sequence.all(elements.entrySet(),
                new Func<Map.Entry<String, String>, Boolean>() {

                  @Override public Boolean apply(Entry<String, String> entry) {
                    final Object value = annotation.elements.get(entry.getKey());
                    if (value == null) {
                      return false;
                    }
                    if (value.getClass().isArray()) {
                      final Object[] values = (Object[]) value;
                      for (Object element : values) {
                        if (entry.getValue().equals(element.toString())) {
                          return true;
                        }
                      }
                      return false;
                    } else {
                      return entry.getValue().equals(value.toString());
                    }
                  }
                });
          }
        }));
  }

  public HashSet<Id> createSet(String name) {
    HashSet<Id> result = new HashSet<Id>();
    return assignSet(name, result);
  }

  public HashSet<Id> assignSet(String name, final HashSet<Id> values) {
    sets.put(name, Suppliers.ofInstance(values));
    if (name.equals(UNEXTENSIBLE_PACKAGES)) {
      createExternalsPredefinedSet();
    }
    return getSet(name);
  }

  public void deleteSet(String name) {
    sets.remove(name);
    if (name.equals(UNEXTENSIBLE_PACKAGES)) {
      createExternalsPredefinedSet();
    }
  }

  private void createPredefinedSet(PredefinedSet name) {
    createPredefinedSet(name, Sequence.<Id>empty());
  }

  private void deletePredefinedSet(String name) {
    predefinedSetNames.remove(name);
    deleteSet(name);
  }

  private void createPredefinedSetFromInfos(PredefinedSet name, Iterable<Info> values) {
    createPredefinedSet(name, Info.idsOfInfos(values));
  }

  private void createPredefinedSet(PredefinedSet set, final Iterable<Id> values) {
    createPredefinedSet(set.toString(), values);
  }
  
  private void createPredefinedSet(String name, final Iterable<Id> values) {
    predefinedSetNames.add(name);
    sets.put(name, Suppliers.memoize(
        new Supplier<HashSet<Id>>() {

          @Override public HashSet<Id> get() {
            HashSet<Id> result = new HashSet<Id>();
            Sequence.addAll(result, values);
            return result;
          }
        }));
  }

  public Iterable<String> getSetNames() {
    return sets.keySet();
  }

  public void addSymbolsFile(String fileName, HashSet<Id> values) {
    createPredefinedSet(fileName, values);
    symbolFiles.add(fileName);
    this.predefinedSetsStale = true;
  }

  public void removeSymbolsFile(String fileName) {
    deletePredefinedSet(fileName);
    symbolFiles.remove(fileName);
  }

  public Iterable<Id> getSet(Id id) {
    return id.idAndDescendants();
  }

  private void setInfos(InfoTable infos) {
    this.infos = infos;
    this.predefinedSetsStale = true;
  }

  public InfoTable getInfos() {
    return infos;
  }

  private boolean canExtendExternally(PackageInfo info) {
    if (info.getParent() != null && !canExtendExternally(info.getParent())) {
      return false;
    }
    return !getUnextensiblePackages().contains(info.id);
  }

  public boolean hasExternalVisibility(Info info) {
    if (info.getType() == IdType.Package) {
      return true;
    }

    if (info.isPrivate()) {
      return false;
    }

    if (!hasExternalVisibility(info.getParent())) {
      return false;
    }

    if (info.isPublic()) {
      return true;
    }

    if (info.isPackagePrivate()) {
      return canExtendExternally(info.getContainingPackage());
    }

    // isProtected()
    return canExtendExternally(info.getContainingPackage()) || !info.getContainingType().isFinal();
  }

}
