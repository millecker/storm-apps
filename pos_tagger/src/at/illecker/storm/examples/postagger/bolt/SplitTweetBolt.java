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
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;

public class SplitTweetBolt extends BaseRichBolt {
  private static final long serialVersionUID = 7115509761367150035L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SplitTweetBolt.class);
  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("sentence")); // key of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    Status status = (Status) tuple.getValueByField("tweet");
    String tweetText = status.getText();
    // LOG.info("@" + status.getUser().getScreenName() + " - " + tweetText);

    // Cleanup Tweet
    // \\n - newlines
    Pattern pattern = Pattern.compile("\\n");
    tweetText = pattern.matcher(tweetText).replaceAll(" ");

    List<HasWord> sentence = Sentence.toWordList(tweetText.split(" "));
    this.m_collector.emit(new Values(sentence));
    this.m_collector.ack(tuple);
  }
}
