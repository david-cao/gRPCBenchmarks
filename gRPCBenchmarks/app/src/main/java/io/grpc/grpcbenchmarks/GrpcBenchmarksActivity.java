package io.grpc.grpcbenchmarks;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
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
        new BenchmarkTask().execute("--address=192.168.42.245:50052");
    }

    private class BenchmarkTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected void onPreExecute() {
//
//        }

        @Override
        protected String doInBackground(String... args) {
            try {
                String ip="www.google.com"; String pingResult="  ";

                Runtime r=Runtime.getRuntime();
                Process p=r.exec(new String[] {"ping", "-c 4", ip});
                BufferedReader in=new BufferedReader(new InputStreamReader(p.getInputStream()));
                String inputLine;
                while( (inputLine = in.readLine()) != null)
                {
                    pingResult=inputLine;
                }
                System.out.println(pingResult);
                in.close();

//                String args[] = {"", "2"};

                ClientConfiguration.Builder configBuilder = ClientConfiguration.newBuilder(
                        ADDRESS, CHANNELS, OUTSTANDING_RPCS, CLIENT_PAYLOAD, SERVER_PAYLOAD,
                        TLS, TESTCA, USE_DEFAULT_CIPHERS, TRANSPORT, DURATION, WARMUP_DURATION, DIRECTEXECUTOR,
                        SAVE_HISTOGRAM, STREAMING_RPCS);
                ClientConfiguration config;
                try {
                    config = configBuilder.build(args);
                    AsyncClient client = new AsyncClient(config);
                    client.run();
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    configBuilder.printUsage();
                }

            } catch (IOException e) {
                System.out.println("Exception " + e);
            }

            ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            return cm.getActiveNetworkInfo().toString();
        }

        @Override
        protected void onPostExecute(String result) {
            System.out.println(result);
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
//                return "Failed... : " + Arrays.toString(e.getStackTrace());
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
