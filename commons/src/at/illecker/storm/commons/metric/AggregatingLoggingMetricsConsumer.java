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
package at.illecker.storm.commons.metric;

import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import backtype.storm.metric.api.IMetricsConsumer;
import backtype.storm.task.IErrorReporter;
import backtype.storm.task.TopologyContext;

public class AggregatingLoggingMetricsConsumer implements IMetricsConsumer {
  private static final Logger LOG = LoggerFactory
      .getLogger(AggregatingLoggingMetricsConsumer.class);
  private static String padding = "                       ";

  @Override
  public void prepare(Map stormConf, Object registrationArgument,
      TopologyContext context, IErrorReporter errorReporter) {
  }

  @Override
  public void cleanup() {
  }

  @Override
  public void handleDataPoints(TaskInfo taskInfo,
      Collection<DataPoint> dataPoints) {
    StringBuilder sb = new StringBuilder();
    String header = String.format("%d\t%15s:%-4d\t%3d:%-11s\t",
        taskInfo.timestamp, taskInfo.srcWorkerHost, taskInfo.srcWorkerPort,
        taskInfo.srcTaskId, taskInfo.srcComponentId);
    sb.append(header);

    for (DataPoint p : dataPoints) {
      sb.delete(header.length(), sb.length());
      sb.append(p.name).append(padding)
          .delete(header.length() + 23, sb.length()).append("\t")
          .append(p.value);

      LOG.info(sb.toString());
    }
  }

}
