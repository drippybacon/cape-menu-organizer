package com.capemenuorganizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CapeMenuOrganizerConfig.GROUP)
public interface CapeMenuOrganizerConfig extends Config
{
	String GROUP = "cape-menu-organizer";

	@ConfigItem(keyName = "showSidebar", name = "Show sidebar", description = "Show the menu editor in the sidebar. Custom cape menus stay active when hidden.")
	default boolean showSidebar()
	{
		return true;
	}

	@ConfigItem(keyName = "layout", name = "Layout", description = "Edited through the Cape Menu Organizer sidebar", hidden = true)
	default String layout()
	{
		return "";
	}

	@ConfigItem(keyName = "menus", name = "Normal and Shift layouts", description = "Edited through the Cape Menu Organizer sidebar", hidden = true)
	default String menus()
	{
		return "";
	}
}
