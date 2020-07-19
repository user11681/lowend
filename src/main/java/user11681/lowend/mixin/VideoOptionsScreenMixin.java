package user11681.lowend.mixin;

import java.util.Arrays;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.VideoOptionsScreen;
import net.minecraft.client.gui.screen.options.GameOptionsScreen;
import net.minecraft.client.options.BooleanOption;
import net.minecraft.client.options.CyclingOption;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.Option;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import user11681.lowend.AverageType;
import user11681.lowend.LowEndConfiguration;

@Environment(EnvType.CLIENT)
@Mixin(VideoOptionsScreen.class)
public abstract class VideoOptionsScreenMixin extends GameOptionsScreen {
    @Unique
    private static final String AVERAGE_TYPE_KEY = "options.lowend.average_type";
    @Unique
    private static final String LEAVE_TRANSPARENCY_KEY = "options.lowend.leave_transparency";

    @Unique
    private static final LowEndConfiguration CONFIGURATION = LowEndConfiguration.INSTANCE;

    @Unique
    private static final CyclingOption AVERAGE_TYPE = new CyclingOption(
            AVERAGE_TYPE_KEY,
            (final GameOptions options, final Integer amount) -> {
                final AverageType type = CONFIGURATION.averageType;
                final AverageType[] types = AverageType.values();

                CONFIGURATION.averageType = types[(type.ordinal() + amount) % types.length];
            },
            (final GameOptions options, final CyclingOption option) -> {
                final String name = CONFIGURATION.averageType.name().toLowerCase();

                return new TranslatableText("%s: %s", I18n.translate(AVERAGE_TYPE_KEY), name.substring(0, 1).toUpperCase() + name.substring(1));
            }
    );

    @Unique
    private static final BooleanOption LEAVE_TRANSPARENCY = new BooleanOption(
            LEAVE_TRANSPARENCY_KEY,
            (final GameOptions options) -> CONFIGURATION.leaveTransparency,
            (final GameOptions options, final Boolean value) -> CONFIGURATION.leaveTransparency = value
    );

    @Shadow
    @Final
    @Mutable
    @SuppressWarnings("unused")
    private static final Option[] OPTIONS = addButtons();

    @Shadow
    @Final
    private int mipmapLevels;

    @Unique
    private final AverageType averageType = CONFIGURATION.averageType;

    @Unique
    private final boolean leaveTransparency = CONFIGURATION.leaveTransparency;

    public VideoOptionsScreenMixin(final Screen parent, final GameOptions gameOptions, final Text title) {
        super(parent, gameOptions, title);
    }

    @Accessor("OPTIONS")
    private static Option[] getOptions() {
        return null;
    }

    private static Option[] addButtons() {
        final Option[] options = getOptions();
        final Option[] newOptions = Arrays.copyOf(options, options.length + 2);

        newOptions[options.length] = AVERAGE_TYPE;
        newOptions[options.length + 1] = LEAVE_TRANSPARENCY;

        return newOptions;
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void reloadTextures(final CallbackInfo info) throws Throwable {
        if (this.mipmapLevels == this.gameOptions.mipmapLevels && (this.averageType != CONFIGURATION.averageType || this.leaveTransparency != CONFIGURATION.leaveTransparency)) {
            LowEndConfiguration.INSTANCE.write();

            assert this.client != null;

            this.client.reloadResourcesConcurrently();
        }
    }
}
