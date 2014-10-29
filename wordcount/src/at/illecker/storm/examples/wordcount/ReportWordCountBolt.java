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
package at.illecker.storm.examples.wordcount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class ReportWordCountBolt extends BaseRichBolt {
  private static final long serialVersionUID = -5143042721802719313L;
  private static final Logger LOG = LoggerFactory
      .getLogger(ReportWordCountBolt.class);
  private OutputCollector m_collector;
  private Map<String, Long> m_counts;

  public ReportWordCountBolt(int period) {
    m_counts = new HashMap<String, Long>();
    // Start ReportTimer
    Timer timer = new Timer();
    timer.schedule(new ReportTask(), 0, period);
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // this bolt does not emit anything
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    // LOG.info(tuple.toString());
    String word = tuple.getStringByField("word");
    Long count = tuple.getLongByField("count");
    this.m_counts.put(word, count);
    this.m_collector.ack(tuple);
    // LOG.info("WordCounts: " + m_counts.size());
  }

  class ReportTask extends TimerTask {
    @Override
    public void run() {
      LOG.info("WordCounts: " + m_counts.size());
      if (m_counts.size() > 0) {
        // Sort words
        List<String> keys = new ArrayList<String>();
        keys.addAll(m_counts.keySet());
        Collections.sort(keys);
        // Print counts
        for (String key : keys) {
          LOG.info(key + " : " + m_counts.get(key));
        }
      }
    }
  }
}
