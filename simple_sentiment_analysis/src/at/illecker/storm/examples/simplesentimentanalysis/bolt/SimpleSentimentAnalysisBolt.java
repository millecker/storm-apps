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
package at.illecker.storm.examples.simplesentimentanalysis.bolt;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * Simple Sentiment Analysis Bolt is based on AFINN from Finn Årup Nielsen
 *
 * http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010
 * 
 */
public class SimpleSentimentAnalysisBolt extends BaseRichBolt {
  private static final long serialVersionUID = -5959679287318116521L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SimpleSentimentAnalysisBolt.class);
  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;

    if (config.get("afinn.sentiment.file") != null) {
      String model = config.get("afinn.sentiment.file").toString();
      // Load AFINN word ratings
      // TODO
    } else {
      throw new RuntimeException("afinn.sentiment.file property was not set!");
    }
  }

  public void execute(Tuple tuple) {
    String word = tuple.getStringByField("word");
    int rating = 0;
    LOG.info("word: " + word + " rating: " + rating);
    this.m_collector.ack(tuple);
  }
}
