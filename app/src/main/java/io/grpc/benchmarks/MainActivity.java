package io.grpc.benchmarks;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void showProtobufBenchmarks(View v) {
        System.out.println("Show protobuf view here");
        Intent intent = new Intent(this, ProtobufBenchmarksActivity.class);
        startActivity(intent);
    }

    public void showGrpcBenchmarks(View v) {
        System.out.println("Show grpc view here");
        Intent intent = new Intent(this, GrpcBenchmarksActivity.class);
        startActivity(intent);
    }
}
