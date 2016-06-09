package io.grpc.benchmarks;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.protobuf.nano.MessageNano;

import java.util.ArrayList;
import java.util.List;

public class ProtobufBenchmarksActivity extends Activity implements AdapterView.OnItemSelectedListener {
    List<CardView> cardViews;
    List<Benchmark> benchmarks;

    private Button mBenchmarkButton;
    private Spinner mSpinner;
    private CheckBox mCheckBox;

    private MessageNano sharedMessage;

    private int selected = 0;
    private int tasksRunning = 0;
    private boolean useGzip = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protobuf_benchmarks);

        mBenchmarkButton = (Button) findViewById(R.id.protobuf_benchmarks_button);
        mCheckBox = (CheckBox) findViewById(R.id.protobuf_benchmarks_gzipcheck);

        // set up spinner
        mSpinner = (Spinner) findViewById(R.id.protobuf_benchmarks_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.protobuf_benchmarks_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(this);

        // set up benchmark cards
        initializeBenchmarks();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        LinearLayout l = (LinearLayout) findViewById(R.id.protobuf_benchmark_cardlayoutlinear);
        cardViews = new ArrayList<>();

        for (final Benchmark b: benchmarks) {
            final CardView cv = (CardView) inflater.inflate(R.layout.protobuf_cv, null, false);
            cv.setCardBackgroundColor(getResources().getColor(R.color.cardview_light_background));
            TextView tv = (TextView) cv.findViewById(R.id.protobuf_benchmark_title);
            TextView descrip = (TextView) cv.findViewById(R.id.protobuf_benchmark_description);
            ImageButton button = (ImageButton) cv.findViewById(R.id.protobuf_benchmark_start);
            tv.setText(b.title);
            descrip.setText(b.description);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startBenchmark(cv, b);
                }
            });

            cardViews.add(cv);
            l.addView(cv);
        }
    }

    private void initializeBenchmarks() {
        benchmarks = new ArrayList<>();
        benchmarks.add(new Benchmark("Serialize to byte array", "description", 0));
        benchmarks.add(new Benchmark("Serialize to CodedOutputByteBufferNano", "description", 1));
        benchmarks.add(new Benchmark("Deserialize from byte array", "description", 2));
        benchmarks.add(new Benchmark("JSON serialize to byte array", "description", 3));
        benchmarks.add(new Benchmark("JSON deserialize from byte array", "description", 4));
    }

    public void beginAllBenchmarks(View v) {
        System.out.println("Beginning protobuf benchmarks");

        if (tasksRunning == 0) {
            switch (selected) {
                case 0:
                    sharedMessage = ProtobufRandomWriter.randomProto0();
                    break;
                case 1:
                    sharedMessage = ProtobufRandomWriter.randomProto1();
                    break;
                case 2:
                    sharedMessage = ProtobufRandomWriter.randomProto2();
                    break;
                case 3:
                    sharedMessage = ProtobufRandomWriter.randomProto3(60, false);
                    break;
                case 4:
                    sharedMessage = ProtobufRandomWriter.randomProto3(60, true);
                    break;
                case 5:
                    sharedMessage = ProtobufRandomWriter.randomProto3(10, true);
                    break;
                default:
                    sharedMessage = ProtobufRandomWriter.randomProto0();
                    break;
            }
            mBenchmarkButton.setEnabled(false);
            mBenchmarkButton.setText(R.string.allBenchmarksButtonDisabled);
            for (CardView cv: cardViews) {
                cv.findViewById(R.id.protobuf_benchmark_start).performClick();
            }
        }
    }

    public void startBenchmark(CardView cv, Benchmark b) {
        BenchmarkAsyncTask task = new BenchmarkAsyncTask(cv, b);
        task.execute();
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

// ************************************************************************************************

    private class Benchmark {
        String title;
        String description;
        int methodNumber;

        Benchmark(String title, String description, int methodNumber) {
            this.title = title;
            this.description = description;
            this.methodNumber = methodNumber;
        }

        public BenchmarkResult run(MessageNano message, String jsonString, boolean useGzip)
                throws Exception
        {
            switch (methodNumber) {
                case 0:
                    return ProtobufBenchmarker.serializeProtobufToByteArray(message);
                case 1:
                    return ProtobufBenchmarker.serializeProtobufToByteBuffer(message);
                case 2:
                    return ProtobufBenchmarker.deserializeProtobufFromByteArray(message);
                case 3:
                    return ProtobufBenchmarker.serializeJsonToByteArray(jsonString, useGzip);
                case 4:
                    return ProtobufBenchmarker.deserializeJsonfromByteArray(jsonString, useGzip);
                default:
                    return ProtobufBenchmarker.serializeProtobufToByteArray(message);
            }
        }
    }

    //TODO: Expand into multiple
    private class BenchmarkAsyncTask extends AsyncTask<Integer, Integer, BenchmarkResult> {

        CardView cv;
        Benchmark b;

        BenchmarkAsyncTask(CardView cv, Benchmark b) {
            this.cv = cv;
            this.b = b;
        }

        @Override
        protected void onPreExecute() {
            useGzip = mCheckBox.isChecked();
            tasksRunning++;
            mBenchmarkButton.setEnabled(false);
            mBenchmarkButton.setText(R.string.allBenchmarksButtonDisabled);
            cv.findViewById(R.id.protobuf_benchmark_start).setEnabled(false);
            cv.findViewById(R.id.protobuf_benchmark_start).setVisibility(View.INVISIBLE);
            cv.findViewById(R.id.protobuf_benchmark_progress).setVisibility(View.VISIBLE);
        }

        @Override
        protected BenchmarkResult doInBackground(Integer... inputs) {
            try {
                MessageNano message;
                String jsonString;

                if (sharedMessage != null) {
                    System.out.println("Using shared message");
                    message = sharedMessage;
                } else {
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
                }
                jsonString = ProtobufRandomWriter.protoToJsonString(message);

                BenchmarkResult res = b.run(message, jsonString, useGzip);

                System.out.println(res.toString());
                return res;
            } catch (Exception e) {
                System.out.println("Exception while running benchmarks: " + e);
            }
            return null;
        }

        @Override
        protected void onCancelled() {
            System.out.println("onCancelled called");
        }

        @Override
        protected void onPostExecute(BenchmarkResult res) {
            tasksRunning--;
            cv.findViewById(R.id.protobuf_benchmark_progress).setVisibility(View.INVISIBLE);
            cv.findViewById(R.id.protobuf_benchmark_start).setEnabled(true);
            cv.findViewById(R.id.protobuf_benchmark_start).setVisibility(View.VISIBLE);
            TextView descrip = (TextView) cv.findViewById(R.id.protobuf_benchmark_description);
            descrip.setText(res.toString());

            if (tasksRunning == 0) {
                sharedMessage = null;
                mBenchmarkButton.setEnabled(true);
                mBenchmarkButton.setText(R.string.allBenchmarksButtonEnabled);
            }
        }
    }
}
