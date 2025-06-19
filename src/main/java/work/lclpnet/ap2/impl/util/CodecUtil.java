package work.lclpnet.ap2.impl.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;

public class CodecUtil {

    public static final Codec<Double> FINITE_DOUBLE = validate(Codec.DOUBLE, Double::isFinite, v -> "Value must be finite: " + v);

    public static final Codec<byte[]> BYTE_ARRAY = Codec.BYTE.listOf().xmap(list -> {
        byte[] arr = new byte[list.size()];

        for (int i = 0; i < arr.length; i++) {
            arr[i] = list.get(i);
        }

        return arr;
    }, arr -> {
        var list = new ArrayList<Byte>(arr.length);

        for (byte b : arr) {
            list.add(b);
        }

        return list;
    });

    public static <T> Codec<T> validate(Codec<T> codec, Predicate<T> predicate, Function<T, String> errorMessage) {
        return codec.validate(value -> predicate.test(value)
                ? DataResult.success(value)
                : DataResult.error(() -> errorMessage.apply(value)));
    }

    private CodecUtil() {}
}
