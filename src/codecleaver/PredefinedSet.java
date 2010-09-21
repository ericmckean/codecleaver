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

import java.util.ArrayList;
import java.util.Collection;

/**
 * The list of predefined sets in CodeCleaver. Each set just has a name and help documentation.
 * The logic for creating the set is built into the State type.
 */
public class PredefinedSet implements HelpTopic {

  private static final ArrayList<PredefinedSet> sets = new ArrayList<PredefinedSet>();
  
  private final String name;
  private final String[] help;

  // The names of all predefined sets
  // TODO(peterhal): Super, Bridge, Enum? predefined sets
  public static final PredefinedSet ALL 
    = new PredefinedSet("All",
        "Every Symbol known to the system.");
  public static final PredefinedSet EMPTY 
    = new PredefinedSet("Empty",
        "The empty set.");
  public static final PredefinedSet PACKAGES 
    = new PredefinedSet("Packages",
        "All package symbols currently loaded.");
  public static final PredefinedSet TYPES 
    = new PredefinedSet("Types",
        "All type symbols currently loaded.");
  public static final PredefinedSet METHODS 
    = new PredefinedSet("Methods",
        "All method symbols currently loaded.");
  public static final PredefinedSet FIELDS 
    = new PredefinedSet("Fields",
        "All field symbols currently loaded.");
  public static final PredefinedSet DEFINITIONS 
    = new PredefinedSet("Definitions",
        "Symbols which have been added to the dependency graph.");
  public static final PredefinedSet NATIVE_METHODS 
    = new PredefinedSet("NativeMethods",
        "All methods which are implemented by native (C++) code in the dependency graph.");
  public static final PredefinedSet ABSTRACT_METHODS 
    = new PredefinedSet("AbstractMethods",
        "All methods which are abstract. Includes both abstract methods in classes as well as "
          + "abstract methods in interfaces.",
        "Note: Only methods loaded using open are included in AbstractMethods set. Symbols loaded "
          + "with openSymbols are never added to NativeMethods.");
  public static final PredefinedSet INTERFACES 
    = new PredefinedSet("Interfaces",
        "All interface types.");
  public static final PredefinedSet CLASSES 
    = new PredefinedSet("Classes",
        "All types which are not interfaces.", 
        "Note: class/interface is only set correctly for symbols loaded using open. All types "
          + "loaded with openSymbols are considered as classes.");
  public static final PredefinedSet PUBLICS 
    = new PredefinedSet("Publics",
        "All types, fields, and methods which are public.");
  public static final PredefinedSet PRIVATES 
    = new PredefinedSet("Privates",
        "All types, fields, and methods which are private.");
  public static final PredefinedSet PROTECTEDS 
    = new PredefinedSet("Protecteds",
        "All types, fields, and methods which are protected.");
  public static final PredefinedSet PACKAGE_PRIVATES 
    = new PredefinedSet("PackagePrivates",
        "All types, fields, and methods which are package private.");
  public static final PredefinedSet STATICS 
    = new PredefinedSet("Statics",
        "All static fields and methods.");
  public static final PredefinedSet FINALS 
    = new PredefinedSet("Finals",
        "All types, fields and methods which are final.");
  public static final PredefinedSet SYNCHRONIZED_METHODS 
    = new PredefinedSet("SynchronizedMethods",
        "Methods which are synchronized.");
  public static final PredefinedSet VOLATILE_FIELDS 
    = new PredefinedSet("VolatileFields",
        "Fields which are read and written by the VM in exactly the order described in source. "
          + "Prevents VM optimizations which may be unsafe during multi-threaded access.");
  public static final PredefinedSet VARARGS_METHODS 
    = new PredefinedSet("VarArgsMethods",
        "Methods whose last argument is a varargs array and may be called with a variable number "
          + "of arguments.");
  public static final PredefinedSet TRANSIENT_FIELDS 
    = new PredefinedSet("TransientFields",
        "Fields that are not part of the persistent state of the object.");
  public static final PredefinedSet ABSTRACT_CLASSES 
    = new PredefinedSet("AbstractClasses",
        "Abstract classes. Does not include interfaces.");
  public static final PredefinedSet STRICTS 
    = new PredefinedSet("Stricts",
        "All classes and methods which perform strict precision floating point operations. By "
          + "default floating point operations and intermediate values may have more precision "
          + "than the minimum required.");
  public static final PredefinedSet SYNTHETICS 
    = new PredefinedSet("Synthetics",
        "Types, fields and methods which are synthetic. Synthetic symbols are symbols which are "
          + "not found in source. Typically means they are compiler generated for anonymous inner "
          + "classes for example.");
  public static final PredefinedSet ANNOTATIONS 
    = new PredefinedSet("Annotations",
        "Types which are annotations.");
  public static final PredefinedSet EXTERNALS 
    = new PredefinedSet("Externals",
        "All packages, types, fields, and methods which are visible outside their containing jar. "
          + "Package private members in the packages in the user defined set UnextensiblePackages "
          + "will be considered non-External.");
  public static final PredefinedSet OVERRIDES 
    = new PredefinedSet("Overrides",
        "All methods which override a method from a base class or implement an interface method.");
  public static final PredefinedSet UNEXTENSIBLE_PACKAGES 
    = new PredefinedSet("UnextensiblePackages",
        "A mutable set used to determine which package privates are visible externally.");
  public static final PredefinedSet INITIALIZERS 
    = new PredefinedSet("Initializers",
        "All methods which are instance initializers. Aka constructors.");
  public static final PredefinedSet STATIC_INITIALIZERS 
    = new PredefinedSet("StaticInitializers",
        "All methods which are static initializers.");

  public static Collection<PredefinedSet> getAll() {
    return sets;
  }
  
  private PredefinedSet(String name, String... help) {
    this.name = name;
    this.help = help;
    
    sets.add(this);
  }

  @Override public String getTopic() {
    return name;
  }
  
  @Override public String getArguments() {
    return "";
  }

  @Override public String[] getHelp() {
    return help;
  }
  
  @Override public String toString() {
    return name;
  }
}
