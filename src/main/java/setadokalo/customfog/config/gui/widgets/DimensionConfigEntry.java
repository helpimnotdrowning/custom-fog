package setadokalo.customfog.config.gui.widgets;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.ParentElement;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

import setadokalo.customfog.CustomFog;
import setadokalo.customfog.CustomFogClient;
import setadokalo.customfog.Utils;
import setadokalo.customfog.config.DimensionConfig;
import setadokalo.customfog.config.gui.CustomFogConfigScreen;
import setadokalo.customfog.config.gui.DimensionConfigScreen;

public class DimensionConfigEntry extends AlwaysSelectedEntryListWidget.Entry<DimensionConfigEntry> implements ParentElement {
	private static final int REMOVE_WIDGET_WIDTH = 20;
	@Nullable
	private Element focused;
	private boolean dragging;
	public final boolean removable;
	protected final boolean nonDimensionEntry; // I'm lazy as hell and I'm not gonna implement this properly
	protected TextRenderer textRenderer;
	protected final DimensionConfigListWidget parentList;
	public final Identifier originalDimId;
	@Nullable
	public Identifier dimensionId;
	@Nullable
	public Text name;
	public DimensionConfig config;
	protected final List<Element> children = new ArrayList<>();
	@Nullable
	protected TextFieldWidget dimNameWidget;
	protected TexturedButtonWidget removeWidget;
	protected ButtonWidget addWidget;
	protected ButtonWidget configureWidget;
	protected ButtonWidget pushToServerWidget;
	protected ButtonWidget pushAsOverrideWidget;

	public DimensionConfigEntry(DimensionConfigListWidget parent, boolean removable, @Nullable Identifier dimId,
										 DimensionConfig config) {
		nonDimensionEntry = false;
		this.removable = removable;
		parentList = parent;
		this.textRenderer = parent.getTextRenderer();
		dimensionId = dimId;
		originalDimId = dimId;
		this.config = config;
		if (removable) {
			dimNameWidget = new TextFieldWidget(textRenderer, 0, 0, 150, 20, Text.literal(""));
			dimNameWidget.setText(dimensionId == null ? "" : dimensionId.toString());
			lintInput();
			dimNameWidget.setChangedListener(str -> {
				dimensionId = Identifier.tryParse(str);
				lintInput();
			});
			children.add(dimNameWidget);
			removeWidget = new TexturedButtonWidget(
					-20000, -20000,
					REMOVE_WIDGET_WIDTH, 20,
					0, 20,
					20,
					new Identifier("custom-fog", "textures/gui/cfog-gui.png"),
					256, 256,
					btn -> {
						// there should never be an entry that has a visible remove widget
						// that is not also in the dimensions array with the originalDimId key
						CustomFogClient.config.dimensions.remove(this.originalDimId);
						parentList.remove(this);
					}
			);
			children.add(removeWidget);
		}
		if (CustomFogClient.serverConfig != null
			&& MinecraftClient.getInstance().player != null
			&& MinecraftClient.getInstance().player.hasPermissionLevel(3)) {
			pushToServerWidget = new TexturedButtonWidget(
				-20000, -20000,
				20, 20,
				40, 0,
				20,
				new Identifier("custom-fog", "textures/gui/cfog-gui.png"),
				256, 256,
				btn -> sendToServer(null),
				//(button, matrices, mouseX, mouseY) -> DimensionConfigEntry.this.parentList.getParent().renderTooltip(matrices, Text.translatable("tooltip.customfog.pushtoserver"), mouseX, mouseY),
				Text.literal(""));
			pushToServerWidget.setTooltip(Tooltip.of(Text.translatable("tooltip.customfog.pushtoserver")));
			children.add(pushToServerWidget);
		}
		setupConfigureButton();
	}

