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

import libsvm.svm_model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.svm.SVM;
import at.illecker.storm.examples.util.svm.classifier.DynamicScoreClassifier;
import at.illecker.storm.examples.util.tweet.FeaturedTweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;

public class SVMBolt extends BaseRichBolt {
  public static final String ID = "support-vector-maschine-bolt";
  private static final long serialVersionUID = -18278802726186268L;
  private static final Logger LOG = LoggerFactory.getLogger(SVMBolt.class);
  private static final boolean LOGGING = true;
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;
  private int m_totalClasses;
  private DynamicScoreClassifier m_dsc;
  private svm_model m_model;

  public SVMBolt(String[] inputFields, String[] outputFields) {
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

    // TODO load model instead of training...
    LOG.info("Train model...");
    // m_model = SVM.train(svmProb, svmParam);
  }

  public void execute(Tuple tuple) {
    FeaturedTweet tweet = (FeaturedTweet) tuple
        .getValueByField(m_inputFields[0]);

    double predictedClass = SVM.evaluate(tweet, m_model, m_totalClasses, m_dsc);

    LOG.info("Tweet: \"" + tweet.getText() + "\" score: " + tweet.getScore()
        + " expectedClass: " + m_dsc.classfyScore(tweet.getScore())
        + " predictedClass: " + predictedClass);

    String featureVectorStr = "";
    for (Map.Entry<Integer, Double> feature : tweet.getFeatureVector()
        .entrySet()) {
      featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
    }
    LOG.info("FeatureVector: " + featureVectorStr);

    this.m_collector.ack(tuple);
  }
}
