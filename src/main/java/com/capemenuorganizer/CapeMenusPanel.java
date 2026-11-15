package com.capemenuorganizer;

import java.awt.BorderLayout;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JTabbedPane;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

/** Tab choice controls editing only; the actual Shift key selects the in-game layout. */
final class CapeMenusPanel extends PluginPanel
{
	private final LayoutCodec codec;
	private final CapeMenuPanel normal;
	private final CapeMenuPanel shift;

	CapeMenusPanel(LayoutCodec codec, ColorPickerManager colorPickerManager)
	{
		this.codec = codec;
		normal = new CapeMenuPanel(codec, colorPickerManager);
		shift = new CapeMenuPanel(codec, colorPickerManager);
		normal.setName("Normal");
		shift.setName("Shift");
		JTabbedPane tabs = new JTabbedPane();
		tabs.addTab("Normal", normal);
		tabs.addTab("Shift", shift);
		setLayout(new BorderLayout());
		add(tabs, BorderLayout.NORTH);
	}

	void showLayouts(MenuLayouts menus, List<String> catalog, Map<String, String> capeActions,
		String message, boolean editable, BiConsumer<Boolean, MenuLayout> save, Consumer<Boolean> reset)
	{
		MenuLayouts displayed = menus == null ? codec.loadMenus(null, null) : menus;
		normal.showLayout(displayed.normal, catalog, capeActions, message, editable,
			value -> save.accept(false, value), () -> reset.accept(false));
		shift.showLayout(displayed.shift, catalog, capeActions, message, editable,
			value -> save.accept(true, value), () -> reset.accept(true));
	}

	void closeColorPicker()
	{
		normal.closeColorPicker();
		shift.closeColorPicker();
	}
}
