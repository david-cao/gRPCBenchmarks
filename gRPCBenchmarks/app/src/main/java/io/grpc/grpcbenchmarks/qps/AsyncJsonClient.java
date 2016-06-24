package io.grpc.grpcbenchmarks.qps;

import static io.grpc.grpcbenchmarks.Utils.HISTOGRAM_MAX_VALUE;
import static io.grpc.grpcbenchmarks.Utils.HISTOGRAM_PRECISION;

import com.google.common.base.Preconditions;

import android.os.AsyncTask;

import org.HdrHistogram.Histogram;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.grpc.Context;
import io.grpc.grpcbenchmarks.GrpcBenchmarkResult;

/**
 * Created by davidcao on 6/22/16.
 */
public class AsyncJsonClient {
    private URL url;

    public static void main(String[] args) {

    }

    public AsyncJsonClient(URL url) {
        this.url = url;
    }

    public void run() throws Exception {
        System.out.println("Starting json benchmarking");
        String simpleRequest = newJsonRequest();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        System.out.println("writing histogram");
        long startTime = System.nanoTime();
        long endTime = startTime + TimeUnit.NANOSECONDS.toNanos(10 * 1000);
        Histogram histogram = doPosts(connection, simpleRequest, endTime);
        long elapsedTime = System.nanoTime() - startTime;

        printStats(histogram, elapsedTime);

//        long latency50 = histogram.getValueAtPercentile(50);
//        long latency90 = histogram.getValueAtPercentile(90);
//        long latency95 = histogram.getValueAtPercentile(95);
//        long latency99 = histogram.getValueAtPercentile(99);
//        long latency999 = histogram.getValueAtPercentile(99.9);
//        long latencyMax = histogram.getValueAtPercentile(100);
//        long queriesPerSecond = histogram.getTotalCount() * 1000000000L / elapsedTime;
    }

    private String newJsonRequest() throws Exception {
        JSONObject simpleRequest = new JSONObject();
        JSONObject payload = new JSONObject();
        payload.put("type", 0);
        payload.put("body", new byte[100]);

        simpleRequest.put("payload", payload);
        simpleRequest.put("type", 0);
        simpleRequest.put("responseSize", 100);

        return simpleRequest.toString();
    }

    private Histogram doPosts(HttpURLConnection connection, String simpleRequest,
                              long endTime) throws Exception {

        //possibly some checks here if we have different types of calls
        final Histogram histogram = new Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION);
        final HistogramFuture future = new HistogramFuture(histogram);

        new ConnectionTask(connection, histogram, future, endTime).execute(simpleRequest);

        return future.get();
    }

//    private Future<Histogram> doPosts(HttpURLConnection connection, byte[] simpleRequest,
//                                      Histogram histogram, Future<Histogram> future, long endTime) {
//
//        long lastCall = System.nanoTime();
//
//        try {
//            connection.setRequestMethod("POST");
//            connection.setDoOutput(true);
////                connection.setChunkedStreamingMode(0);
//            connection.setFixedLengthStreamingMode(simpleRequest.length);
//
//            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
//            out.write(simpleRequest);
//            out.close();
//
//            InputStream in = new BufferedInputStream(connection.getInputStream());
//            String response = IOUtils.toString(in);
//            System.out.println("Reponse: " + response);
//
//            in.close();
//
//            long now = System.nanoTime();
//            histogram.recordValue((now - lastCall) / 1000);
//            if (endTime > now) {
//                return
//            } else {
//
//            }
//
//        } catch (IOException e) {
//
//        }
//    }

    private void printStats(Histogram histogram, long elapsedTime) {
        long latency50 = histogram.getValueAtPercentile(50);
        long latency90 = histogram.getValueAtPercentile(90);
        long latency95 = histogram.getValueAtPercentile(95);
        long latency99 = histogram.getValueAtPercentile(99);
        long latency999 = histogram.getValueAtPercentile(99.9);
        long latencyMax = histogram.getValueAtPercentile(100);
        long queriesPerSecond = histogram.getTotalCount() * 1000000000L / elapsedTime;

        StringBuilder values = new StringBuilder();
        values.append("Channels:                       ").append("TODO").append('\n')
                .append("Outstanding RPCs per Channel:   ").append("TODO").append('\n')
                .append("Server Payload Size:            ").append("TODO").append('\n')
                .append("Client Payload Size:            ").append("TODO").append('\n')
                .append("50%ile Latency (in micros):     ").append(latency50).append('\n')
                .append("90%ile Latency (in micros):     ").append(latency90).append('\n')
                .append("95%ile Latency (in micros):     ").append(latency95).append('\n')
                .append("99%ile Latency (in micros):     ").append(latency99).append('\n')
                .append("99.9%ile Latency (in micros):   ").append(latency999).append('\n')
                .append("Maximum Latency (in micros):    ").append(latencyMax).append('\n')
                .append("QPS:                            ").append(queriesPerSecond).append('\n');
        System.out.println(values);
    }

    private class ConnectionTask extends AsyncTask<String, Void, Void> {
        private Histogram histogram;
        private HistogramFuture future;
        private HttpURLConnection connection;
        private long lastCall = System.nanoTime();
        private long endTime;

        public ConnectionTask(HttpURLConnection connection, Histogram histogram,
                              HistogramFuture future, long endTime) {
            this.connection = connection;
            this.histogram = histogram;
            this.future = future;
            this.endTime = endTime;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                byte simpleRequest[] = params[0].getBytes();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(simpleRequest.length);

                lastCall = System.nanoTime();
                System.out.println("time: " + lastCall + ", end: " + endTime);
                while (lastCall < endTime) {
                    OutputStream out = new BufferedOutputStream(connection.getOutputStream());
                    out.write(simpleRequest);
                    out.close();

                    InputStream in = new BufferedInputStream(connection.getInputStream());
                    String response = IOUtils.toString(in);
                    System.out.println("Reponse: " + response);

                    in.close();

                    long now = System.nanoTime();
                    histogram.recordValue((now - lastCall) / 1000);
                    lastCall = now;
                }
            } catch (IOException e) {
                System.out.println("IO EXCEPTION IN ASYNC: " + e);
            } finally {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Void nothing) {
            future.done();
        }
    }

    private static class HistogramFuture implements Future<Histogram> {
        private final Histogram histogram;
        private boolean canceled;
        private boolean done;

        HistogramFuture(Histogram histogram) {
            Preconditions.checkNotNull(histogram, "histogram");
            this.histogram = histogram;
        }

        @Override
        public synchronized boolean cancel(boolean mayInterruptIfRunning) {
            if (!done && !canceled) {
                canceled = true;
                notifyAll();
                return true;
            }
            return false;
        }

        @Override
        public synchronized boolean isCancelled() {
            return canceled;
        }

        @Override
        public synchronized boolean isDone() {
            return done || canceled;
        }

        @Override
        public synchronized Histogram get() throws InterruptedException, ExecutionException {
            while (!isDone() && !isCancelled()) {
                wait();
            }

            if (isCancelled()) {
                throw new CancellationException();
            }

            done = true;
            return histogram;
        }

        @Override
        public Histogram get(long timeout, TimeUnit unit) throws InterruptedException,
                ExecutionException,
                TimeoutException {
            throw new UnsupportedOperationException();
        }

        private synchronized void done() {
            done = true;
            notifyAll();
        }
    }

}