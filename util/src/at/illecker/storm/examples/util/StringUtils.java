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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtils {
  private static final Logger LOG = LoggerFactory.getLogger(StringUtils.class);

  public static boolean isURL(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.URL_PATTERN.matcher(str).matches();
  }

  public static boolean isEmoticon(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.EMOTICON_PATTERN.matcher(str).matches();
  }

  public static boolean isEmail(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.EMAIL_ADDRESS_PATTERN.matcher(str).matches();
  }

  public static boolean isHashTag(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.HASH_TAG_PATTERN.matcher(str).matches();
  }

  public static boolean isUser(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.USER_NAME_PATTERN.matcher(str).matches();
  }

  public static boolean isRetweet(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.RETWEET_PATTERN.matcher(str).matches();
  }

  public static boolean consitsOfPunctuations(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.PUNCTUATIONS_PATTERN.matcher(str).matches();
  }

  public static boolean isNumeric(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.NUMBER_PATTERN.matcher(str).matches();
  }

  public static boolean isPunctuation(char c) {
    return c == ',' || c == '.' || c == '!' || c == '?' || c == ':' || c == ';';
  }

  public static boolean isSpecialChar(char c) {
    return c == '-' || c == '_' || c == '&' || c == '|' || c == '/'
        || c == '\\' || c == '"' || c == '\'' || c == '`' || c == '´'
        || c == '<' || c == '>' || c == '~' || c == '*' || c == '+' || c == '('
        || c == ')' || c == '[' || c == ']';
  }

  public static boolean startsWithAlphabeticChar(String str) {
    if (str == null) {
      return false;
    }
    return RegexUtils.STARTS_WITH_ALPHABETIC_CHAR_PATTERN.matcher(str)
        .matches();
  }

  public static String trimPunctuation(String str) {
    int valueLen = str.length();
    // count starting punctuations
    int startingPunctuations = 0;
    for (int i = 0; i < valueLen; i++) {
      if (isPunctuation(str.charAt(i)) || isSpecialChar((str.charAt(i)))) {
        startingPunctuations++;
      } else {
        break;
      }
    }

    // count ending punctuations
    int endingPunctuations = 0;
    for (int i = valueLen - 1; i >= 0; i--) {
      if (isPunctuation(str.charAt(i)) || isSpecialChar((str.charAt(i)))) {
        endingPunctuations++;
      } else {
        break;
      }
    }

    // case 1) no characters were punctuation
    if ((startingPunctuations == 0) && (endingPunctuations == 0)) {
      return str;
    }

    // case 2) all characters were punctuation
    if ((startingPunctuations == valueLen) && (endingPunctuations == valueLen)) {
      return "";
    }

    // case 3) substring
    int endIndex = valueLen - endingPunctuations;
    if (startingPunctuations >= endIndex) {
      LOG.error("trimPunctuation is not possible for '" + str + "' valueLen: "
          + valueLen + " startingPunc: " + startingPunctuations
          + " endingPunc: " + endingPunctuations + " endIndex: " + endIndex);
      return str;
    } else {
      LOG.info("trimPunctuation from '" + str + "' to '"
          + str.substring(startingPunctuations, endIndex) + "'");
      return str.substring(startingPunctuations, endIndex);
    }
  }

  public static void main(String[] args) {
    // test URLs
    String[] testURLs = new String[] { "www.google.com",
        "http://www.google.com", "https://www.google.com",
        "ftp://www.google.com", "google.com" };
    for (String s : testURLs) {
      System.out.println("isURL(" + s + "): " + isURL(s));
    }

    // test userNames
    String[] userNames = new String[] { "@hugo", "@@hugo", " @hugu2", "@hugo_",
        "2@hugo_", "a@a", "@h0_", "@0abc", "@abc0" };
    for (String s : userNames) {
      System.out.println("isUser(" + s + "): " + isUser(s));
    }

    // test hashTags
    String[] hashTags = new String[] { "#hugo", "##hugo", " #hugu2", "#hugo_",
        "a#a", "#h0_", "#0abc", "#abc0" };
    for (String s : hashTags) {
      System.out.println("isHashTag(" + s + "): " + isHashTag(s));
    }

    // test retweets
    String[] retweets = new String[] { "RT", "retweet", "Retweet",
        "RT@username", "RT @ username" };
    for (String s : retweets) {
      System.out.println("isRetweet(" + s + "): " + isRetweet(s));
    }

    // test punctuations
    String[] testPunctuations = new String[] { ".a .", "..--", ".. --" };
    for (String s : testPunctuations) {
      System.out.println("consitsOfPunctuations(" + s + "): "
          + consitsOfPunctuations(s));
    }
    testPunctuations = new String[] { ".asdf.", "asdf.:--", "--asdf-!", ":-)",
        ">:-[", "\"All", "\"abc\"", "\"abc\".." };
    for (String s : testPunctuations) {
      System.out.println("trimPunctuation(" + s + "): " + trimPunctuation(s));
    }

    // test startsWithAlphabeticChar
    String[] testStartsWithAlphabeticChar = new String[] { "Hello", "hello",
        "-hello", "0hello", "@hello" };
    for (String s : testStartsWithAlphabeticChar) {
      System.out.println("startsWithAlphabeticChar(" + s + "): "
          + startsWithAlphabeticChar(s));
    }

    // test numerics
    String[] testNumerics = new String[] { "1", "1000", "1,000", "+1", "-1",
        "+1.0", "+1.", "-.5", "000000" };
    for (String s : testNumerics) {
      System.out.println("isNumeric(" + s + "): " + isNumeric(s));
    }

    // test emoticon
    String testEmoticons = ":-) :-)) :) :D :o) :] :3 :c) :> =] 8) =) :} :^)"
        + " :-D 8-D 8D x-D xD X-D XD =-D =D =-3 =3"
        + " >:[ :-( :( :-c :c :-< :< :-[ :[ :{" + " ;( :-|| :@ >:("
        + " :'-( :'( :'-) :')" + " D:< D: D8 D; D= DX"
        + " >:O :-O :O :-o :o 8-0 O_O o-o O_o o_O o_o O-O" + " :* :^*"
        + " ;-) ;) *-) *) ;-] ;] ;D ;^) :-,"
        + " >:P :-P :P X-P x-p xp XP :-p :p =p :-b :b d:"
        + " >:\\ >:/ :-/ :-. :/ :\\ =/ =\\ :L =L :S >.<" + " :| :-|" + " :$"
        + " :-X :X :-# :#" + " O:-) 0:-3 0:3 0:-) 0:) 0;^)" + " >:) >;) >:-)"
        + " }:-) }:) 3:-) 3:)" + " o/\\o" + " |;-) |-O" + " :-J" + " :-& :&"
        + " #-)" + " %-) %)" + " <:-|" + " <3" + " </3 :Oc";
    for (String s : testEmoticons.split(" ")) {
      boolean isEmoticon = isEmoticon(s);
      if (!isEmoticon) {
        System.out.println("isEmoticon( " + s + " ): " + isEmoticon(s));
      }
    }
  }
}
