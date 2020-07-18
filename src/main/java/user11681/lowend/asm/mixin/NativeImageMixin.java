package user11681.lowend.asm.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.nio.ByteBuffer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(NativeImage.class)
public abstract class NativeImageMixin {
    @Shadow
    public int width;

    @Shadow
    public int height;

    @Shadow public abstract int getPixelColor(final int x, final int y);

    @Shadow public abstract void setPixelColor(final int x, final int y, final int color);

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "read(Lnet/minecraft/client/texture/NativeImage$Format;Ljava/nio/ByteBuffer;)Lnet/minecraft/client/texture/NativeImage;", at = @At("RETURN"), cancellable = true)
    private static void reduceTexture(final NativeImage.Format format, final ByteBuffer byteBuffer, final CallbackInfoReturnable<NativeImage> info) {
        final NativeImageMixin image = (NativeImageMixin) (Object) info.getReturnValue();
        final int height = image.height;
        final int width = image.width;
        final int[][] originalColors = new int[height][width];
        final Int2IntMap counts = new Int2IntOpenHashMap(height * width);

        for (int v = 0; v < height; v++) {
            for (int u = 0; u < image.width; u++) {
                final int color = image.getPixelColor(u, v);

                if (color != 0) {
                    counts.put(color, counts.getOrDefault(color, 0) + 1);
                    originalColors[v][u] = color;
                }
            }
        }

        int mode = 0;
        int highestCount = 0;

        for (final Int2IntMap.Entry entry : counts.int2IntEntrySet()) {
            final int count = entry.getIntValue();

            if (count > highestCount) {
                mode = entry.getIntKey();
            }
        }

        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                image.setPixelColor(u, v, originalColors[v][u] & 0xFF000000 | mode);
            }
        }
    }
}
