package user11681.lowend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class LowEndConfiguration {
    public static final LowEndConfiguration INSTANCE = new LowEndConfiguration("lowend.json");

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().excludeFieldsWithModifiers(Modifier.FINAL).create();
    public static final JsonParser PARSER = new JsonParser();

    public final File file;

    public AverageType averageType;

    public boolean leaveTransparency;

    public LowEndConfiguration(final String file) {
        this.file = new File(FabricLoader.getInstance().getConfigDir().toFile(), file);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void write() throws Throwable {
        this.file.createNewFile();

        new FileOutputStream(this.file).write(GSON.toJson(this).getBytes());
    }

    @SuppressWarnings("StatementWithEmptyBody")
    public void read() throws Throwable {
        if (this.file.createNewFile()) {
            this.averageType = AverageType.MEAN;
            this.leaveTransparency = true;

            this.write();
        } else {
            final InputStream input = new FileInputStream(this.file);
            final byte[] content = new byte[input.available()];

            while (input.read(content) > -1);

            final JsonObject configuration = (JsonObject) PARSER.parse(new String(content));

            this.averageType = AverageType.valueOf(configuration.get("averageType").getAsString().toUpperCase());
            this.leaveTransparency = configuration.get("leaveTransparency").getAsBoolean();
        }
    }
}
