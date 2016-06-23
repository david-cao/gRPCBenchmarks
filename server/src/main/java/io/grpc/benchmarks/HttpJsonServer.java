package io.grpc.benchmarks;

import static spark.Spark.*;

/**
 * Created by davidcao on 6/21/16.
 */
public class HttpJsonServer {

    public static void main(String[] args) {

        get("/hello", Routes.getPayload());
        post("/postPayload", Routes.postPayload());

    }
}
