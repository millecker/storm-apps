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

import at.illecker.storm.examples.util.tweet.SentimentTweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class JsonTweetExtractorBolt extends BaseRichBolt {
  public static final String ID = "json-tweet-extractor-bolt";
  private static final long serialVersionUID = -7467317303659214525L;
  private static final Logger LOG = LoggerFactory
      .getLogger(JsonTweetExtractorBolt.class);
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;

  public JsonTweetExtractorBolt(String[] inputFields, String[] outputFields) {
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
  }

  public void execute(Tuple tuple) {
    Map<String, Object> element = (Map<String, Object>) tuple
        .getValueByField(m_inputFields[0]);

    SentimentTweet tweet = SentimentTweet.fromJsonElement(element);
    // LOG.info(tweet.toString());

    this.m_collector.emit(tuple, new Values(tweet));
    this.m_collector.ack(tuple);
  }
}
