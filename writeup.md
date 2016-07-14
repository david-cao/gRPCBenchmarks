Mobile gRPC Benchmarks
======================
This readme outlines the methodology used for the mobile benchmarks, results 
observed, and instructions to replicate our results. There are two main 
sections: protobuf (de)serialization benchmarks and gRPC benchmarks. The 
protobuf benchmarks are run against similar JSON structures, and the gRPC 
benchmarks are run against a similar RESTful HTTP service using JSON. 

Methodology
-----------
### Protobuf Benchmarks
After a user chooses which message to benchmark, an instance with 
[randomally generated fields]() is instantiated. After a short warmup, the 
message is then either serialized or deserialized over and over until a certain 
amount of time has passed, ten seconds in this case. In order to keep 
benchmarks as accurate as possible, we wrap the code to be benchmarked in an interface and simply loop it for a set number of iterations.
```Java
interface Action {
    void execute();
}

// Sample benchmark of multiplication
Action a = new Action() {
    @Override
    public void execute() {
        int x = 1000 * 123456;
    }
}

for (int i = 0; i < 100; ++i) {
    a.execute();
}
```
We determine the number of iterations to be done by first timing a single 
iteration, seeing if it surpasses a minimum sample time (2 seconds), and 
repeating with 2 * previous number of iterations if it doesn't. If it does, 
we then guess the number of iterations it would take to run for 10 seconds by 
doing a little math. The code for this can be seen in 
[ProtobufBenchmarker.java](). 

The same method is used for the JSON equivalent. The JSON benchmarks can also be run using gzip to compress the data after serialization.

### gRPC Benchmarks
The gRPC benchmarks are mostly adapted from 
[AsyncClient.java](https://github.com/grpc/grpc-java/blob/master/benchmarks/src/main/java/io/grpc/benchmarks/qps/AsyncClient.java) in the `benchmarks/` folder of the grpc-java repo. Essentially what happens is a channel is opened, a message with a specified payload size is sent, a response with the same payload size is recieved, and we repeat until 60 seconds have passed. Total end to end times are recorded in a historgram (this includes processing time). 

Unlike the protobuf benchmarks, we don't mind if we check or get the time every 
iteration since the latency of the connection will always overshadow it. 

The JSON benchmark uses the same method as the gRPC benchmark.

Results
-------
TODO: Make some pretty visualizations

Using sheets gives us something like:
![Graph of latencies for RPC calls](https://github.com/david-cao/gRPCBenchmarks/blob/master/benchmark_results/latencies.png)

Or maybe using google charts, [example here.](https://github.com/david-cao/gRPCBenchmarks/blob/master/benchmark_results/CodedOutputStream.html)

Main issue is that there's lots of things to show, how should we do it?

### Protobuf vs. JSON

[Results in text for now]()

[OkHttp results]()

#### Considerations
Protobuf needs to calculate the size of its message when serializing in order to allocate a large enough byte array. However, when it's called once it gets cached, thus leading to skewed results with successive runs.

Gzip is disabled for the "Small request" proto, since it actually increases size.

### gRPC vs. RESTful HTTP JSON API

[Results in text for now]()

#### Considerations
As you can see the results for a POST vs. a GET are drastically different. This is due to the fact that for each POST request done in Android, an output stream needs to be opened, written to, then closed before sending the request. Using Square's OkHttp library makes this a bit better, but still results in a large difference between a gRPC request and a POST request. 

Replicating Results
-------------------
TODO: Nothing below is tested...

In order to run the benchmarks on your own device, you'll first need to clone the grpc-java repo and build the benchmarks.
```
$ git clone https://github.com/grpc/grpc-java.git
$ cd grpc-java/
$ ./gradlew :grpc-benchmarks:installDist
```

Then change directories to the mobile benchmark folder, and run
```
$ ./gradlew installDebug
```
to build the application. From there use `adb` to run the application on your device.

Alternatively, open the folder in Android Studio and simply sync and build.
