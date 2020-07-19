package user11681.lowend;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum AverageType {
    NONE,
    MEAN,
    MEDIAN,
    MODE
}
