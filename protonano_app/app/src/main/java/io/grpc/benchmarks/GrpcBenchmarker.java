package io.grpc.benchmarks;

import java.util.Random;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.nano.GreeterGrpc;
import io.grpc.examples.helloworld.nano.HelloReply;
import io.grpc.examples.helloworld.nano.HelloRequest;

/**
 * Created by davidcao on 6/9/16.
 */
public class GrpcBenchmarker {

    private static final long MIN_SAMPLE_TIME_MS = 2 * 1000;
    private static final long TARGET_TIME_MS = 10 * 1000;

    private static String randomAsciiStringFixed(Random r, int len) {
        char s[] = new char[len];
        for (int i = 0; i < len; ++i) {
            // generate a random ascii character that is a valid character
            s[i] = (char)(r.nextInt(95) + 32);
        }
        return new String(s);
    }

    private static String randomAsciiString(Random r, int maxLen) {
        // add 1 since this is exclusive
        int len = r.nextInt(maxLen) + 1;
        return randomAsciiStringFixed(r, len);
    }

    public static BenchmarkResult helloWorld(String host, int port) {
        ManagedChannel mChannel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext(true)
                .build();
        final GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(mChannel);
        final HelloRequest message = new HelloRequest();
        message.name = randomAsciiString(new Random(), 20);
        HelloReply reply = stub.sayHello(message);
        long size = message.getSerializedSize() + reply.getSerializedSize();
        return benchmark("Sending and recieving hello world greeting (gRPC)",
                size, new Action() {
                    public void execute() {
                        stub.sayHello(message);
                    }
                });
    }

    private static BenchmarkResult benchmark(String name, long dataSize, Action action) {
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

    private static long timeAction(Action action, int iterations) {
        System.gc();
        long start = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            action.execute();
        }
        long end = System.currentTimeMillis();
        return end - start;
    }

    interface Action {
        void execute();
    }
}
