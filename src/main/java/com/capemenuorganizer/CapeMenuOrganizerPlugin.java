package com.capemenuorganizer;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;

@Slf4j
@PluginDescriptor(name = "Cape Menu Organizer", description = "Organize max cape teleport menus", tags = {"cape", "teleport", "menu"})
public class CapeMenuOrganizerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar toolbar;
	@Inject private Gson gson;
	@Inject private ColorPickerManager colorPickerManager;
	private final CapeMenuTransformer transformer = new CapeMenuTransformer();
	private volatile LayoutState state;
	private volatile CapeMenusPanel panel;
	private volatile NavigationButton navigation;
	private volatile boolean running;
	private LayoutCodec codec;

	@Override
	protected void startUp()
	{
		codec = new LayoutCodec(gson);
		state = new LayoutState(codec);
		running = true;
		reload();
		LayoutState activeState = state;
		onEdt(() ->
		{
			if (!running || state != activeState) { return; }
			panel = new CapeMenusPanel(codec, colorPickerManager);
			navigation = NavigationButton.builder().tooltip("Cape Menu Organizer").icon(icon()).panel(panel).priority(7).build();
			toolbar.addNavigation(navigation);
			refresh("Cape actions loaded. Hover the cape to refresh its current options.");
		});
	}

	@Override
	protected void shutDown()
	{
		running = false;
		NavigationButton oldNavigation = navigation;
		CapeMenusPanel oldPanel = panel;
		navigation = null;
		panel = null;
		onEdt(() ->
		{
			if (oldPanel != null) { oldPanel.closeColorPicker(); }
			if (oldNavigation != null) { toolbar.removeNavigation(oldNavigation); }
		});
	}

	@Subscribe(priority = -1)
	public void onMenuOpened(MenuOpened event)
	{
		observeMenu(true);
	}

	@Subscribe(priority = -2)
	public void onPostMenuSort(PostMenuSort event)
	{
		if (!client.isMenuOpen()) { observeMenu(false); }
	}

	private void observeMenu(boolean applyLayout)
	{
		LayoutState active = state;
		if (!running || active == null) { return; }
		LayoutState.Snapshot snapshot = active.current();
		if (snapshot == null || snapshot.menus == null || snapshot.profileId != configManager.getProfile().getId()) { return; }
		Menu menu = client.getMenu();
		CapeContext context = CapeContext.detect(menu.getMenuEntries());
		if (context == null) { return; }
		try
		{
			boolean shiftPressed = client.isKeyPressed(KeyCode.KC_SHIFT);
			MenuLayout selected = snapshot.menus.forShift(shiftPressed);
			CapeMenuTransformer.Result result = applyLayout ? transformer.transform(menu, selected, context)
				: transformer.discover(menu, selected, context);
			boolean learned = active.observe(snapshot, result.destinations);
			learned |= active.observeActions(snapshot, result.capeActions);
			if (applyLayout || learned) { refresh(result.status); }
		}
		catch (IllegalArgumentException ex)
		{
			log.debug("Cape menu layout was not applied", ex);
			refresh("Layout could not be applied. See the debug log for details.");
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (CapeMenuOrganizerConfig.GROUP.equals(event.getGroup())
			&& ("layout".equals(event.getKey()) || "menus".equals(event.getKey()))) { reload(); }
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged event)
	{
		reload();
	}

	private void reload()
	{
		LayoutState active = state;
		if (!running || active == null) { return; }
		synchronized (configManager)
		{
			active.reload(configManager.getProfile().getId(), configManager.getConfiguration(CapeMenuOrganizerConfig.GROUP, "layout"),
				configManager.getConfiguration(CapeMenuOrganizerConfig.GROUP, "menus"));
		}
		refresh("Cape actions loaded. Hover the cape to refresh its current options.");
	}

	private void refresh(String message)
	{
		LayoutState active = state;
		LayoutState.Snapshot snapshot = active == null ? null : active.current();
		if (snapshot == null) { return; }
		onEdt(() ->
		{
			CapeMenusPanel editor = panel;
			if (!running || editor == null || state != active || active.current() != snapshot) { return; }
			editor.showLayouts(snapshot.menus,
				active.catalog(snapshot), active.capeActions(snapshot),
				snapshot.error == null ? message : snapshot.error, snapshot.menus != null,
				(shift, value) -> save(active, snapshot, value, false, shift),
				shift -> save(active, snapshot, MenuLayout.defaults(), true, shift));
		});
	}

	private void save(LayoutState active, LayoutState.Snapshot expected, MenuLayout value, boolean reset, boolean shift)
	{
		try
		{
			synchronized (configManager)
			{
				if (!running || state != active) { return; }
				String json = active.prepareSave(expected, value, configManager.getProfile().getId(), reset, shift);
				configManager.setConfiguration(CapeMenuOrganizerConfig.GROUP, "menus", json);
				// Also refresh when the encoded value was unchanged and no ConfigChanged event fired.
				reload();
			}
		}
		catch (IllegalArgumentException ex)
		{
			refresh(ex.getMessage());
		}
	}

	private static void onEdt(Runnable action)
	{
		if (SwingUtilities.isEventDispatchThread()) { action.run(); }
		else { SwingUtilities.invokeLater(action); }
	}

	private static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(202, 166, 81));
		graphics.fillPolygon(new int[]{8, 16, 21, 3}, new int[]{3, 3, 21, 21}, 4);
		graphics.setColor(new Color(54, 44, 65));
		graphics.setStroke(new BasicStroke(2));
		for (int y = 10; y <= 18; y += 4) { graphics.drawLine(8, y, 16, y); }
		graphics.dispose();
		return image;
	}

	@Provides
	CapeMenuOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CapeMenuOrganizerConfig.class);
	}
}
