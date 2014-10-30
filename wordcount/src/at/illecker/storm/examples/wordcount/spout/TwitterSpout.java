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

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import backtype.storm.Config;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class TwitterSpout extends BaseRichSpout {
  private static final long serialVersionUID = -6034380175814078633L;
  private SpoutOutputCollector m_collector;
  private LinkedBlockingQueue<Status> m_queue = null;
  private TwitterStream m_twitterStream;
  private String m_consumerKey;
  private String m_consumerSecret;
  private String m_accessToken;
  private String m_accessTokenSecret;
  private String[] m_keyWords;

  public TwitterSpout(String consumerKey, String consumerSecret,
      String accessToken, String accessTokenSecret, String[] keyWords) {
    this.m_consumerKey = consumerKey;
    this.m_consumerSecret = consumerSecret;
    this.m_accessToken = accessToken;
    this.m_accessTokenSecret = accessTokenSecret;
    this.m_keyWords = keyWords;
  }

  @Override
  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("tweet"));
  }
  
  @Override
  public void open(Map conf, TopologyContext context,
      SpoutOutputCollector collector) {
    m_collector = collector;
    m_queue = new LinkedBlockingQueue<Status>(1000);

    StatusListener listener = new StatusListener() {
      @Override
      public void onStatus(Status status) {
        queue.offer(status);
      }

      @Override
      public void onDeletionNotice(StatusDeletionNotice sdn) {
      }

      @Override
      public void onTrackLimitationNotice(int i) {
      }

      @Override
      public void onScrubGeo(long l, long l1) {
      }

      @Override
      public void onException(Exception ex) {
      }

      @Override
      public void onStallWarning(StallWarning arg0) {
      }
    };

    TwitterStream twitterStream = new TwitterStreamFactory(
        new ConfigurationBuilder().setJSONStoreEnabled(true).build())
        .getInstance();

    twitterStream.addListener(listener);
    twitterStream.setOAuthConsumer(consumerKey, consumerSecret);
    AccessToken token = new AccessToken(accessToken, accessTokenSecret);
    twitterStream.setOAuthAccessToken(token);

    if (keyWords.length == 0) {
      twitterStream.sample();
    } else {
      FilterQuery query = new FilterQuery().track(keyWords);
      twitterStream.filter(query);
    }
  }

  @Override
  public void nextTuple() {
    Status ret = m_queue.poll();
    if (ret == null) {
      Utils.sleep(50);
    } else {
      m_collector.emit(new Values(ret));
    }
  }

  @Override
  public void close() {
    m_twitterStream.shutdown();
  }

  @Override
  public Map<String, Object> getComponentConfiguration() {
    Config ret = new Config();
    ret.setMaxTaskParallelism(1);
    return ret;
  }

  @Override
  public void ack(Object id) {
  }

  @Override
  public void fail(Object id) {
  }
}
