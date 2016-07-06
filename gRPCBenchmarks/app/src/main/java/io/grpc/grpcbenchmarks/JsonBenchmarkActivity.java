package io.grpc.grpcbenchmarks;

import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.net.URL;

import io.grpc.grpcbenchmarks.qps.AsyncJsonClient;

public class JsonBenchmarkActivity extends AppCompatActivity {
    private Button mSendButton;
    private Button mBenchmarkButton;
    private EditText mHostEdit;
    private EditText mPortEdit;
    private TextView mResultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_json_benchmark);

        mSendButton = (Button) findViewById(R.id.send_button);
        mBenchmarkButton = (Button) findViewById(R.id.benchmark_button);
        mHostEdit = (EditText) findViewById(R.id.host_edit_text);
        mPortEdit = (EditText) findViewById(R.id.port_edit_text);
        mResultText = (TextView) findViewById(R.id.grpc_response_text);
    }

    public void sendMessage(View v) {
//        String host = mHostEdit.getText().toString();
//        String port = mPortEdit.getText().toString();
//        String urlString = "http://" + host + ":" + port + "/postPayload";
//        try {
//            AsyncJsonClient jsonClient = new AsyncJsonClient(new URL(urlString));
//            jsonClient.run();
//        } catch (Exception e) {
//            System.out.println("Exception! " + e);
//        }
    }

    public void beginBenchmark(View v) {
        String host = mHostEdit.getText().toString();
        String port = mPortEdit.getText().toString();
        String urlString = "http://" + host + ":" + port + "/postPayload";

        // We don't want benchmarks to run in parallel, so make sure they are in serial order
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new CallAPI().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, urlString);
        } else {
            new CallAPI().execute(urlString);
        }
    }

    private class CallAPI extends AsyncTask<String, Void, String> {
        @Override
        public void onPreExecute() {
            mBenchmarkButton.setEnabled(false);
        }

        // Format should be url, payload, other params
        @Override
        protected String doInBackground(String... params) {
//            String result = "";
//            String urlString = params[0];
//            try {
//                AsyncJsonClient jsonClient = new AsyncJsonClient(new URL(urlString));
//                RpcBenchmarkResult res = jsonClient.run();
//                result = res.toString();
//            } catch (Exception e) {
//                System.out.println("Exception! " + e);
//            } finally {
//                return result;
//            }
            return "blah";
        }

        @Override
        protected void onPostExecute(String res) {
            mBenchmarkButton.setEnabled(true);
            mResultText.setText(res);
        }
    }
}
