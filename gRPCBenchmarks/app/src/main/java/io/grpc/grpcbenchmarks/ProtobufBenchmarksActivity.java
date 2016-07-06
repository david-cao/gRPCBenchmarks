package io.grpc.grpcbenchmarks;

import com.google.protobuf.MessageLite;

import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

public class ProtobufBenchmarksActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    List<CardView> cardViews;
    List<Benchmark> benchmarks;

    private Button mBenchmarkButton;
    private Spinner mSpinner;
    private CheckBox mCheckBox;

    private MessageLite sharedMessage;
    private String sharedJson;
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

        for (final Benchmark b : benchmarks) {
            final CardView cv = (CardView) inflater.inflate(R.layout.protobuf_cv, l, false);
            cv.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(),
                    R.color.cardview_light_background));
            TextView tv = (TextView) cv.findViewById(R.id.protobuf_benchmark_title);
            TextView descrip = (TextView) cv.findViewById(R.id.protobuf_benchmark_description);
            ImageButton button = (ImageButton) cv.findViewById(R.id.protobuf_benchmark_start);
            tv.setText(b.title);
            descrip.setText(b.description);
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

    private void initializeBenchmarks() {
        benchmarks = new ArrayList<>();
        benchmarks.add(new Benchmark("Serialize protobuf to byte array", "", 0));
        benchmarks.add(new Benchmark("Serialize protobuf to CodedOutputStream", "", 1));
        benchmarks.add(new Benchmark("Serialize protobuf to ByteArrayOutputStream", "", 2));
        benchmarks.add(new Benchmark("Deserialize protobuf from byte array", "", 3));
        benchmarks.add(new Benchmark("Deserialize protobuf from CodedInputStream", "", 4));
        benchmarks.add(new Benchmark("Deserialize protobuf from ByteArrayInputStream", "", 5));
        benchmarks.add(new Benchmark("Serialize JSON to byte array", "", 6));
        benchmarks.add(new Benchmark("Deserialize JSON from byte array", "", 7));
    }

    public void beginAllBenchmarks(View v) {
        System.out.println("Beginning protobuf benchmarks");

        if (tasksRunning == 0) {
            switch (selected) {
                case 0:
                    sharedMessage = ProtobufRandomWriter.randomProto0();
                    sharedJson = ProtobufRandomWriter.protoToJsonString0(sharedMessage);
                    break;
                case 1:
                    sharedMessage = ProtobufRandomWriter.randomProto1();
                    sharedJson = ProtobufRandomWriter.protoToJsonString1(sharedMessage);
                    break;
                case 2:
                    sharedMessage = ProtobufRandomWriter.randomProto2();
                    sharedJson = ProtobufRandomWriter.protoToJsonString2(sharedMessage);
                    break;
                case 3:
                    sharedMessage = ProtobufRandomWriter.randomProto3(60, false);
                    sharedJson = ProtobufRandomWriter.protoToJsonString3(sharedMessage);
                    break;
                case 4:
                    sharedMessage = ProtobufRandomWriter.randomProto3(60, true);
                    sharedJson = ProtobufRandomWriter.protoToJsonString3(sharedMessage);
                    break;
                case 5:
                    sharedMessage = ProtobufRandomWriter.randomProto3(10, true);
                    sharedJson = ProtobufRandomWriter.protoToJsonString3(sharedMessage);
                    break;
                default:
                    sharedMessage = ProtobufRandomWriter.randomProto0();
                    sharedJson = ProtobufRandomWriter.protoToJsonString0(sharedMessage);
                    break;
            }
            mBenchmarkButton.setEnabled(false);
            mBenchmarkButton.setText(R.string.allBenchmarksButtonDisabled);
            for (CardView cv : cardViews) {
                cv.findViewById(R.id.protobuf_benchmark_start).performClick();
            }
        }
    }

    public void startBenchmark(CardView cv, Benchmark b) {
        BenchmarkAsyncTask task = new BenchmarkAsyncTask(cv, b);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        } else {
            task.execute();
        }
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
                MessageLite message;
                String jsonString;
                if (sharedMessage != null) {
                    System.out.println("Using shared message");
                    message = sharedMessage;
                    jsonString = sharedJson;
                } else {
                    switch (selected) {
                        case 0:
                            message = ProtobufRandomWriter.randomProto0();
                            jsonString = ProtobufRandomWriter.protoToJsonString0(message);
                            break;
                        case 1:
                            message = ProtobufRandomWriter.randomProto1();
                            jsonString = ProtobufRandomWriter.protoToJsonString1(message);
                            break;
                        case 2:
                            message = ProtobufRandomWriter.randomProto2();
                            jsonString = ProtobufRandomWriter.protoToJsonString2(message);
                            break;
                        case 3:
                            message = ProtobufRandomWriter.randomProto3(60, false);
                            jsonString = ProtobufRandomWriter.protoToJsonString3(message);
                            break;
                        case 4:
                            message = ProtobufRandomWriter.randomProto3(60, true);
                            jsonString = ProtobufRandomWriter.protoToJsonString3(message);
                            break;
                        case 5:
                            message = ProtobufRandomWriter.randomProto3(10, true);
                            jsonString = ProtobufRandomWriter.protoToJsonString3(message);
                            break;
                        default:
                            message = ProtobufRandomWriter.randomProto0();
                            jsonString = ProtobufRandomWriter.protoToJsonString0(message);
                            break;
                    }
                }

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
