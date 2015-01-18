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
package at.illecker.storm.examples.util.preprocessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.RegexUtils;
import at.illecker.storm.examples.util.StringUtils;
import at.illecker.storm.examples.util.dictionaries.FirstNames;
import at.illecker.storm.examples.util.dictionaries.SlangCorrection;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordnet.WordNet;

public class Preprocessor {
  private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);
  private static final boolean LOGGING = true;
  private static final Preprocessor instance = new Preprocessor();

  private WordNet m_wordnet;
  private SlangCorrection m_slangCorrection;
  private FirstNames m_firstNames;

  private Preprocessor() {
    // Load WordNet
    m_wordnet = WordNet.getInstance();
    // Load Slang correction vocabulary
    m_slangCorrection = SlangCorrection.getInstance();
    // Load FirstNames
    m_firstNames = FirstNames.getInstance();
  }

  public static Preprocessor getInstance() {
    return instance;
  }

  public List<String> preprocess(List<String> tokens) {
    // run tail recursion
    return preprocessAccumulator(new LinkedList<String>(tokens),
        new ArrayList<String>());
  }

  // TODO return List<TaggedWord>
  // TODO create Matcher and use reset() method
  private List<String> preprocessAccumulator(LinkedList<String> tokens,
      List<String> processedTokens) {

    if (tokens.isEmpty()) {
      return processedTokens;
    } else {
      // remove token from queue
      String token = tokens.removeFirst();

      // identify token
      boolean tokenContainsPunctuation = StringUtils
          .consitsOfPunctuations(token);
      boolean tokenIsEmoticon = StringUtils.isEmoticon(token);
      boolean tokenIsURL = StringUtils.isURL(token);
      boolean tokenIsNumeric = StringUtils.isNumeric(token);

      // Step 1) Unify Emoticons remove repeating chars
      if ((tokenIsEmoticon) && (!tokenIsURL) && (!tokenIsNumeric)) {
        Matcher m = RegexUtils.TWO_OR_MORE_REPEATING_CHARS_PATTERN
            .matcher(token);
        if (m.find()) {
          boolean isSpecialEmoticon = m.group(1).equals("^");
          String reducedToken = m.replaceAll("$1");
          if (isSpecialEmoticon) { // keep ^^
            reducedToken += "^";
          }
          // else {
          // Preprocess token again if there are recursive patterns in it
          // e.g., :):):) -> :):) -> :) Not possible because of Tokenizer
          // tokens.add(0, reducedToken);
          // }
          if (LOGGING) {
            LOG.info("Unify Emoticon from '" + token + "' to '" + reducedToken
                + "'");
          }
          processedTokens.add(reducedToken);
          return preprocessAccumulator(tokens, processedTokens);
        }
      } else if (tokenContainsPunctuation) {
        // If token is no Emoticon then there is no further
        // preprocessing for punctuations
        processedTokens.add(token);
        return preprocessAccumulator(tokens, processedTokens);
      }

      // identify token further
      boolean tokenIsUser = StringUtils.isUser(token);
      boolean tokenIsHashTag = StringUtils.isHashTag(token);
      boolean tokenIsSlang = StringUtils.isSlang(token);
      boolean tokenIsEmail = StringUtils.isEmail(token);
      boolean tokenIsPhone = StringUtils.isPhone(token);
      boolean tokenIsSpecialNumeric = StringUtils.isSpecialNumeric(token);
      boolean tokenIsSeparatedNumeric = StringUtils.isSeparatedNumeric(token);

      // Step 2) Slang Correction
      // TODO prevent slang correction if all UPPERCASE
      // 'FC' to [fruit, cake]
      // 'Ajax' to [Asynchronous, Javascript, and, XML]
      // 'TL' to [dr too, long, didn't, read]
      // S.O.L - SOL - [s**t, outta, luck]
      // 'AC/DC' to 'AC' and 'DC' - 'DC' to [don't, care]
      // TODO update dictionary O/U O/A
      if ((!tokenIsEmoticon) && (!tokenIsURL) && (!tokenIsUser)
          && (!tokenIsHashTag) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            processedTokens.add(slangCorrection[i]);
          }
          if (LOGGING) {
            LOG.info("Slang Correction from '" + token + "' to "
                + Arrays.toString(slangCorrection));
          }
          return preprocessAccumulator(tokens, processedTokens);
        } else if (tokenIsSlang) {
          if (token.startsWith("w/")) {
            processedTokens.add("with");
            processedTokens.add(token.substring(2));
            if (LOGGING) {
              LOG.info("Slang Correction from '" + token + "' to " + "[with, "
                  + token.substring(2) + "]");
            }
            return preprocessAccumulator(tokens, processedTokens);
          } else {
            if (LOGGING) {
              LOG.info("Slang Correction might be missing for '" + token + "'");
            }
          }
        }
      }

      // Step 3) Check if there are punctuations between words
      // e.g., L.O.V.E
      if ((!tokenIsEmoticon) && (!tokenIsURL) && (!tokenIsUser)
          && (!tokenIsHashTag) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {
        // remove alternating letter dot pattern e.g., L.O.V.E
        Matcher m = RegexUtils.ALTERNATING_LETTER_DOT_PATTERN.matcher(token);
        if (m.matches()) {
          String newToken = token.replaceAll("\\.", "");
          if (m_wordnet.contains(newToken)) {
            if (LOGGING) {
              LOG.info("Remove punctuations in word from '" + token + "' to '"
                  + newToken + "'");
            }
            token = newToken;
          }
          processedTokens.add(token);
          return preprocessAccumulator(tokens, processedTokens);
        }
      }

      // Step 4) Add missing g in gerund forms e.g., goin
      if ((!tokenIsUser) && (!tokenIsHashTag) && (token.endsWith("in"))
          && (!m_firstNames.isFirstName(token))
          && (!m_wordnet.contains(token.toLowerCase()))) {
        // append "g" if a word ends with "in" and is not in the vocabulary
        if (LOGGING) {
          LOG.info("Add missing \"g\" from '" + token + "' to '" + token + "g'");
        }
        token = token + "g";
        processedTokens.add(token);
        return preprocessAccumulator(tokens, processedTokens);
      }

      // Step 5) Remove elongations of characters (suuuper)
      // 'lollll' to 'loll' because 'loll' is found in dict
      // TODO 'AHHHHH' to 'AH'
      if ((!tokenIsEmoticon) && (!tokenIsURL) && (!tokenIsUser)
          && (!tokenIsHashTag) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {

        // remove repeating chars
        token = removeRepeatingChars(token);

        // Step 5b) Try Slang Correction again
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            processedTokens.add(slangCorrection[i]);
          }
          if (LOGGING) {
            LOG.info("Slang Correction from '" + token + "' to "
                + Arrays.toString(slangCorrection));
          }
          return preprocessAccumulator(tokens, processedTokens);
        }
      }

      // add token to processed list
      processedTokens.add(token);
      return preprocessAccumulator(tokens, processedTokens);
    }
  }

  private String removeRepeatingChars(String value) {
    // if there are three repeating equal chars
    // then remove one char until the word is found in the vocabulary
    // else if the word is not found reduce the repeating chars to one

    // collect matches for sub-token search
    List<int[]> matches = new ArrayList<int[]>();

    Matcher matcher = RegexUtils.THREE_OR_MORE_REPEATING_CHARS_PATTERN
        .matcher(value);
    while (matcher.find()) {
      int start = matcher.start();
      int end = matcher.end();
      // String c = matcher.group(1);
      // LOG.info("token: '" + value + "' match at start: " + start + " end: "
      // + end);

      // check if token is not in the vocabulary
      if (!m_wordnet.contains(value)) {
        // collect matches for subtoken check
        matches.add(new int[] { start, end });

        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < end - start - 1; i++) {
          sb.deleteCharAt(start); // delete repeating char

          // LOG.info("check token: '" + sb.toString() + "'");
          // check if token is in the vocabulary
          if (m_wordnet.contains(sb.toString())) {
            if (LOGGING) {
              LOG.info("removeRepeatingChars from token '" + value + "' to '"
                  + sb + "'");
            }
            return sb.toString();
          }

          // if the token is not in the vocabulary check all combinations
          // of prior matches
          for (int j = 0; j < matches.size(); j++) {
            int startSub = matches.get(j)[0];
            int endSub = matches.get(j)[1];
            if (startSub != start) {
              StringBuilder subSb = new StringBuilder(sb);
              for (int k = 0; k < endSub - startSub - 1; k++) {
                subSb.deleteCharAt(startSub);

                // LOG.info("check subtoken: '" + subSb.toString() + "'");
                if (m_wordnet.contains(subSb.toString())) {
                  if (LOGGING) {
                    LOG.info("removeRepeatingChars from '" + value + "' to '"
                        + subSb + "'");
                  }
                  return subSb.toString();
                }
              }
            }
          }
        }
      }
    }

    // no match have been found
    // reduce all repeating chars
    if (!matches.isEmpty()) {
      String reducedToken = matcher.replaceAll("$1");
      if (LOGGING) {
        LOG.info("removeRepeatingChars(not found in dict) from '" + value
            + "' to '" + reducedToken + "'");
      }
      value = reducedToken;
    }
    return value;
  }

  public void preprocessTweets(List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      for (List<String> sentence : tweet.getSentences()) {
        List<String> preprocessedSentence = this.preprocess(sentence);
        tweet.addPreprocessedSentence(preprocessedSentence);
      }
    }
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    List<Tweet> tweets = null;
    boolean extendedTest = true;

    // load tweets
    if (extendedTest) {
      // Twitter crawler
      // List<Status> extendedTweets = Configuration
      // .getDataSetUibkCrawlerTest("en");
      // tweets = new ArrayList<Tweet>();
      // for (Status tweet : extendedTweets) {
      // tweets.add(new Tweet(tweet.getId(), tweet.getText(), 0));
      // }

      // SemEval2013
      Dataset dataset = Configuration.getDataSetSemEval2013();
      tweets = dataset.getTrainTweets(true);

    } else { // test tweets
      tweets = Tweet.getTestTweets();
      tweets.add(new Tweet(0, "2moro afaik bbq hf lol loool lollll"));
      tweets
          .add(new Tweet(
              0,
              "suuuper suuper professional tell aahh aaahh aahhh aaahhh aaaahhhhh gaaahh gaaahhhaaag haaahaaa hhhaaaahhhaaa"));
      tweets.add(new Tweet(0, "Martin martin kevin Kevin Justin justin"));
      tweets.add(new Tweet(0, "10,000 1000 +111 -111,0000.4444"));
      tweets
          .add(new Tweet(0, "bankruptcy\ud83d\ude05 happy:-) said:-) ;-)yeah"));
      tweets.add(new Tweet(0, "I\u2019m shit\u002c fan\\u002c \\u2019t"));
      tweets
          .add(new Tweet(
              0,
              "like...and vegas.just hosp.now lies\u002c1st lies,1st candy....wasn\u2019t Nevada\u002cFlorida\u002cOhio\u002cTuesday lol.,.lol lol...lol.."));
      tweets.add(new Tweet(0, "L.O.V.E D.R.U.G.S K.R.I.T"));
      tweets
          .add(new Tweet(
              0,
              "Lamar.....I free..edom free.edom star.Kisses,Star Yes..a Oh,I it!!!Go Jenks/sagna"));
      tweets
          .add(new Tweet(
              0,
              "32.50 $3.25 49.3% 97.1FM 97.1fm 8.30pm 12.45am 12.45AM 12.45PM 6-7pm 5-8p 6pm-9pm @9.15 tonight... 10,000 199,400 149,597,900 20,000+ 10.45,9 8/11/12"));
      tweets
          .add(new Tweet(0,
              "(6ft.10) 2),Chap 85.3%(6513 (att@m80.com) awayDAWN.com www.asdf.org"));
    }

    // preprocess tweets
    long startTime = System.currentTimeMillis();
    for (Tweet tweet : tweets) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);
      tweet.addPreprocessedSentence(preprocessedTokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Preprocessed: '" + preprocessedTokens + "'");
    }
    LOG.info("Preprocess finished after "
        + (System.currentTimeMillis() - startTime) + " ms");
  }

}
