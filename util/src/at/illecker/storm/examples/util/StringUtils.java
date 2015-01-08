/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.illecker.storm.examples.util;

public class StringUtils {

  public static boolean isURL(String value) {
    return ((value.indexOf(".com") > -1) || (value.indexOf("http:") == 0) || (value
        .indexOf("www.") == 0));
  }

  public static boolean isPunctuation(char c) {
    return c == ',' || c == '.' || c == '!' || c == '?' || c == ':' || c == ';';
  }

  public static boolean isSpecialChar(char c) {
    return c == '-' || c == '_' || c == '&' || c == '|' || c == '/'
        || c == '\\' || c == '"' || c == '\'' || c == '`' || c == '´';
  }

  public static String trimPunctuation(String value) {
    int valueLen = value.length();

    // count starting punctuations
    int startingPunctuations = 0;
    for (int i = 0; i < valueLen; i++) {
      if (isPunctuation(value.charAt(i)) || isSpecialChar((value.charAt(i)))) {
        startingPunctuations++;
      } else {
        break;
      }
    }

    // count ending punctuations
    int endingPunctuations = 0;
    for (int i = valueLen - 1; i >= 0; i--) {
      if (isPunctuation(value.charAt(i)) || isSpecialChar((value.charAt(i)))) {
        endingPunctuations++;
      } else {
        break;
      }
    }

    // case 1) no characters were punctuation
    if ((startingPunctuations == 0) && (endingPunctuations == 0)) {
      return value;
    }

    // case 2) all characters were punctuation
    if ((startingPunctuations == valueLen) && (endingPunctuations == valueLen)) {
      return "";
    }

    // case 3) substring
    return value.substring(startingPunctuations, valueLen - endingPunctuations
        - startingPunctuations);
  }

}
