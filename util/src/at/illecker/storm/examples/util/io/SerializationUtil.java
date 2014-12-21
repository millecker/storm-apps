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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tweet.Tweet;

public class SerializationUtil {
  private static final Logger LOG = LoggerFactory
      .getLogger(SerializationUtil.class);

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
