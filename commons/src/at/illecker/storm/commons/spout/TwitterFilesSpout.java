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
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.io.JsonUtils;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class TwitterFilesSpout extends BaseRichSpout {
  public static final String ID = "twitter-files-spout";
  public static final String CONF_TWITTER_DIR = "twitter.dir";
  private static final long serialVersionUID = -4277696098291748609L;
  private String[] m_outputFields;
  private SpoutOutputCollector m_collector;
  private List<Status> m_tweets;
  private int m_index = 0;
  private String m_filterLanguage;

  public TwitterFilesSpout(String[] outputFields, String filterLanguage) {
    this.m_outputFields = outputFields;
    this.m_filterLanguage = filterLanguage; // "en"
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields(m_outputFields));
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;

    if (config.get(CONF_TWITTER_DIR) != null) {
      String twitterDirPath = config.get(CONF_TWITTER_DIR).toString();
      m_tweets = JsonUtils
          .readTweetsDirectory(twitterDirPath, m_filterLanguage);
    } else {
      throw new RuntimeException(CONF_TWITTER_DIR + " property was not set!");
    }
  }

  public void nextTuple() {
    Status tweet = m_tweets.get(m_index);
    m_index++;
    if (m_index >= m_tweets.size()) {
      m_index = 0;
    }
    // Emit tweet
    m_collector.emit(new Values(new Tweet(tweet.getId(), tweet.getText())));
    // TODO minimize sleep time
    // default sleep 1 ms
    Utils.sleep(500); // for development
  }
}
