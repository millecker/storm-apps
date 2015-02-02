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
package at.illecker.storm.commons.bolt;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.dict.SentimentDictionary;
import at.illecker.storm.commons.dict.SentimentResult;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import cmu.arktweetnlp.Tagger.TaggedToken;

public class SentimentDetectionBolt extends BaseRichBolt {
  public static final String ID = "sentiment-detection-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = 707679458760007989L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentDetectionBolt.class);
  private boolean m_logging = false;
  private OutputCollector m_collector;
  private SentimentDictionary m_sentimentDict;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // no output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    // Optional set logging
    if (config.get(CONF_LOGGING) != null) {
      m_logging = (Boolean) config.get(CONF_LOGGING);
    } else {
      m_logging = false;
    }
    m_sentimentDict = SentimentDictionary.getInstance();
  }

  public void cleanup() {
    m_sentimentDict.close();
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    String text = tuple.getStringByField("text");
    List<TaggedToken> taggedTokens = (List<TaggedToken>) tuple
        .getValueByField("taggedTokens");

    // Calculate sentiment
    Map<Integer, SentimentResult> tweetSentiments = m_sentimentDict
        .getSentenceSentimentFromTaggedToken(taggedTokens);

    double totalSentimentScore = Double.MIN_VALUE;
    if (tweetSentiments != null) {
      for (SentimentResult tweetSentiment : tweetSentiments.values()) {
        totalSentimentScore += tweetSentiment.getAvgSum();
      }
      totalSentimentScore /= tweetSentiments.size();
    }

    if (m_logging) {
      if (totalSentimentScore != Double.MIN_VALUE) {
        if (totalSentimentScore > SentimentResult.POSITIVE_THRESHOLD) {
          LOG.info("Tweet[" + tweetId + "]: " + text
              + " Sentiment: POSITIVE Score: " + totalSentimentScore);
        } else if (totalSentimentScore < SentimentResult.NEGATIVE_THRESHOLD) {
          LOG.info("Tweet[" + tweetId + "]: " + text
              + " Sentiment: NEGATIVE Score: " + totalSentimentScore);
        } else {
          LOG.info("Tweet[" + tweetId + "]: " + text
              + " Sentiment: NEUTAL Score: " + totalSentimentScore);
        }
      } else {
        LOG.info("Tweet[" + tweetId + "]: " + text + " Sentiment: UNKNOWN");
      }
    }

    this.m_collector.ack(tuple);
  }

}
