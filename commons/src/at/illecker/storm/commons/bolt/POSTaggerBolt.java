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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import cmu.arktweetnlp.Tagger.TaggedToken;
import cmu.arktweetnlp.impl.Model;
import cmu.arktweetnlp.impl.ModelSentence;
import cmu.arktweetnlp.impl.Sentence;
import cmu.arktweetnlp.impl.features.FeatureExtractor;

public class POSTaggerBolt extends BaseRichBolt {
  public static final String ID = "pos-tagger-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  public static final String CONF_MODEL = ID + ".model";
  private static final long serialVersionUID = 8389930087364663504L;
  private static final Logger LOG = LoggerFactory
      .getLogger(POSTaggerBolt.class);
  private boolean m_logging = false;
  private OutputCollector m_collector;

  private Model m_model;
  private FeatureExtractor m_featureExtractor;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("id", "score", "taggedTokens"));
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
    // Load ARK POS Tagger
    try {
      String taggingModel = Configuration
          .get("global.resources.postagger.ark.model.path");
      LOG.info("Load ARK POS Tagger with model: " + taggingModel);
      // TODO absolute path needed for resource
      if ((Configuration.RUNNING_WITHIN_JAR) && (!taggingModel.startsWith("/"))) {
        taggingModel = "/" + taggingModel;
      }
      m_model = Model.loadModelFromText(taggingModel);
      m_featureExtractor = new FeatureExtractor(m_model, false);
    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
    }
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    List<String> preprocessedTokens = (List<String>) tuple
        .getValueByField("preprocessedTokens");

    // POS Tagging
    Sentence sentence = new Sentence();
    sentence.tokens = preprocessedTokens;
    ModelSentence ms = new ModelSentence(sentence.T());
    m_featureExtractor.computeFeatures(sentence, ms);
    m_model.greedyDecode(ms, false);

    List<TaggedToken> taggedTokens = new ArrayList<TaggedToken>();
    for (int t = 0; t < sentence.T(); t++) {
      TaggedToken tt = new TaggedToken(preprocessedTokens.get(t),
          m_model.labelVocab.name(ms.labels[t]));
      taggedTokens.add(tt);
    }

    if (m_logging) {
      LOG.info("Tweet[" + tweetId + "]: " + taggedTokens);
    }

    // Emit new tuples
    this.m_collector.emit(tuple, new Values(tweetId, score, taggedTokens));
  }

}
