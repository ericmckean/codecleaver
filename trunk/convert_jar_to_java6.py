#!/usr/bin/python
#
# Copyright (C) 2010 Google Inc. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#
#     * Redistributions of source code must retain the above copyright
# notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
# copyright notice, this list of conditions and the following disclaimer
# in the documentation and/or other materials provided with the
# distribution.
#     * Neither the name of Google Inc. nor the names of its
# contributors may be used to endorse or promote products derived from
# this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
# "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
# LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
# A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
# OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
# SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
# LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
# THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
# OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

# This python script converts a Java 1.5 jar to Java 1.6 bytecode format.

import errno
import os
import shutil
import sys
import tempfile
from optparse import OptionParser

def quote(fileName):
  return "\"" + fileName + "\""

def quoteAbsPath(fileName):
  return quote(os.path.abspath(fileName))

def fileNameInScriptDir(baseFileName):
  return os.path.join(os.path.dirname(sys.argv[0]), baseFileName)

def convertJar(codecleaver, input, output, references):
  # write codecleaver commands to temp file
  (tempFileDescriptor, tempFileName) = tempfile.mkstemp()
  tempFile = os.fdopen(tempFileDescriptor, "w")
  try:
    # open target jar first, in case of duplicate definitions
    tempFile.write("openAssign InputJar " + quoteAbsPath(input) + "\n")
    for reference in references:
      tempFile.write("open " + quoteAbsPath(reference) + "\n")
    tempFile.write("cleave " + quoteAbsPath(input) + " " 
                          + quoteAbsPath(output) + " InputJar\n")
    tempFile.flush()
    tempFile.close()
    
    # run codecleaver
    command = [codecleaver, "<", tempFileName, ">", "/dev/null"]
    if os.system(" ".join(command)):
      sys.stderr.write('codecleaver failed\n')
      return False

  finally:
    os.remove(tempFileName)
  
  return True

def getJDKLibraryPath():
  javaHome = os.getenv("JAVA_HOME")
  if (javaHome != None):
    return os.path.join(javaHome, "jre/lib/rt.jar")
  macJDKLibraryPath = '/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Classes/classes.jar'
  if (os.path.exists(macJDKLibraryPath)):
    return macJDKLibraryPath

  return None


def main(args):
  try:

    # Parse input.
    parser = OptionParser()
    parser.add_option("-c", "--codecleaver", dest="codecleaver", 
                      action="store", type="string", 
                      default=fileNameInScriptDir("codecleaver.sh"), 
                      help="codecleaver command")
    parser.add_option("-o", "--output", dest="output", action="store", 
                      type="string", help="Name of output jar file")
    parser.add_option("-i", "--input", dest="input", action="store", 
                      type="string", help="Name of input jar file")
    parser.add_option("-r", "--reference", dest="references", default=[],
                      action="append", 
                      help="Add a jar referenced by the input jar")
    parser.add_option("-n", "--no-stdlib", dest="include_stdlib", 
                      action="store_false", default=True, 
                      help="do not reference JDK library")

    (options, args) = parser.parse_args()

    # validate input
    if not options.input:
      if len(args) < 1:
        sys.stderr.write('input jar file not specified\n')
        return -1
      options.input = args.pop(0)
    if len(args) != 0:
      sys.stderr.write('unexpected arguments: ' + " ".join(args));
      return -1
    if not os.path.exists(options.codecleaver):
      sys.stderr.write('codecleaver command not found: ' + options.codecleaver 
                       + '\n')
      return -1
    if not os.path.exists(options.input):
      sys.stderr.write('input jar not found: ' + options.input + '\n')
      return -1

    if options.include_stdlib:
      javaJDKLib = getJDKLibraryPath()
      if (javaJDKLib == None):
        sys.stderr.write('JAVA_HOME environment variable not set, or no Java installed')
        return -1
      options.references.append(javaJDKLib)
    for reference in options.references:
      if not os.path.exists(reference):
        sys.stderr.write('referenced jar not found: ' + reference + '\n')
        return -1

    if not options.output:
      if options.input.endswith('.jar'):
        options.output = options.input[:len(options.input)-4] + '-java6.jar'
      else:
        options.output = options.input + '-java6'
    outputDirectory = os.path.abspath(os.path.dirname(options.output))
    try:
      os.makedirs(outputDirectory)
    except OSError, exc: # Python >2.5
      if exc.errno != errno.EEXIST:
        sys.stderr.write('cannot create output directory : ' + outputDirectory)
        return -1

    if not convertJar(options.codecleaver, options.input, options.output, 
                      options.references):
      return -1

    return 0
  except Exception, inst:
    sys.stderr.write('convert_jar_to_java16.py exception\n')
    sys.stderr.write(str(inst))
    return -1

if __name__ == '__main__':
  sys.exit(main(sys.argv))
