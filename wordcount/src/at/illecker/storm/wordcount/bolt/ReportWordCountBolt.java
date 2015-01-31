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
package at.illecker.storm.wordcount.bolt;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
  public static final String ID = "report-wordcount-bolt";
  private static final long serialVersionUID = -5143042721802719313L;
  private static final Logger LOG = LoggerFactory
      .getLogger(ReportWordCountBolt.class);
  private OutputCollector m_collector;
  private Map<String, Long> m_counts;
  private int m_timerPeriod;

  public ReportWordCountBolt(int timerPeriod) {
    m_timerPeriod = timerPeriod;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // no output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    this.m_counts = new HashMap<String, Long>();
    // Start ReportTimer in prepare method
    // because it has to be in the same JVM than the worker
    Timer timer = new Timer();
    timer.schedule(new ReportTask(), 500, this.m_timerPeriod);
  }

  public void execute(Tuple tuple) {
    // LOG.info(tuple.toString());
    // LOG.info("WordCounts: " + m_counts.size());
    String word = tuple.getStringByField("word");
    Long count = tuple.getLongByField("count");
    this.m_counts.put(word, count);
    this.m_collector.ack(tuple);
  }

  class ReportTask extends TimerTask {
    @Override
    public void run() {
      LOG.info("\n\n\nWordCounts: " + m_counts.size());
      if (m_counts.size() > 0) {
        // Sort by word count
        Map<String, Long> sortedMap = sortByValues(m_counts);
        // Print counts
        for (Map.Entry<String, Long> entry : sortedMap.entrySet()) {
          LOG.info(entry.getKey() + " : " + entry.getValue());
        }
      }
    }
  }

  public static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(
      Map<K, V> map) {

    List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
        map.entrySet());

    Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
      @Override
      public int compare(Entry<K, V> o1, Entry<K, V> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });

    // LinkedHashMap will keep the keys in the order they are inserted
    // which is currently sorted on natural ordering
    Map<K, V> sortedMap = new LinkedHashMap<K, V>();
    for (Map.Entry<K, V> entry : entries) {
      sortedMap.put(entry.getKey(), entry.getValue());
    }

    return sortedMap;
  }

}
