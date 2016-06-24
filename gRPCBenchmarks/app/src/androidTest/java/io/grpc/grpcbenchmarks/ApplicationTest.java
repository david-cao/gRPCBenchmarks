package io.grpc.grpcbenchmarks;

import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.test.suitebuilder.annotation.SmallTest;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ApplicationTest {
    @Test
    public void randomProto0Json_isCorrect() throws Exception {
        String json = ProtobufRandomWriter.protoToJsonString0(ProtobufRandomWriter.randomProto0(0));
        // -181206098
        System.out.println(json.hashCode());
    }

    @Test
    public void randomProto1Json_isCorrect() throws Exception {
        String json = ProtobufRandomWriter.protoToJsonString1(ProtobufRandomWriter.randomProto1(0));
        // -1211926999
        System.out.println(json.hashCode());
    }

    @Test
    public void randomProto2Json_isCorrect() throws Exception {
        String json = ProtobufRandomWriter.protoToJsonString2(ProtobufRandomWriter.randomProto2(0));
        // -618177441
        System.out.println(json.hashCode());
    }

    @Test
    public void randomProto3Json_isCorrect() throws Exception {
        String json = ProtobufRandomWriter.protoToJsonString3(
                ProtobufRandomWriter.randomProto3(0, 10, false));
        // -989027474
        System.out.println(json.hashCode());
    }
}