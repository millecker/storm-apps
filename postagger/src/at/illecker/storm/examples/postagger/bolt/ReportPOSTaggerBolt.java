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

public class ReportPOSTaggerBolt extends BaseRichBolt {
  private static final long serialVersionUID = -735115120325956191L;
  private static final Logger LOG = LoggerFactory
      .getLogger(ReportPOSTaggerBolt.class);
  private OutputCollector m_collector;
  private int m_timerPeriod;

  public ReportPOSTaggerBolt(int timerPeriod) {
    m_timerPeriod = timerPeriod;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // this bolt does not emit anything
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    // Start ReportTimer in prepare method
    // because it has to be in the same JVM than the worker
    Timer timer = new Timer();
    timer.schedule(new ReportTask(), 500, this.m_timerPeriod);
  }

  public void execute(Tuple tuple) {
    LOG.info(tuple.toString());
  }

  class ReportTask extends TimerTask {
    @Override
    public void run() {
      LOG.info("\n\n\nPOSTagger: ");
    }
  }
}
