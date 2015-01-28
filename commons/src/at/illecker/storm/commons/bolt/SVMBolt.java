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

import libsvm.svm_model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.svm.SVM;
import at.illecker.storm.commons.svm.scoreclassifier.IdentityScoreClassifier;
import at.illecker.storm.commons.svm.scoreclassifier.ScoreClassifier;
import at.illecker.storm.commons.tweet.FeaturedTweet;
import at.illecker.storm.commons.util.io.SerializationUtils;
import backtype.storm.metric.api.CountMetric;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SVMBolt extends BaseRichBolt {
  public static final String ID = "support-vector-maschine-bolt";
  private static final long serialVersionUID = -3235291265771813064L;
  private static final Logger LOG = LoggerFactory.getLogger(SVMBolt.class);
  private String[] m_inputFields;
  private String[] m_outputFields;
  private Dataset m_dataset;
  private OutputCollector m_collector;
  private int m_totalClasses;
  private ScoreClassifier m_classifier;
  private svm_model m_model;
  // Metrics
  // Note: these must be declared as transient since they are not Serializable
  transient CountMetric m_countMetric;

  public SVMBolt(String[] inputFields, String[] outputFields, Dataset dataset) {
    this.m_inputFields = inputFields;
    this.m_outputFields = outputFields;
    this.m_dataset = dataset;
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

    m_countMetric = new CountMetric();
    context.registerMetric("tuple_count", m_countMetric, 10);

    m_totalClasses = 3;
    m_classifier = new IdentityScoreClassifier();

    LOG.info("Loading SVM model...");
    m_model = SerializationUtils.deserialize(m_dataset.getDatasetPath()
        + File.separator + SVM.SVM_MODEL_FILE_SER);

    if (m_model == null) {
      LOG.error("Could not load SVM model! File: " + m_dataset.getDatasetPath()
          + File.separator + SVM.SVM_MODEL_FILE_SER);
    }
  }

  public void execute(Tuple tuple) {
    FeaturedTweet tweet = (FeaturedTweet) tuple
        .getValueByField(m_inputFields[0]);

    double predictedClass = SVM.evaluate(tweet, m_model, m_totalClasses,
        m_classifier);

    // LOG.info("Tweet: \"" + tweet.getText() + "\" score: " + tweet.getScore()
    // + " expectedClass: " + m_classifier.classfyScore(tweet.getScore())
    // + " predictedClass: " + predictedClass);

    m_countMetric.incr();
    m_collector.ack(tuple);
  }
}
