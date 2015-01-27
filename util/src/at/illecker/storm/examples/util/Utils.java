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
package at.illecker.storm.examples.util;

import backtype.storm.utils.Time;

public class Utils {

  public static void sleepMillis(long millis) {
    try {
      Time.sleep(millis);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  // 1 ms = 1 000 000 ns
  public static void sleepNanos(long nanos) {
    long start = System.nanoTime();
    long end = 0;
    do {
      end = System.nanoTime();
    } while (start + nanos >= end);
    // System.out.println("waited time: " + (end - start) + " ns");
  }
}
