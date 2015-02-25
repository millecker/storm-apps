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

import at.illecker.storm.commons.dict.FirstNames;
import at.illecker.storm.commons.dict.Interjections;
import at.illecker.storm.commons.dict.NameEntities;
import at.illecker.storm.commons.dict.SlangCorrection;
import at.illecker.storm.commons.util.StringUtils;
import at.illecker.storm.commons.wordnet.WordNet;
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

  private WordNet m_wordnet;
  private SlangCorrection m_slangCorrection;
  private FirstNames m_firstNames;
  private NameEntities m_nameEntities;
  private Interjections m_interjections;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("id", "score", "preprocessedTokens"));
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
    // Load WordNet
    m_wordnet = WordNet.getInstance();
    // Load Slang correction
    m_slangCorrection = SlangCorrection.getInstance();
    // Load FirstNames
    m_firstNames = FirstNames.getInstance();
    // Load NameEntities
    m_nameEntities = NameEntities.getInstance();
    // Load Interjections
    m_interjections = Interjections.getInstance();
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    List<String> tokens = (List<String>) tuple.getValueByField("tokens");

    // Preprocess
    List<String> preprocessedTokens = new ArrayList<String>();
    for (String token : tokens) {
      preprocessedTokens.add(preprocess(token));
    }

    if (m_logging) {
      LOG.info("Tweet[" + tweetId + "]: " + preprocessedTokens);
    }

    // Emit new tuples
    this.m_collector
        .emit(tuple, new Values(tweetId, score, preprocessedTokens));
    this.m_collector.ack(tuple);
  }

  private String preprocess(String token) {
    // identify token
    boolean tokenContainsPunctuation = StringUtils.consitsOfPunctuations(token);
    boolean tokenIsEmoticon = StringUtils.isEmoticon(token);
    boolean tokenIsURL = StringUtils.isURL(token);
    boolean tokenIsNumeric = StringUtils.isNumeric(token);

    // TODO
    return token;
  }
}
