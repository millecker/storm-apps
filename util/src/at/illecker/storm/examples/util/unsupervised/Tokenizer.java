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
package at.illecker.storm.examples.util.unsupervised;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tokenizer {
  private static final Logger LOG = LoggerFactory.getLogger(Tokenizer.class);

  public static List<String> tokenize(String text) {
    List<String> tokens = new ArrayList<String>();
    // split at one ore more blanks
    String[] inputTokens = text.split("\\s+");
    for (String inputToken : inputTokens) {
      // LOG.info("inputToken : '" + inputToken + "'");
      tokens.add(inputToken);
    }
    return tokens;
  }

  public static void main(String[] args) {
    Tokenizer
        .tokenize("Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)");
  }
}
