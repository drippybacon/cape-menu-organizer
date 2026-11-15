package com.capemenuorganizer;

import com.google.gson.Gson;
import com.google.inject.Provides;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.KeyCode;
import net.runelite.api.Menu;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.util.AsyncBufferedImage;

@Slf4j
@PluginDescriptor(name = "Cape Menu Organizer", description = "Organize max cape teleport menus", tags = {"cape", "teleport", "menu"})
public class CapeMenuOrganizerPlugin extends Plugin
{
	@Inject private Client client;
	@Inject private ConfigManager configManager;
	@Inject private ClientToolbar toolbar;
	@Inject private Gson gson;
	@Inject private ColorPickerManager colorPickerManager;
	@Inject private ItemManager itemManager;
	@Inject private CapeMenuOrganizerConfig config;
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
		AsyncBufferedImage capeIcon = itemManager.getImage(ItemID.SKILLCAPE_MAX);
		onEdt(() ->
		{
			if (!running || state != activeState) { return; }
			panel = new CapeMenusPanel(codec, colorPickerManager);
			capeIcon.onLoaded(() -> onEdt(() ->
			{
				if (!running || state != activeState) { return; }
				navigation = NavigationButton.builder().tooltip("Cape Menu Organizer").icon(capeIcon).panel(panel).priority(7).build();
				updateNavigation();
			}));
			refresh("");
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
			if (applyLayout || learned) { refresh(applyLayout && !result.changed ? result.status : ""); }
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
		if (CapeMenuOrganizerConfig.GROUP.equals(event.getGroup()) && "showSidebar".equals(event.getKey()))
		{
			updateNavigation();
		}
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
		refresh("");
		updateNavigation();
	}

	private void updateNavigation()
	{
		onEdt(() ->
		{
			if (!running || navigation == null) { return; }
			if (config.showSidebar()) { toolbar.addNavigation(navigation); }
			else
			{
				if (panel != null) { panel.closeColorPicker(); }
				toolbar.removeNavigation(navigation);
			}
		});
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
				shift -> save(active, snapshot, MenuLayout.defaults(), true, shift),
				fromShift -> copy(active, snapshot, fromShift));
		});
	}

	private void copy(LayoutState active, LayoutState.Snapshot expected, boolean fromShift)
	{
		try
		{
			synchronized (configManager)
			{
				if (!running || state != active) { return; }
				String json = active.prepareCopy(expected, configManager.getProfile().getId(), fromShift);
				configManager.setConfiguration(CapeMenuOrganizerConfig.GROUP, "menus", json);
				reload();
			}
		}
		catch (IllegalArgumentException ex) { refresh(ex.getMessage()); }
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

	@Provides
	CapeMenuOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CapeMenuOrganizerConfig.class);
	}
}
