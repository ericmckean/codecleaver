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
 * The result of scanning(aka lexing) a CodeCleaver command. Token's include a type and the 
 * start/end index into the command string. Tokens are Immutable.
 */
public class Token {
  protected Token(TokenType type, int startIndex, int endIndex) {
    this.type = type;
    this.startIndex = startIndex;
    this.endIndex = endIndex;
  }

  public final TokenType type;
  public final int startIndex;
  public final int endIndex;
}
