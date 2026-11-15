package com.capemenuorganizer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(CapeMenuOrganizerConfig.GROUP)
public interface CapeMenuOrganizerConfig extends Config
{
	String GROUP = "cape-menu-organizer";

	@ConfigItem(keyName = "layout", name = "Layout", description = "Edited through the Cape Menu Organizer sidebar", hidden = true)
	default String layout()
	{
		return "";
	}
}
