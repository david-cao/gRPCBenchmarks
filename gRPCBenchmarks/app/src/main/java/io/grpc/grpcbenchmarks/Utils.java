/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.grpc.grpcbenchmarks;

import static io.grpc.benchmarks.proto.Messages.SimpleResponse;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.ManagedChannel;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.benchmarks.proto.Messages;
import io.grpc.benchmarks.proto.Messages.Payload;
import io.grpc.benchmarks.proto.Messages.SimpleRequest;
import io.grpc.internal.GrpcUtil;
//import io.grpc.netty.GrpcSslContexts;
//import io.grpc.netty.NegotiationType;
//import io.grpc.netty.NettyChannelBuilder;
import io.grpc.okhttp.OkHttpChannelBuilder;
//import io.grpc.grpcbenchmarks.TestUtils;
//import io.netty.channel.EventLoopGroup;
//import io.netty.channel.epoll.EpollDomainSocketChannel;
//import io.netty.channel.epoll.EpollEventLoopGroup;
//import io.netty.channel.epoll.EpollSocketChannel;
//import io.netty.channel.nio.NioEventLoopGroup;
//import io.netty.channel.socket.nio.NioSocketChannel;
//import io.netty.channel.unix.DomainSocketAddress;
//import io.netty.handler.ssl.SslContext;
//import io.netty.handler.ssl.SslContextBuilder;
//import io.netty.handler.ssl.SslProvider;

import org.HdrHistogram.Histogram;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
//import java.util.concurrent.ThreadFactory;

import javax.annotation.Nullable;
import javax.net.ssl.SSLSocketFactory;

/**
 * Utility methods to support benchmarking classes.
 */
public final class Utils {
    private static final String UNIX_DOMAIN_SOCKET_PREFIX = "unix://";

    // The histogram can record values between 1 microsecond and 1 min.
    public static final long HISTOGRAM_MAX_VALUE = 60000000L;
    // Value quantization will be no larger than 1/10^3 = 0.1%.
    public static final int HISTOGRAM_PRECISION = 3;

    //TODO: Figure out if this matters
//  public static final int DEFAULT_FLOW_CONTROL_WINDOW =
//      NettyChannelBuilder.DEFAULT_FLOW_CONTROL_WINDOW;

    private Utils() {
    }

    public static boolean parseBoolean(String value) {
        return value.isEmpty() || Boolean.parseBoolean(value);
    }

