# Android App for Benchmarking

The application is split into two main sections, comparing protobuf to JSON, and comparing gRPC to a RESTful JSON API.

## ProtoLite vs. JSON Benchmarks
This section benchmarks how fast either protobuf or JSON serialization/deserialization is. Choose your protofile to use, whether or not to gzip the JSON, and then run your benchmarks. You can either choose a specific benchmark to run, or to run them all at once. A proto is filled with randomly generated data then used for the benchmark(s).

### Method
Each benchmark begins by warming up a bit, then runs a number of iterations, times it, and if the time is less than a minimum sample time (2 seconds in this case), multiplies the number of iterations to be done by 2. Once the minimum sample time is hit, we estimate the number of iterations needed to run for 10 seconds, and then execute that many iterations. We use Mbps processed as a metric by getting the size of the data and number of iterations run.

This way we avoid having a time check inside the for loop, allowing for a more accurate benchmark.

## gRPC vs. RESTful JSON Benchmarks