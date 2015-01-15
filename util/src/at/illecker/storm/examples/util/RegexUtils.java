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

import java.util.regex.Pattern;

public class RegexUtils {

  /**
   * Good characters for Internationalized Resource Identifiers (IRI). This
   * comprises most common used Unicode characters allowed in IRI as detailed in
   * RFC 3987. Specifically, those two byte Unicode characters are not included.
   */
  private static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

  /**
   * RFC 1035 Section 2.3.4 limits the labels to a maximum 63 octets.
   */
  private static final String IRI = "[" + GOOD_IRI_CHAR + "]([" + GOOD_IRI_CHAR
      + "\\-]{0,61}[" + GOOD_IRI_CHAR + "]){0,1}";

  private static final String GOOD_GTLD_CHAR = "a-zA-Z\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";
  private static final String GTLD = "[" + GOOD_GTLD_CHAR + "]{2,63}";
  private static final String HOST_NAME = "(" + IRI + "\\.)+" + GTLD;

  public static final Pattern IP_ADDRESS = Pattern
      .compile("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
          + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
          + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
          + "|[1-9][0-9]|[0-9]))");

  public static final Pattern DOMAIN_NAME = Pattern.compile("(" + HOST_NAME
      + "|" + IP_ADDRESS + ")");

  /**
   * Regular expression pattern to match most part of RFC 3987 Internationalized
   * URLs, aka IRIs. Commonly used Unicode characters are added.
   */
  // "(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?"
  public static final Pattern WEB_URL = Pattern
      .compile("((?:(http|https|Http|Https|ftp|Ftp|rtsp|Rtsp):\\/\\/(?:(?:[a-zA-Z0-9\\$\\-\\_\\.\\+\\!\\*\\'\\(\\)"
          + "\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,64}(?:\\:(?:[a-zA-Z0-9\\$\\-\\_"
          + "\\.\\+\\!\\*\\'\\(\\)\\,\\;\\?\\&\\=]|(?:\\%[a-fA-F0-9]{2})){1,25})?\\@)?)?"
          + "(?:"
          + DOMAIN_NAME
          + ")"
          + "(?:\\:\\d{1,5})?)" // plus option port number
          + "(\\/(?:(?:["
          + GOOD_IRI_CHAR
          + "\\;\\/\\?\\:\\@\\&\\=\\#\\~" // plus option query params
          + "\\-\\.\\+\\!\\*\\'\\(\\)\\,\\_])|(?:\\%[a-fA-F0-9]{2}))*)?"
          + "(?:\\b|$)"); // and finally, a word boundary or end of
                          // input. This is to stop foo.sure from
                          // matching as foo.su

  public static final Pattern EMAIL_ADDRESS = Pattern
      .compile("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@"
          + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
          + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+");

  public static final Pattern USER = Pattern.compile("\\p{Blank}*" + "[@]+"
      + "([A-Za-z]+[A-Za-z0-9_]+)");

  public static final Pattern HASHTAG = Pattern.compile("\\p{Blank}*" + "[#]+"
      + "([A-Za-z]+[A-Za-z0-9_]+)");

  public static final Pattern RETWEET = Pattern.compile("(RT|retweet|from|via)"
      + "((?:\\b\\W*@\\w+)*)", Pattern.CASE_INSENSITIVE);

  // "(.)\\1{1,}" means any character (added to group 1)
  // followed by itself at least one times, this means two equal chars
  public static final Pattern TWO_OR_MORE_REPEATING_CHARS = Pattern
      .compile("(.+)\\1{1,}");

  // "(.)\\1{2,}" means any character (added to group 1)
  // followed by itself at least two times, this means three equal chars
  public static final Pattern THREE_OR_MORE_REPEATING_CHARS = Pattern
      .compile("(.)\\1{2,}");

  // one or more punctuations
  public static final Pattern ONE_OR_MORE_PUNCTUATIONS = Pattern
      .compile("^\\p{Punct}+$");

  // starts with an alphabetic character
  public static final Pattern STARTS_WITH_ALPHABETIC_CHAR = Pattern
      .compile("^[a-zA-Z].*$");

  // isNumeric
  public static final Pattern IS_NUMERIC = Pattern.compile("^[+-]?" + "\\d+"
      + "(\\,\\d+)?" + "(\\.\\d+)?$");
}
