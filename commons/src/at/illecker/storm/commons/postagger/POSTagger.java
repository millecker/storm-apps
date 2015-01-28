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
      TaggerConfig posTaggerConf = new TaggerConfig("-model", taggingModel);
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

  public static void testPOSTagger() {
    String text = "ikr smh he asked fir yo last name so he can add u on fb lololol";
    final int testRounds = 1;

    // Measurements
    List<Long> tagger1TextTimes = new ArrayList<Long>();
    List<Long> tagger2TextTimes = new ArrayList<Long>();
    List<Long> tagger1SentenceTimes = new ArrayList<Long>();
    List<Long> tagger2SentenceTimes = new ArrayList<Long>();

    // Load tagger and models
    MaxentTagger tagger1 = new MaxentTagger(Configuration.getPOSTaggingModel());
    MaxentTagger tagger2 = new MaxentTagger(
        Configuration.getPOSTaggingModelFast());

    // Test tagger1 via strings
    String taggedText1 = "";
    for (int i = 0; i < testRounds; i++) {
      long startTime = System.currentTimeMillis();
      taggedText1 = tagger1.tagString(text);
      tagger1TextTimes.add(System.currentTimeMillis() - startTime);
    }

    // Test tagger2 via strings
    String taggedText2 = "";
    for (int i = 0; i < testRounds; i++) {
      long startTime = System.currentTimeMillis();
      taggedText2 = tagger2.tagString(text);
      tagger2TextTimes.add(System.currentTimeMillis() - startTime);
    }

    // Manually tokenize
    List<HasWord> sentence = Sentence.toWordList(text.split(" "));

    // Test tagger1 via sentence
    List<TaggedWord> taggedSentence1 = null;
    for (int i = 0; i < testRounds; i++) {
      long startTime = System.currentTimeMillis();
      taggedSentence1 = tagger1.tagSentence(sentence);
      tagger1SentenceTimes.add(System.currentTimeMillis() - startTime);
    }
    // Test tagger2 via sentence
    List<TaggedWord> taggedSentence2 = null;
    for (int i = 0; i < testRounds; i++) {
      long startTime = System.currentTimeMillis();
      taggedSentence2 = tagger2.tagSentence(sentence);
      tagger2SentenceTimes.add(System.currentTimeMillis() - startTime);
    }

    // Output results
    System.out.println("Input: " + text);
    System.out.println("Tagger1 Text output: " + taggedText1);
    System.out.println("Tagger2 Text output: " + taggedText2);
    System.out.println("Tagger1 Sentence output: ");
    for (TaggedWord tw : taggedSentence1) {
      System.out.println("word: " + tw.word() + " ::  tag: " + tw.tag());
      // tw.tag().startsWith("JJ")
    }
    System.out.println("Tagger2 Sentence output: ");
    for (TaggedWord tw : taggedSentence2) {
      System.out.println("word: " + tw.word() + " ::  tag: " + tw.tag());
    }

    System.out.println("Time Measurements: ");
    System.out
        .println("Tagger1 (gate-EN-twitter.model): TextTagging (incl tokenize): "
            + getAvg(tagger1TextTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2TextTimes) + " ms");
    System.out
        .println("Tagger2 (gate-EN-twitter-fast.model): TextTagging (incl tokenize): "
            + getAvg(tagger1SentenceTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2SentenceTimes) + " ms");
  }

  private static long getAvg(List<Long> list) {
    long sum = 0;
    for (Long l : list) {
      sum += l.longValue();
    }
    return (sum / list.size());
  }

  public static void main(String[] args) {
    POSTagger posTagger = POSTagger.getInstance();
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
    } else {
      tweets = Tweet.getTestTweets();
    }

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
    LOG.info("POSTagger finished after "
        + (System.currentTimeMillis() - startTime) + " ms");
  }
}
