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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * The list of all help topics in CodeCleaver. The list includes commands, predefined sets, as 
 * well as overview material.
 */
public final class HelpTopics {
  private static final String TOPICS = "topics";

  private HelpTopics() {}
  
  private static final ArrayList<HelpTopic> topics;
  private static final HashMap<String, HelpTopic> topicsByName;
  
  private static HelpTopic createTopic(final String topic, final String arguments, final String... help) {
    return new HelpTopic() {

      @Override public String getTopic() {
        return topic;
      }
  
      @Override public String getArguments() {
        return arguments;
      }
  
      @Override public String[] getHelp() {
        return help;
      }
    };
  }

  private static void addTopic(final String topic, final String arguments, Iterable<String> help) {
    addTopic(topic, arguments, Iterables.toArray(help, String.class));
  }
  
  private static void addTopic(final String topic, final String arguments, final String... help) {
    addTopic(createTopic(topic, arguments, help));
  }

  private static void addTopic(HelpTopic topic) {
    addTopicAt(topics.size(), topic);
  }

  private static void addTopicAt(int index, HelpTopic topic) {
    topics.add(index, topic);
    topicsByName.put(topic.getTopic(), topic);
  }

  private static void addAllTopics(HelpTopic[] topics) {
    for (HelpTopic topic : topics) {
      addTopic(topic);
    }
  }

  private static void addAllTopics(Iterable<? extends HelpTopic> topics) {
    for (HelpTopic topic : topics) {
      addTopic(topic);
    }
  }

  private static void addAlias(String from, String to) {
    topicsByName.put(from, topicsByName.get(to));
  }

