package io.grpc.grpcbenchmarks;

import org.junit.Test;

import io.grpc.benchmarks.AddressBook;
import io.grpc.benchmarks.FriendsList;
import io.grpc.benchmarks.SmallRequest;
import io.grpc.benchmarks.Things;

import static org.junit.Assert.*;

public class ProtobufUnitTest {

    /*
    Note: Unit testing with random functions is hard! Usually you want to run some sort of
    statistical analysis, but this should be fine for sanity checks.
     */

    @Test
    public void randomProto0_isCorrect() throws Exception {
        SmallRequest s = (SmallRequest) ProtobufRandomWriter.randomProto0(0);
        assertEquals(s.getName(), "K");
        System.out.println(ProtobufRandomWriter.randomProto0(0));
    }

    @Test
    public void randomProto1_isCorrect() throws Exception {
        AddressBook a = (AddressBook) ProtobufRandomWriter.randomProto1(0);
        assertEquals(a.hashCode(), -1324536560);
        System.out.println(a.hashCode());
    }

    @Test
    public void randomProto2_isCorrect() throws Exception {
        FriendsList f = (FriendsList) ProtobufRandomWriter.randomProto2(0);
        assertEquals(f.hashCode(), 588207811);
        System.out.println(f.hashCode());
    }

    @Test
    public void randomProto3_isCorrect() throws Exception {
        Things t = (Things) ProtobufRandomWriter.randomProto3(0, 10, false);
        assertEquals(t.hashCode(), 501397407);
        System.out.println(t.hashCode());
    }

}