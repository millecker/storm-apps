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
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SlangCorrection;

public class Preprocessor {
  private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);
  private static final boolean LOGGING = false;
  private static final Preprocessor instance = new Preprocessor();

  private SlangCorrection m_slangCorrection;

  private Preprocessor() {
    // Load SlangCorrection
    m_slangCorrection = SlangCorrection.getInstance();
  }

  public static Preprocessor getInstance() {
    return instance;
  }

  // STEP 1
  // First unify all URLs, e-mail addresses and user names by replacing them
  // with unique tokens.
  // All hash marks were stripped from words, and emoticons were mapped to
  // special tokens representing their emotion categories
  // These special tokens were then added to the polarity lexicons used by
  // SO-CAL.

  // STEP 2
  // Social media specific slang expressions and abbreviations like “2 b” (for
  // “to be”) or “im- sry” (for “I am sorry”) were translated to their ap-
  // propriate standard language forms. For this, we used a dictionary of
  // 5,424 expressions that we gathered from publicly available resources.
  // http://www.noslang.com/dictionary/
  // http://onlineslangdictionary.com/
  // http: //www.urbandictionary.com/

  // STEP 3
  // Tackles two typical spelling phenomena:
  // a) the omission of final g in gerund forms (goin), and
  // b) elongations of characters (suuuper).
  // For the former, we appended the character g to words ending with -in if
  // these words are unknown to vo- cabulary,4 while the corresponding
  // ‘g’-forms are in- vocabulary words (IVW). For the latter problem, we
  // first tried to subsequently remove each repeat- ing character until we
  // hit an IVW. For cases re- sisting this treatment, we adopted the method
  // sug- gested by (Brody/Diakopoulos, 2011) and generated a squeezed form of
  // the prolongated word, subse- quently looking it up in a probability table
  // that has previously been gathered from a training corpus.

  public List<String> preprocess(List<String> tokens) {
    // LOG.info("preprocess: " + tokens.toString());
    List<String> processedTokens = new ArrayList<String>();

    Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String token = iter.next();
      String tokenLowerCase = token.toLowerCase();

      // Slang correction
      String correction = m_slangCorrection.getCorrection(tokenLowerCase);
      if (correction != null) {
        if (LOGGING) {
          LOG.info("SlangCorrecting from " + token + " to " + correction);
        }
        token = correction;
      }

      processedTokens.add(token);
    }

    if (LOGGING) {
      LOG.info("preprocessed: " + processedTokens.toString());
    }

    return processedTokens;
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
