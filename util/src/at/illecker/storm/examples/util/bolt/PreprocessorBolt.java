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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tweet.PreprocessedTweet;
import at.illecker.storm.examples.util.tweet.TokenizedTweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PreprocessorBolt extends BaseRichBolt {
  public static final String ID = "preprocessor-bolt";
  private static final long serialVersionUID = 5767153574646034298L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PreprocessorBolt.class);
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;
  private Preprocessor m_preprocessor;

  public PreprocessorBolt(String[] inputFields, String[] outputFields) {
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
    this.m_preprocessor = Preprocessor.getInstance();
  }

  public void execute(Tuple tuple) {
    TokenizedTweet tweet = (TokenizedTweet) tuple
        .getValueByField(m_inputFields[0]);
    // LOG.info(tweet.toString());

    List<List<String>> preprocessedSentences = new ArrayList<List<String>>();
    for (List<String> sentence : tweet.getSentences()) {
      preprocessedSentences.add(m_preprocessor.preprocess(sentence));
    }

    this.m_collector.emit(tuple, new Values(
        new PreprocessedTweet(tweet.getId(), tweet.getText(), tweet.getScore(),
            preprocessedSentences)));
    this.m_collector.ack(tuple);
  }
}
