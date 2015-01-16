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
package at.illecker.storm.examples.util.tokenizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.HtmlUtils;
import at.illecker.storm.examples.util.UnicodeUtils;
import at.illecker.storm.examples.util.tweet.Tweet;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tokenizer {
  private static final Logger LOG = LoggerFactory.getLogger(Tokenizer.class);

  public static List<String> tokenize(String text) {
    // trim text
    text = text.trim();

    // split at one or more blanks
    String[] tokens = text.split("\\s+");

    List<String> resultTokens = new ArrayList<String>();
    for (String token : tokens) {

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

      resultTokens.add(token);
    }
    return resultTokens;
  }

  public static void tokenizeTweets(List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      List<String> tokens = tokenize(tweet.getText());
      tweet.addSentence(tokens);
    }
  }

  public static List<List<HasWord>> tokenizeSentences(String text) {
    TokenizerFactory<Word> tokenizer = PTBTokenizerFactory
        .newTokenizerFactory();
    tokenizer.setOptions("ptb3Escaping=false");

    return MaxentTagger.tokenizeText(new StringReader(text), tokenizer);
  }

  public static void main(String[] args) {
    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Tokens: " + tokens);
    }
  }
}
