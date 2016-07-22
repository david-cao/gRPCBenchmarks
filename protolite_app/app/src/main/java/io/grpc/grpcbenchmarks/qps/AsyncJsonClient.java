package io.grpc.grpcbenchmarks.qps;

import static io.grpc.grpcbenchmarks.Utils.HISTOGRAM_MAX_VALUE;
import static io.grpc.grpcbenchmarks.Utils.HISTOGRAM_PRECISION;

import com.google.common.base.Preconditions;

import android.util.Base64;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPOutputStream;

import io.grpc.grpcbenchmarks.RpcBenchmarkResult;

/**
 * Created by davidcao on 6/22/16.
 */
public class AsyncJsonClient {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final long DURATION = 60 * 1000000000L;
    private static final long WARMUP_DURATION = 10 * 1000000000L;

    private URL url;
    private int outstandingConnections;
    private int clientPayload;
    private int serverPayload;
    private boolean useGzip;

    public AsyncJsonClient(URL url, int outstandingConnections, int payloadSize, boolean useGzip) {
        this.url = url;
        this.clientPayload = payloadSize;
        this.serverPayload = payloadSize;
        this.outstandingConnections = outstandingConnections;
        this.useGzip = useGzip;
    }

    public RpcBenchmarkResult run() throws Exception {
        System.out.println("Starting json benchmarking");
        String simpleRequest = newJsonRequest();

        // Run the connection once to get an estimate for packet size, then run benchmarks
        long estimatedHeaderSize = estimateHeaderSize(simpleRequest.getBytes());
        estimatedHeaderSize *= 2;
        estimatedHeaderSize += simpleRequest.getBytes().length;
        //TODO (davidcao): Header will not be the same both ways, have two methods

        System.setProperty("http.keepAlive", "true");

        // Run warmups for 10 seconds
        warmUp(url, simpleRequest, WARMUP_DURATION);

        // Run actual benchmarks
        long startTime = System.nanoTime();
        long endTime = startTime + DURATION;
        List<Histogram> histograms = doBenchmarks(url, simpleRequest, endTime);
        long elapsedTime = System.nanoTime() - startTime;
        Histogram merged = merge(histograms);

        printStats(merged, estimatedHeaderSize, elapsedTime);

        long latency50 = merged.getValueAtPercentile(50);
        long latency90 = merged.getValueAtPercentile(90);
        long latency95 = merged.getValueAtPercentile(95);
        long latency99 = merged.getValueAtPercentile(99);
        long latency999 = merged.getValueAtPercentile(99.9);
        long latencyMax = merged.getValueAtPercentile(100);
        long queriesPerSecond = merged.getTotalCount() * 1000000000L / elapsedTime;

        return new RpcBenchmarkResult(1, outstandingConnections, serverPayload, clientPayload,
                latency50, latency90, latency95, latency99, latency999, latencyMax,
                queriesPerSecond, estimatedHeaderSize);
    }

    public RpcBenchmarkResult runOkHttp() throws Exception {
        System.out.println("Starting json benchmarking");
        String simpleRequest = newJsonRequest();

        // Run the connection once to get an estimate for packet size, then run benchmarks
        long estimatedHeaderSize = estimateHeaderSize(simpleRequest.getBytes());
        estimatedHeaderSize *= 2;
        estimatedHeaderSize += simpleRequest.getBytes().length;
        //TODO (davidcao): Header will not be the same both ways, have two methods

        long startTime = System.nanoTime();
        long endTime = startTime + DURATION;
        List<Histogram> histograms = doBenchmarksOkHttp(url, simpleRequest, endTime);
        long elapsedTime = System.nanoTime() - startTime;
        Histogram merged = merge(histograms);

        printStats(merged, estimatedHeaderSize, elapsedTime);

        long latency50 = merged.getValueAtPercentile(50);
        long latency90 = merged.getValueAtPercentile(90);
        long latency95 = merged.getValueAtPercentile(95);
        long latency99 = merged.getValueAtPercentile(99);
        long latency999 = merged.getValueAtPercentile(99.9);
        long latencyMax = merged.getValueAtPercentile(100);
        long queriesPerSecond = merged.getTotalCount() * 1000000000L / elapsedTime;

        return new RpcBenchmarkResult(1, outstandingConnections, serverPayload, clientPayload,
                latency50, latency90, latency95, latency99, latency999, latencyMax,
                queriesPerSecond, estimatedHeaderSize);
    }

    private void warmUp(URL url, String simpleRequest, long duration) throws Exception {
        long warmupEndTime = System.nanoTime() + duration;
        doBenchmarks(url, simpleRequest, warmupEndTime);

        System.gc();
    }

    // Really rough way of getting packet size, since if we wanted to get actual packet size
    // we'd either need to use a deprecated library or write our own socket handler.
    // TODO (davidcao): figure out a better way to do this
    private long estimateHeaderSize(byte[] simpleRequest) {
        long packetSize = -1;
        try {
            packetSize = 0;
            HttpURLConnection connection;
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setFixedLengthStreamingMode(simpleRequest.length);

            OutputStream out = new BufferedOutputStream(connection.getOutputStream());
            out.write(simpleRequest);
            out.close();

            Map<String, List<String>> headers = connection.getHeaderFields();
            for (Map.Entry<String, List<String>> entry: headers.entrySet()) {
                if (entry.getKey() != null) {
                    packetSize += entry.getKey().getBytes().length;
                }
                if (entry.getValue() != null) {
                    for (String field: entry.getValue()) {
                        packetSize += field.getBytes().length;
                    }
                }
            }

            connection.disconnect();
        } catch (Exception e) {
            System.out.println("Failed to estimate packet size: " + e);
        } finally {
            return packetSize;
        }
    }

