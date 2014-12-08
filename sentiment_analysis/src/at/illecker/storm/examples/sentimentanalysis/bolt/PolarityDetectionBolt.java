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
package at.illecker.storm.examples.sentimentanalysis.bolt;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.sentimentanalysis.util.Tweet;
import at.illecker.storm.examples.util.unsupervised.WordListMap;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import edu.stanford.nlp.ling.TaggedWord;

/**
 * Polarity Detector Bolt
 * 
 */
public class PolarityDetectionBolt extends BaseRichBolt {
  public static String CONF_SENTIMENT_WORD_LIST1_FILE = "word.list1.file";
  public static String CONF_SENTIMENT_WORD_LIST2_FILE = "word.list2.file";
  private static final long serialVersionUID = -549704444828609491L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PolarityDetectionBolt.class);

  private OutputCollector m_collector;
  private WordListMap<Double> m_wordRatings1;
  private WordListMap<Double> m_wordRatings2;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;

    if (config.get(CONF_SENTIMENT_WORD_LIST1_FILE) != null) {
      m_wordRatings1 = WordListMap.loadWordRatings(
          ClassLoader.getSystemResourceAsStream(config.get(
              CONF_SENTIMENT_WORD_LIST1_FILE).toString()), -5, 5);
    }
    if (config.get(CONF_SENTIMENT_WORD_LIST2_FILE) != null) {
      m_wordRatings2 = WordListMap.loadWordRatings(
          ClassLoader.getSystemResourceAsStream(config.get(
              CONF_SENTIMENT_WORD_LIST2_FILE).toString()), -5, 5);
    }
    if ((m_wordRatings1 == null) && (m_wordRatings2 == null)) {
      throw new RuntimeException("No word lists available!");
    }
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("taggedTweet");
    // LOG.info(tweet.toString());

    double tweetSentiment1 = 0;
    double tweetSentiment2 = 0;
    int tweetWords = 0;
    for (List<TaggedWord> taggedSentence : tweet.getTaggedSentences()) {
      // LOG.info("TaggedSentence: " + taggedSentence.toString());

      String sentimentSentenceString = "";
      double sentenceSentiment1 = 0;
      double sentenceSentiment2 = 0;
      int sentenceWords = 0;
      for (TaggedWord taggedWord : taggedSentence) {
        // See tags http://www.clips.ua.ac.be/pages/mbsp-tags
        // Skip punctuations
        if (taggedWord.tag().equals(".") || taggedWord.tag().equals(",")
            || taggedWord.tag().equals(":")) {
          continue;
        }
        // Skip cardinal numbers and symbols
        if (taggedWord.tag().equals("CD") || taggedWord.tag().equals("SYM")) {
          continue;
        }

        String word = taggedWord.word().toLowerCase().trim();
        sentenceWords++;

        Double rating1 = null;
        if (m_wordRatings1 != null) {
          rating1 = m_wordRatings1.matchKey(word, false);
        }
        Double rating2 = null;
        if (m_wordRatings2 != null) {
          rating2 = m_wordRatings2.matchKey(word, false);
        }

        // Update sentiment sum
        if (rating1 != null) {
          sentenceSentiment1 += rating1;
        }
        if (rating2 != null) {
          sentenceSentiment2 += rating2;
        }

        sentimentSentenceString += word + "/"
            + ((rating1 != null) ? rating1 : "NA") + "|"
            + ((rating2 != null) ? rating2 : "NA") + " ";
      }
      tweetWords += sentenceWords;
      tweetSentiment1 += sentenceSentiment1;
      tweetSentiment2 += sentenceSentiment2;

      // Debug
      LOG.info("TaggedSentence: " + taggedSentence.toString()
          + " SentimentSentence: " + sentimentSentenceString + " Words: "
          + sentenceWords + " SentenceSentiment1: " + sentenceSentiment1
          + " SentimentScore1: " + (sentenceSentiment1 / sentenceWords)
          + " SentenceSentiment2: " + sentenceSentiment2 + " SentimentScore2: "
          + (sentenceSentiment2 / sentenceWords));
    }

    // Debug
    LOG.info("Tweet: " + tweet.toString() + " Words: " + tweetWords
        + " TweetSentiment1: " + tweetSentiment1 + " SentimentScore1: "
        + (tweetSentiment1 / tweetWords) + " TweetSentiment2: "
        + tweetSentiment2 + " SentimentScore2: "
        + (tweetSentiment2 / tweetWords));

    this.m_collector.ack(tuple);
  }
}
