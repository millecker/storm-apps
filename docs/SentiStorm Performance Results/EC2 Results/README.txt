###############################################################################
##### BenchmarkResults R Script                                           #####
###############################################################################

# Command-line Arguments

./BenchmarkResults.R <CSVInputFile> <MeasurementSeconds>
   [<Title>]
   [<XaxisDescription>]
   [<YaxisDescription>]
   [XticksIncrement]
   [YticksIncrement]
   [XticksStart]
   [YticksStart]

# Examples

./BenchmarkResults.R c3.8xlarge/tuples.csv 10 "SentiStorm Performance on c3.8xlarge nodes" "Nodes" "Tweets / Second" 1 2000 1 3000
