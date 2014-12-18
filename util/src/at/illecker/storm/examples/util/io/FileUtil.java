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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.wordlist.WordListMap;

public class FileUtil {
  public static final int BUFFER_SIZE = 2048;
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  public static void extractTarGz(String inputTarGz, String outDir)
      throws IOException {
    FileUtil.extractTarGz(inputTarGz, outDir, false);
  }

  public static void extractTarGz(String inputTarGz, String outDir,
      boolean logging) throws IOException {

    FileInputStream fin = new FileInputStream(inputTarGz);
    BufferedInputStream in = new BufferedInputStream(fin);
    GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
    TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

    TarArchiveEntry entry = null;
    // read Tar entries
    while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
      if (logging) {
        LOG.info("Extracting: " + outDir + File.separator + entry.getName());
      }
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
        // String posTags = values[2]; // ignore
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

  public static Map<String, String> readFile(InputStream is, String splitRegex) {
    return readFile(is, splitRegex, false);
  }

  public static Map<String, String> readFile(InputStream is, String splitRegex,
      boolean logging) {
    Map<String, String> table = new HashMap<String, String>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex, 2);
        table.put(values[0].trim(), values[1].trim());
        if (logging) {
          LOG.info("Add entry key: '" + values[0].trim() + "' value: '"
              + values[1].trim() + "'");
        }
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
    LOG.info("Loaded total " + table.size() + " entries");
    return table;
  }

  public static Map<String, Double> readFile(InputStream is, String splitRegex,
      boolean featureScaling, double minValue, double maxValue) {
    return readFile(is, splitRegex, featureScaling, minValue, maxValue, false);
  }

  public static Map<String, Double> readFile(InputStream is, String splitRegex,
      boolean featureScaling, double minValue, double maxValue, boolean logging) {
    Map<String, Double> map = new HashMap<String, Double>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    double actualMaxValue = Double.MIN_VALUE;
    double actualMinValue = Double.MAX_VALUE;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex, 2);
        String key = values[0].trim();
        double value = Double.parseDouble(values[1].trim());

        if (featureScaling) {
          // Feature scaling
          double normalizedValue = (value - minValue) / (maxValue - minValue);

          if (logging) {
            LOG.info("Add Key: '" + key + "' Value: '" + value
                + "' normalizedValue: '" + normalizedValue + "'");
          }

          // check min and max values
          if (value > actualMaxValue) {
            actualMaxValue = value;
          }
          if (value < actualMinValue) {
            actualMinValue = value;
          }

          value = normalizedValue;
        }

        map.put(key, value);
        if (logging) {
          LOG.info("Add entry key: '" + key + "' value: '" + value + "'");
        }
      }
    } catch (IOException e) {
      LOG.error(e.getMessage());
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
    LOG.info("Loaded total " + map.size() + " items [minValue: "
        + actualMinValue + ", maxValue: " + actualMaxValue + "]");

    if (featureScaling) {
      if (minValue != actualMinValue) {
        LOG.error("minValue is incorrect! actual minValue: " + actualMinValue
            + " given minValue: " + minValue
            + " (normalized values might be wrong!)");
      }
      if (maxValue != actualMaxValue) {
        LOG.error("maxValue is incorrect! actual maxValue: " + actualMaxValue
            + " given maxValue: " + maxValue
            + " (normalized values might be wrong!)");
      }
    }
    return map;
  }

  public static WordListMap<Double> readWordListMap(InputStream is,
      String splitRegex, boolean featureScaling, double minValue,
      double maxValue) {
    return readWordListMap(is, splitRegex, featureScaling, minValue, maxValue,
        false);
  }

  public static WordListMap<Double> readWordListMap(InputStream is,
      String splitRegex, boolean featureScaling, double minValue,
      double maxValue, boolean logging) {
    WordListMap<Double> wordListMap = new WordListMap<Double>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    double actualMaxValue = Double.MIN_VALUE;
    double actualMinValue = Double.MAX_VALUE;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex);
        String key = values[0].trim();
        double value = Double.parseDouble(values[1].trim());

        if (featureScaling) {
          // Feature scaling
          double normalizedValue = (value - minValue) / (maxValue - minValue);

          if (logging) {
            LOG.info("Add Key: '" + key + "' Value: '" + value
                + "' normalizedValue: '" + normalizedValue + "'");
          }

          // check min and max values
          if (value > actualMaxValue) {
            actualMaxValue = value;
          }
          if (value < actualMinValue) {
            actualMinValue = value;
          }

          value = normalizedValue;
        }
        wordListMap.put(key, value);
      }
      LOG.info("Loaded " + wordListMap.size() + " items [minValue: "
          + actualMinValue + ", maxValue: " + actualMaxValue + "]");

      if (featureScaling) {
        if (minValue != actualMinValue) {
          LOG.error("minValue is incorrect! actual minValue: " + actualMinValue
              + " given minValue: " + minValue
              + " (normalized values might be wrong!)");
        }
        if (maxValue != actualMaxValue) {
          LOG.error("maxValue is incorrect! actual maxValue: " + actualMaxValue
              + " given maxValue: " + maxValue
              + " (normalized values might be wrong!)");
        }
      }

      return wordListMap;

    } catch (IOException e) {
      LOG.error(e.getMessage());
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
    return null;
  }

  public static Set<String> readFile(InputStream is) {
    return FileUtil.readFile(is, false);
  }

  public static Set<String> readFile(InputStream is, boolean logging) {
    Set<String> set = new HashSet<String>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        set.add(line.trim());
        if (logging) {
          LOG.info("Add entry: '" + line.trim() + "'");
        }
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
    LOG.info("Loaded total " + set.size() + " entries");
    return set;
  }

  public static void delete(File file) throws IOException {
    FileUtil.delete(file, false);
  }

  public static void delete(File file, boolean logging) throws IOException {
    if (logging) {
      LOG.info("Delete: " + file.getAbsolutePath());
    }
    if (file.isDirectory()) {
      for (File c : file.listFiles())
        delete(c, logging);
    }
    if (!file.delete()) {
      throw new FileNotFoundException("Failed to delete file: " + file);
    }
  }

  public static void serializeTweets(List<Tweet> tweets, String file) {
    try {
      FileOutputStream fos = new FileOutputStream(file);
      ObjectOutputStream oos = new ObjectOutputStream(fos);
      oos.writeObject(tweets);
      oos.close();
      fos.close();
      LOG.info("Serialized tweets in " + file);
    } catch (FileNotFoundException fnfe) {
      LOG.error(fnfe.getMessage());
    } catch (IOException ioe) {
      LOG.error(ioe.getMessage());
    }
  }

  public static List<Tweet> deserializeTweets(String file) {
    List<Tweet> tweets = null;
    try {
      FileInputStream fis = new FileInputStream(file);
      ObjectInputStream ois = new ObjectInputStream(fis);
      tweets = (List<Tweet>) ois.readObject();
      ois.close();
      fis.close();
      LOG.info("Deserialized tweets from " + file);
    } catch (FileNotFoundException fnfe) {
      LOG.error(fnfe.getMessage());
    } catch (IOException ioe) {
      LOG.error(ioe.getMessage());
    } catch (ClassNotFoundException c) {
      LOG.error(c.getMessage());
    }
    return tweets;
  }
}
