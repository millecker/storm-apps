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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexUtils {
  private static final String SPACE_EXCEPTIONS = "\\n\\r";
  public static final String SPACE_CHAR_CLASS = "\\p{C}\\p{Z}&&[^"
      + SPACE_EXCEPTIONS + "\\p{Cs}]";
  public static final String SPACE_REGEX = "[" + SPACE_CHAR_CLASS + "]";

  public static final String PUNCTUATION_CHAR_CLASS = "\\p{P}\\p{M}\\p{S}"
      + SPACE_EXCEPTIONS;
  public static final String PUNCTUATION_REGEX = "[" + PUNCTUATION_CHAR_CLASS
      + "]";

  // URL Regex
  public static final String URL = "(?:" + "(?i)(https?|ftp)://" + "(-\\.)?"
      + "([^\\s/?\\.#-]+\\.?)*(/[^\\s\\.]*)?" + ")";
  public static final Pattern URL_PATTERN = Pattern.compile(URL);
  public static final Matcher URL_MATCHER = URL_PATTERN.matcher("");

  // Email Regex
  public static final String EMAIL_ADDRESS = "(?:"
      + "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" + "\\@"
      + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" + "(" + "\\."
      + "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" + ")+" + ")";
  public static final Pattern EMAIL_ADDRESS_PATTERN = Pattern
      .compile(EMAIL_ADDRESS);
  public static final Matcher EMAIL_ADDRESS_MATCHER = EMAIL_ADDRESS_PATTERN
      .matcher("");

  // Phone Regex
  public static final String PHONE = "(?:" + "(?:\\+?[01][\\-\\s.]*)?"
      + "(?:[\\(]?\\d{3}[\\-\\s.\\)]*)?" + "\\d{3}" + "[\\-\\s.]*" + "\\d{4}"
      + ")";
  public static final Pattern PHONE_PATTERN = Pattern.compile(PHONE);
  public static final Matcher PHONE_MATCHER = PHONE_PATTERN.matcher("");

  // Username Regex
  public static final String USER_NAME = "(?:" + "\\@+([A-Za-z]+[A-Za-z0-9_]+)"
      + ")";
  public static final Pattern USER_NAME_PATTERN = Pattern.compile(USER_NAME);
  public static final Matcher USER_NAME_MATCHER = USER_NAME_PATTERN.matcher("");

  // Hashtag Regex
  public static final String HASH_TAG = "(?:"
      + "\\#+([A-Za-z]+[A-Za-z0-9_\\'\\-]*[A-Za-z0-9_]+)" + ")";
  public static final Pattern HASH_TAG_PATTERN = Pattern.compile(HASH_TAG);
  public static final Matcher HASH_TAG_MATCHER = HASH_TAG_PATTERN.matcher("");

  // Retweet Regex
  public static final String RETWEET = "(?:" + "(?i)(RT|retweet|from|via)"
      + "((?:\\b\\W*\\@\\w+)*)" + ")";
  public static final Pattern RETWEET_PATTERN = Pattern.compile(RETWEET);
  public static final Matcher RETWEET_MATCHER = RETWEET_PATTERN.matcher("");

  // Matches a HTML tag
  public static final String HTML_TAG = "<[^>]+>";
  public static final Pattern HTML_TAG_PATTERN = Pattern.compile(HTML_TAG);
  public static final Matcher HTML_TAG_MATCHER = HTML_TAG_PATTERN.matcher("");

  // Contains HTML symbols
  public static final String CONTAINS_HTML_SYMBOLS = "(?:" + "&#[0-9]{2,4};)"
      + "|" + "(?:&[a-zA-Z0-9]{2,6};" + ")";
  public static final Pattern CONTAINS_HTML_SYMBOLS_PATTERN = Pattern
      .compile(CONTAINS_HTML_SYMBOLS);
  public static final Matcher CONTAINS_HTML_SYMBOLS_MATCHER = CONTAINS_HTML_SYMBOLS_PATTERN
      .matcher("");

  // Contains Unicode symbols
  public static final String CONTAINS_UNICODE_SYMBOLS = "(?:"
      + "[^\\u0000-\\u007F]+" + ")" + "|" + "(?:" + "\\\\u[0-9a-fA-F]{4,5}+"
      + ")";
  public static final Pattern CONTAINS_UNICODE_SYMBOLS_PATTERN = Pattern
      .compile(CONTAINS_UNICODE_SYMBOLS);
  public static final Matcher CONTAINS_UNICODE_SYMBOLS_MATCHER = CONTAINS_UNICODE_SYMBOLS_PATTERN
      .matcher("");

  // Two or more repeating chars
  // "(.)\\1{1,}" means any character (added to group 1)
  // followed by itself at least one times, this means two equal chars
  public static final String TWO_OR_MORE_REPEATING_CHARS = "(.)\\1{1,}";
  public static final Pattern TWO_OR_MORE_REPEATING_CHARS_PATTERN = Pattern
      .compile(TWO_OR_MORE_REPEATING_CHARS);
  public static final Matcher TWO_OR_MORE_REPEATING_CHARS_MATCHER = TWO_OR_MORE_REPEATING_CHARS_PATTERN
      .matcher("");

  // Three or more repeating chars
  // "(.)\\1{2,}" means any character (added to group 1)
  // followed by itself at least two times, this means three equal chars
  public static final String THREE_OR_MORE_REPEATING_CHARS = "(.)\\1{2,}";
  public static final Pattern THREE_OR_MORE_REPEATING_CHARS_PATTERN = Pattern
      .compile(THREE_OR_MORE_REPEATING_CHARS);
  public static final Matcher THREE_OR_MORE_REPEATING_CHARS_MATCHER = THREE_OR_MORE_REPEATING_CHARS_PATTERN
      .matcher("");

  // Punctuation Regex
  public static final String PUNCTUATIONS = "^[\\p{Punct}\\s]+$";
  public static final Pattern PUNCTUATIONS_PATTERN = Pattern
      .compile(PUNCTUATIONS);
  public static final Matcher PUNCTUATIONS_MATCHER = PUNCTUATIONS_PATTERN
      .matcher("");

  // Underscore Regex
  public static final String UNDERSCORES = "^_+$";
  public static final Pattern UNDERSCORES_PATTERN = Pattern
      .compile(UNDERSCORES);
  public static final Matcher UNDERSCORES_MATCHER = UNDERSCORES_PATTERN
      .matcher("");

  // Starts with an alphabetic character
  public static final String STARTS_WITH_ALPHABETIC_CHAR = "^[a-zA-Z].*$";
  public static final Pattern STARTS_WITH_ALPHABETIC_CHAR_PATTERN = Pattern
      .compile(STARTS_WITH_ALPHABETIC_CHAR);
  public static final Matcher STARTS_WITH_ALPHABETIC_CHAR_MATCHER = STARTS_WITH_ALPHABETIC_CHAR_PATTERN
      .matcher("");

  // Number Regex
  public static final String NUMBER = "(?:" + "[+\\-]?" + "\\d+" + "(\\,\\d+)?"
      + "(\\.\\d+)?" + ")";
  public static final Pattern NUMBER_PATTERN = Pattern.compile(NUMBER);
  public static final Matcher NUMBER_MATCHER = NUMBER_PATTERN.matcher("");

  // Special number Regex
  public static final String SPECIAL_NUMBER = "(?:" + "\\$?" + "[+\\-]?"
      + "\\d+" + "([\\.|\\,|\\:|\\-]\\d+)*" + "(?:" + "(?i)(%|fm|am|pm|p|lb)"
      + "|" + "(c|p|st|nd|rd|th)" + ")?" + ")";
  public static final Pattern SPECIAL_NUMBER_PATTERN = Pattern
      .compile(SPECIAL_NUMBER);
  public static final Matcher SPECIAL_NUMBER_MATCHER = SPECIAL_NUMBER_PATTERN
      .matcher("");

  // Separated number Regex
  public static final String SEPARATED_NUMBER = "(?:" + "\\d+" + "(?:"
      + "(?i)(am|pm)" + "|" + "(c|st|nd|rd|th)" + ")?" + "[\\/|\\,|\\-]+"
      + "\\d+" + "([\\/|\\,|\\-]\\d+)*" + "(?:" + "(?i)(%|fm|am|pm|lb)" + "|"
      + "(c|p|st|nd|rd|th)" + ")?" + ")";
  public static final Pattern SEPARATED_NUMBER_PATTERN = Pattern
      .compile(SEPARATED_NUMBER);
  public static final Matcher SEPARATED_NUMBER_MATCHER = SEPARATED_NUMBER_PATTERN
      .matcher("");

  // Punctuation between words
  public static final String PUNCTUATION_BETWEEN_WORDS = "^(.*[^\\.|\\,|\\!|\\/|\\-])"
      + "[\\.|\\,|\\!|\\/|\\-]+" + "([^\\.|\\,|\\!|\\/|\\-].*)$";
  public static final Pattern PUNCTUATION_BETWEEN_WORDS_PATTERN = Pattern
      .compile(PUNCTUATION_BETWEEN_WORDS);
  public static final Matcher PUNCTUATION_BETWEEN_WORDS_MATCHER = PUNCTUATION_BETWEEN_WORDS_PATTERN
      .matcher("");

  // Alternating letter dot pattern e.g., L.O.V.E
  public static final String ALTERNATING_LETTER_DOT = "[a-zA-Z]\\.(?:[a-zA-Z](\\.)?)+";
  public static final Pattern ALTERNATING_LETTER_DOT_PATTERN = Pattern
      .compile(ALTERNATING_LETTER_DOT);
  public static final Matcher ALTERNATING_LETTER_DOT_MATCHER = ALTERNATING_LETTER_DOT_PATTERN
      .matcher("");

  // Words with apostrophes or dashes
  public static final String WORDS_WITH_APOSTROPHES_DASHES = "(?:"
      + "[a-zA-Z][a-zA-Z\\'\\-\\_]+[a-zA-Z]" + ")";

  // Words without apostrophes or dashes
  public static final String WORDS_WITHOUT_APOSTROPHES_DASHES = "(?:"
      + "[\\w_]+" + ")";

  // Ellipsis dots, sequences of two or more periods
  public static final String ELLIPSIS_DOTS = "(?:" + "\\.(?:\\s*\\.){1,}" + ")";

  // Non-whitespace char
  public static final String NOT_A_WHITESPACE = "(?:" + "\\S" + ")";

  // Emoticon Regex
  // TODO word:-) is not valid
  private static final String EMOTICON_EYES = "[:;=8xX*<>=^|#%]"; // eyes
  private static final String EMOTICON_NOSE = "[']?" + "[-_co^./]?" + "[\\\\]?"; // nose
  private static final String EMOTICON_MOUTH = "[0]?" // mouth
      + "([\\(\\)\\[\\]\\/\\\\}{*.^<>=@|,bdDpPLScoO$X#J3&]" + ")\\1{0,}";
  public static final String EMOTICON = "(?:" + "[<>oO0}3|]?" + EMOTICON_EYES
      + EMOTICON_NOSE + EMOTICON_MOUTH + "|" /* reverse */
      + EMOTICON_MOUTH + EMOTICON_EYES + "[<>]?" + ")";
  private static final String EMOTICON_DELIMITER = SPACE_REGEX + "|"
      + PUNCTUATION_REGEX;
  public static final Pattern EMOTICON_PATTERN = Pattern.compile("(?<=^|"
      + EMOTICON_DELIMITER + ")" + EMOTICON + "(?=$|" + EMOTICON_DELIMITER
      + ")");
  public static final Matcher EMOTICON_MATCHER = EMOTICON_PATTERN.matcher("");

  // Slang pattern to match w/
  public static final String SLANG = "[a-zA-Z]\\/[a-zA-z]*" + "|" + "\\\\m\\/";
  public static final Pattern SLANG_PATTERN = Pattern.compile("(?<=^|"
      + EMOTICON_DELIMITER + ")" + SLANG + "(?=$|" + EMOTICON_DELIMITER + ")");
  public static final Matcher SLANG_MATCHER = SLANG_PATTERN.matcher("");

  // Attention the order does matter
  public static final Pattern TOKENIZER_PATTERN = Pattern
      .compile(EMOTICON_PATTERN.pattern() + "|" + URL + "|" + PHONE + "|"
          + EMAIL_ADDRESS + "|" + USER_NAME + "|" + HASH_TAG + "|"
          + SLANG_PATTERN.pattern() + "|" + ALTERNATING_LETTER_DOT + "|"
          + WORDS_WITH_APOSTROPHES_DASHES + "|" + SEPARATED_NUMBER + "|"
          + SPECIAL_NUMBER + "|" + WORDS_WITHOUT_APOSTROPHES_DASHES + "|"
          + ELLIPSIS_DOTS + "|" + NOT_A_WHITESPACE);
  public static final Matcher TOKENIZER_MATCHER = TOKENIZER_PATTERN.matcher("");

}
