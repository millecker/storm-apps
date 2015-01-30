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
import at.illecker.storm.commons.tweet.PreprocessedTweet;
import at.illecker.storm.commons.tweet.TaggedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

public class POSTagger {
  private static final Logger LOG = LoggerFactory.getLogger(POSTagger.class);
  private static final POSTagger instance = new POSTagger();
  private MaxentTagger m_posTagger;

  private POSTagger() {
    // Load POS Tagger
    String taggingModel = Configuration.getPOSTaggingModelFast();
    try {
      LOG.info("Load POSTagger with model: " + taggingModel);
      TaggerConfig posTaggerConf = new TaggerConfig("-model", taggingModel,
          "-nthreads", "4");
      LOG.info("POSTagger uses " + posTaggerConf.getNThreads() + " threads...");
      m_posTagger = new MaxentTagger(taggingModel, posTaggerConf, false);
    } catch (RuntimeIOException e) {
      LOG.error("RuntimeIOException: " + e.getMessage());
    }
  }

  public static POSTagger getInstance() {
    return instance;
  }

  public List<TaggedWord> tagSentence(List<TaggedWord> pretaggedTokens) {
    return m_posTagger.tagSentence(pretaggedTokens, true);
  }

  public List<TaggedTweet> tagTweets(List<PreprocessedTweet> tweets) {
    List<TaggedTweet> taggedTweets = new ArrayList<TaggedTweet>();
    for (PreprocessedTweet tweet : tweets) {
      List<List<TaggedWord>> taggedSentences = new ArrayList<List<TaggedWord>>();
      for (List<TaggedWord> sentence : tweet.getPreprocessedSentences()) {
        taggedSentences.add(this.tagSentence(sentence));
      }
      taggedTweets.add(new TaggedTweet(tweet.getId(), tweet.getText(), tweet
          .getScore(), taggedSentences));
    }
    return taggedTweets;
  }

  private static List<Long> measurePOSTagger(MaxentTagger tagger,
      List<Tweet> tweets, int testRounds, boolean manualTokenizing) {
    List<Long> times = new ArrayList<Long>();
    for (int i = 0; i < testRounds; i++) {
      for (Tweet t : tweets) {
        String text = t.getText();
        if (manualTokenizing) {
          List<HasWord> sentence = Sentence.toWordList(text.split(" "));
          long startTime = System.currentTimeMillis();
          tagger.tagSentence(sentence, true); // reuseTags = true
          times.add(System.currentTimeMillis() - startTime);
        } else {
          long startTime = System.currentTimeMillis();
          tagger.tagString(text);
          times.add(System.currentTimeMillis() - startTime);
        }
      }
    }
    return times;
  }

  private static long getAvg(List<Long> list) {
    long sum = 0;
    for (Long l : list) {
      sum += l.longValue();
    }
    return (sum / list.size());
  }

  public static void testPOSTagger(List<Tweet> tweets, int testRounds) {
    // Load tagger and models
    String taggingModel1 = Configuration.getPOSTaggingModelFast();
    TaggerConfig gateTagger1Conf = new TaggerConfig("-model", taggingModel1);
    MaxentTagger gateTagger1 = new MaxentTagger(taggingModel1, gateTagger1Conf,
        true);

    String taggingModel2 = Configuration.getPOSTaggingModelFast();
    TaggerConfig gateTagger2Conf = new TaggerConfig("-model", taggingModel2);
    MaxentTagger gateTagger2 = new MaxentTagger(taggingModel2, gateTagger2Conf,
        true);

    // Test gateTagger1 with model1 via strings
    List<Long> tagger1TextTimes = measurePOSTagger(gateTagger1, tweets,
        testRounds, false);
    // Test gateTagger2 with model2 via strings
    List<Long> tagger2TextTimes = measurePOSTagger(gateTagger2, tweets,
        testRounds, false);

    // Test gateTagger1 with model1 via sentence, manually tokenize
    List<Long> tagger1SentenceTimes = measurePOSTagger(gateTagger1, tweets,
        testRounds, true);
    // Test gateTagger2 with model2 via sentence, manually tokenize
    List<Long> tagger2SentenceTimes = measurePOSTagger(gateTagger2, tweets,
        testRounds, true);

    // Output results
    System.out.println("Time Measurements: ");
    System.out
        .println("GateTagger1 (gate-EN-twitter.model): TextTagging (incl tokenize): "
            + getAvg(tagger1TextTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2TextTimes) + " ms");
    System.out
        .println("GateTagger2 (gate-EN-twitter-fast.model): TextTagging (incl tokenize): "
            + getAvg(tagger1SentenceTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2SentenceTimes) + " ms");
  }

  public static void main(String[] args) {
    boolean testPOSTagger = false;
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

    if (testPOSTagger) {
      testPOSTagger(tweets, 1);

    } else {
      POSTagger posTagger = POSTagger.getInstance();
      Preprocessor preprocessor = Preprocessor.getInstance();

      // process tweets
      long startTime = System.currentTimeMillis();
      for (Tweet tweet : tweets) {
        // Tokenize
        List<String> tokens = Tokenizer.tokenize(tweet.getText());

        // Preprocess
        List<TaggedWord> preprocessedTokens = preprocessor.preprocess(tokens);

        // POS Tagging
        List<TaggedWord> taggedSentence = posTagger
            .tagSentence(preprocessedTokens);

        LOG.info("Tweet: '" + tweet + "'");
        LOG.info("TaggedSentence: " + taggedSentence);
      }
      long elapsedTime = System.currentTimeMillis() - startTime;
      LOG.info("POSTagger finished after " + elapsedTime + " ms");
      LOG.info("Total tweets: " + tweets.size());
      LOG.info((elapsedTime / (double) tweets.size()) + " ms per Tweet");
    }
  }
}