    /**
     * Parse a {@link SocketAddress} from the given string.
     */
    public static SocketAddress parseSocketAddress(String value) {
        if (value.startsWith(UNIX_DOMAIN_SOCKET_PREFIX)) {
            throw new IllegalArgumentException("Must use a standard TCP/IP address");
            // Unix Domain Socket address.
            // Create the underlying file for the Unix Domain Socket.
//      String filePath = value.substring(UNIX_DOMAIN_SOCKET_PREFIX.length());
//      File file = new File(filePath);
//      if (!file.isAbsolute()) {
//        throw new IllegalArgumentException("File path must be absolute: " + filePath);
//      }
//      try {
//        if (file.createNewFile()) {
//          // If this application created the file, delete it when the application exits.
//          file.deleteOnExit();
//        }
//      } catch (IOException ex) {
//        throw new RuntimeException(ex);
//      }
//      // Create the SocketAddress referencing the file.
//      return new DomainSocketAddress(file);
        } else {
            // Standard TCP/IP address.
            String[] parts = value.split(":", 2);
            if (parts.length < 2) {
                throw new IllegalArgumentException(
                        "Address must be a unix:// path or be in the form host:port. Got: " + value);
            }
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);
            return new InetSocketAddress(host, port);
        }
    }

    /**
     * Create a {@link ManagedChannel} for the given parameters.
     */
    public static ManagedChannel newClientChannel(Transport transport, SocketAddress address,
                                                  boolean tls, boolean testca, @Nullable String authorityOverride, /*boolean useDefaultCiphers,
        int flowControlWindow,*/ boolean directExecutor) throws IOException {
        if (transport == Transport.OK_HTTP) {
            InetSocketAddress addr = (InetSocketAddress) address;
            OkHttpChannelBuilder builder = OkHttpChannelBuilder
                    .forAddress(addr.getHostName(), addr.getPort());
            if (directExecutor) {
                builder.directExecutor();
            }
            builder.negotiationType(tls ? io.grpc.okhttp.NegotiationType.TLS
                    : io.grpc.okhttp.NegotiationType.PLAINTEXT);
            if (tls) {
                SSLSocketFactory factory;
                if (testca) {
                    builder.overrideAuthority(
                            GrpcUtil.authorityFromHostAndPort(authorityOverride, addr.getPort()));
                    try {
                        factory = TestUtils.newSslSocketFactoryForCa(TestUtils.loadCert("ca.pem"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                }
                builder.sslSocketFactory(factory);
            }
            if (authorityOverride != null) {
                builder.overrideAuthority(authorityOverride);
            }
            return builder.build();
        }
        throw new IllegalArgumentException("Unsupported transport (Only use OK_HTTP): " + transport);

//    // It's a Netty transport.
//    SslContext sslContext = null;
//    NegotiationType negotiationType = tls ? NegotiationType.TLS : NegotiationType.PLAINTEXT;
//    if (tls && testca) {
//      File cert = TestUtils.loadCert("ca.pem");
//      SslContextBuilder sslContextBuilder = GrpcSslContexts.forClient().trustManager(cert);
//      if (transport == Transport.NETTY_NIO) {
//        sslContextBuilder = GrpcSslContexts.configure(sslContextBuilder, SslProvider.JDK);
//      } else {
//        // Native transport with OpenSSL
//        sslContextBuilder = GrpcSslContexts.configure(sslContextBuilder, SslProvider.OPENSSL);
//      }
//      if (useDefaultCiphers) {
//        sslContextBuilder.ciphers(null);
//      }
//      sslContext = sslContextBuilder.build();
//    }
//    final EventLoopGroup group;
//    final Class<? extends io.netty.channel.Channel> channelType;
//    ThreadFactory tf = new ThreadFactoryBuilder()
//        .setDaemon(true)
//        .setNameFormat("ELG-%d")
//        .build();
//    switch (transport) {
//      case NETTY_NIO:
//        group = new NioEventLoopGroup(0, tf);
//        channelType = NioSocketChannel.class;
//        break;
//
//      case NETTY_EPOLL:
//        // These classes only work on Linux.
//        group = new EpollEventLoopGroup(0, tf);
//        channelType = EpollSocketChannel.class;
//        break;
//
//      case NETTY_UNIX_DOMAIN_SOCKET:
//        // These classes only work on Linux.
//        group = new EpollEventLoopGroup(0, tf);
//        channelType = EpollDomainSocketChannel.class;
//        break;
//
//      default:
//        // Should never get here.
//        throw new IllegalArgumentException("Unsupported transport: " + transport);
//    }
//    NettyChannelBuilder builder = NettyChannelBuilder
//        .forAddress(address)
//        .eventLoopGroup(group)
//        .channelType(channelType)
//        .negotiationType(negotiationType)
//        .sslContext(sslContext)
//        .flowControlWindow(flowControlWindow);
//    if (authorityOverride != null) {
//      builder.overrideAuthority(authorityOverride);
//    }
//    if (directExecutor) {
//      builder.directExecutor();
//    }
//    return builder.build();
    }

    /**
     * Save a {@link Histogram} to a file.
     */
    public static void saveHistogram(Histogram histogram, String filename) throws IOException {
        File file;
        PrintStream log = null;
        try {
            file = new File(filename);
            if (file.exists() && !file.delete()) {
                System.err.println("Failed deleting previous histogram file: " + file.getAbsolutePath());
            }
            log = new PrintStream(new FileOutputStream(file), false);
            histogram.outputPercentileDistribution(log, 1.0);
        } finally {
            if (log != null) {
                log.close();
            }
        }
    }

    /**
     * Construct a {@link SimpleResponse} for the given request.
     */
    public static SimpleResponse makeResponse(SimpleRequest request) {
        if (request.getResponseSize() > 0) {
            if (!Messages.PayloadType.COMPRESSABLE.equals(request.getResponseType())) {
                throw Status.INTERNAL.augmentDescription("Error creating payload.").asRuntimeException();
            }

            ByteString body = ByteString.copyFrom(new byte[request.getResponseSize()]);
            Messages.PayloadType type = request.getResponseType();

            Payload payload = Payload.newBuilder().setType(type).setBody(body).build();
            return SimpleResponse.newBuilder().setPayload(payload).build();
        }
        return SimpleResponse.getDefaultInstance();
    }

    /**
     * Construct a {@link SimpleRequest} with the specified dimensions.
     */
    public static SimpleRequest makeRequest(Messages.PayloadType payloadType, int reqLength,
                                            int respLength) {
        ByteString body = ByteString.copyFrom(new byte[reqLength]);
        Payload payload = Payload.newBuilder()
                .setType(payloadType)
                .setBody(body)
                .build();

        return SimpleRequest.newBuilder()
                .setResponseType(payloadType)
                .setResponseSize(respLength)
                .setPayload(payload)
                .build();
    }

    // Taken from ProtoUtils, editted for lite
    public static <T extends Message> MethodDescriptor.Marshaller<T> jsonMarshaller(final T defaultInstance) {
        final JsonFormat.Printer printer = JsonFormat.printer();
        // TODO(carl-mastrangelo): Add support for ExtensionRegistry (TypeRegistry?)
        final JsonFormat.Parser parser = JsonFormat.parser();
        final Charset charset = Charset.forName("UTF-8");

        return new MethodDescriptor.Marshaller<T>() {
            @Override
            public InputStream stream(T value) {
                try {
                    return new ByteArrayInputStream(printer.print(value).getBytes(charset));
                } catch (InvalidProtocolBufferException e) {
                    throw Status.INTERNAL
                            .withCause(e)
                            .withDescription("Unable to print json proto")
                            .asRuntimeException();
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public T parse(InputStream stream) {
                Message.Builder builder = defaultInstance.newBuilderForType();
                Reader reader = new InputStreamReader(stream, charset);
                T proto;
                try {
                    parser.merge(reader, builder);
                    proto = (T) builder.build();
                    reader.close();
                } catch (InvalidProtocolBufferException e) {
                    throw Status.INTERNAL.withDescription("Invalid protobuf byte sequence")
                            .withCause(e).asRuntimeException();
                } catch (IOException e) {
                    // Same for now, might be unavailable
                    throw Status.INTERNAL.withDescription("Invalid protobuf byte sequence")
                            .withCause(e).asRuntimeException();
                }
                return proto;
            }
        };
    }

}
