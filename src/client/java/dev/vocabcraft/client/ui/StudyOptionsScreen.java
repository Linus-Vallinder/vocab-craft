package dev.vocabcraft.client.ui;

import dev.vocabcraft.client.VocabCraftClient;
import dev.vocabcraft.client.study.StudySettings;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class StudyOptionsScreen extends Screen {
	private static final int[] COOLDOWNS = {0, 10, 30, 60, 120};
	private static final int[] DAILY = {0, 5, 10, 20, 50};
	private static final int[] PENDING = {0, 10, 20, 50, 100};

	private final Screen parent;

	public StudyOptionsScreen(Screen parent) {
		super(Component.literal("Study"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		StudySettings settings = StudySettings.get();
		int inner = Math.min(300, Math.max(160, this.width - 40));
		HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 36, 40);
		LinearLayout body = LinearLayout.vertical().spacing(2);
		body.defaultCellSetting().alignHorizontallyCenter();
		body.addChild(new StringWidget(this.title, this.font));
		body.addChild(button(inner, "Queue mode: " + onOff(settings.queueMode()), button -> settings.toggleQueueMode()));
		body.addChild(button(inner, "Notices: " + (settings.actionBar() ? "action bar" : "chat"), button -> settings.toggleActionBar()));
		body.addChild(button(inner, "Cooldown: " + seconds(settings.cooldownSeconds()), button ->
			settings.setCooldownSeconds(next(COOLDOWNS, settings.cooldownSeconds()))
		));
		body.addChild(button(inner, "Pause notices in combat: " + onOff(settings.suppressCombat()), button -> settings.toggleSuppressCombat()));
		body.addChild(button(inner, "Pause notices while flying: " + onOff(settings.suppressFlying()), button -> settings.toggleSuppressFlying()));
		body.addChild(button(inner, "Pause notices while swimming: " + onOff(settings.suppressSwimming()), button -> settings.toggleSuppressSwimming()));
		body.addChild(button(inner, "Daily new cards: " + cap(settings.dailyNew()), button -> settings.setDailyNew(next(DAILY, settings.dailyNew()))));
		body.addChild(button(inner, "Pending cap: " + cap(settings.maxPending()), button -> settings.setMaxPending(next(PENDING, settings.maxPending()))));
		body.addChild(new StringWidget(Component.literal("Review key: " + VocabCraftClient.REVIEW.getTranslatedKeyMessage().getString()), this.font));
		body.addChild(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(inner).build());
		layout.addToContents(body);
		layout.visitWidgets(this::addRenderableWidget);
		layout.arrangeElements();
	}

	private Button button(int width, String label, Button.OnPress action) {
		return Button.builder(Component.literal(label), button -> {
			action.onPress(button);
			rebuildWidgets();
		}).width(width).build();
	}

	private static String onOff(boolean enabled) {
		return enabled ? "on" : "off";
	}

	private static String seconds(int value) {
		return value == 0 ? "off" : value + "s";
	}

	private static String cap(int value) {
		return value == 0 ? "no cap" : Integer.toString(value);
	}

	private static int next(int[] values, int current) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] == current) {
				return values[(i + 1) % values.length];
			}
		}
		return values[0];
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		this.extractMenuBackground(graphics);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreenAndShow(this.parent);
	}
}
