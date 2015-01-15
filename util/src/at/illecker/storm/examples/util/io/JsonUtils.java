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

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import com.google.gson.GsonBuilder;

public class JsonUtils {
  private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

  public static List<Map<String, Object>> readJsonFile(String jsonFile) {
    LOG.info("Load file " + jsonFile);
    return readJsonStream(IOUtils.getInputStream(jsonFile));
  }

  public static List<Map<String, Object>> readJsonStream(
      InputStream jsonInputStream) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(jsonInputStream));
      GsonBuilder builder = new GsonBuilder();
      List<Map<String, Object>> elements = (List<Map<String, Object>>) builder
          .create().fromJson(br, Object.class);
      LOG.info("Loaded " + " elements: " + elements.size());
      return elements;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
    }
  }

  public static List<Status> readTweets(InputStream tweetsFile,
      String filterLanguage) {
    List<Status> tweets = new ArrayList<Status>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(tweetsFile, "UTF-8");
      br = new BufferedReader(isr);
      String rawJSON = "";
      while ((rawJSON = br.readLine()) != null) {
        // rawJSON may include multiple status objects within one line
        String regex = "\"created_at\":";
        String[] rawJSONTweets = rawJSON.split("\\}\\{" + regex);

        if (rawJSONTweets.length == 0) { // only one object
          try {
            Status status = TwitterObjectFactory.createStatus(rawJSON);
            tweets.add(status);
            // LOG.info("@" + status.getUser().getScreenName() + " - "
            // + status.getText());
          } catch (TwitterException twe) {
            LOG.error("Mailformed JSON Tweet: " + twe.getMessage());
          }

        } else { // read multiple objects
          for (int j = 0; j < rawJSONTweets.length; j++) {
            if (j == 0) {
              rawJSONTweets[j] = rawJSONTweets[j] + "}";
            } else if (j == rawJSONTweets.length) {
              rawJSONTweets[j] = "{" + regex + rawJSONTweets[j];
            } else {
              rawJSONTweets[j] = "{" + regex + rawJSONTweets[j] + "}";
            }
            try {
              Status status = TwitterObjectFactory
                  .createStatus(rawJSONTweets[j]);
              if (filterLanguage != null) {
                if (status.getLang().equals(filterLanguage)) {
                  tweets.add(status);
                }
              } else {
                tweets.add(status);
              }
              // LOG.info("@" + status.getUser().getScreenName() + " - "
              // + status.getText());
            } catch (TwitterException twe) {
              LOG.error("Mailformed JSON Tweet: " + twe.getMessage());
            }
          }
        }
      }

    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
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
    }
    LOG.info("Loaded total " + tweets.size() + " tweets");
    return tweets;
  }

  public static List<Status> readTweetsDirectory(String tweetsDirectory,
      String filterLanguage) {
    File tweetsDir = new File(tweetsDirectory);
    if (!tweetsDir.isDirectory()) {
      LOG.error("readTweetsDirectory - No valid directory: " + tweetsDirectory);
      return null;
    }

    List<Status> tweets = new ArrayList<Status>();

    // List all gz files
    File[] twitterFiles = tweetsDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(".gz");
      }
    });

    // Load each file
    for (int i = 0; i < twitterFiles.length; i++) {
      LOG.info("Load file " + twitterFiles[i].getName());
      tweets.addAll(readTweets(IOUtils.getInputStream(twitterFiles[i]),
          filterLanguage));
    }

    LOG.info("Loaded total " + tweets.size() + " tweets");
    return tweets;
  }

}
