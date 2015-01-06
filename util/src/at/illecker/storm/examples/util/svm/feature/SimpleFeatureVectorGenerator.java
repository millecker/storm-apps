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
package at.illecker.storm.examples.util.svm.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentResult;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
import edu.stanford.nlp.ling.TaggedWord;

public class SimpleFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(SimpleFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;
  private static final SimpleFeatureVectorGenerator instance = new SimpleFeatureVectorGenerator();

  private SentimentWordLists m_sentimentWordLists;

  private SimpleFeatureVectorGenerator() {
    m_sentimentWordLists = SentimentWordLists.getInstance();
  }

  public static SimpleFeatureVectorGenerator getInstance() {
    return instance;
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  @Override
  public int getFeatureVectorSize() {
    return 14;
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(Tweet tweet) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    SentimentResult tweetSentiment = m_sentimentWordLists
        .getTweetSentiment(tweet);
    if (tweetSentiment != null) {
      // LOG.info("tweetSentiment: " + tweetSentiment);
      if (tweetSentiment.getAvgPosCount() != 0)
        resultFeatureVector.put(1, tweetSentiment.getAvgPosCount());
      if (tweetSentiment.getAvgNeutralCount() != 0)
        resultFeatureVector.put(2, tweetSentiment.getAvgNeutralCount());
      if (tweetSentiment.getAvgNegCount() != 0)
        resultFeatureVector.put(3, tweetSentiment.getAvgNegCount());
      if (tweetSentiment.getAvgSum() != 0)
        resultFeatureVector.put(4, tweetSentiment.getAvgSum());
      if (tweetSentiment.getCount() != 0)
        resultFeatureVector.put(5, (double) tweetSentiment.getCount());
      if (tweetSentiment.getMaxPos() != 0)
        resultFeatureVector.put(6, tweetSentiment.getMaxPos());
      if (tweetSentiment.getMaxNeg() != 0)
        resultFeatureVector.put(7, tweetSentiment.getMaxNeg());
    }

    double[] posTags = countPOSTags(tweet);
    if (posTags != null) {
      if (posTags[0] != 0) // nouns / wordCount
        resultFeatureVector.put(8, posTags[0]);
      if (posTags[1] != 0) // verbs / wordCount
        resultFeatureVector.put(9, posTags[1]);
      if (posTags[2] != 0) // adjectives / wordCount
        resultFeatureVector.put(10, posTags[2]);
      if (posTags[3] != 0) // adverbs / wordCount
        resultFeatureVector.put(11, posTags[3]);
      if (posTags[4] != 0) // interjections / wordCount
        resultFeatureVector.put(12, posTags[4]);
      if (posTags[5] != 0) // punctuations / wordCount
        resultFeatureVector.put(13, posTags[5]);
      if (posTags[6] != 0) // hashtags / wordCount
        resultFeatureVector.put(14, posTags[6]);
    }

    if (LOGGING) {
      LOG.info("TweetSentiment: " + tweetSentiment);
      LOG.info("POStags: " + Arrays.toString(posTags));
    }

    return resultFeatureVector;
  }

  private double[] countPOSTags(Tweet tweet) {
    // [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAGS]
    double[] posTags = new double[] { 0d, 0d, 0d, 0d, 0d, 0d, 0d };
    int wordCount = 0;
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      for (TaggedWord word : sentence) {
        wordCount++;
        String pennTag = word.tag();
        if (pennTag.startsWith("NN")) {
          posTags[0]++;
        } else if (pennTag.startsWith("VB")) {
          posTags[1]++;
        } else if (pennTag.startsWith("JJ")) {
          posTags[2]++;
        } else if (pennTag.startsWith("RB")) {
          posTags[3]++;
        } else if (pennTag.startsWith("UH")) {
          posTags[4]++;
        } else if ((pennTag.equals(".")) || (pennTag.equals(":"))) {
          posTags[5]++;
        } else if (pennTag.startsWith("HT")) {
          posTags[6]++;
        }
      }
    }
    // normalize
    for (int i = 0; i < posTags.length; i++) {
      posTags[i] /= wordCount;
    }
    return posTags;
  }

  public static List<Tweet> getTestTweets() {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets.add(new Tweet(0L,
        "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)",
        1));
    tweets
        .add(new Tweet(
            0L,
            "@oluoch @victor_otti @kunjand I just watched it ! Sridevi's comeback .... U remember her from the 90s ?? Sun mornings on NTA ;)",
            1));
    tweets
        .add(new Tweet(
            0L,
            "PBR & @mokbpresents bring you Jim White at the @Do317 Lounge on October 23rd at 7 pm ! http://t.co/7x8OfC56",
            0.5));
    tweets
        .add(new Tweet(
            0L,
            "Why is it so hard to find the @TVGuideMagazine these days ? Went to 3 stores for the Castle cover issue . NONE . Will search again tomorrow ...",
            0));
    tweets.add(new Tweet(0L,
        "called in sick for the third straight day.  ugh.", 0));
    tweets
        .add(new Tweet(
            0L,
            "Here we go.  BANK FAIL FRIDAY -- The FDIC says the Bradford Bank in Baltimore, Maryland has become the 82nd bank failure of the year.",
            0));
    tweets.add(new Tweet(0L,
        "Oh, I'm afraid your Windows-using friends will not survive.", 0));
    return tweets;
  }

  public static void main(String[] args) {
    POSTagger posTagger = POSTagger.getInstance();
    SimpleFeatureVectorGenerator sfvg = SimpleFeatureVectorGenerator
        .getInstance();

    for (Tweet tweet : getTestTweets()) {
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);
      tweet.addTaggedSentence(taggedSentence);

      System.out.println("Tweet: " + tweet);
      System.out.print("FeatureVector:");
      for (Map.Entry<Integer, Double> feature : sfvg.calculateFeatureVector(
          tweet).entrySet()) {
        System.out.print(" " + feature.getKey() + ":" + feature.getValue());
      }
      System.out.println();
    }

    sfvg.getSentimentWordLists().close();
  }
}
