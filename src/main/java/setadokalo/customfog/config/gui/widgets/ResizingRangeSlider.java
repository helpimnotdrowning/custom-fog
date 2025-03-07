package setadokalo.customfog.config.gui.widgets;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.client.gui.DrawContext;
import org.jetbrains.annotations.NotNull;

import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public class ResizingRangeSlider extends SliderWidget {
	final boolean displayPercent;
	final double defaultMax;
	double max;
	final Consumer<Double> setter;
	final Function<Double, Text> displayTextProducer;

	public ResizingRangeSlider(
			int x, int y,
			int width, int height,
			boolean displayPercent,
			double value, double defaultMax,
			Consumer<Double> setter,
			Function<Double, Text> display) {
		super(x, y, width, height, Text.literal(""), value / defaultMax);
		this.displayPercent = displayPercent;
		this.defaultMax = defaultMax;
		this.max = defaultMax;
		this.setter = setter;
		displayTextProducer = display;
		this.updateMessage();
		setValue(getValue());
	}

	private double clickOriginX = 0.0;
	private double clickOriginY = 0.0;
	protected boolean isDrag = false;

	/// Used for displaying the cursor
	protected int focusedTicks;
	// start and end of cursor selection - if no text is selected, they are equal
	protected int selectedStart;
	protected int selectedEnd;
	protected boolean isTyping = false;
	@NotNull
	protected String currentText = "";

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (this.active && this.visible) {
			if (this.isValidClickButton(button)) {
				if (this.clicked(mouseX, mouseY)) {
					this.playDownSound(MinecraftClient.getInstance().getSoundManager());
					this.onClick(mouseX, mouseY);
					return true;
				} else if (this.isTyping) {
						this.finishTyping();
				}
			}

		}
		return false;
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		isDrag = false;
		clickOriginX = mouseX;
		clickOriginY = mouseY;
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double deltaX, double deltaY) {
		if (isDrag || Math.abs(clickOriginX - mouseX) > 3 || Math.abs(clickOriginY - mouseY) > 3) {
			super.onDrag(mouseX, mouseY, deltaX, deltaY);
			isDrag = true;
		}
	}

	@Override
	public void onRelease(double mouseX, double mouseY) {
		if (!isDrag && Math.abs(clickOriginX - mouseX) < 3 && Math.abs(clickOriginY - mouseY) < 3) {
			if (!isTyping) {
				setFocused(true);
				startTyping();
			} else {
				int i = MathHelper.floor(mouseX) - this.getX() - 4;
				selectedStart = (MinecraftClient.getInstance().textRenderer.trimToWidth(currentText, i).length());
				if (!Screen.hasShiftDown()) {
					selectedEnd = selectedStart;
				}
			}
		}
		super.onRelease(mouseX, mouseY);
		setValue(getValue());
	}

	private static boolean isNumericChar(char c, boolean isFirst) {
		return switch (c) {
			case '-', '+' -> isFirst;
			case '.' -> !isFirst;
			default -> c >= '0' && c <= '9';
		};
	}

	protected void typeString(String str) {
		if (selectedStart != currentText.length() || selectedEnd != currentText.length()) {
			int curStart = Math.min(selectedStart, selectedEnd);
			int curEnd = Math.max(selectedStart, selectedEnd);
			currentText = currentText.substring(0, curStart) + str + currentText.substring(curEnd);
			selectedStart = curStart + str.length();
			selectedEnd = curStart + str.length();
		} else {
			currentText = currentText + str;
			selectedStart += str.length();
			selectedEnd += str.length();
		}
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (isNumericChar(chr, currentText.isEmpty()) && currentText.length() < 20) {
			typeString(String.valueOf(chr));
			updateMessage();
		}

		return true;
	}

	private double truncateVal(double value) {
		return ((double)Math.round(value * (displayPercent ? 10000.0 : 100.0))) / (displayPercent ? 10000.0 : 100.0);
	}

	protected void setValue(double val) {
		val = truncateVal(val);

		double d = getValue();
		if (val <= max / 2.0 && max > defaultMax) {
			while (val <= max / 2.0 && max > defaultMax) {
				max = Math.round(max / 2.0);
			}
		} else if (val > max) {
			while (val > max) {
				max = max * 2.0;
			}
		}
		this.value = toInternal(val);
		if (d != getValue()) {
			this.applyValue();
		}
		this.updateMessage();
	}

	protected void startTyping() {
		isTyping = true;
		focusedTicks = 0;
		currentText = Double.toString(getValue() * (displayPercent ? 100.0 : 1.0));
		int decimalPos = currentText.indexOf(".");
		if (decimalPos != -1 && decimalPos < currentText.length() - 3) {
			currentText = currentText.substring(0, decimalPos + 2);
		}
		selectedStart = currentText.length();
		selectedEnd = selectedStart;
		updateMessage();
	}

	protected void finishTyping() {
		isTyping = false;
		try {
			setValue(Float.parseFloat(currentText) / (displayPercent ? 100.0 : 1.0));
		} catch (NumberFormatException e) {
			setValue(fromInternal(value));
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!this.isTyping) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		} else {
			switch (keyCode) {
				case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
					finishTyping();
					return true;
				}
				case GLFW.GLFW_KEY_BACKSPACE -> {
					if (currentText.length() > 0) {
						if (selectedStart != currentText.length() || selectedEnd != currentText.length()) {
							if (selectedStart != selectedEnd) {
								int curStart = Math.min(selectedStart, selectedEnd);
								int curEnd = Math.max(selectedStart, selectedEnd);
								currentText = currentText.substring(0, curStart) + currentText.substring(curEnd);
								selectedStart = curStart;
								selectedEnd = curStart;
							} else if (selectedStart > 0) {
								currentText = currentText.substring(0, selectedStart - 1) + currentText.substring(selectedStart);
								selectedStart -= 1;
								selectedEnd = selectedStart;
							}
						} else {
							currentText = currentText.substring(0, currentText.length()-1);
							selectedStart -= 1;
							selectedEnd -= 1;
						}
						updateMessage();
					}
					return true;
				}
				case GLFW.GLFW_KEY_DELETE -> {
					if (currentText.length() > 0) {
						if (selectedStart != currentText.length() || selectedEnd != currentText.length()) {
							if (selectedStart != selectedEnd) {
								int curStart = Math.min(selectedStart, selectedEnd);
								int curEnd = Math.max(selectedStart, selectedEnd);
								currentText = currentText.substring(0, curStart) + currentText.substring(curEnd);
								selectedStart = curStart;
								selectedEnd = curStart;
							} else if (selectedStart < currentText.length()) {
								currentText = currentText.substring(0, selectedStart) + currentText.substring(selectedStart + 1);
							}
						}
						updateMessage();
					}
					return true;
				}
				case GLFW.GLFW_KEY_END, GLFW.GLFW_KEY_PAGE_DOWN -> {
					selectedStart = currentText.length();
					if (!Screen.hasShiftDown())
						selectedEnd = selectedStart;
				}
				case GLFW.GLFW_KEY_HOME, GLFW.GLFW_KEY_PAGE_UP -> {
					selectedStart = 0;
					if (!Screen.hasShiftDown())
						selectedEnd = selectedStart;
				}
				case GLFW.GLFW_KEY_LEFT -> {
					selectedStart = Math.max(selectedStart - 1, 0);
					if (!Screen.hasShiftDown())
						selectedEnd = selectedStart;
				}
				case GLFW.GLFW_KEY_RIGHT -> {
					selectedStart = Math.min(selectedStart + 1, currentText.length());
					if (!Screen.hasShiftDown())
						selectedEnd = selectedStart;
				}
			}
			return false;
		}
	}

	public void tick() {
		++this.focusedTicks;
	}

	@Override
	protected void updateMessage() {
		if (!isTyping) {
			this.setMessage(displayTextProducer.apply(getValue() * (displayPercent ? 100.0 : 1.0)));
		} else {
			this.setMessage(Text.literal(""));
		}
	}

	protected double toInternal(double value) {
		return value / max;
	}

	protected double fromInternal(double value) {
		return value * max;
	}

	public double getValue() {
		return fromInternal(this.value);
	}

	@Override
	protected void applyValue() {
		setter.accept(getValue());
	}

	@Override
	public void setFocused(boolean focused) {
		super.setFocused(focused);
	}

	//@Override
	//protected void onFocusedChanged(boolean newFocused) {
	//	if (!newFocused && isTyping) {
	//		finishTyping();
	//	}
	//}

	@Override
	public void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderButton(context, mouseX, mouseY, delta);
		if (isTyping) {
			MinecraftClient minecraftClient = MinecraftClient.getInstance();
			TextRenderer textRenderer = minecraftClient.textRenderer;
			boolean isValid = true;
			try {
				Double.parseDouble(currentText);
			} catch (NumberFormatException e) {
				isValid = false;
			}
			context.drawTextWithShadow(textRenderer, Text.of(currentText), getX() + 4, this.getY() + (this.height / 2 - 4), isValid ? 0xFFFFFF : 0xFF5555);

			if (selectedEnd != selectedStart) {
				int sStart = Math.min(Math.min(selectedStart, selectedEnd), currentText.length());
				int sEnd = Math.min(Math.max(selectedStart, selectedEnd), currentText.length());
				int startX = this.getX() + 4 + textRenderer.getWidth(currentText.substring(0, sStart));
				int endX = this.getX() + 4 + textRenderer.getWidth(currentText.substring(0, sEnd));
				RenderSystem.setShader(GameRenderer::getPositionColorProgram);
				BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
				bufferBuilder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
				Matrix4f mat = context.getMatrices().peek().getPositionMatrix();
				bufferBuilder.vertex(mat, (float)startX, (float)(this.getY() + ((this.height - 8)) / 2) + textRenderer.fontHeight + 1.0F, -40.0F)
						.color(255, 255, 255, 255).next();
				bufferBuilder.vertex(mat, (float)endX, (float)(this.getY() + ((this.height - 8)) / 2) + textRenderer.fontHeight + 1.0F, -40.0F)
						.color(255, 255 ,255, 255).next();
				bufferBuilder.vertex(mat, (float)endX,   (float)(this.getY() + ((this.height - 8)) / 2) - 1.0F, -40.0F)
						.color(255, 255, 255, 255).next();
				bufferBuilder.vertex(mat, (float)startX,   (float)(this.getY() + ((this.height - 8)) / 2) - 1.0F, -40.0F)
						.color(255, 255, 255, 255).next();
				BufferRenderer.draw(bufferBuilder.end());
				context.drawTextWithShadow(textRenderer, currentText.substring(sStart, sEnd), startX + 1, this.getY() + (this.height / 2 - 3), 0x30000000);
				context.drawTextWithShadow(textRenderer, currentText.substring(sStart, sEnd), startX, this.getY() + (this.height / 2 - 4), 0x0000FF);
			}

			if (focusedTicks / 6 % 2 == 0) {
				if (selectedStart != currentText.length() || selectedEnd != currentText.length()) {
					int textWidth = textRenderer.getWidth(currentText.substring(0, selectedStart));
					context.drawTextWithShadow(textRenderer, "|", getX() + 4 + textWidth, this.getY() + (this.height - 8) / 2, 0x888888);
				} else {
					int textWidth = textRenderer.getWidth(currentText);
					context.drawTextWithShadow(textRenderer, "_", getX() + 4 + textWidth, this.getY() + (this.height - 8) / 2, 0x888888);
				}
			}
		}
	}

	// TODO: FIXME "No black box when clicking to type slider value"
	//@Override
	protected void renderBackground(DrawContext context, MinecraftClient client, int mouseX, int mouseY) {
		if (isTyping) {
			//fill(matrices, this.getX() - 1, this.getY() - 1, this.getX() + this.width + 1, this.getY() + this.height + 1, -1);
			//fill(matrices, this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, -16777216);
		} else {
			//super.renderBackground(context, client, mouseX, mouseY);
		}
	}
}
