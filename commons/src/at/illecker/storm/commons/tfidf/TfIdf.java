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
package at.illecker.storm.commons.tfidf;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.storm.guava.collect.ImmutableSet;
import org.apache.storm.guava.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Term Frequency - Inverse Document Frequency
 *
 */
public class TfIdf {
  private static final Logger LOG = LoggerFactory.getLogger(TfIdf.class);
  private static final boolean LOGGING = false;

  public static <T> Map<T, Double> tf(Collection<T> document) {
    return tf(new HashMap<T, Double>(), document);
  }

  public static <T> Map<T, Double> tf(Map<T, Double> termFreq,
      Collection<T> terms) {
    // compute term frequency
    for (T term : terms) {
      Double v = termFreq.get(term);
      termFreq.put(term, (v == null) ? 1d : v + 1);
    }
    return termFreq;
  }

  public static <T> Map<T, Double> normalizeTf(Map<T, Double> termFreq,
      TfType type) {
    // normalize term frequency
    if (type != TfType.RAW) {
      for (T term : termFreq.keySet()) {
        switch (type) {
          case LOG:
            termFreq.put(term, 1 + Math.log(termFreq.get(term)));
            break;
          case BOOL:
            termFreq.put(term, 1d);
            break;
          default:
            break;
        }
      }
    }
    return termFreq;
  }

  public static <T> Map<T, Double> idf(Map<?, Map<T, Double>> termFreq) {
    // compute document frequency
    // number of documents containing the term
    Map<T, Long> docFreq = new HashMap<T, Long>();
    for (Map<T, Double> document : termFreq.values()) {
      for (T term : document.keySet()) {
        Long v = docFreq.get(term);
        docFreq.put(term, (v == null) ? 1l : v + 1);
      }
    }

    // compute inverse document frequency
    int totalDocuments = termFreq.size();
    Map<T, Double> idf = new HashMap<T, Double>();
    for (Map.Entry<T, Long> e : docFreq.entrySet()) {
      T term = e.getKey();
      double documentFreq = e.getValue();
      if (LOGGING) {
        LOG.info("term: " + term.toString() + " idf: log(" + totalDocuments
            + "/" + documentFreq + ") + 1 = "
            + (Math.log(totalDocuments / documentFreq) + 1));
      }
      idf.put(term, Math.log(totalDocuments / documentFreq) + 1);
    }
    return idf;
  }

  public static <T> Map<T, Double> tfIdf(Map<T, Double> termFreq,
      Map<T, Double> inverseDocFreq, TfIdfNormalization normalization) {

    Map<T, Double> tfIdf = new HashMap<T, Double>();

    // compute tf * idf
    for (T term : termFreq.keySet()) {
      Double idf = inverseDocFreq.get(term);
      tfIdf.put(term, termFreq.get(term) * ((idf != null) ? idf : 0));
    }

    // compute normalization
    if (normalization == TfIdfNormalization.COS) {
      double n = 0.0;
      for (double x : tfIdf.values()) {
        n += x * x;
      }
      n = Math.sqrt(n);

      for (T term : tfIdf.keySet()) {
        tfIdf.put(term, tfIdf.get(term) / n);
      }
    }
    return tfIdf;
  }

  public static void main(String[] args) {
    List<String> documents = Arrays.asList("to be or not to be", "or to jump");
    Set<String> document1Terms = ImmutableSet.of("to", "be", "or", "not", "to",
        "be", "to be or", "be or not", "or not to", "not to be");
    Set<String> document2Terms = ImmutableSet.of("or", "to", "jump",
        "or to jump");

    Map<String, Double> tf;

    LOG.info("document1: " + Arrays.toString(documents.get(0).split("\\s+")));
    tf = TfIdf.tf(Lists.newArrayList(documents.get(0).split("\\s+")));
    // assertEquals(document1Terms, tf.keySet());
    assertEquals((Double) 2.0, tf.get("to"));
    assertEquals((Double) 2.0, tf.get("be"));
    assertEquals((Double) 1.0, tf.get("or"));
    assertEquals((Double) 1.0, tf.get("not"));

    LOG.info("document2: " + Arrays.toString(documents.get(1).split("\\s+")));
    tf = TfIdf.tf(Lists.newArrayList(documents.get(1).split("\\s+")));
    // assertEquals(document2Terms, tf.keySet());
    assertEquals((Double) 1.0, tf.get("or"));
    assertEquals((Double) 1.0, tf.get("to"));
    assertEquals((Double) 1.0, tf.get("jump"));
  }
}