	private void sendToServer(@Nullable Identifier as) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeIdentifier(as != null ? as : (
			this.dimensionId != null ? this.dimensionId : new Identifier("_customfog_internal:__/default/__")
		));
		buf.writeBoolean(config.getEnabled());
		buf.writeEnumConstant(config.getType());
		buf.writeFloat(config.getLinearStart());
		buf.writeFloat(config.getLinearEnd());
		buf.writeFloat(config.getExp());
		buf.writeFloat(config.getExp2());
		ClientPlayNetworking.send(
			CustomFog.OP_UPDATE_CONFIG_PACKET_ID,
			buf
		);
	}

	public DimensionConfigEntry(DimensionConfigListWidget parent, boolean removable, @Nullable Identifier dimId,
										  DimensionConfig config, @Nullable Text nameOverride) {
		this(parent, removable, dimId, config);
		name = nameOverride;
	}

	public DimensionConfigEntry(DimensionConfigListWidget parent, boolean trueDummy) {
		originalDimId = Identifier.tryParse("");
		parentList = parent;
		nonDimensionEntry = true;
		removable = false;
		if (!trueDummy) {
			addWidget = new ButtonWidget
					.Builder(
							Text.translatable("button.customfog.add"),
							btn -> {
								parentList.removeNonDimEntries();
								parentList.add(new DimensionConfigEntry(parentList, true, null, CustomFogClient.config.defaultConfig.copy()));
							}
					)
					.dimensions(
							-2000,
							-2000,
							80,
							20
					)
					.build();
			children.add(addWidget);
		}
	}

	// Special case constructor for the "Default" entry
	public DimensionConfigEntry(@NotNull DimensionConfigListWidget parent, DimensionConfig config) {
		nonDimensionEntry = false;
		parentList = parent;
		this.textRenderer = parent.getTextRenderer();
		dimensionId = null;
		originalDimId = null;
		this.config = config;
		this.removable = false;
		setupConfigureButton();

		if (CustomFogClient.serverConfig != null
			&& MinecraftClient.getInstance().player != null
			&& MinecraftClient.getInstance().player.hasPermissionLevel(3)) {
			pushToServerWidget = new TexturedButtonWidget(
				-20000, -20000,
				20, 20,
				40, 0,
				20,
				new Identifier("custom-fog", "textures/gui/cfog-gui.png"),
				256, 256,
				btn -> sendToServer(null),
				//(button, matrices, mouseX, mouseY) -> DimensionConfigEntry.this.parentList.getParent().renderTooltip(matrices, Text.translatable("tooltip.customfog.pushtoserver"), mouseX, mouseY),
				Text.literal(""));
			pushToServerWidget.setTooltip(Tooltip.of(Text.translatable("tooltip.customfog.pushtoserver")));
			children.add(pushToServerWidget);

			pushAsOverrideWidget = new TexturedButtonWidget(
				-20000, -20000,
				20, 20,
				60, 0,
				20,
				new Identifier("custom-fog", "textures/gui/cfog-gui.png"),
				256, 256,
				btn -> sendToServer(new Identifier("_customfog_internal:__/universal/__")),
				//(button, matrices, mouseX, mouseY) -> DimensionConfigEntry.this.parentList.getParent().renderTooltip(matrices, Text.translatable("tooltip.customfog.pushtouniversal"), mouseX, mouseY),
				Text.literal(""));
			pushAsOverrideWidget.setTooltip(Tooltip.of(Text.translatable("tooltip.customfog.pushtouniversal")));
			children.add(pushAsOverrideWidget);
		}
	}

	private void setupConfigureButton() {
		// if I don't pull this part out, I get very silly errors and i dont know why

		Text tooltip = Text.literal("");
		if ((this.dimensionId != null && Utils.getDimensionConfigFor(this.dimensionId) != this.config) ||
				(this.dimensionId == null &&
						CustomFogClient.serverConfig != null &&
						CustomFogClient.serverConfig.defaultOverride != null
				)
		) {
			tooltip = Text.literal("This config is overridden by the server's config!")
					.formatted(Formatting.RED);
		}

		configureWidget = new ButtonWidget
				.Builder(
						Text.translatable("button.customfog.configure"),
						btn -> ((CustomFogConfigScreen) this.parentList.getParent()).openScreen(
								new DimensionConfigScreen(this.parentList.getParent(), this)
						)
				)
				.dimensions(
						-20000,
						-20000,
						removable ? 80 : 84 + REMOVE_WIDGET_WIDTH,
						20
				)
				.tooltip(Tooltip.of(tooltip))
				.build();
		children.add(configureWidget);
	}

	private void lintInput() {
		if (dimNameWidget == null) return;
		if (MinecraftClient.getInstance().world != null) {
			if (
				!MinecraftClient.getInstance().world.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).containsId(
					Identifier.tryParse(dimNameWidget.getText())
				)
			)
				dimNameWidget.setEditableColor(0xFF5555);
			else
				dimNameWidget.setEditableColor(0xFFFFFF);
		}
	}

	@Override
	public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX,
					   int mouseY, boolean hovered, float tickDelta) {
		if (nonDimensionEntry) {
			if (addWidget != null) {
				addWidget.setWidth(Math.min(200, entryWidth - 10));
				addWidget.setX(x + entryWidth / 2 - addWidget.getWidth() / 2);
				addWidget.setY(y);
				addWidget.render(context, mouseX, mouseY, tickDelta);
			}
			return;
		}
		if (dimNameWidget != null && removable) {
			removeWidget.setX(x + entryWidth - removeWidget.getWidth() - 8);
			removeWidget.setY(y);
			removeWidget.render(context, mouseX, mouseY, tickDelta);

			dimNameWidget.setX(x + 8);
			dimNameWidget.setY(y);
			dimNameWidget.render(context, mouseX, mouseY, tickDelta);

			configureWidget.setX(removeWidget.getX() - 4 - configureWidget.getWidth());

		} else {
			configureWidget.setX(x + entryWidth - 8 - configureWidget.getWidth());
			drawText(context, textRenderer, name != null ?
					name :
				dimensionId == null ? Text.translatable("config.customfog.default") : Text.literal(dimensionId.toString()), x + 12, y + 4, 0xFFFFFF);
		}
		configureWidget.setY(y);
		configureWidget.render(context, mouseX, mouseY, tickDelta);
		if (pushToServerWidget != null) {
			pushToServerWidget.setY(y);
			pushToServerWidget.setX(configureWidget.getX() - 4 - pushToServerWidget.getWidth());
			pushToServerWidget.render(context, mouseX, mouseY, tickDelta);
			if (pushAsOverrideWidget != null) {
				pushAsOverrideWidget.setY(y);
				pushAsOverrideWidget.setX(pushToServerWidget.getX() - 4 - pushAsOverrideWidget.getWidth());
				pushAsOverrideWidget.render(context, mouseX, mouseY, tickDelta);
			}
		}
	}

	public static void drawText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
		OrderedText orderedText = text.asOrderedText();
		context.drawTextWithShadow(textRenderer, orderedText, x, y, color);
	}
	
	public void tick() {
		if (dimNameWidget != null)
			dimNameWidget.tick();
		this.setFocused(focused);
	}

	@Override
	public List<? extends Element> children() {
		return this.children;
	}

	@Override
	public boolean isDragging() {
		return this.dragging;
	}

	@Override
	public void setDragging(boolean dragging) {
      this.dragging = dragging;
	}

	@Override
	public @Nullable Element getFocused() {
		return this.focused;
	}

	@Override
	public void setFocused(@Nullable Element focused) {
		this.focused = focused;
	}

	@Override
	public Text getNarration() {
		return Text.literal(dimensionId == null ? "Default" : dimensionId.toString());
	}
}
