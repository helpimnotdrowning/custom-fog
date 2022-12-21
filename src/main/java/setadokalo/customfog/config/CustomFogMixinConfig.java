package setadokalo.customfog.config;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import setadokalo.customfog.CustomFogLogger;

public class CustomFogMixinConfig extends BaseConfig {
	public CustomFogMixinConfig(File file) {
		super(file);
	}
	public boolean useAggressiveFog = false;
	private static transient CustomFogMixinConfig config;

	@NotNull
	public static CustomFogMixinConfig getConfig() {
		if (config == null)
			config = ConfigLoader.getConfig(CustomFogMixinConfig.class, CustomFogLogger.MOD_ID + "-mixin");
		return config;
	}
}
