package io.grpc.benchmarks;

import spark.Route;

/**
 * Created by davidcao on 6/21/16.
 */
public class Routes {

    public static Route getPayload() {
        return (req, res) -> {
            res.status(200);
            res.type("application/json");
            System.out.println("get");
            return "HELLO DAVID";
        };
    }

    public static Route postPayload() {
        return (req, res) -> {
            String payload = req.body();
//            System.out.println(payload);

            if (payload != null && payload.length() > 0) {
                return "{\"payload\":" + payload + "}";
            } else {
                return "{\"Error\":\"This is an error message\"}";
            }
        };
    }

}
