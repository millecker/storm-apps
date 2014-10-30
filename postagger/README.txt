###############################################################################
##### POS Tagger Example                                                  #####
###############################################################################

# Use Apache Ant to build and run example

# Clean all files
ant clean

# Build jar file
ant jar-cpu

# Submit Task to Storm
ant run-cpu

###############################################################################