    // TODO: Make this like the actual gRPC benchmark
    private String newJsonRequest() throws Exception {
        JSONObject simpleRequest = new JSONObject();
        JSONObject payload = new JSONObject();
        payload.put("type", 0);
        payload.put("body", Base64.encodeToString(new byte[clientPayload], Base64.DEFAULT));

        simpleRequest.put("payload", payload);
        simpleRequest.put("type", 0);
        simpleRequest.put("responseSize", serverPayload);

        System.out.println("JSON: " + simpleRequest.toString());

        return simpleRequest.toString();
    }

    private List<Histogram> doBenchmarks(URL url, String simpleRequest,
                                   long endTime) throws Exception {
        //TODO (davidcao): possibly some checks here if we have different types of calls (unlikely)
        List<HistogramFuture> futures = new ArrayList<HistogramFuture>();
        for (int i = 0; i < outstandingConnections; ++i) {
            final Histogram histogram = new Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION);
            final HistogramFuture future = new HistogramFuture(histogram);
            futures.add(future);

            doPosts(histogram, future, url, simpleRequest, endTime);
        }

        List<Histogram> histograms = new ArrayList<Histogram>();
        for (HistogramFuture future: futures) {
            histograms.add(future.get());
        }
        return histograms;
    }


    private void doPosts(Histogram histogram, HistogramFuture future,
                         URL url, String payload, long endTime) {
        try {
            byte simpleRequest[] = payload.getBytes();
            HttpURLConnection connection;
            long lastCall = System.nanoTime();

            while (lastCall < endTime) {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                OutputStream out;
                if (useGzip) {
                    out = new GZIPOutputStream(connection.getOutputStream());
                } else {
                    connection.setFixedLengthStreamingMode(simpleRequest.length);
                    out = new BufferedOutputStream(connection.getOutputStream());
                }
                out.write(simpleRequest);
                out.close();

                InputStream in = new BufferedInputStream(connection.getInputStream());
                // read input to simulate actual use case
                IOUtils.toString(in);
                in.close();

                connection.disconnect();

                long now = System.nanoTime();
                histogram.recordValue((now - lastCall) / 1000);
                lastCall = now;
            }
        } catch (IOException e) {
            System.out.println("IO EXCEPTION IN ASYNC: " + e);
        } finally {
            future.done();
        }
    }

    private List<Histogram> doBenchmarksOkHttp(URL url, String simpleRequest,
                                               long endTime) throws Exception {
        List<Histogram> histograms = new ArrayList<Histogram>();
        OkHttpClient client = new OkHttpClient();

        RequestBody body;
        if (useGzip) {
            byte payloadBytes[] = simpleRequest.getBytes();
            ByteArrayOutputStream bos = new ByteArrayOutputStream(payloadBytes.length);
            GZIPOutputStream gos = new GZIPOutputStream(bos);
            gos.write(payloadBytes);
            gos.close();
            bos.close();
            body = RequestBody.create(JSON, bos.toByteArray());
        } else {
            body = RequestBody.create(JSON, simpleRequest);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        for (int i = 0; i < outstandingConnections; ++i) {

            Histogram histogram = new Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION);
            long lastCall = System.nanoTime();
            while (lastCall < endTime) {
                Response response = client.newCall(request).execute();
                response.body().string();

                long now = System.nanoTime();
                histogram.recordValue((now - lastCall) / 1000);
                lastCall = now;
            }

            histograms.add(histogram);
        }
        return histograms;
    }

    private Histogram merge(List<Histogram> histograms) {
        Histogram merged = new Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION);
        for (Histogram histogram : histograms) {
            for (HistogramIterationValue value : histogram.allValues()) {
                long latency = value.getValueIteratedTo();
                long count = value.getCountAtValueIteratedTo();
                merged.recordValueWithCount(latency, count);
            }
        }
        return merged;
    }

    private void printStats(Histogram histogram, long serializedSize, long elapsedTime) {
        long latency50 = histogram.getValueAtPercentile(50);
        long latency90 = histogram.getValueAtPercentile(90);
        long latency95 = histogram.getValueAtPercentile(95);
        long latency99 = histogram.getValueAtPercentile(99);
        long latency999 = histogram.getValueAtPercentile(99.9);
        long latencyMax = histogram.getValueAtPercentile(100);
        long queriesPerSecond = histogram.getTotalCount() * 1000000000L / elapsedTime;

        StringBuilder values = new StringBuilder();
        values
                .append("Channels:                       ").append("TODO").append('\n')
                .append("Outstanding RPCs per Channel:   ").append("TODO").append('\n')
                .append("Server Payload Size:            ").append(serverPayload).append('\n')
                .append("Client Payload Size:            ").append(clientPayload).append('\n')
                .append("50%ile Latency (in micros):     ")
                .append(latency50).append('\n')
                .append("90%ile Latency (in micros):     ")
                .append(latency90).append('\n')
                .append("95%ile Latency (in micros):     ")
                .append(latency95).append('\n')
                .append("99%ile Latency (in micros):     ")
                .append(latency99).append('\n')
                .append("99.9%ile Latency (in micros):   ")
                .append(latency999).append('\n')
                .append("Maximum Latency (in micros):    ")
                .append(latencyMax).append('\n')
                .append("QPS:                            ").append(queriesPerSecond).append('\n')
                .append("Size of request:                ").append(serializedSize).append('\n');
        System.out.println(values);
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