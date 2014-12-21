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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
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

  private double[] countPOSTags(Tweet tweet) {
    // { NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAGS }
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

  @Override
  public double[] calculateFeatureVector(Tweet tweet) {
    // [POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, MAX_POS_SCORE,
    double[] resultFeatureVector;
    double tweetSentiment[] = m_sentimentWordLists.getTweetSentiment(tweet);
    if (tweetSentiment == null) {
      tweetSentiment = new double[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    } else {
      tweetSentiment[0] = tweetSentiment[0] / tweetSentiment[4];
      tweetSentiment[1] = tweetSentiment[1] / tweetSentiment[4];
      tweetSentiment[2] = tweetSentiment[2] / tweetSentiment[4];
      tweetSentiment[3] = tweetSentiment[3] / tweetSentiment[4]; // AVG
    }
    resultFeatureVector = tweetSentiment;

    double[] posTags = countPOSTags(tweet);
    resultFeatureVector = concat(resultFeatureVector, posTags);

    if (LOGGING) {
      LOG.info("tweetSentiment: " + Arrays.toString(tweetSentiment));
      LOG.info("POStags: " + Arrays.toString(posTags));
    }

    return resultFeatureVector;
  }

  private static double[] concat(double[] a, double[] b) {
    int aLen = a.length;
    int bLen = b.length;
    double[] c = new double[aLen + bLen];
    System.arraycopy(a, 0, c, 0, aLen);
    System.arraycopy(b, 0, c, aLen, bLen);
    return c;
  }

  public void close() {
    m_sentimentWordLists.close();
  }

  public static List<Tweet> getTestTweets() {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets.add(new Tweet(264183816548130816L,
        "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)",
        1));
    tweets
        .add(new Tweet(
            263398998675693568L,
            "@oluoch @victor_otti @kunjand I just watched it ! Sridevi's comeback .... U remember her from the 90s ?? Sun mornings on NTA ;)",
            1));
    tweets
        .add(new Tweet(
            259724822978904064L,
            "PBR & @mokbpresents bring you Jim White at the @Do317 Lounge on October 23rd at 7 pm ! http://t.co/7x8OfC56",
            0.5));
    tweets
        .add(new Tweet(
            264259830590603264L,
            "Why is it so hard to find the @TVGuideMagazine these days ? Went to 3 stores for the Castle cover issue . NONE . Will search again tomorrow ...",
            0));
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
      System.out.println("FeatureVector: "
          + Arrays.toString(sfvg.calculateFeatureVector(tweet)));
    }

    sfvg.close();
  }
}
