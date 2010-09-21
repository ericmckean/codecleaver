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

/**
 * A conduit for reporting errors. Error reports include source location and a string suitable for
 * display to the user.
 */
public abstract class ErrorReporter {

  /**
   * Report an error.
   *
   * @param startIndex start location of the error
   * @param endIndex end location of the error
   * @param format string suitable for formatting arguments to generate a user error message
   * @param arguments
   */
  public void reportError(int startIndex, int endIndex, String format, Object... arguments) {
    hadError = true;
    reportMessage(startIndex, endIndex, "Error", format, arguments);
  }

  /**
   * Report a warning.
   *
   * @param startIndex start location of the warning
   * @param endIndex end location of the warning
   * @param format string suitable for formatting arguments to generate a user warning message
   * @param arguments
   */
  public void reportWarning(int startIndex, int endIndex, String format, Object... arguments) {
    reportMessage(startIndex, endIndex, "Warning", format, arguments);
  }

  /**
   * Report a message.
   * 
   * @param startIndex Character index of the start of the error.
   * @param endIndex Character index of the end of the error.
   * @param messageKind The kind of message - Error or Warning. Suitable for display to the user.
   * @param format String format of the message.
   * @param arguments Arguments for the message.
   */
  protected abstract void reportMessage(
      int startIndex, int endIndex, String messageKind, String format, Object... arguments);

  /**
   * Has reportError been called since the last time clearError was called.
   */
  public boolean hadError() {
    return hadError;
  }

  /**
   * Resets the result of hadError().
   */
  public void clearError() {
    hadError = false;
  }

  private boolean hadError;
}
