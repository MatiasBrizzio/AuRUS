# How to run experiments

## Generating data

The script [`./unreal-repair.sh`](./unreal-repair.sh) runs the AuRUS tool with a given unrealisable specification `spec.tlsf`, generating candidate repairs. It can be configured with many commandline flags.

To run a full set of experiments, I wrote [`./repeat-unreal-repair.sh`](./unreal-repair-test.sh) that generates all of the desired configurations to run the repair with. Each configuration will end with a directory containing the results of its runs.

You can configure the parameters directly in [`./repeat-unreal-repair.sh`](./unreal-repair-test.sh) before running it.

## Analysing data

You can analyse your results using [`./read-results.sh`](./read-results.sh), which will compute averages and standard deviations for a variety of statistics. For example:

```
$ ./read-results.sh result/arbiter-70-10-10-10
Average total #Sol: 480 (StdDev: 37.39)
Average weaker than original #Sol: 237 (StdDev: 39.57)
Average weaker than original ratio: .49 (StdDev: .080)
Average weaker than genuine #Sol: 97 (StdDev: 37.38)
```

This is:
1. The total number of realizable repairs generated
2. The number of realizable repairs that are logically weaker than the original specification
3. The ratio of (2) and (1)
4. The number of realizable repairs that are logically weaker than one of the reference solutions to the realizability problem
