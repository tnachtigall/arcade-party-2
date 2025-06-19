package work.lclpnet.ap2.impl.ds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JavaOps;
import org.apache.commons.rng.simple.RandomSource;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class RestorableQueueTest {

    @RepeatedTest(100)
    void createCodec_codecRestores() {
        // pretend we have some queue and random state that was used along with it
        var foo = List.of("foo", "bar", "baz");
        var random = RandomSource.XO_RO_SHI_RO_64_SS.create();

        // change the internal state of the random by some amount to introduce some extra entropy
        int amount = 31 + random.nextInt(1000);

        for (int i = 0; i < amount; i++) {
            random.nextInt();
        }

        var queue = new RestorableQueue<>(foo, random);

        // now encode
        var codec = RestorableQueue.createCodec(Codec.STRING, RandomSource.XO_RO_SHI_RO_64_SS::create);

        Object encoded = codec.encodeStart(JavaOps.INSTANCE, queue)
                .resultOrPartial(err -> System.err.println("Error encoding: " + err))
                .orElseThrow();


        // now decode
        var decoded = codec.decode(JavaOps.INSTANCE, encoded)
                .resultOrPartial(err -> System.err.println("Error decoding: " + err))
                .orElseThrow()
                .getFirst();

        // validate
        assertEquals(queue.queue(), decoded.queue());
        assertNotSame(queue.random(), decoded.random());

        for (int i = 0; i < 1000; i++) {
            assertEquals(queue.random().nextInt(), decoded.random().nextInt());
        }
    }
}