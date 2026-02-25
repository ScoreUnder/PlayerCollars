package org.jlortiz.playercollars.network;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.codecs.EitherCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record PawsPermissionData<T>(
        @NotNull List<Either<TagKey<T>, RegistryKey<T>>> permittedList,
        boolean isDenyList
) {
    public static <T> @NotNull PawsPermissionData<T> empty() {
        return new PawsPermissionData<>(List.of(), false);
    }

    public static <T> @NotNull Codec<PawsPermissionData<T>> getCodec(@NotNull RegistryKey<Registry<T>> key) {
        return Codec.withAlternative(
                RecordCodecBuilder.create(i -> i.group(
                                getListCodec(key).fieldOf("permittedList").forGetter(PawsPermissionData::permittedList),
                                Codec.BOOL.fieldOf("isDenyList").forGetter(PawsPermissionData::isDenyList))
                        .apply(i, PawsPermissionData::new)),
                getDeprecatedCodecs(key));
    }

    private static <T> @NotNull Codec<PawsPermissionData<T>> getDeprecatedCodecs(@NotNull RegistryKey<Registry<T>> key) {
        return Codec.of(Encoder.error("deprecated"), Codec.withAlternative(
                new ListCodec<>(new EitherCodec<>(TagKey.codec(key), RegistryKey.createCodec(key)), 0, 65536),
                Codec.of(Encoder.error("deprecated"), new ListCodec<>(Identifier.CODEC, 0, 65535).map((x) -> {
                    List<Either<TagKey<T>, RegistryKey<T>>> ls = new ArrayList<>(x.size());
                    for (Identifier id : x) {
                        ls.add(Either.right(RegistryKey.of(key, id)));
                    }
                    return ls;
                }))).map(l -> new PawsPermissionData<>(l, false)));
    }

    public static <T> @NotNull ListCodec<Either<TagKey<T>, RegistryKey<T>>> getListCodec(@NotNull RegistryKey<Registry<T>> key) {
        return new ListCodec<>(new EitherCodec<>(TagKey.codec(key), RegistryKey.createCodec(key)), 0, 65535);
    }
}
