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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tweet.PreprocessedTweet;
import at.illecker.storm.examples.util.tweet.TaggedTweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import edu.stanford.nlp.ling.TaggedWord;

public class POSTaggerBolt extends BaseRichBolt {
  public static final String ID = "pos-tagger-bolt";
  private static final long serialVersionUID = -2931810659942708343L;
  private static final Logger LOG = LoggerFactory
      .getLogger(POSTaggerBolt.class);
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;
  private POSTagger m_posTagger;

  public POSTaggerBolt(String inputFields[], String outputFields[]) {
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
    this.m_posTagger = POSTagger.getInstance();
  }

  public void execute(Tuple tuple) {
    PreprocessedTweet tweet = (PreprocessedTweet) tuple
        .getValueByField(m_inputFields[0]);
    // LOG.info(tweet.toString());

    List<List<TaggedWord>> taggedSentences = new ArrayList<List<TaggedWord>>();
    for (List<String> sentence : tweet.getPreprocessedSentences()) {
      taggedSentences.add(m_posTagger.tagSentence(sentence));
    }

    this.m_collector.emit(tuple, new Values(new TaggedTweet(tweet.getId(),
        tweet.getText(), tweet.getScore(), taggedSentences)));
    this.m_collector.ack(tuple);
  }
}
