package user11681.lowend.mixin;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import user11681.lowend.LowEndConfiguration;

@Environment(EnvType.CLIENT)
@Mixin(NativeImage.class)
public abstract class NativeImageMixin {
    private final NativeImage self = (NativeImage) (Object) this;

    @Shadow
    public static int getRed(final int color) {
        return 0;
    }

    @Shadow
    public static int getGreen(final int color) {
        return 0;
    }

    @Shadow
    public static int getBlue(final int color) {
        return 0;
    }

    @Shadow
    public static int getAbgrColor(final int alpha, final int blue, final int green, final int red) {
        return 0;
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void loadConfig(final CallbackInfo info) throws Throwable {
        LowEndConfiguration.INSTANCE.read();
    }

    @Shadow
    public abstract byte getPixelOpacity(final int x, final int y);

    @SuppressWarnings("InvalidMemberReference")
    @Inject(method = {"<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZJ)V", "<init>(Lnet/minecraft/client/texture/NativeImage$Format;IIZ)V"}, at = @At("RETURN"))
    private void reduceTexture(final NativeImage.Format format, final int width, final int height, final boolean useStb, final long pointer, final CallbackInfo info) {
        switch (LowEndConfiguration.INSTANCE.averageType) {
            case MEAN:
                this.mean(width, height);
                break;
            case MEDIAN:
                this.median(width, height);
                break;
            case MODE:
                this.mode(width, height);
        }
    }

    @Unique
    private void mean(final int width, final int height) {
        final int[] components = new int[4];
        int visible = 0;

        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                final int rgba = self.getPixelColor(u, v);
                int alpha = self.getPixelOpacity(u, v);

                if (alpha < 0) {
                    alpha += 0xFF;
                }

                components[0] += getRed(rgba);
                components[1] += getGreen(rgba);
                components[2] += getBlue(rgba);
                components[3] += alpha;

                if (alpha != 0) {
                    ++visible;
                }
            }
        }

        if (visible > 0) {
            for (int i = 0; i < components.length; i++) {
                components[i] /= visible;
            }

            if (LowEndConfiguration.INSTANCE.leaveTransparency) {
                for (int v = 0; v < height; v++) {
                    for (int u = 0; u < width; u++) {
                        self.setPixelColor(u, v, getAbgrColor(self.getPixelOpacity(u, v), components[2], components[1], components[0]));
                    }
                }
            } else {
                for (int v = 0; v < height; v++) {
                    for (int u = 0; u < width; u++) {
                        self.setPixelColor(u, v, getAbgrColor(components[3], components[2], components[1], components[0]));
                    }
                }
            }
        }
    }

    @Unique
    private void median(final int width, final int height) {
        final IntList colors = this.getColors(width, height, false);
        final int size = colors.size();

        if (size > 0) {
            final int color = colors.getInt(colors.size() / 2);

            if (LowEndConfiguration.INSTANCE.leaveTransparency) {
                for (int v = 0; v < height; v++) {
                    for (int u = 0; u < width; u++) {
                        self.setPixelColor(u, v, this.getPixelOpacity(u, v) << 24 | color & 0xFFFFFF);
                    }
                }
            } else {
                for (int v = 0; v < height; v++) {
                    for (int u = 0; u < width; u++) {
                        self.setPixelColor(u, v, color);
                    }
                }
            }
        }
    }

    @Unique
    private void mode(final int width, final int height) {
        final Int2IntMap counts = new Int2IntOpenHashMap(height * width);
        final boolean leaveTransparency = LowEndConfiguration.INSTANCE.leaveTransparency;

        if (leaveTransparency) {
            for (int v = 0; v < height; v++) {
                for (int u = 0; u < width; u++) {
                    final int color = self.getPixelColor(u, v);

                    counts.put(color, counts.getOrDefault(color, 0) + 1);
                }
            }
        } else {
            for (int v = 0; v < height; v++) {
                for (int u = 0; u < width; u++) {
                    final int color = self.getPixelColor(u, v);

                    counts.put(color, counts.getOrDefault(color, 0) + 1);
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

        mode &= 0xFFFFFF;

        if (leaveTransparency) {
            for (int v = 0; v < height; v++) {
                for (int u = 0; u < width; u++) {
                    self.setPixelColor(u, v, self.getPixelOpacity(u, v) << 24 | mode);
                }
            }
        } else {
            for (int v = 0; v < height; v++) {
                for (int u = 0; u < width; u++) {
                    self.setPixelColor(u, v, mode);
                }
            }
        }
    }

    private IntList getColors(final int width, final int height, final boolean sort) {
        final IntList colors = new IntArrayList();

        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                if (self.getPixelOpacity(u, v) != 0) {
                    final int color = self.getPixelColor(u, v);

                    colors.add(color > 0 ? color : 0xFF000000 + color + 0xFFFFFF);
                }
            }
        }

        if (sort) {
            colors.sort((final Integer first, final Integer second) -> {
                final int firstRed = getRed(first);
                final int secondRed = getRed(second);

                if (firstRed > secondRed) {
                    return firstRed - secondRed;
                }

                return secondRed - firstRed;
            });
        }

        return colors;
    }
}
