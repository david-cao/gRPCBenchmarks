package io.grpc.grpcbenchmarks;

import com.google.protobuf.MessageLite;

/**
 * Created by davidcao on 6/13/16.
 */
public class Benchmark {
    String title;
    String description;
    int methodNumber;

    Benchmark(String title, String description, int methodNumber) {
        this.title = title;
        this.description = description;
        this.methodNumber = methodNumber;
    }

    public BenchmarkResult run(MessageLite msg, String json, boolean useGzip) throws Exception {
        switch (methodNumber) {
            case 0:
                return ProtobufBenchmarker.serializeProtobufToByteArray(msg);
            case 1:
                return ProtobufBenchmarker.serializeProtobufToCodedOutputStream(msg);
            case 2:
                return ProtobufBenchmarker.serializeProtobufToByteArrayOutputStream(msg);
            case 3:
                return ProtobufBenchmarker.deserializeProtobufFromByteArray(msg);
            case 4:
                return ProtobufBenchmarker.deserializeProtobufFromCodedInputStream(msg);
            case 5:
                return ProtobufBenchmarker.deserializeProtobufFromByteArrayInputStream(msg);
            case 6:
                return ProtobufBenchmarker.serializeJsonToByteArray(json, useGzip);
            case 7:
                return ProtobufBenchmarker.deserializeJsonfromByteArray(json, useGzip);
            default:
                throw new Exception("Invalid method number. Did you set it correctly?");
        }
    }
}
