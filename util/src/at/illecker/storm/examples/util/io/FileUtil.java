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
package at.illecker.storm.examples.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;

public class FileUtil {
  public static final int BUFFER_SIZE = 2048;
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  public static void extractTarGz(String inputTarGz, String outDir)
      throws IOException {

    FileInputStream fin = new FileInputStream(inputTarGz);
    BufferedInputStream in = new BufferedInputStream(fin);
    GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
    TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

    TarArchiveEntry entry = null;
    // read Tar entries
    while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
      LOG.info("Extracting: " + outDir + File.separator + entry.getName());

      if (entry.isDirectory()) { // create directory
        File f = new File(outDir + File.separator + entry.getName());
        f.mkdirs();
      } else { // decompress file
        int count;
        byte data[] = new byte[BUFFER_SIZE];

        FileOutputStream fos = new FileOutputStream(outDir + File.separator
            + entry.getName());
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
        while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    }
    // close input stream
    tarIn.close();
  }

  public static List<Tweet> readTweets(InputStream is) {
    List<Tweet> tweets = new ArrayList<Tweet>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        String[] values = line.split("\t");
        long id = Long.parseLong(values[0]);
        String text = values[1];
        String posTags = values[2]; // ignore
        String label = values[3].toLowerCase().trim();
        double score = 0;
        if (label.equals("negative")) {
          score = -1;
        } else if (label.equals("neutral")) {
          score = 0;
        } else if (label.equals("positive")) {
          score = 1;
        }
        tweets.add(new Tweet(id, text, score));
      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    LOG.info("Loaded total " + tweets.size() + " tweets");
    return tweets;
  }

  public static void delete(File f) throws IOException {
    LOG.info("Delete: " + f.getAbsolutePath());
    if (f.isDirectory()) {
      for (File c : f.listFiles())
        delete(c);
    }
    if (!f.delete()) {
      throw new FileNotFoundException("Failed to delete file: " + f);
    }
  }
}