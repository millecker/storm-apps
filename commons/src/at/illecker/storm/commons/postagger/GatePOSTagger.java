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

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

public class GatePOSTagger {
  private static final Logger LOG = LoggerFactory
      .getLogger(GatePOSTagger.class);
  private static final boolean LOGGING = Configuration.get(
      "commons.postagger.logging", false);
  private static final GatePOSTagger INSTANCE = new GatePOSTagger();
  private MaxentTagger m_posTagger;

  private GatePOSTagger() {
    // Load Stanford POS Tagger with GATE model
    String taggingModel = Configuration
        .get("global.resources.postagger.gate.model.path");
    LOG.info("Load Stanford POS Tagger with model: " + taggingModel);
    TaggerConfig posTaggerConf = new TaggerConfig("-model", taggingModel);
    m_posTagger = new MaxentTagger(taggingModel, posTaggerConf, false);
  }

  public static GatePOSTagger getInstance() {
    return INSTANCE;
  }

  public List<List<TaggedWord>> tagTweets(List<List<TaggedWord>> tweets) {
    List<List<TaggedWord>> taggedTweets = new ArrayList<List<TaggedWord>>();
    for (List<TaggedWord> tweet : tweets) {
      taggedTweets.add(tag(tweet));
    }
    return taggedTweets;
  }

  public List<TaggedWord> tag(List<TaggedWord> pretaggedTokens) {
    return m_posTagger.tagSentence(pretaggedTokens, true);
  }

  public static void main(String[] args) {
    boolean extendedTest = true;

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
    GatePOSTagger posTagger = GatePOSTagger.getInstance();

    // process tweets
    long startTime = System.currentTimeMillis();
    for (Tweet tweet : tweets) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      List<TaggedWord> preprocessedTokens = preprocessor
          .preprocessAndTag(tokens);

      // POS Tagging
      List<TaggedWord> taggedTokens = posTagger.tag(preprocessedTokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedTweet: " + taggedTokens);
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    LOG.info("POSTagger finished after " + elapsedTime + " ms");
    LOG.info("Total tweets: " + tweets.size());
    LOG.info((elapsedTime / (double) tweets.size()) + " ms per Tweet");
  }

}
