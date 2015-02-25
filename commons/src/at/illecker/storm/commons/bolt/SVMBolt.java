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

import java.io.File;
import java.util.Map;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.svm.SVM;
import at.illecker.storm.commons.util.io.SerializationUtils;
import backtype.storm.metric.api.CountMetric;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class SVMBolt extends BaseRichBolt {
  public static final String ID = "support-vector-maschine-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  public static final String CONF_METRIC_LOGGING_INTERVALL = ID
      + ".metric.logging.intervall";
  private static final long serialVersionUID = -3235291265771813064L;
  private static final Logger LOG = LoggerFactory.getLogger(SVMBolt.class);
  private boolean m_logging = false;
  private Dataset m_dataset;
  private OutputCollector m_collector;
  private svm_model m_model;
  // Metrics
  // Note: these must be declared as transient since they are not Serializable
  transient CountMetric m_countMetric = null;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // no output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    this.m_dataset = Configuration.getDataSetSemEval2013();

    // Optional set logging
    if (config.get(CONF_LOGGING) != null) {
      m_logging = (Boolean) config.get(CONF_LOGGING);
    } else {
      m_logging = false;
    }

    // Tuple counter metric
    if (config.get(CONF_METRIC_LOGGING_INTERVALL) != null) {
      m_countMetric = new CountMetric();
      context.registerMetric("tuple_count", m_countMetric,
          ((Long) config.get(CONF_METRIC_LOGGING_INTERVALL)).intValue());
    }

    LOG.info("Loading SVM model...");
    m_model = SerializationUtils.deserialize(m_dataset.getDatasetPath()
        + File.separator + SVM.SVM_MODEL_FILE_SER);

    if (m_model == null) {
      LOG.error("Could not load SVM model! File: " + m_dataset.getDatasetPath()
          + File.separator + SVM.SVM_MODEL_FILE_SER);
      throw new RuntimeException();
    }
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    Map<Integer, Double> featureVector = (Map<Integer, Double>) tuple
        .getValueByField("featureVector");

    // create feature nodes
    svm_node[] testNodes = new svm_node[featureVector.size()];
    int i = 0;
    for (Map.Entry<Integer, Double> feature : featureVector.entrySet()) {
      svm_node node = new svm_node();
      node.index = feature.getKey();
      node.value = feature.getValue();
      testNodes[i] = node;
      i++;
    }

    double predictedClass = svm.svm_predict(m_model, testNodes);

    if (m_logging) {
      LOG.info("Tweet[" + tweetId + "]:  score: " + score + " predictedClass: "
          + predictedClass);
    }

    if (m_countMetric != null) {
      m_countMetric.incr();
    }
  }

}
