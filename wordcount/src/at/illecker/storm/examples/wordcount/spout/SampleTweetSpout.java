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
package at.illecker.storm.examples.wordcount.spout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.illecker.storm.examples.util.tweet.Tweet;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class SampleTweetSpout extends BaseRichSpout {
  public static final String ID = "sample-tweet-spout";
  private static final long serialVersionUID = 3621927972989123163L;
  private String[] m_outputFields;
  private SpoutOutputCollector m_collector;
  private List<Tweet> m_tweets;
  private int m_index = 0;

  public SampleTweetSpout(String[] outputFields) {
    m_outputFields = outputFields;
    m_tweets = new ArrayList<Tweet>();
    m_tweets.add(new Tweet(1L, "this is the first tweet"));
    m_tweets.add(new Tweet(2L, "followed by a second tweet"));
    m_tweets.add(new Tweet(3L, "and a third tweet"));
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields(m_outputFields)); // key of output tuples
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;
  }

  public void nextTuple() {
    this.m_collector.emit(new Values(m_tweets.get(m_index)));
    m_index++;
    if (m_index >= m_tweets.size()) {
      m_index = 0;
    }
    try {
      Thread.sleep(1); // sleep 1 ms
    } catch (InterruptedException e) {
    }
  }
}
