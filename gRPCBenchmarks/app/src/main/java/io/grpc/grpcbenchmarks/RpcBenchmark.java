package io.grpc.grpcbenchmarks;

import java.net.URL;

import io.grpc.grpcbenchmarks.qps.AsyncClient;
import io.grpc.grpcbenchmarks.qps.AsyncJsonClient;
import io.grpc.grpcbenchmarks.qps.ClientConfiguration;

import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.ADDRESS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.CHANNELS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.CLIENT_PAYLOAD;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.DIRECTEXECUTOR;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.DURATION;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.OUTSTANDING_RPCS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.SAVE_HISTOGRAM;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.SERVER_PAYLOAD;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.STREAMING_RPCS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.TESTCA;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.TLS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.TRANSPORT;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.USE_DEFAULT_CIPHERS;
import static io.grpc.grpcbenchmarks.qps.ClientConfiguration.ClientParam.WARMUP_DURATION;

/**
 * Created by davidcao on 6/30/16.
 */
public class RpcBenchmark {

    String title;
    String description;
    int methodNumber;

    public RpcBenchmark(String title, String description, int methodNumber) {
        this.title = title;
        this.description = description;
        this.methodNumber = methodNumber;
    }

    public RpcBenchmarkResult run(String urlString, String numConnections) throws Exception {
        switch (methodNumber) {
            // TODO: Allow for customization!
            case 0:
                String addr = "--address=" + urlString + ":50052";
                String[] args = {addr, "--channels=1", "--outstanding_rpcs=" + numConnections,
                        "--client_payload=100", "--server_payload=100"};
                ClientConfiguration.Builder configBuilder = ClientConfiguration.newBuilder(
                        ADDRESS, CHANNELS, OUTSTANDING_RPCS, CLIENT_PAYLOAD, SERVER_PAYLOAD,
                        TLS, TESTCA, USE_DEFAULT_CIPHERS, TRANSPORT, DURATION, WARMUP_DURATION,
                        DIRECTEXECUTOR, SAVE_HISTOGRAM, STREAMING_RPCS);
                ClientConfiguration config;
                config = configBuilder.build(args);
                AsyncClient client = new AsyncClient(config);
                return client.run();
            case 1:
                int outstandingConnections = Integer.parseInt(numConnections);
                AsyncJsonClient jsonClient = new AsyncJsonClient(new URL("http://" + urlString +
                        ":4567/postPayload"), outstandingConnections);
                return jsonClient.run();
            default:
                throw new IllegalArgumentException("Invalid method number/tag was" +
                        " used for RpcBenchmark!");
        }
    }
}
