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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.StringUtils;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.Interjections;
import at.illecker.storm.examples.util.wordlist.SlangCorrection;
import at.illecker.storm.examples.util.wordnet.WordNet;

public class Preprocessor {
  private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);
  private static final boolean LOGGING = true;
  private static final Preprocessor instance = new Preprocessor();

  private WordNet m_wordnet;
  private SlangCorrection m_slangCorrection;
  private Interjections m_interjections;

  private Preprocessor() {
    // load WordNet
    m_wordnet = WordNet.getInstance();
    // load SlangCorrection dictionaries
    m_slangCorrection = SlangCorrection.getInstance();
    // load interjections
    m_interjections = Interjections.getInstance();
  }

  public static Preprocessor getInstance() {
    return instance;
  }

  public List<String> preprocess(List<String> tokens) {
    // LOG.info("preprocess: " + tokens.toString());
    List<String> processedTokens = new ArrayList<String>();

    for (String token : tokens) {

      // Step 1) Replace HTML symbols
      token = replaceHTMLSymbols(token);

      // Step 2) Remove punctuation and special chars at beginning and ending
      if (!m_interjections.isInterjection(token.toLowerCase())) {
        token = StringUtils.trimPunctuation(token);
      }

      // Step 3) Fix omission of final g in gerund forms (goin)
      if ((token.endsWith("in")) && (!m_wordnet.contains(token.toLowerCase()))) {
        // append "g" if a word ends with "in" and is not in the vocabulary
        if (LOGGING) {
          LOG.info("Add missing \"g\" from '" + token + "' to '" + token + "g'");
        }
        token = token + "g";
      }

      // Step 4) Remove elongations of characters (suuuper).
      token = removeRepeatedChars(token);

      // we first tried to subsequently remove each repeating character until we
      // hit an in-vocabulary word.
      // For cases resisting this treatment, we adopted the method
      // suggested by (Brody/Diakopoulos, 2011) and generated a squeezed form of
      // the prolongated word, subsequently looking it up in a probability table
      // that has previously been gathered from a training corpus.

      // Step 5) Slang correction
      String[] correction = m_slangCorrection
          .getCorrection(token.toLowerCase());

      if (correction != null) {
        for (int i = 0; i < correction.length; i++) {
          processedTokens.add(correction[i]);
        }
        if (LOGGING) {
          LOG.info("SlangCorrecting from " + token + " to "
              + Arrays.toString(correction));
        }
        token = "";
      }

      // add unmodified token
      if (!token.isEmpty()) {
        processedTokens.add(token);
      }
    }

    return processedTokens;
  }

  private String replaceHTMLSymbols(String value) {
    String result = value;
    result = result.replaceAll("&quot;", "\"");
    result = result.replaceAll("&amp;", "&");
    result = result.replaceAll("&lt;", "<");
    result = result.replaceAll("&gt;", ">");
    result = result.replaceAll("&nbsp;", " ");
    return result;
  }

  private String removeRepeatedChars(String value) {
    // "(.)\\1{3,}" means any character (added to group 1)
    // followed by itself at least three times
    return value.replaceAll("(.)\\1{3,}", "$1");
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);
      tweet.addPreprocessedSentence(preprocessedTokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Preprocessed: '" + preprocessedTokens + "'");
    }
  }
}
