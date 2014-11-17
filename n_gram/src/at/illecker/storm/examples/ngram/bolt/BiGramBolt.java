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
package at.illecker.storm.examples.ngram.bolt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import at.illecker.storm.examples.ngram.NGram;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class BiGramBolt extends BaseRichBolt {
  private static final long serialVersionUID = 7554959448481890289L;
  private static final Logger LOG = LoggerFactory.getLogger(BiGramBolt.class);

  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("bigrams")); // key of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    Status status = (Status) tuple.getValueByField("tweet");
    String tweetText = status.getText();
    LOG.info("User: " + status.getUser().getScreenName() + " Tweet: "
        + tweetText);

    // Cleanup Tweet (remove @user #hashtag RT)
    // \\n - newlines
    // @\\w* - @users
    // #\\w* - hashtags
    // \\bRT\\b - Retweets RT
    // \\p{Punct} - removes smileys
    // [^@#\\p{L}\\p{N} ]+
    // URL
    // (https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?
    // -
    Pattern pattern = Pattern
        .compile("\\n|@\\w*|#\\w*|\\bRT\\b|"
            + "(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?");
    String cleanupTweet = pattern.matcher(tweetText).replaceAll(" ");

    // Tweet to lower case
    cleanupTweet = cleanupTweet.toLowerCase();
    LOG.info("Tweet: " + cleanupTweet);

    String[] words = cleanupTweet.split(" ");

    // Bigrams
    List<String[]> bigrams = NGram.getBigrams(words);
    for (int i = 0; i < bigrams.size(); i++) {
      LOG.info("bigram[" + i + "]: " + Arrays.toString(bigrams.get(i)));
    }

    // Trigrams
    List<String[]> trigrams = NGram.getTrigrams(words);
    for (int i = 0; i < trigrams.size(); i++) {
      LOG.info("trigrams[" + i + "]: " + Arrays.toString(trigrams.get(i)));
    }

    this.m_collector.emit(new Values(bigrams));
    this.m_collector.ack(tuple);
  }
}
