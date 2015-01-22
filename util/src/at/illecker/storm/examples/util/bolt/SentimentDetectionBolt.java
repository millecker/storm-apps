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
package at.illecker.storm.examples.util.bolt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.dictionaries.SentimentResult;
import at.illecker.storm.examples.util.dictionaries.SentimentWordLists;
import at.illecker.storm.examples.util.tweet.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SentimentDetectionBolt extends BaseRichBolt {
  public static final String ID = "sentiment-detection-bolt";
  private static final long serialVersionUID = -3279220626656829348L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentDetectionBolt.class);
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;
  private SentimentWordLists m_sentimentWordLists;

  public SentimentDetectionBolt(String[] inputFields, String[] outputFields) {
    this.m_inputFields = inputFields;
    this.m_outputFields = outputFields;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    if (m_outputFields != null) {
      declarer.declare(new Fields(m_outputFields));
    }
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
    Tweet tweet = (Tweet) tuple.getValueByField(m_inputFields[0]);
    // LOG.info(tweet.toString());

    Map<Integer, SentimentResult> tweetSentiments = m_sentimentWordLists
        .getTweetSentiment(tweet);

    double totalSentimentScore = Double.MIN_VALUE;
    if (tweetSentiments != null) {
      for (SentimentResult tweetSentiment : tweetSentiments.values()) {
        totalSentimentScore += tweetSentiment.getAvgSum();
      }
      totalSentimentScore /= tweetSentiments.size();
    }

    // Debug
    if (totalSentimentScore != Double.MIN_VALUE) {
      if (totalSentimentScore > SentimentResult.POSITIVE_THRESHOLD) {
        LOG.info("Tweet: " + tweet.toString() + " Sentiment: POSITIVE Score: "
            + totalSentimentScore);
      } else if (totalSentimentScore < SentimentResult.NEGATIVE_THRESHOLD) {
        LOG.info("Tweet: " + tweet.toString() + " Sentiment: NEGATIVE Score: "
            + totalSentimentScore);
      } else {
        LOG.info("Tweet: " + tweet.toString() + " Sentiment: NEUTAL Score: "
            + totalSentimentScore);
      }
    } else {
      LOG.info("Tweet: " + tweet.toString() + " Sentiment: UNKNOWN");
    }

    this.m_collector.ack(tuple);
  }
}
