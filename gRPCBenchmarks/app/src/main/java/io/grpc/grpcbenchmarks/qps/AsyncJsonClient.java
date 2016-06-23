package io.grpc.grpcbenchmarks.qps;

import org.json.JSONObject;

import io.grpc.grpcbenchmarks.GrpcBenchmarkResult;

/**
 * Created by davidcao on 6/22/16.
 */
public class AsyncJsonClient {
//    public GrpcBenchmarkResult run() throws Exception {
//
//    }

    public static void main(String[] args) {
        try {
            System.out.println(newJsonRequest());
        } catch (Exception e) {
            System.out.println("failed: " + e);
        }
    }

    private static String newJsonRequest() throws Exception {
        JSONObject simpleRequest = new JSONObject();
        JSONObject payload = new JSONObject();
        payload.put("type", 0);
        payload.put("body", new byte[100]);

        simpleRequest.put("payload", payload);
        simpleRequest.put("type", 0);
        simpleRequest.put("responseSize", 100);

        return simpleRequest.toString();
    }
}