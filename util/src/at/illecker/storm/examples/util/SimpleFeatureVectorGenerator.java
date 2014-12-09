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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleFeatureVectorGenerator implements FeatureVectorGenerator {

  @Override
  public double[] calculateFeatureVector(Tweet tweet) {

    return null;
  }

  public static List<Tweet> getTestTweets() {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets.add(new Tweet(264183816548130816L,
        "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)",
        1));
    tweets
        .add(new Tweet(
            263398998675693568L,
            "@oluoch @victor_otti @kunjand I just watched it ! Sridevi's comeback .... U remember her from the 90s ?? Sun mornings on NTA ;)",
            1));
    tweets
        .add(new Tweet(
            264259830590603264L,
            "Why is it so hard to find the @TVGuideMagazine these days ? Went to 3 stores for the Castle cover issue . NONE . Will search again tomorrow ...",
            0));
    tweets
        .add(new Tweet(
            259724822978904064L,
            "PBR & @mokbpresents bring you Jim White at the @Do317 Lounge on October 23rd at 7 pm ! http://t.co/7x8OfC56",
            0));
    tweets
        .add(new Tweet(
            243725520724967424L,
            "If you're on the Isle of Man next Thurs , I'll be talking about Safe House and signing books at Waterstones from 6.30 pm . @WaterstonesIoM",
            0));
    tweets
        .add(new Tweet(
            243725520724967424L,
            "If you're on the Isle of Man next Thurs , I'll be talking about Safe House and signing books at Waterstones from 6.30 pm . @WaterstonesIoM",
            0));
    return tweets;
  }

  public static void main(String[] args) {
    List<Tweet> tweets = getTestTweets();
    FeatureVectorGenerator fvg = new SimpleFeatureVectorGenerator();
    for (Tweet tweet : tweets) {
      System.out.println("Tweet: " + tweet);
      System.out.println("FeatureVector: "
          + Arrays.toString(fvg.calculateFeatureVector(tweet)));
    }
  }
}
