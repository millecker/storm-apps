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

import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.TimeUtils;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class DatasetSpout extends BaseRichSpout {
  public static final String ID = "dataset-spout";
  private static final long serialVersionUID = -7033277532867702166L;
  private String[] m_outputFields;
  private Dataset m_dataset;
  private SpoutOutputCollector m_collector;
  private List<Tweet> m_tweets;
  private int m_index = 0;

  public DatasetSpout(String[] outputFields, Dataset dataset) {
    this.m_outputFields = outputFields;
    this.m_dataset = dataset;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields(m_outputFields));
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;
    this.m_tweets = m_dataset.getTestTweets();
    TimeUtils.sleepMillis(20000);
  }

  public void nextTuple() {
    Tweet tweet = m_tweets.get(m_index);
    m_index++;
    if (m_index >= m_tweets.size()) {
      m_index = 0;
    }
    // Emit tweet
    m_collector.emit(new Values(tweet));
    // Utils.sleepMillis(1); // 1 ms
    // Utils.sleepNanos(500000);// 500000 ns - 0.5 ms
  }
}
