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

import at.illecker.storm.commons.preprocessor.Preprocessor;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PreprocessorBolt extends BaseRichBolt {
  public static final String ID = "preprocessor-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = -1623010654971791418L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PreprocessorBolt.class);
  private boolean m_logging = false;
  private OutputCollector m_collector;
  private Preprocessor m_preprocessor;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("id", "score", "text", "preprocessedTokens"));
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
    m_preprocessor = Preprocessor.getInstance();
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    String text = tuple.getStringByField("text");
    List<String> tokens = (List<String>) tuple.getValueByField("tokens");

    // Preprocess
    List<String> preprocessedTokens = m_preprocessor.preprocess(tokens);

    if (m_logging) {
      LOG.info("Tweet[" + tweetId + "]: \"" + text + "\" Preprocessed: "
          + preprocessedTokens);
    }

    // Emit new tuples
    this.m_collector.emit(tuple, new Values(tweetId, score, text,
        preprocessedTokens));
    this.m_collector.ack(tuple);
  }

}
