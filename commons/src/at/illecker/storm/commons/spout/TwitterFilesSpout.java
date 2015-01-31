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
package at.illecker.storm.commons.spout;

import java.util.List;
import java.util.Map;

import twitter4j.Status;
import at.illecker.storm.commons.util.TimeUtils;
import at.illecker.storm.commons.util.io.JsonUtils;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class TwitterFilesSpout extends BaseRichSpout {
  public static final String ID = "twitter-files-spout";
  public static final String CONF_TWITTER_DIR = ID + ".twitter.dir";
  public static final String CONF_FILTER_LANGUAGE = ID + ".filter.language";
  public static final String CONF_STARTUP_SLEEP_MS = ID + ".startup.sleep.ms";
  public static final String CONF_TUPLE_SLEEP_MS = ID + ".tuple.sleep.ms";
  private static final long serialVersionUID = 1584431147989513848L;
  private SpoutOutputCollector m_collector;
  private List<Status> m_tweets;
  private int m_index = 0;
  private long m_tupleSleepMs = 0;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("id", "text", "score"));
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;

    String filterLanguage = null;
    if (config.get(CONF_FILTER_LANGUAGE) != null) {
      filterLanguage = (String) config.get(CONF_FILTER_LANGUAGE);
    }

    if (config.get(CONF_TWITTER_DIR) != null) {
      String twitterDirPath = config.get(CONF_TWITTER_DIR).toString();
      m_tweets = JsonUtils.readTweetsDirectory(twitterDirPath, filterLanguage);
    } else {
      throw new RuntimeException(CONF_TWITTER_DIR + " property was not set!");
    }

    // Optional sleep between tuples emitting
    if (config.get(CONF_TUPLE_SLEEP_MS) != null) {
      m_tupleSleepMs = (Long) config.get(CONF_TUPLE_SLEEP_MS);
    } else {
      m_tupleSleepMs = 0;
    }

    // Optional startup sleep to finish bolt preparation
    // before spout starts emitting
    if (config.get(CONF_STARTUP_SLEEP_MS) != null) {
      long startupSleepMillis = (Long) config.get(CONF_STARTUP_SLEEP_MS);
      TimeUtils.sleepMillis(startupSleepMillis);
    }
  }

  public void nextTuple() {
    Status tweet = m_tweets.get(m_index);

    // infinite loop
    m_index++;
    if (m_index >= m_tweets.size()) {
      m_index = 0;
    }

    // Emit tweet
    m_collector.emit(new Values(tweet.getId(), tweet.getText(), null));

    // Optional sleep between emitting tuples
    if (m_tupleSleepMs != 0) {
      TimeUtils.sleepMillis(m_tupleSleepMs);
    }
  }
}
