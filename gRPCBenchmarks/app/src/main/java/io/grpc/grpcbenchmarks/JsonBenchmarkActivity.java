package io.grpc.grpcbenchmarks;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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
        String host = mHostEdit.getText().toString();
        String port = mPortEdit.getText().toString();
        new CallAPI().execute("http://" + host + ":" + port + "/postPayload", "TEST");
    }

    public void beginBenchmark(View v) {

    }

    private class CallAPI extends AsyncTask<String, Void, String> {

        public CallAPI() {
        }

        // Format should be url, payload, other params
        @Override
        protected String doInBackground(String... params) {
            String urlString = params[0];
            byte payload[] = params[1].getBytes();

            InputStream in;
            try {
                URL url = new URL(urlString);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
//                connection.setChunkedStreamingMode(0);
                connection.setFixedLengthStreamingMode(payload.length);

                OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                out.write(payload);
                out.close();

                in = new BufferedInputStream(connection.getInputStream());

                String response = IOUtils.toString(in);
                System.out.println("Reponse: " + response);

                connection.disconnect();

                return response;
            } catch (Exception e) {
                System.out.println("Connection error: " + e);
                return "Error";
            }
        }

        @Override
        protected void onPostExecute(String res) {
            mResultText.setText(res);
        }
    }

}
