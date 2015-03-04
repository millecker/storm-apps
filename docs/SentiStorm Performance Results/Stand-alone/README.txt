###############################################################################
##### BenchmarkResults R Script                                           #####
###############################################################################

# Command-line Arguments

./BenchmarkResults.R <CSVInputFile> <TotalTuples>
   [<Title>]
   [<XaxisDescription>]
   [<YaxisDescription>]
   [XticksIncrement]
   [YticksIncrement]
   [XticksStart]
   [YticksStart]

# Examples

./BenchmarkResults.R c3.8xlarge/38130_tuple.csv 38130 "SentiStorm Stand-alone Performance for 38130 test tweets on one c3.8xlarge" "Threads" "Tweets / Second" 1 200 1 200

./BenchmarkResults.R c3.8xlarge/38130_tuples_ark.csv 38130 "SentiStorm Stand-alone Ark Performance for 38130 test tweets on one c3.8xlarge" "Threads" "Tweets / Second" 1 200 1 200

./BenchmarkResults.R c3.8xlarge/38130_tuples_gate.csv 38130 "SentiStorm Stand-alone Gate Performance for 38130 test tweets on one c3.8xlarge" "Threads" "Tweets / Second" 1 200 1 200


./CombinedBenchmarkResults.R c3.4xlarge/38130_tuples_ark.csv c3.4xlarge/38130_tuples_gate.csv 38130 "SentiStorm Stand-alone Performance for 38130 test tweets on one c3.4xlarge" "Threads" "Tweets / Second" 1 200 1 200

./CombinedBenchmarkResults.R c3.8xlarge/38130_tuples_ark_combined.csv c3.8xlarge/38130_tuples_gate_combined.csv 38130 "SentiStorm Stand-alone Performance for 38130 test tweets on one c3.8xlarge" "Threads" "Tweets / Second" 2 200 0 200
