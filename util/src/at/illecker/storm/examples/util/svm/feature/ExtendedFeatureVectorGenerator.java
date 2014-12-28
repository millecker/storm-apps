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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.ArraysUtils;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
import edu.stanford.nlp.ling.TaggedWord;

public class ExtendedFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(ExtendedFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;

  private SimpleFeatureVectorGenerator m_sfvg;
  private SentimentWordLists m_sentimentWordLists;
  private TweetTfIdf m_tweetTfIdf = null;

  public ExtendedFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_tweetTfIdf = tweetTfIdf;
    this.m_sfvg = SimpleFeatureVectorGenerator.getInstance();
    this.m_sentimentWordLists = m_sfvg.getSentimentWordLists();
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  public double[] getTfIdsVector(Tweet tweet) {
    double[] featureVector = null;
    if (m_tweetTfIdf != null) {
      Map<String, Double> tfIdf = m_tweetTfIdf.tfIdf(tweet);

      featureVector = new double[m_tweetTfIdf.getInverseDocFreq().size()];
      int i = 0;
      for (String key : m_tweetTfIdf.getInverseDocFreq().keySet()) {
        Double v = tfIdf.get(key);
        featureVector[i] = (v != null) ? v : 0;
        i++;
      }

      LOG.info("TfIdsVector: " + Arrays.toString(featureVector));
    }
    return featureVector;
  }

  @Override
  public double[] calculateFeatureVector(Tweet tweet) {
    // [POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, MAX_POS_SCORE,
    // MAX_NEG_SCORE]
    // [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAGS]
    // [TfIdsVector]
    double[] resultFeatureVector = m_sfvg.calculateFeatureVector(tweet);

    double[] tfIdsVector = getTfIdsVector(tweet);
    if (tfIdsVector != null) {
      resultFeatureVector = ArraysUtils
          .concat(resultFeatureVector, tfIdsVector);
    }
    return resultFeatureVector;
  }

  public static void main(String[] args) {
    List<Tweet> tweets = SimpleFeatureVectorGenerator.getTestTweets();
    POSTagger posTagger = POSTagger.getInstance();
    boolean tfIdfusePOSTags = false;

    // prepare Tweets
    for (Tweet tweet : tweets) {
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);
      tweet.addTaggedSentence(taggedSentence);
    }

    ExtendedFeatureVectorGenerator efvg = new ExtendedFeatureVectorGenerator(
        new TweetTfIdf(tweets, tfIdfusePOSTags));

    // generate Feature Vector
    for (Tweet tweet : tweets) {
      System.out.println("Tweet: " + tweet);
      System.out.println("FeatureVector: "
          + Arrays.toString(efvg.calculateFeatureVector(tweet)));
    }

    // close wordnet dic
    efvg.getSentimentWordLists().close();
  }
}
