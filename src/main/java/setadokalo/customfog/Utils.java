package setadokalo.customfog;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.util.Identifier;
import setadokalo.customfog.config.DimensionConfig;
import setadokalo.customfog.config.ServerConfig;

public class Utils {

	public static final String WATER_CONFIG = "_customfog_internal:__/water/__";
	public static final String POWDER_SNOW_CONFIG = "_customfog_internal:__/snow/__";

	@NotNull
	public static DimensionConfig getDimensionConfigFor(@Nullable Identifier value) {
		ServerConfig serverConfig = CustomFogClient.serverConfig;
		if (CustomFogClient.config.overrideConfig != null)
			return CustomFogClient.config.overrideConfig;
		if (value != null && value.toString().equals(Utils.WATER_CONFIG)) {
			return Objects.requireNonNullElse(serverConfig != null ? serverConfig.waterOverride : null, CustomFogClient.config.waterConfig);
		} else if (value != null && value.toString().equals(Utils.POWDER_SNOW_CONFIG)) {
			return Objects.requireNonNullElse(serverConfig != null ? serverConfig.snowOverride : null, CustomFogClient.config.snowConfig);
		}
		if (serverConfig != null) {
			if (serverConfig.overrides.get(value) != null)
				return serverConfig.overrides.get(value);
			if (serverConfig.universalOverride != null)
				return serverConfig.universalOverride;
		}

		return Objects.requireNonNullElse(
			CustomFogClient.config.dimensions.get(value),
			Objects.requireNonNullElse(serverConfig == null ? null : serverConfig.defaultOverride,
				CustomFogClient.config.defaultConfig
			)
		);
	}

	public static boolean universalOverride() {
		return CustomFogClient.serverConfig != null && CustomFogClient.serverConfig.universalOverride != null;
	}
}