  static {
    topics = new ArrayList<HelpTopic>();
    topicsByName = new HashMap<String, HelpTopic>();
    
    // overview
    addTopic("codecleaver", "",
        "Code Cleaver is a tool for understanding dependencies in large Java codebases. Code "
        + "Cleaver can help you understand how to split large Java jar files into smaller jars. "
        + "Code Cleaver can also help in porting a java library from one java platform to another "
        + "- from J2SE to J2ME for example.",
        "Code Cleaver operates on 2 levels. First, Code Cleaver manipulates sets of Java symbols. "
        + "Symbols include packages, types, methods, and fields. Second, Code Cleaver allows "
        + "querying of the dependency graph between symbols. An entry in the dependency graph is "
        + "made whenever a symbol requires another symbol to run. For example if method print() "
        + "calls toString() then the print() symbol depends on the toString() symbol. There are "
        + "many ways that symbols can depend on each other. Some of the dependencies include: "
        + "Class symbols depend on the symbols representing the class's base class and implemented "
        + "interfaces. Field symbols depend on the symbol representing the field's type. Method "
        + "symbols depend on all other methods called, fields referenced as well as the method's "
        + "return type, parameter types and throws types.");
    addAlias("overview", "codecleaver");
    addTopic("workflow", "",
        "Code Cleaver has a command line interface. Executing Code Cleaver yields a command "
        + "prompt. The command prompt is found in the Console Window when running Code Cleaver in "
        + "Eclipse.",

        "A typical work flow would include:",
        "\t1 - Loading some java jar files into Code Cleaver with the open command.",
        "\t2 - Building user defined sets of symbols using the create, assign, add and remove "
        + "commands.",
        "\t3 - Querying the dependency graph with the list command and set/graph query "
        + "expressions.",
        "\t4 - Modifying your user defined sets using the add, remove and move commands and query "
        + "expressions.",
        "\t5 - Iterating through steps 2, 3, and 4 until you have an answer to your dependency "
        + "question.",
        "\t6 - Saving your command history with the write command so that you can restore your "
        + "state with the read command.",
        "\t7 - Writing out set expressions with the writeList command.",
        "\t8 - Removing parts of a jar and converting it to Java 1.6 bytecode format using the "
        + "cleave command.");
    
    // commands
    addTopic("commands", "",
        "Each line of input is treated as a command.",
        "\tcommand:",
        "\t\t#  comment",
        "\t\tnamed-command",
        "\t\tset-expression",
        "\t\tassignment-statement",
        "A set-expression command is equivalent to the list set-expression named command.");
    // named commands
    addTopic("named-commands", "", Iterables.transform(Arrays.asList(CommandType.values()),
        new Function<CommandType, String>() {

          @Override public String apply(CommandType from) {
            return from.toString();
          }}));
    addAllTopics(CommandType.values());

    // predefined sets
    addTopic("predefined-sets", "", Iterables.transform(PredefinedSet.getAll(),
        new Function<PredefinedSet, String>() {

      @Override public String apply(PredefinedSet from) {
        return from.getTopic();
      }}));
    addAllTopics(PredefinedSet.getAll());
    
    // symbols
    addTopic("symbols", "",
        "Symbols include packages, types, methods and fields.", 
        "",
        "Packages and types use the '/' character as separator.", 
        "",
        "Example packages are 'java/lang', or 'com/google/common'.",
        "Example types are 'java/lang/Object' or 'java/io/File'.",
        "",
        "Member names use '.' as separator and have member signature appended.",
        "",
        "Example member names:",
        "java/lang/Object.<init>.()V",
        "java/lang/Object.clone.()Ljava/lang/Object;",
        "java/lang/Object.equals.(Ljava/lang/Object;)Z",
        "java/lang/Object.finalize.()V",
        "java/lang/Object.getClass.()Ljava/lang/Class;");
    
    // set expressions
    addTopic("set-expressions", "",
		"Sets of symbols are specified using set expressions.", 
        "Syntax:",
        "\tset-expression:",
        "\t    binary-expression");
    addTopic("binary-expression", "", 
        "Syntax:",
        "\tbinary-expression:",
        "\t    binary-expression + unary-expression",
        "\t    binary-expression - unary-expression",
        "\t    binary-expression ^ unary-expression",
        "\t    unary-expression",
        "",
        "Binary operators are:",
        "\t    +  union",
        "\t    -  difference",
        "\t    ^  intersection");
    addTopic("unary-expression", "",
        "Unary Expressions",
        "Syntax:",
        "\tunary-expression:",
        "\t    <  unary-expression",
        "\t    >  unary-expression",
        "\t    <*  unary-expression",
        "\t    >*  unary-expression",
        "\t    [  unary-expression",
        "\t    ]  unary-expression",
        "\t    [*  unary-expression",
        "\t    ]*  unary-expression",
        "\t    !  unary-expression",
        "\t    !*  unary-expression",
        "\t    primary-expression",
        "",
        "Unary operators are:",
        "",
        "\tDependency Graph Query Operators:",
        "\t    <  direct callees",
        "\t    >  direct callers",
        "\t    <*  transitive closure of direct callees",
        "\t    >*  transitive closure of direct callers",
        "\tInheritance Graph Query Operators:",
        "\t    [  direct overrides/inherits from",
        "\t    ]  directly overridden by/super class of",
        "\t    [*  transitive overrides/inherits from",
        "\t    ]*  transitive overridden by/super class of",
        "\tExpand Operators:",
        "\t    !  direct children of all elements of the set",
        "\t    !*  all descendants of all elements of the set, including the original set");
    addTopic("primary-expression", "",
        "Primary Expressions",
        "Syntax:",
        "\tprimary-expression:",
        "\t    (  expression  )",
        "\t    annotation-expression",
        "\t    predefined-set",
        "\t    symbol",
        "\t    jar-file-name",
        "Primary expressions are either a predefined set, a symbol or a jar file. Primary "
          + "expressions can be quoted in double quotes. There are no escapes within quoted "
          + "expressions.",
        "When types, packages and jar file are used in a set expression the resulting expression "
          + "includes all of their contained symbols: methods, fields and nested types.");
    addTopic("annotation-expression", "",
        "Annotation Expressions",
        "Syntax:",
        "\tannotation-expression:",
        "\t    @  type-symbol",
        "\t    @  type-symbol  (  )",
        "\t    @  type-symbol  (  element-value  )",
        "\t    @  type-symbol  (  element-name  =  element-value",
        "\t            [  ,  element-name   =  element-value ] *  )",
        "Annotation expressions match any symbol with a matching annotation. The first two forms "
          + "match any symbol with an annotation of the 'type-symbol' type regardless of "
          + "annotation elements. The 'element-value' form is shorthand for @type-symbol(value="
          + "element-value). The form with element name value pairs matches symbols with an "
          + "annotation of 'type-symbol' type, that have an element of name 'element-name' whose "
          + "value, when converted to a string, is equal to 'element-value'. If the type of the "
          + "element named by 'element-name' is an array type then the pair matches if the element "
          + "value contains 'element-value'.",
        "For Example:",
        "\t@java/lang/annotation/Target(METHOD)",
        "Matches any annotation type which can target a method.");
    addTopic("assignment-statement", "",
        "Assignment Statement",
        "Syntax:",
        "\tassignment-statement:",
        "\t    set-name  =  set-expression",
        "\t    set-name  +=  set-expression",
        "\t    set-name  -=  set-expression",
        "\t    set-name  ^=  set-expression",
        "The simple assignment statement:",
        "\tset-name  =  set-expression",
        "is equivalent to:",
        "\tassign set-name set-expression",
        "A compound assignment:",
        "\tset-name op= set-expression",
        "is equivalent to:",
        "\tassign set-name ( set-name op set-expression ).");
    
    addTopic(createTopic(TOPICS, "", Iterables.toArray(Iterables.transform(topics,
        new Function<HelpTopic, String>() {

      @Override public String apply(HelpTopic from) {
        return from.getTopic();
      }}), String.class)));
    
    addAlias("symbol", "symbols");
    
    addAlias("set-expression", "set-expressions");
    addAlias("set", "set-expressions");
    addAlias("sets", "set-expressions");
    addAlias("expression", "set-expressions");
    addAlias("expressions", "set-expressions");
    
    // binary operators
    for (TokenType type : TokenType.values()) {
      if (type.binaryOperator != null) {
        addAlias(type.value, "binary-expression");
      }
      if (type.unaryOperator != null) {
        addAlias(type.value, "unary-expression");
      }
      if (type.assignmentOperator != null) {
        addAlias(type.value, "assignment-expression");
      }
    }

    // annotation expressions
    addAlias("@", "annotation-expression");
  }
  
  public static HelpTopic getTopicsTopic() {
    return getTopic(TOPICS);
  }
  
  public static Iterable<HelpTopic> getTopics() {
    return topics;
  }
  
  public static boolean hasTopic(String topic) {
    return topicsByName.containsKey(topic);
  }
  
  public static HelpTopic getTopic(String topic) {
    return topicsByName.get(topic);
  }
}
