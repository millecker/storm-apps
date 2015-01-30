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

import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.svm.featurevector.CombinedFeatureVectorGenerator;
import at.illecker.storm.commons.svm.featurevector.FeatureVectorGenerator;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tweet.TaggedTweet;
import at.illecker.storm.commons.util.io.SerializationUtils;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import edu.stanford.nlp.ling.TaggedWord;

public class FeatureGenerationBolt extends BaseRichBolt {
  public static final String ID = "feature-generation-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = 8704674836362723368L;
  private static final Logger LOG = LoggerFactory
      .getLogger(FeatureGenerationBolt.class);
  private boolean m_logging = false;
  private Dataset m_dataset;
  private OutputCollector m_collector;
  private FeatureVectorGenerator m_fvg = null;

  public FeatureGenerationBolt(Dataset dataset) {
    this.m_dataset = dataset;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("id", "score", "text", "featureVector"));
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

    // TODO LOAD FULL CombinedFeatureVectorGenerator

    List<TaggedTweet> taggedTweets = SerializationUtils.deserialize(m_dataset
        .getTrainTaggedDataSerializationFile());
    if (taggedTweets != null) {
      TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTweets, TfType.RAW,
          TfIdfNormalization.COS, true);
      LOG.info("Load CombinedFeatureVectorGenerator...");
      m_fvg = new CombinedFeatureVectorGenerator(tweetTfIdf);
      LOG.info("CombinedFeatureVectorGenerator loaded");
    } else {
      LOG.error("TaggedTweets could not be found! File is missing: "
          + m_dataset.getTrainTaggedDataSerializationFile());
    }
  }

  public void execute(Tuple tuple) {
    Long tweetId = tuple.getLongByField("id");
    Double score = tuple.getDoubleByField("score");
    String text = tuple.getStringByField("text");
    List<TaggedWord> taggedTokens = (List<TaggedWord>) tuple
        .getValueByField("taggedTokens");

    // Generate Feature Vector
    Map<Integer, Double> featureVector = m_fvg
        .calculateFeatureVector(taggedTokens);

    if (m_logging) {
      LOG.info("Tweet[" + tweetId + "]: \"" + text + "\" FeatureVector: "
          + featureVector);
    }

    // Emit new tuples
    this.m_collector.emit(tuple,
        new Values(tweetId, score, text, featureVector));
    this.m_collector.ack(tuple);
  }

}
