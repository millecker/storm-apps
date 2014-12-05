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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;

public class WordNet {

  public static final String workingDirPath = System.getProperty("user.dir");
  public static final String wordNetDirPath = workingDirPath + File.separator;
  public static final String wordNetDictPath = workingDirPath + File.separator
      + "resources" + File.separator + "wn3.1.dict.tar.gz";

  public static void testDictionary(File wordNetDirPath) throws IOException,
      InterruptedException {
    IRAMDictionary dict = new RAMDictionary(wordNetDirPath, ILoadPolicy.NO_LOAD);
    dict.open();

    // do something
    trek(dict);

    // now load into memory
    System.out.print("\nLoading Wordnet into memory...");
    long t = System.currentTimeMillis();
    dict.load(true);
    System.out.printf("done (%1d msec)\n", System.currentTimeMillis() - t);

    // try it again, this time in memory
    trek(dict);

    // look up first sense of the word "dog"
    IIndexWord idxWord = dict.getIndexWord("dog", POS.NOUN);
    IWordID wordID = idxWord.getWordIDs().get(0);
    IWord word = dict.getWord(wordID);
    System.out.println("Id = " + wordID);
    System.out.println("Lemma = " + word.getLemma());
    System.out.println("Gloss = " + word.getSynset().getGloss());
  }

  public static void trek(IDictionary dict) {
    int tickNext = 0;
    int tickSize = 20000;
    int seen = 0;
    System.out.print("Treking across Wordnet");
    long t = System.currentTimeMillis();
    for (POS pos : POS.values())
      for (Iterator<IIndexWord> i = dict.getIndexWordIterator(pos); i.hasNext();)
        for (IWordID wid : i.next().getWordIDs()) {
          seen += dict.getWord(wid).getSynset().getWords().size();
          if (seen > tickNext) {
            System.out.print(".");
            tickNext = seen + tickSize;
          }
        }
    System.out.printf("done (%1d msec)\n", System.currentTimeMillis() - t);
    System.out.println("In my trek I saw " + seen + " words");
  }

  public static void main(String[] args) {
    try {
      System.out.println("WordNetDict: " + wordNetDictPath);
      System.out.println("WordNetDir: " + wordNetDirPath);

      File wordNetDir = new File(wordNetDirPath + "dict");
      if (wordNetDir.exists()) {
        FileUtil.delete(wordNetDir);
      }

      // extract tar.gz file
      FileUtil.extractTarGz(wordNetDictPath, wordNetDirPath);

      testDictionary(wordNetDir);

      FileUtil.delete(wordNetDir);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}
