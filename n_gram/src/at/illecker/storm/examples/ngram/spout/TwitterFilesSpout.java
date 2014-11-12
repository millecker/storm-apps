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
package at.illecker.storm.examples.ngram.spout;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import at.illecker.storm.examples.postagger.POSTaggerTopology;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class TwitterFilesSpout extends BaseRichSpout {
  private static final long serialVersionUID = 8929312049622286713L;
  private static final Logger LOG = LoggerFactory
      .getLogger(TwitterFilesSpout.class);
  public static final String FILE_EXTENSION = ".gz";
  private SpoutOutputCollector m_collector;
  private List<Status> m_tweets;
  private int m_index = 0;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("tweet")); // key of output tuples
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;

    if (config.get("twitter.dir") != null) {
      String twitterDirPath = config.get("twitter.dir").toString();
      File twitterDir = new File(twitterDirPath);
      if (twitterDir.isDirectory()) {
        m_tweets = readTweets(twitterDir);
      } else {
        throw new RuntimeException("Error reading directory " + twitterDirPath);
      }
    } else {
      throw new RuntimeException("twitter.dir property was not set!");
    }
  }

  public void nextTuple() {
    this.m_collector.emit(new Values(m_tweets.get(m_index)));
    m_index++;
    if (m_index >= m_tweets.size()) {
      m_index = 0;
    }
    try {
      // TODO minimize sleep time
      Thread.sleep(1000); // sleep 1 ms
    } catch (InterruptedException e) {
    }
  }

  private List<Status> readTweets(File twitterDir) {
    List<Status> tweets = new ArrayList<Status>();
    // List all gz files
    File[] twitterFiles = twitterDir.listFiles(new FilenameFilter() {
      public boolean accept(File dir, String name) {
        return name.toLowerCase().endsWith(FILE_EXTENSION);
      }
    });
    // Load each file
    for (int i = 0; i < twitterFiles.length; i++) {
      LOG.info("Load file " + twitterFiles[i].getName());
      FileInputStream fis = null;
      GZIPInputStream gis = null;
      InputStreamReader isr = null;
      BufferedReader br = null;
      try {
        fis = new FileInputStream(twitterFiles[i]);
        gis = new GZIPInputStream(fis);
        isr = new InputStreamReader(gis, "UTF-8");
        br = new BufferedReader(isr);
        String rawJSON = "";
        long count = 0;
        while ((rawJSON = br.readLine()) != null) {
          // rawJSON may include multiple status objects within one line
          String regex = "\"created_at\":";
          String[] rawJSONTweets = rawJSON.split("\\}\\{" + regex);

          if (rawJSONTweets.length == 0) { // only one object
            try {
              Status status = TwitterObjectFactory.createStatus(rawJSON);
              tweets.add(status);
              count++;
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
                if (status.getLang().equals(POSTaggerTopology.FILTER_LANG)) {
                  tweets.add(status);
                  count++;
                }
                // LOG.info("@" + status.getUser().getScreenName() + " - "
                // + status.getText());
              } catch (TwitterException twe) {
                LOG.error("Mailformed JSON Tweet: " + twe.getMessage());
              }
            }
          }
        }
        LOG.info("Loaded " + count + " tweets");
      } catch (IOException ioe) {
        ioe.printStackTrace();
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
        if (gis != null) {
          try {
            gis.close();
          } catch (IOException ignore) {
          }
        }
        if (fis != null) {
          try {
            fis.close();
          } catch (IOException ignore) {
          }
        }
      }
    }
    LOG.info("Total " + " tweets: " + tweets.size());
    return tweets;
  }
}
