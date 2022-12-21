package setadokalo.customfog.config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.Level;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;

import setadokalo.customfog.CustomFog;

public class ServerConfig extends BaseConfig {
	public static String CONFIG_NAME = CustomFog.MOD_ID + "-server";
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(Identifier.class, new Identifier.Serializer())
		.enableComplexMapKeySerialization()
		.serializeNulls()
		.setPrettyPrinting()
		.create();

	public ServerConfig(File file) {
		super(file);
	}

	public static ServerConfig getConfig() {
		CustomFog.log(Level.INFO, "Loading server config file");
		return ConfigLoader.getConfig(ServerConfig.class, CONFIG_NAME, GSON);
	}

	public void saveConfig() {
		ConfigLoader.saveConfig(this);
	}



	private transient File file;

	public boolean baseModAllowed = true;
	//TODO: Implement lava configuration

	@Nullable
	public DimensionConfig defaultOverride = null;
	@Nullable
	public DimensionConfig waterOverride = null;
	@Nullable
	public DimensionConfig snowOverride = null;
	@Nullable
	public DimensionConfig universalOverride = null;

	public Map<Identifier, DimensionConfig> overrides = new HashMap<>();
}
