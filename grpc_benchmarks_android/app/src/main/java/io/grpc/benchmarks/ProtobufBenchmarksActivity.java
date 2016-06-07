package io.grpc.benchmarks;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.protobuf.nano.MessageNano;

public class ProtobufBenchmarksActivity extends Activity implements AdapterView.OnItemSelectedListener {
    private Button mBenchmarkButton;
    private TextView mTextView;
    private Spinner mSpinner;
    private CheckBox mCheckBox;
    private BenchmarkAsyncTask benchmarkTask;
    private int selected = 0;
    private boolean useGzip = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protobuf_benchmarks);
        mBenchmarkButton = (Button) findViewById(R.id.protobuf_benchmarks_button);
        mTextView = (TextView) findViewById(R.id.protobuf_benchmarks_textview);
        mCheckBox = (CheckBox) findViewById(R.id.protobuf_benchmarks_gzipcheck);
        mSpinner = (Spinner) findViewById(R.id.protobuf_benchmarks_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.protobuf_benchmarks_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);
    }

    public void beginBenchmarks(View v) {
        System.out.println("Beginning protobuf benchmarks");

        v.setKeepScreenOn(true);

        mBenchmarkButton.setEnabled(false);
        benchmarkTask = new BenchmarkAsyncTask();
        benchmarkTask.execute();
    }

    public void stopBenchmarks(View v) {
        if (benchmarkTask.cancel(true)) {
            mTextView.append("\nCanceled benchmarks");
            resetBenchmarkButton();
        }
    }

    public void resetBenchmarkButton() {
        mBenchmarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                beginBenchmarks(v);
            }
        });
        mBenchmarkButton.setText("Begin Benchmarks");
        mBenchmarkButton.setEnabled(true);
    }

    //BEGIN OnItemSelectedListener
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        System.out.println("picked " + pos);
        selected = pos;
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    //TODO: Expand into multiple
    private class BenchmarkAsyncTask extends AsyncTask<Integer, String, Void> {

        @Override
        protected void onPreExecute() {
            mTextView.setText("Running benchmarks...");
            useGzip = mCheckBox.isChecked();
            mBenchmarkButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    stopBenchmarks(v);
                }
            });
            mBenchmarkButton.setText("Stop Benchmarks");
            mBenchmarkButton.setEnabled(true);
        }

        @Override
        protected Void doInBackground(Integer... protos) {
            try {
                MessageNano message;
                String jsonString;
                System.out.println("Selected is " + selected + ", use gzip: " + useGzip);
                switch (selected) {
                    case 0:
                        message = ProtobufRandomWriter.randomProto0();
                        break;
                    case 1:
                        message = ProtobufRandomWriter.randomProto1();
                        break;
                    case 2:
                        message = ProtobufRandomWriter.randomProto2();
                        break;
                    case 3:
                        message = ProtobufRandomWriter.randomProto3(60, false);
                        break;
                    case 4:
                        message = ProtobufRandomWriter.randomProto3(60, true);
                        break;
                    case 5:
                        message = ProtobufRandomWriter.randomProto3(10, true);
                        break;
                    default:
                        message = ProtobufRandomWriter.randomProto0();
                        break;
                }
                jsonString = ProtobufRandomWriter.protoToJsonString(message);

                // protobuf benchmarks
                BenchmarkResult res = ProtobufBenchmarker.serializeProtobufToByteArray(message);
                publishProgress(res.toString());
                res = ProtobufBenchmarker.serializeProtobufToByteBuffer(message);
                publishProgress(res.toString());
                res = ProtobufBenchmarker.deserializeProtobufFromByteArray(message);
                publishProgress(res.toString());

                // JSON benchmarks
                res = ProtobufBenchmarker.serializeJsonToByteArray(jsonString, useGzip);
                publishProgress(res.toString());
                res = ProtobufBenchmarker.deserializeJsonfromByteArray(jsonString, useGzip);
                publishProgress(res.toString());
            } catch (Exception e) {
                System.out.println("Exception while running benchmarks: " + e);
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... results) {
            mTextView.append("\n- " + results[0] + "\n");
            System.out.println(results[0]);
        }

        @Override
        protected void onPostExecute(Void nothing) {
            mTextView.append("\nDone with async benchmark");
            resetBenchmarkButton();
        }
    }
}
