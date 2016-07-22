gRPC Mobile Benchmarks
======================
As gRPC has become a better and faster RPC framework, we've consistently gotten the question, "How _much_ faster is gRPC?" We already have comprehensive server-side benchmarks, but we don't have client-side benchmarks. Benchmarking a client is a bit different than benchmarking a server. We care more about things such as latency, request size, and battery life and less about things like queries per second (QPS) and number of concurrent threads. Thus we built an Android app in order to quantify these factors and provide solid numbers behind them. 

Specifically what we want to benchmark is client side Protobuf vs. JSON serialization/deserialization and gRPC vs. a RESTful HTTP JSON service. For the serialization benchmarks, we want to measure the size of messages and speed at which we serialize and deserialize. For the RPC benchmarks, we want to measure the latency of end-to-end requests, packet size, and battery life drain. 

Protobuf vs. JSON
-----------------
In order to benchmark Protobuf and JSON, we ran serializations and deserializations over and over on randomly generated protos, which can be seen [here](/protolite_app/app/src/main/proto). These protos varied quite a bit in size and complexity, from just a few bytes to over 100kb. JSON equivalents were created and then also benchmarked. For the Protobuf messages, we had three main methods of serializing and deserializing: simply using a byte array, `CodedOutputStream`/`CodedInputStream` which is Protobuf's own implementation of input and output streams, and Java's `ByteArrayOutputStream` and `ByteArrayInputStream`. For JSON we used `org.json`'s [`JSONObject`](https://developer.android.com/reference/org/json/JSONObject.html). This only had one method to serialize and deserialize, `toString()` and `new JSONObject()`, respectively. 

In order to keep benchmarks as accurate as possible, we wrapped the code to be benchmarked in an interface and simply looped it for a set number of iterations. This way we discounted any time spent checking the system time.
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
Before running a benchmark, we ran a warmup in order to clean out any erratic behaviour by the JVM, and then calculated the number of iterations needed to run for a set time (10 seconds in the Protobuf vs. JSON case). To do this, we started with 1 iteration, measured the time it took for that run, and compared it to a minimum sample time (2 seconds in our case). If the number of iterations took long enough, we estimated the number of iterations needed to run for 10 seconds by doing some math. Otherwise, we multiplied the number of iterations by 2 and repeated.
```Java
// This can be found in ProtobufBenchmarker.java benchmark()
int iterations = 1;
// Time action simply reports the time it takes to run a certain action for that number of iterations
long elapsed = timeAction(action, iterations);
while (elapsed < MIN_SAMPLE_TIME_MS) {
    iterations *= 2;
    elapsed = timeAction(action, iterations);
}
// Estimate number of iterations to run for 10 seconds
iterations = (int) ((TARGET_TIME_MS / (double) elapsed) * iterations);
```

### Results
Benchmarks were run on Protobuf, JSON, and Gzipped JSON.

We found that regardless of the serialization/deserialization method used for Protobuf, it was consistently about 3x faster for serializing than JSON. For deserialization, JSON is actually a bit faster for small messages (<1kb), around 1.5x, but for larger messages (>15kb) Protobuf is 2x faster. For Gzipped JSON, Protobuf is well over 5x faster in serialization, regardless of size. For deserialization, both are about the same at small messages, but Protobuf is about 3x faster for larger messages. Results can be explored in more depth and replicated [here](/github_readme).

gRPC vs. HTTP JSON
------------------
To benchmark RPC calls, we want to measure end-to-end latency, bandwidth size, and battery usage. To do this, we ping pong with a serverfor 60 seconds, using the same message each time, and measure the latency and message size. The message consists of some fields for the server to read, and a payload of bytes. We compared gRPC's unary call to a simple RESTful HTTP JSON service. The gRPC benchmark creates a channel, and starts a unary call that repeats when it recieves a response until 60 seconds have passed. The response contains a proto with the same payload sent.

Similarly for the HTTP JSON benchmarks, it sends a POST request to the server with an equivalent JSON object, and the server sends back a JSON object with the same payload.
```Java
// This can be found in AsyncClient.java doUnaryCalls()
// Make stub to send unary call
final BenchmarkServiceStub stub = BenchmarkServiceGrpc.newStub(channel);
stub.unaryCall(request, new StreamObserver<SimpleResponse>() {
    long lastCall = System.nanoTime();
    // Do nothing on next
    @Override
    public void onNext(SimpleResponse value) {
    }

    @Override
    public void onError(Throwable t) {
        Status status = Status.fromThrowable(t);
        System.err.println("Encountered an error in unaryCall. Status is " + status);
        t.printStackTrace();

        future.cancel(true);
    }
    // Repeat if time isn't reached
    @Override
    public void onCompleted() {
        long now = System.nanoTime();
        // Record the latencies in microseconds
        histogram.recordValue((now - lastCall) / 1000);
        lastCall = now;

        Context prevCtx = Context.ROOT.attach();
        try {
            if (endTime > now) {
                stub.unaryCall(request, this);
            } else {
                future.done();
            }
        } finally {
            Context.current().detach(prevCtx);
        }
    }
});
```

TODO: Mention streaming?
Streaming wasn't compared since it's an HTTP/2 specific feature (and since it blew HTTP JSON out of the water!)

### Results
In terms of latency, gRPC is 5x-10x faster up to the 95th percentile, with averages of around 2 milliseconds for an end-to-end request. For bandwidth, gRPC is about 3x faster for small requests (100-1000 byte payload), and consistently 2x faster for large requests (10kb-100kb payload). To replicate these results or explore in more depth, check out our [repository]().

TODO: Battery measurements?