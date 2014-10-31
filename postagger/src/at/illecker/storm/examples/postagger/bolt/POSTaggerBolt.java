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
package at.illecker.storm.examples.postagger.bolt;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * POS Tagger Bolt is based on the Stanford NLP library and the
 * gate-EN-twitter.model
 *
 * http://nlp.stanford.edu/software/corenlp.shtml
 * http://nlp.stanford.edu:8080/corenlp/process
 * https://gate.ac.uk/wiki/twitter-postagger.html
 * 
 */
public class POSTaggerBolt extends BaseRichBolt {
  private static final long serialVersionUID = -8171288984418423575L;
  private static final Logger LOG = LoggerFactory
      .getLogger(POSTaggerBolt.class);
  private OutputCollector m_collector;
  private MaxentTagger m_tagger;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;

    if (config.get("tagger.model") != null) {
      String model = config.get("tagger.model").toString();
      // Load tagger and model
      m_tagger = new MaxentTagger(model);
    } else {
      throw new RuntimeException("tagger.model property was not set!");
    }
  }

  public void execute(Tuple tuple) {
    List<HasWord> sentence = (List<HasWord>) tuple.getValueByField("sentence");
    List<TaggedWord> taggedSentence = m_tagger.tagSentence(sentence);
    LOG.info("Tweet: " + sentence.toString() + " TaggedTweet: "
        + taggedSentence.toString());
    this.m_collector.ack(tuple);
  }
}
