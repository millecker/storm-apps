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
package at.illecker.storm.commons.postagger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.io.SerializationUtils;
import cmu.arktweetnlp.Tagger.TaggedToken;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;

public class ArkPOSTagger {
  private static final Logger LOG = LoggerFactory.getLogger(ArkPOSTagger.class);
  private static final boolean LOGGING = Configuration.get(
      "commons.postagger.logging", false);
  private static final ArkPOSTagger INSTANCE = new ArkPOSTagger();
  String m_taggingModel;
  private Model m_model;
  private FeatureExtractor m_featureExtractor;

  private ArkPOSTagger() {
    // Load ARK POS Tagger
    try {
      m_taggingModel = Configuration
          .get("global.resources.postagger.ark.model.path");
      LOG.info("Load ARK POS Tagger with model: " + m_taggingModel);
      // TODO absolute path needed for resource
      if ((Configuration.RUNNING_WITHIN_JAR)
          && (!m_taggingModel.startsWith("/"))) {
        m_taggingModel = "/" + m_taggingModel;
      }
      m_model = Model.loadModelFromText(m_taggingModel);
      m_featureExtractor = new FeatureExtractor(m_model, false);
    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
    }
  }

  public static ArkPOSTagger getInstance() {
    return INSTANCE;
  }

  public List<List<TaggedToken>> tagTweets(List<List<String>> tweets) {
    List<List<TaggedToken>> taggedTweets = new ArrayList<List<TaggedToken>>();
    for (List<String> tweet : tweets) {
      taggedTweets.add(tag(tweet));
    }
    return taggedTweets;
  }

  public List<TaggedToken> tag(List<String> tokens) {
    Sentence sentence = new Sentence();
    sentence.tokens = tokens;
    ModelSentence ms = new ModelSentence(sentence.T());
    m_featureExtractor.computeFeatures(sentence, ms);
    m_model.greedyDecode(ms, false);

    List<TaggedToken> taggedTokens = new ArrayList<TaggedToken>();
    for (int t = 0; t < sentence.T(); t++) {
      TaggedToken tt = new TaggedToken(tokens.get(t),
          m_model.labelVocab.name(ms.labels[t]));
      taggedTokens.add(tt);
    }
    return taggedTokens;
  }

  public void serializeModel() {
    SerializationUtils.serialize(m_model, m_taggingModel + "_model.ser");
  }

  public void serializeFeatureExtractor() {
    SerializationUtils.serialize(m_featureExtractor, m_taggingModel
        + "_featureExtractor.ser");
  }

  public static void main(String[] args) {
    boolean extendedTest = false;
    boolean useSerialization = true;

    // load tweets
    List<Tweet> tweets = null;
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
    } else {
      tweets = Tweet.getTestTweets();
    }

    Preprocessor preprocessor = Preprocessor.getInstance();
    ArkPOSTagger posTagger = ArkPOSTagger.getInstance();

    if (useSerialization) {
      posTagger.serializeModel();
      posTagger.serializeFeatureExtractor();
    }

    // process tweets
    long startTime = System.currentTimeMillis();
    for (Tweet tweet : tweets) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);

      // POS Tagging
      List<TaggedToken> taggedTokens = posTagger.tag(preprocessedTokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedTweet: " + taggedTokens);
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    LOG.info("POSTagger finished after " + elapsedTime + " ms");
    LOG.info("Total tweets: " + tweets.size());
    LOG.info((elapsedTime / (double) tweets.size()) + " ms per Tweet");
  }

}
