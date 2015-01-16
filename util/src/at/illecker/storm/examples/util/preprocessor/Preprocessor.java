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
import at.illecker.storm.examples.util.HtmlUtils;
import at.illecker.storm.examples.util.RegexUtils;
import at.illecker.storm.examples.util.StringUtils;
import at.illecker.storm.examples.util.UnicodeUtils;
import at.illecker.storm.examples.util.dictionaries.Emoticons;
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
  private Emoticons m_emoticons;
  private FirstNames m_firstNames;

  private Preprocessor() {
    // Load WordNet
    m_wordnet = WordNet.getInstance();
    // Load Slang correction vocabulary
    m_slangCorrection = SlangCorrection.getInstance();
    // Load Emoticons
    m_emoticons = Emoticons.getInstance();
    // Load FirstNames
    m_firstNames = FirstNames.getInstance();
  }

  public static Preprocessor getInstance() {
    return instance;
  }

  public List<String> preprocess(List<String> tokens) {
    // run tail recursion
    return preprocessAccumulator(new LinkedList<String>(tokens),
        new ArrayList<String>(), null);
  }

  private List<String> preprocessAccumulator(LinkedList<String> tokens,
      List<String> processedTokens) {
    return preprocessAccumulator(tokens, processedTokens, null);
  }

  private List<String> preprocessAccumulator(LinkedList<String> tokens,
      List<String> processedTokens, Boolean forceIsURL) {

    if (tokens.isEmpty()) {
      return processedTokens;
    } else {
      String token = tokens.removeFirst();

      boolean tokenIsURL = (forceIsURL != null) ? forceIsURL : StringUtils
          .isURL(token);
      if ((tokenIsURL == false) && (token.startsWith("http://"))) {
        tokenIsURL = true; // force true when starts with http://
      }
      boolean tokenIsUSR = StringUtils.isUser(token);
      boolean tokenIsHashTag = StringUtils.isHashTag(token);
      boolean tokenIsNumeric = StringUtils.isNumeric(token);

      // Step 1) Replace Unicode symbols \u0000
      if (UnicodeUtils.containsUnicode(token)) {
        String replacedToken = UnicodeUtils.replaceUnicodeSymbols(token);
        // LOG.info("Replaced Unicode symbols from '" + token + "' to '"
        // + replacedToken + "'");
        if (replacedToken.equals(token)) {
          LOG.error("Unicode symbols could not be replaced: '" + token + "'");
        }
        token = replacedToken;
      }

      // Step 2) Replace HTML symbols &#[0-9];
      if (HtmlUtils.containsHtml(token)) {
        String replacedToken = HtmlUtils.replaceHtmlSymbols(token);
        // LOG.info("Replaced HTML symbols from '" + token + "' to '"
        // + replacedToken + "'");
        if (replacedToken.equals(token)) {
          LOG.error("HTML symbols could not be replaced: '" + token + "'");
        }
        token = replacedToken;
      }

      // Step 3) Check if token contains a Emoticon after Unicode replacement
      boolean tokenContainsEmoticon = m_emoticons.containsEmoticon(token);
      if ((tokenContainsEmoticon) && (!tokenIsURL)) {
        // TODO Missing =.=

        // Step 3a) Split word and emoticons if necessary
        String[] splittedTokens = m_emoticons.splitEmoticon(token);
        if ((splittedTokens != null) && (splittedTokens.length > 1)) {
          LOG.info("splitEmoticon: " + Arrays.toString(splittedTokens));
          tokens.add(0, splittedTokens[1]);
          tokens.add(0, splittedTokens[0]);
          return preprocessAccumulator(tokens, processedTokens);
        }

        // Step 3b) Unify emoticons, remove repeating chars
        // TODO
        // Unify emoticon from '^^' to '^
        Matcher matcher = RegexUtils.TWO_OR_MORE_REPEATING_CHARS.matcher(token);
        if (matcher.find()) {
          String reducedToken = matcher.replaceAll("$1");
          LOG.info("Unify emoticon from '" + token + "' to '" + reducedToken
              + "'");
          tokens.add(0, reducedToken);
          // preprocess token again if there are recursive patterns in it
          // e.g., :):):) -> :):) -> :)
          return preprocessAccumulator(tokens, processedTokens);
        }
      }

      // Step 4) Remove punctuation and special chars at beginning and ending
      if ((!tokenContainsEmoticon) && (!tokenIsNumeric) && (!tokenIsUSR)
          && (!tokenIsURL)) {
        token = StringUtils.trimPunctuation(token);
        // check if token is numeric again e.g., 20,000+ -> 20,000 after trim
        tokenIsNumeric = StringUtils.isNumeric(token);
        // check if token is now a hashTag e.g., #okaaaay! -> #okaaaay
        tokenIsHashTag = StringUtils.isHashTag(token);
        // check if token is now a URL e.g., www.scotlandrugbyteam.org.
        tokenIsURL = StringUtils.isURL(token);
      }

      // Step 5) slang correction
      // TODO
      // 1) prevent slang correction if all UPPERCASE
      // 'FC' to [fruit, cake]
      // 'Ajax' to [Asynchronous, Javascript, and, XML]
      // 'TL' to [dr too, long, didn't, read]
      // 2) update dictionary
      // t/m k/o w/my b/slisten Rt/follow S/o
      if (!tokenContainsEmoticon) {
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            processedTokens.add(slangCorrection[i]);
          }
          if (LOGGING) {
            LOG.info("slang correction from '" + token + "' to "
                + Arrays.toString(slangCorrection));
          }
          return preprocessAccumulator(tokens, processedTokens);
        }
      }

      // Step 6) Check if there are punctuations between words
      if ((!tokenContainsEmoticon) && (!tokenIsNumeric) && (!tokenIsURL)
          && (!tokenIsHashTag) && (!StringUtils.isEmail(token))) {

        // check if it is a special number $5 5% or 5pm
        Matcher m = RegexUtils.IS_SPECIAL_NUMERIC.matcher(token);
        if (m.matches()) { // if special number check if there is an @
          if (m.group(1) != null) { // @ before number
            tokens.add(0, token.substring(1));
            tokens.add(0, m.group(1)); // @ -> at
            return preprocessAccumulator(tokens, processedTokens);
          }

          // remove alternating letter dot pattern e.g., L.O.V.E
        } else if (RegexUtils.LETTER_DOT_PATTERN.matcher(token).matches()) {
          String newToken = token.replaceAll("\\.", "");
          if (m_wordnet.contains(newToken)) {
            tokens.add(0, newToken);
            return preprocessAccumulator(tokens, processedTokens);
          }

          // if no special number and no numeric try remove punctuations
        } else if (!RegexUtils.IS_SEPARATED_NUMERIC.matcher(token).matches()) {

          m = RegexUtils.PUNCTUATION_BETWEEN_WORDS.matcher(token);
          if (m.find()) {
            // TODO
            // check group 1 for w/
            // e.g., w/Biden w/deals w/you w/the w/her

            LOG.info("Remove punctuations between words: '" + token + "' to '"
                + m.group(1) + "' and '" + m.group(2) + "'");
            tokens.add(0, m.group(2));
            tokens.add(0, m.group(1));
            return preprocessAccumulator(tokens, processedTokens, false);
          }
        }
      }

      // Step 7) Fix omission of final g in gerund forms (goin)
      if ((!tokenIsUSR) && (!tokenIsHashTag) && (token.endsWith("in"))
          && (!m_firstNames.isFirstName(token))
          && (!m_wordnet.contains(token.toLowerCase()))) {
        // append "g" if a word ends with "in" and is not in the vocabulary
        if (LOGGING) {
          LOG.info("Add missing \"g\" from '" + token + "' to '" + token + "g'");
        }
        token = token + "g";
      }

      // Step 8) Remove elongations of characters (suuuper)
      // 'lollll' to 'loll' because 'loll' is found in dict
      // TODO 'AHHHHH' to 'AH'
      if ((!tokenIsURL) && (!tokenIsUSR) && (!tokenIsHashTag)
          && (!tokenContainsEmoticon) && (!tokenIsNumeric)) {

        token = removeRepeatingChars(token);

        // Step 8b) Try slang correction again
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            processedTokens.add(slangCorrection[i]);
          }
          if (LOGGING) {
            LOG.info("slang correction from '" + token + "' to "
                + Arrays.toString(slangCorrection));
          }
          return preprocessAccumulator(tokens, processedTokens);
        }
      }

      // add token to processed list
      if (!token.isEmpty()) { // trimPunctuation could make token empty
        processedTokens.add(token);
      }

      return preprocessAccumulator(tokens, processedTokens);
    }
  }

  private String removeRepeatingChars(String value) {
    // if there are three repeating equal chars
    // then remove one char until the word is found in the vocabulary
    // else if the word is not found reduce the repeating chars to one

    // collect matches for sub-token search
    List<int[]> matches = new ArrayList<int[]>();

    Matcher matcher = RegexUtils.THREE_OR_MORE_REPEATING_CHARS.matcher(value);
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
            LOG.info("removeRepeatingChars from token '" + value + "' to '"
                + sb + "'");
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
                  LOG.info("removeRepeatingChars from '" + value + "' to '"
                      + subSb + "'");
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
      LOG.info("removeRepeatingChars(not found in dict) from '" + value
          + "' to '" + reducedToken + "'");
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
              "32.50 $3.25 49.3% 97.1FM 97.1fm 8.30pm 12.45am 12.45AM 12.45PM @9.15 tonight... 10,000 199,400 149,597,900 20,000+ 10.45,9 8/11/12"));
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
