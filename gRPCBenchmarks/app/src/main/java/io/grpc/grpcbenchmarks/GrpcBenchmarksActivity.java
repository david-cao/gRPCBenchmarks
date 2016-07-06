package io.grpc.grpcbenchmarks;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;

import io.grpc.grpcbenchmarks.qps.AsyncClient;
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

public class GrpcBenchmarksActivity extends AppCompatActivity {
    private Button mSendButton;
    private Button mBenchmarkButton;
    private EditText mHostEdit;
    private EditText mPortEdit;
    private EditText mMessageEdit;
    private TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grpc_benchmarks);

        mSendButton = (Button) findViewById(R.id.send_button);
        mBenchmarkButton = (Button) findViewById(R.id.benchmark_button);
        mHostEdit = (EditText) findViewById(R.id.host_edit_text);
        mPortEdit = (EditText) findViewById(R.id.port_edit_text);
        mMessageEdit = (EditText) findViewById(R.id.message_edit_text);
        mResultText = (TextView) findViewById(R.id.grpc_response_text);
    }

    public void sendMessage(View v) {
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(mHostEdit.getWindowToken(), 0);
        mSendButton.setEnabled(false);
        new GrpcTask().execute();
    }

    public void beginBenchmark(View v) {
        String host = mHostEdit.getText().toString();
        String port = mPortEdit.getText().toString();
        String addr = "--address=" + host + ":" + port;
        testPing(host);

        mBenchmarkButton.setEnabled(false);

        // We don't want benchmarks to run in parallel, so make sure they are in serial order
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new BenchmarkTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, addr,
                    "--channels=1", "--outstanding_rpcs=1", "--client_payload=100",
                    "--server_payload=100", "--streaming_rpcs");
        } else {
            new BenchmarkTask().execute(addr, "--channels=1", "--outstanding_rpcs=1",
                    "--client_payload=100", "--server_payload=100", "--streaming_rpcs");
        }
    }

    public void testPing(String host) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"ping", "-c", "4", host});
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
        } catch (Exception e) {
            System.out.println("failed to ping");
        }
    }

    private class BenchmarkTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            System.out.println("Starting gRPC benchmarks");
            mResultText.setText("Running gRPC benchmarks...");
        }

        @Override
        protected String doInBackground(String... args) {
            String results = "";
            ClientConfiguration.Builder configBuilder = ClientConfiguration.newBuilder(
                    ADDRESS, CHANNELS, OUTSTANDING_RPCS, CLIENT_PAYLOAD, SERVER_PAYLOAD,
                    TLS, TESTCA, USE_DEFAULT_CIPHERS, TRANSPORT, DURATION, WARMUP_DURATION, DIRECTEXECUTOR,
                    SAVE_HISTOGRAM, STREAMING_RPCS);
            try {
                ClientConfiguration config;
                config = configBuilder.build(args);
                AsyncClient client = new AsyncClient(config);
                RpcBenchmarkResult grpcBenchmarkResult = client.run();
                results += grpcBenchmarkResult.toString();
            } catch (Exception e) {
                System.out.println("Benchmarking error: " + e);
                configBuilder.printUsage();
            }

            return results;
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
            mResultText.setText(result);
            mBenchmarkButton.setEnabled(true);
        }
    }

    private class GrpcTask extends AsyncTask<Void, Void, String> {
        private String mHost;
        private String mMessage;
        private int mPort;
        private ManagedChannel mChannel;

        @Override
        protected void onPreExecute() {
            mHost = mHostEdit.getText().toString();
            mMessage = mMessageEdit.getText().toString();
            String portStr = mPortEdit.getText().toString();
            mPort = TextUtils.isEmpty(portStr) ? 0 : Integer.valueOf(portStr);
            mResultText.setText("");
        }

        private String sayHello(ManagedChannel channel) {
            GreeterGrpc.GreeterBlockingStub stub = GreeterGrpc.newBlockingStub(channel);
            HelloRequest message = HelloRequest.newBuilder().setName(mMessage).build();
            System.out.println("built message: " + message);
            HelloReply reply = stub.sayHello(message);
            System.out.println("Got reply: " + reply);
            return reply.getMessage();
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                mChannel = ManagedChannelBuilder.forAddress(mHost, mPort)
                        .usePlaintext(true)
                        .build();
                System.out.println("Channel after build: " + mChannel);
                return sayHello(mChannel);
            } catch (Exception e) {
                return "Failed... : " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println("Result: " + result);
            try {
                System.out.println("Channel: " + mChannel);
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            mResultText.setText(result);
            mSendButton.setEnabled(true);
        }
    }
}
