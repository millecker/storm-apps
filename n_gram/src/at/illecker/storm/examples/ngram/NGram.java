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
package at.illecker.storm.examples.ngram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * N-Gram Java implementations
 * 
 */
public class NGram {

  public static void main(String[] args) {

    List<String> tweets = new ArrayList<String>();
    tweets.add("Colorless green ideas sleep furiously");

    for (String tweet : tweets) {
      // Cleanup Tweet (remove @user #hashtag RT)
      // \\n - newlines
      // @\\w* - @users
      // #\\w* - hashtags
      // \\bRT\\b - Retweets RT
      // \\p{Punct} - removes smileys
      // [^@#\\p{L}\\p{N} ]+
      // URL
      // (https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?
      // -
      Pattern pattern = Pattern
          .compile("\\n|@\\w*|#\\w*|\\bRT\\b|"
              + "(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?");
      String cleanupTweet = pattern.matcher(tweet).replaceAll(" ");

      // Tweet to lower case
      cleanupTweet = cleanupTweet.toLowerCase();
      System.out.println("Tweet: " + cleanupTweet);

      String[] words = cleanupTweet.split(" ");

      // Bigrams
      List<String[]> bigrams = getBigrams(words);
      for (int i = 0; i < bigrams.size(); i++) {
        System.out.println("bigram[" + i + "]: "
            + Arrays.toString(bigrams.get(i)));
      }

      // Trigrams
      List<String[]> trigrams = getTrigrams(words);
      for (int i = 0; i < trigrams.size(); i++) {
        System.out.println("trigrams[" + i + "]: "
            + Arrays.toString(trigrams.get(i)));
      }

    }
  }

  public static List<String[]> getBigrams(String[] words) {
    List<String[]> bigrams = new ArrayList<String[]>();
    for (int i = 0; i < words.length; i++) {
      if (i < words.length - 1) {
        bigrams.add(new String[] { words[i], words[i + 1] });
      }
    }
    return bigrams;
  }

  public static List<String[]> getTrigrams(String[] words) {
    List<String[]> trigrams = new ArrayList<String[]>();
    for (int i = 0; i < words.length; i++) {
      if (i < words.length - 2) {
        trigrams.add(new String[] { words[i], words[i + 1], words[i + 2] });
      }
    }
    return trigrams;
  }

}
