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
package at.illecker.storm.examples.util.tagger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.StringUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.Interjections;
import at.illecker.storm.examples.util.wordlist.NameEntities;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

public class POSTagger {
  private static final Logger LOG = LoggerFactory.getLogger(POSTagger.class);
  private static final boolean LOGGING = false;
  private static final POSTagger instance = new POSTagger();

  private MaxentTagger m_posTagger; // Standford POS Tagger
  private NameEntities m_nameEntities;
  private Interjections m_interjections;

  private POSTagger() {
    // Load POS Tagger
    String taggingModel = Configuration.getPOSTaggingModel();
    try {
      LOG.info("Load POSTagger with model: " + taggingModel);
      TaggerConfig posTaggerConf = new TaggerConfig("-model", taggingModel);
      m_posTagger = new MaxentTagger(taggingModel, posTaggerConf, false);
    } catch (RuntimeIOException e) {
      LOG.error("RuntimeIOException: " + e.getMessage());
    }

    // Load NameEntities
    m_nameEntities = NameEntities.getInstance();

    // Load Interjections
    m_interjections = Interjections.getInstance();
  }

  public static POSTagger getInstance() {
    return instance;
  }

  public List<TaggedWord> tagSentence(List<String> tokens) {
    // LOG.info("tagSentence: " + tokens.toString());
    List<TaggedWord> untaggedTokens = new ArrayList<TaggedWord>();

    Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String token = iter.next();
      TaggedWord preTaggedToken = new TaggedWord(token);
      String tokenLowerCase = token.toLowerCase();

      // set custom tags
      if (StringUtils.isHashTag(token)) {
        preTaggedToken.setTag("HT");
        // if ((token.length() == 1) && (iter.hasNext())) {
        // String nextToken = iter.next();
        // preTaggedToken.setWord(preTaggedToken.word() + nextToken);
        // token = nextToken;
        // }
      }
      if (StringUtils.isURL(token)) {
        preTaggedToken.setTag("USR");
        // if ((token.length() == 1) && (iter.hasNext())) {
        // String nextToken = iter.next();
        // preTaggedToken.setWord(preTaggedToken.word() + nextToken);
        // token = nextToken;
        // if (preTaggedToken.word().indexOf("#") == 0) {
        // preTaggedToken.setTag("HT");
        // }
        // }
      }
      if (StringUtils.isURL(token)) {
        preTaggedToken.setTag("URL");
      }
      if (StringUtils.isRetweet(token)) {
        preTaggedToken.setTag("RT");
      }

      // Name entities
      if (m_nameEntities.isNameEntity(tokenLowerCase)) {
        if (LOGGING) {
          LOG.info("NameEntity labelled for " + token);
        }
        preTaggedToken.setTag("NNP");
      }

      // Interjections
      if (m_interjections.isInterjection(tokenLowerCase)) {
        if (LOGGING) {
          LOG.info("Interjection labelled for " + token);
        }
        preTaggedToken.setTag("UH");
      }

      untaggedTokens.add(preTaggedToken);
    }

    if (LOGGING) {
      LOG.info("tagSentence: " + untaggedTokens.toString());
    }
    return m_posTagger.tagSentence(untaggedTokens, true);
  }

  public static void main(String[] args) {
    POSTagger posTagger = POSTagger.getInstance();
    Preprocessor preprocessor = Preprocessor.getInstance();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);
      tweet.addPreprocessedSentence(preprocessedTokens);

      // POS Tagging
      List<TaggedWord> taggedSentence = posTagger
          .tagSentence(preprocessedTokens);
      tweet.addTaggedSentence(taggedSentence);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedSentence: " + taggedSentence);
    }
  }
}
