package io.grpc.grpcbenchmarks;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

/**
 * Created by davidcao on 6/16/16.
 */
public class GrpcBenchmarker {

    private static final long MIN_SAMPLE_TIME_MS = 2 * 1000;
    private static final long TARGET_TIME_MS = 10 * 1000;

    public static void benchmarkSimpleRpc(String host, int port, int proto) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
        HelloRequest message = HelloRequest.newBuilder().setName("test string").build();
        HelloReply reply = stub.sayHello(message);
    }

    private static BenchmarkResult benchmark(String name, long dataSize, Action action) throws Exception {
        for (int i = 0; i < 100; ++i) {
            action.execute();
        }

        int iterations = 1;
        long elapsed = timeAction(action, iterations);
        while (elapsed < MIN_SAMPLE_TIME_MS) {
            iterations *= 2;
            elapsed = timeAction(action, iterations);
        }

        iterations = (int) ((TARGET_TIME_MS / (double) elapsed) * iterations);
        elapsed = timeAction(action, iterations);
        float mbps = (iterations * dataSize) / (elapsed * 1024 * 1024 / 1000f);
        BenchmarkResult res = new BenchmarkResult(name, iterations, elapsed, mbps, dataSize);
        return res;
    }

    private static long timeAction(Action action, int iterations) throws Exception {
        System.gc();
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            action.execute();
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    interface Action {
        void execute() throws Exception;
    }
}
