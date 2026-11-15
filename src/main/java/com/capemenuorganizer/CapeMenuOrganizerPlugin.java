package com.capemenuorganizer;

import com.google.inject.Provides;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(name = "Cape Menu Organizer", description = "Organize max cape teleport menus", tags = {"cape", "teleport", "menu"})
public class CapeMenuOrganizerPlugin extends Plugin
{
	@Provides
	CapeMenuOrganizerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CapeMenuOrganizerConfig.class);
	}
}
