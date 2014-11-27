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
package at.illecker.storm.examples.sentimentanalysis.bolt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.sentimentanalysis.util.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TweetFeatureExtractorBolt extends BaseRichBolt {
  private static final long serialVersionUID = -8934114541268126264L;
  private static final Logger LOG = LoggerFactory
      .getLogger(TweetFeatureExtractorBolt.class);

  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("tweet")); // key of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    Map<String, Object> element = (Map<String, Object>) tuple
        .getValueByField("jsonElement");

    long id = Long.parseLong((String) element.get("id"));
    double score_amt = (Double) element.get("score_amt");
    double score_amt2 = (Double) element.get("score_amt_wrong");
    double score_mislove = (Double) element.get("score_mislove");
    double score_mislove2 = (Double) element.get("score_mislove2");
    double score_afinn = (Double) element.get("sentiment_afinn");
    double score_sentistrength = (Double) element.get("sentistrength");
    double score_sentistrength_pos = (Double) element
        .get("sentistrength_positive");
    double score_sentistrength_neg = (Double) element
        .get("sentistrength_negative");
    // sentiment_afinn_nonzero=-2.0,
    // sentiment_afinn_quant=-1.0,
    // sentiment_afinn_extreme=-2.0,
    // sentiment_afinn_sum=-4.0

    if ((score_amt != score_amt2) || (score_mislove != score_mislove2)) {
      LOG.error("Inconsistency: " + element.toString());
    }

    Tweet tweet = new Tweet(id, (String) element.get("text"), score_amt,
        score_mislove, score_afinn, score_sentistrength,
        score_sentistrength_pos, score_sentistrength_neg);
    // LOG.info(tweet.toString());

    this.m_collector.emit(tuple, new Values(tweet));
    this.m_collector.ack(tuple);
  }
}
