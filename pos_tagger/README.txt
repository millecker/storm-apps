###############################################################################
##### POS Tagger Example                                                  #####
###############################################################################

# Use Apache Ant to build and run example

# Clean all files
ant clean

# Build jar file
ant jar

# Submit Task to Storm
ant run [ -DtwitterDir=dir \
 -DconsumerKey=key \
 -DconsumerSecret=secret \
 -DaccessToken=token \
 -DaccessTokenSecret=secret]
 [-DkeyWords='word1 word2']

# Example
ant clean && ant run \
 -DtwitterDir=../resources/datasets/uibk_crawler/test/
 
ant clean && ant run \
  -DconsumerKey=XXXX  -DconsumerSecret=XXXX \
  -DaccessToken=XXXX  -DaccessTokenSecret=XXXX


# Modify file conf/storm_env.ini
STORM_JAR_JVM_OPTS:-Xmx8g

# Modify file conf/storm.yaml
worker.childopts: "-Xmx8g"

###############################################################################
