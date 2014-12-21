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

import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
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
  private static final long serialVersionUID = 8507565084136299042L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PolarityDetectionBolt.class);

  private OutputCollector m_collector;
  private SentimentWordLists m_sentimentWordLists;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // no output
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    this.m_sentimentWordLists = SentimentWordLists.getInstance();
  }

  public void cleanup() {
    m_sentimentWordLists.close();
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("taggedTweet");
    // LOG.info(tweet.toString());

    double tweetSentiment = 0;
    int tweetWords = 0;
    for (List<TaggedWord> taggedSentence : tweet.getTaggedSentences()) {
      // LOG.info("TaggedSentence: " + taggedSentence.toString());

      String sentimentSentenceString = "";
      double sentenceSentiment = 0;
      int sentenceWords = 0;
      for (TaggedWord taggedWord : taggedSentence) {

        String word = taggedWord.word().toLowerCase().trim();
        String tag = taggedWord.tag();

        // See tags http://www.clips.ua.ac.be/pages/mbsp-tags
        // Skip punctuations
        if (tag.equals(".") || tag.equals(",") || tag.equals(":")) {
          continue;
        }
        // Skip cardinal numbers and symbols
        if (tag.equals("CD") || tag.equals("SYM")) {
          continue;
        }

        sentenceWords++;

        Double rating = m_sentimentWordLists.getWordSentimentWithStemming(word,
            tag);
        // Update sentiment sum
        if (rating != null) {
          sentenceSentiment += rating;
        }

        sentimentSentenceString += word + "/"
            + ((rating != null) ? rating : "NA");
      }
      tweetWords += sentenceWords;
      tweetSentiment += sentenceSentiment;

      // Debug
      LOG.info("TaggedSentence: " + taggedSentence.toString()
          + " SentimentSentence: " + sentimentSentenceString + " Words: "
          + sentenceWords + " SentenceSentiment: " + sentenceSentiment
          + " SentimentScore: " + (sentenceSentiment / sentenceWords));
    }

    // Debug
    LOG.info("Tweet: " + tweet.toString() + " Words: " + tweetWords
        + " TweetSentiment1: " + tweetSentiment + " SentimentScore: "
        + (tweetSentiment / tweetWords));

    this.m_collector.ack(tuple);
  }
}
