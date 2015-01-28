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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.TokenizedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TokenizerBolt extends BaseRichBolt {
  public static final String ID = "tokenizer-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = 7134328814020366549L;
  private static final Logger LOG = LoggerFactory
      .getLogger(TokenizerBolt.class);
  private boolean m_logging = false;
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;

  public TokenizerBolt(String[] inputFields, String[] outputFields) {
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
    // Optional set logging
    if (config.get(CONF_LOGGING) != null) {
      m_logging = (Boolean) config.get(CONF_LOGGING);
    } else {
      m_logging = false;
    }
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField(m_inputFields[0]);

    // Tokenize tweet text
    List<List<String>> sentences = new ArrayList<List<String>>();
    sentences.add(Tokenizer.tokenize(tweet.getText()));

    if (m_logging) {
      LOG.info("Tweet: \"" + tweet.getText() + "\" Tokenized: "
          + sentences.toString());
    }

    // Emit new immutable TokenizedTweet object
    this.m_collector.emit(tuple, new Values(new TokenizedTweet(tweet.getId(),
        tweet.getText(), tweet.getScore(), sentences)));
    this.m_collector.ack(tuple);
  }
}
