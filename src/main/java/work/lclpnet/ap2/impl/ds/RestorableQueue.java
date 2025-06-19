package work.lclpnet.ap2.impl.ds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.apache.commons.rng.RandomProviderState;
import org.apache.commons.rng.RestorableUniformRandomProvider;
import org.apache.commons.rng.core.RandomProviderDefaultState;
import work.lclpnet.ap2.impl.util.CodecUtil;

import java.util.List;
import java.util.function.Supplier;

public record RestorableQueue<T>(List<T> queue, RestorableUniformRandomProvider random) {

    public static final Codec<RandomProviderState> RANDOM_STATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CodecUtil.BYTE_ARRAY.fieldOf("state").forGetter((RandomProviderState state) -> {
                if (!(state instanceof RandomProviderDefaultState ds)) {
                    throw new IllegalArgumentException("Foreign state class");
                }

                return ds.getState();
            })
    ).apply(instance, RandomProviderDefaultState::new));

    public static <T> Codec<RestorableQueue<T>> createCodec(Codec<T> itemCodec, Supplier<RestorableUniformRandomProvider> randomSupplier) {
        return RecordCodecBuilder.create(instance -> instance.group(
                itemCodec.listOf().fieldOf("queue").forGetter(RestorableQueue::queue),
                createRandomCodec(randomSupplier).fieldOf("random").forGetter(RestorableQueue::random)
        ).apply(instance, RestorableQueue::new));
    }

    public static <T extends RestorableUniformRandomProvider> Codec<T> createRandomCodec(Supplier<T> randomSupplier) {
        return RANDOM_STATE_CODEC.xmap(state -> {
            T random = randomSupplier.get();
            random.restoreState(state);
            return random;
        }, RestorableUniformRandomProvider::saveState);
    }
}
