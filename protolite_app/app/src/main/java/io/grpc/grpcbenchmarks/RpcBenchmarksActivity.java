package io.grpc.grpcbenchmarks;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class RpcBenchmarksActivity extends AppCompatActivity {
    List<CardView> cardViews;
    List<RpcBenchmark> benchmarks;
    private int tasksRunning = 0;

    private Button mBenchmarkButton;
    private EditText mHostEditText;
    private EditText mConnectionsEditText;
    private Button mPingButton;
    private TextView mPingTextView;
    private CheckBox mGzip;
    private CheckBox mOkHttp;
    private EditText mPayloadEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rpc_benchmarks);

        mBenchmarkButton = (Button) findViewById(R.id.rpc_benchmarks_button);
        mHostEditText = (EditText) findViewById(R.id.host_edit_text);
        mConnectionsEditText = (EditText) findViewById(R.id.connections_edit_text);
        mPingButton = (Button) findViewById(R.id.ping_button);
        mPingTextView = (TextView) findViewById(R.id.ping_text_view);
        mGzip = (CheckBox) findViewById(R.id.gzip_json_checkbox);
        mOkHttp = (CheckBox) findViewById(R.id.okhttp_json_checkbox);
        mPayloadEditText = (EditText) findViewById(R.id.payload_edit_text);

        // set up benchmark cards
        initializeBenchmarks();
    }

    private void initializeBenchmarks() {
        benchmarks = new ArrayList<>();
        benchmarks.add(new RpcBenchmark("gRPC benchmarks", "", 0));
        benchmarks.add(new RpcBenchmark("HTTP JSON benchmarks", "", 1));

        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        LinearLayout l = (LinearLayout) findViewById(R.id.rpc_benchmark_cardlayoutlinear);
        cardViews = new ArrayList<>();

        for (final RpcBenchmark b : benchmarks) {
            final CardView cv = (CardView) inflater.inflate(R.layout.protobuf_cv, l, false);
            cv.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.cardview_light_background));
            TextView tv = (TextView) cv.findViewById(R.id.protobuf_benchmark_title);
            TextView descrip = (TextView) cv.findViewById(R.id.protobuf_benchmark_description);
            ImageButton button = (ImageButton) cv.findViewById(R.id.protobuf_benchmark_start);
            tv.setText(b.title);
            descrip.setText(b.description);
            descrip.setTypeface(Typeface.MONOSPACE);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("start benchmark here");
                    startBenchmark(cv, b);
                }
            });
            cardViews.add(cv);
            l.addView(cv);
        }
    }

    public void startBenchmark(CardView cv, RpcBenchmark b) {
        String host = mHostEditText.getText().toString();
        String numConnections = mConnectionsEditText.getText().toString();
        String payloadSize = mPayloadEditText.getText().toString();
        String useGzip = Boolean.toString(mGzip.isChecked());
        String useOkHttp = Boolean.toString(mOkHttp.isChecked());

        if (payloadSize == null || payloadSize.length() == 0) {
            payloadSize = "100";
        }

        BenchmarkAsyncTask task = new BenchmarkAsyncTask(cv, b);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, host, numConnections,
                    payloadSize, useGzip, useOkHttp);
        } else {
            task.execute(host, numConnections, payloadSize, useGzip, useOkHttp);
        }
    }

    public void pingAddress(View v) {
        String host = mHostEditText.getText().toString();
        new PingAsyncTask().execute(host);
    }

    public void beginAllBenchmarks(View v) {
        mBenchmarkButton.setEnabled(false);
        mBenchmarkButton.setText(R.string.allBenchmarksButtonDisabled);
        for (CardView cv : cardViews) {
            cv.findViewById(R.id.protobuf_benchmark_start).performClick();
        }
    }

    private class PingAsyncTask extends AsyncTask<String, Void, String> {
        @Override
        protected void onPreExecute() {
            mPingTextView.setText("Pinging host...");
            mPingButton.setEnabled(false);
        }

        @Override
        protected String doInBackground(String... args) {
            System.out.println("PING HERE");
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"ping", "-c", "4", args[0]});
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

                String s;
                int count = 0;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                    count++;
                    // 4 + 5 to get the last line
                    if (count == 9) {
                        System.out.println("done pinging");
                        return s.trim();
                    }
                }
            } catch (Exception e) {
                System.out.println("failed to ping");
                return "Failed to ping host.";
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mPingButton.setEnabled(true);
            mPingTextView.setText(result);
        }
    }

    private class BenchmarkAsyncTask extends AsyncTask<String, Void, RpcBenchmarkResult> {
        CardView cv;
        RpcBenchmark b;

        BenchmarkAsyncTask(CardView cv, RpcBenchmark b) {
            this.cv = cv;
            this.b = b;
        }

        @Override
        protected void onPreExecute() {
            tasksRunning++;
            mBenchmarkButton.setEnabled(false);
            mBenchmarkButton.setText(R.string.allBenchmarksButtonDisabled);
            cv.findViewById(R.id.protobuf_benchmark_start).setEnabled(false);
            cv.findViewById(R.id.protobuf_benchmark_start).setVisibility(View.INVISIBLE);
            cv.findViewById(R.id.protobuf_benchmark_progress).setVisibility(View.VISIBLE);
        }

        @Override
        protected RpcBenchmarkResult doInBackground(String... args) {
            try {
                boolean useOkHttp = Boolean.parseBoolean(args[4]);
                RpcBenchmarkResult res = b.run(useOkHttp, args[0], args[1], args[2], args[3]);
                return res;
            } catch (Exception e) {
                System.out.println("Exception while running benchmarks: " + e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(RpcBenchmarkResult result) {
            tasksRunning--;
            cv.findViewById(R.id.protobuf_benchmark_progress).setVisibility(View.INVISIBLE);
            cv.findViewById(R.id.protobuf_benchmark_start).setEnabled(true);
            cv.findViewById(R.id.protobuf_benchmark_start).setVisibility(View.VISIBLE);
            TextView descrip = (TextView) cv.findViewById(R.id.protobuf_benchmark_description);
            if (result != null) {
                descrip.setText(result.toString());
            } else {
                descrip.setText("Failed to benchmark. Is server running?");
            }

            if (tasksRunning == 0) {
                mBenchmarkButton.setEnabled(true);
                mBenchmarkButton.setText(R.string.allBenchmarksButtonEnabled);
            }
        }

    }
}
