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
package at.illecker.storm.examples.util.tfidf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
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
 * based on https://github.com/wpm/tfidf
 *
 */
public class TfIdf {
  private static final Logger LOG = LoggerFactory.getLogger(TfIdf.class);

  private TfType m_type = TfType.RAW;

  public TfIdf() {

  }

  public TfIdf(TfType type) {
    this();
    m_type = type;
  }

  public static <T> Map<T, Double> tf(Collection<T> document, TfType type) {
    Map<T, Double> tf = new HashMap<T, Double>();
    for (T term : document) {
      Double v = tf.get(term);
      tf.put(term, (v == null) ? 1 : v + 1);
    }

    if (type != TfType.RAW) {
      for (T term : tf.keySet()) {
        switch (type) {
          case LOG:
            tf.put(term, 1 + Math.log(tf.get(term)));
            break;
          case BOOL:
            tf.put(term, 1.0);
            break;
          default:
            break;
        }
      }
    }
    return tf;
  }

  public static <T> Map<T, Double> tf(Collection<T> document) {
    return tf(document, TfType.RAW);
  }

  public static <T> List<Map<T, Double>> tf(Iterable<Collection<T>> documents,
      TfType type) {

    List<Map<T, Double>> tfs = new ArrayList<Map<T, Double>>();
    for (Collection<T> document : documents) {
      tfs.add(tf(document, type));
    }
    return tfs;
  }

  public static <T> List<Map<T, Double>> tf(Iterable<Collection<T>> documents) {
    return tf(documents, TfType.RAW);
  }

  /**
   * Inverse document frequency for a set of documents
   *
   * @param documentVocabularies sets of terms which appear in the documents
   * @param smooth smooth the counts by treating the document set as if it
   *          contained an additional document with every term in the vocabulary
   * @param addOne add one to idf values to prevent divide by zero errors in
   *          tf-idf
   * @param <TERM> term type
   * @return map of terms to their inverse document frequency
   */
  public static <TERM> Map<TERM, Double> idf(
      Iterable<Iterable<TERM>> documentVocabularies, boolean smooth,
      boolean addOne) {
    Map<TERM, Integer> df = new HashMap<TERM, Integer>();
    int d = smooth ? 1 : 0;
    int a = addOne ? 1 : 0;
    int n = d;
    for (Iterable<TERM> documentVocabulary : documentVocabularies) {
      n += 1;
      for (TERM term : documentVocabulary) {
        Integer v = df.get(term);
        df.put(term, (v == null) ? d : v + 1);
      }
    }
    Map<TERM, Double> idf = new HashMap<TERM, Double>();
    for (Map.Entry<TERM, Integer> e : df.entrySet()) {
      TERM term = e.getKey();
      double f = e.getValue();
      idf.put(term, Math.log(n / f) + a);
    }
    return idf;
  }

  /**
   * Smoothed, add-one inverse document frequency for a set of documents
   *
   * @param documentVocabularies sets of terms which appear in the documents
   * @param <TERM> term type
   * @return map of terms to their inverse document frequency
   */
  public static <TERM> Map<TERM, Double> idf(
      Iterable<Iterable<TERM>> documentVocabularies) {
    return idf(documentVocabularies, true, true);
  }

  /**
   * tf-idf for a document
   *
   * @param tf term frequencies of the document
   * @param idf inverse document frequency for a set of documents
   * @param normalization none or cosine
   * @param <TERM> term type
   * @return map of terms to their tf-idf values
   */
  public static <TERM> Map<TERM, Double> tfIdf(Map<TERM, Double> tf,
      Map<TERM, Double> idf, Normalization normalization) {
    Map<TERM, Double> tfIdf = new HashMap<TERM, Double>();
    for (TERM term : tf.keySet()) {
      tfIdf.put(term, tf.get(term) * idf.get(term));
    }
    if (normalization == Normalization.COSINE) {
      double n = 0.0;
      for (double x : tfIdf.values()) {
        n += x * x;
      }
      n = Math.sqrt(n);

      for (TERM term : tfIdf.keySet()) {
        tfIdf.put(term, tfIdf.get(term) / n);
      }
    }
    return tfIdf;
  }

  /**
   * Unnormalized tf-idf for a document
   *
   * @param tf term frequencies of the document
   * @param idf inverse document frequency for a set of documents
   * @param <TERM> term type
   * @return map of terms to their tf-idf values
   */
  public static <TERM> Map<TERM, Double> tfIdf(Map<TERM, Double> tf,
      Map<TERM, Double> idf) {
    return tfIdf(tf, idf, Normalization.NONE);
  }

  /**
   * Utility to build inverse document frequencies from a set of term
   * frequencies
   *
   * @param tfs term frequencies for a set of documents
   * @param smooth smooth the counts by treating the document set as if it
   *          contained an additional document with every term in the vocabulary
   * @param addOne add one to idf values to prevent divide by zero errors in
   *          tf-idf
   * @param <TERM> term type
   * @return map of terms to their tf-idf values
   */
  public static <TERM> Map<TERM, Double> idfFromTfs(
      Iterable<Map<TERM, Double>> tfs, boolean smooth, boolean addOne) {
    return idf(new KeySetIterable<TERM, Double>(tfs), smooth, addOne);
  }

  /**
   * Utility to build smoothed, add-one inverse document frequencies from a set
   * of term frequencies
   *
   * @param tfs term frequencies for a set of documents
   * @param <TERM> term type
   * @return map of terms to their tf-idf values
   */
  public static <TERM> Map<TERM, Double> idfFromTfs(
      Iterable<Map<TERM, Double>> tfs) {
    return idfFromTfs(tfs, true, true);
  }

  /**
   * Iterator over the key sets of a set of maps.
   *
   * @param <K> map key type
   * @param <V> map value type
   */
  static private class KeySetIterable<K, V> implements Iterable<Iterable<K>> {
    final private Iterator<Map<K, V>> maps;

    public KeySetIterable(Iterable<Map<K, V>> maps) {
      this.maps = maps.iterator();
    }

    @Override
    public Iterator<Iterable<K>> iterator() {
      return new Iterator<Iterable<K>>() {
        @Override
        public boolean hasNext() {
          return maps.hasNext();
        }

        @Override
        public Iterable<K> next() {
          return maps.next().keySet();
        }

        @Override
        public void remove() {
          maps.remove();
        }
      };
    }
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
