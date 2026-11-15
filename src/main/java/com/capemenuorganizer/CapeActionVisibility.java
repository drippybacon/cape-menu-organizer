package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.Text;

/** Plans filtering before touching native submenu arrays. Never changes an action's metadata. */
final class CapeActionVisibility
{
	final Map<String, String> catalog = new LinkedHashMap<>();
	final Map<String, MenuEntry> shortcuts = new LinkedHashMap<>();
	final Map<String, MenuEntry[]> submenus = new LinkedHashMap<>();
	final Set<MenuEntry> hiddenParents = new HashSet<>();
	private final Map<Menu, MenuEntry[]> filteredChildren = new LinkedHashMap<>();

	CapeActionVisibility(MenuEntry[] entries, CapeContext context, MenuLayout layout)
	{
		MenuEntry activeDefault = entries[entries.length - 1];
		for (MenuEntry entry : entries)
		{
			String group = CapeMenuTransformer.key(entry.getOption());
			if (!context.matches(entry) || entry.getSubMenu() == null
				|| !(group.equals("spellbook") || group.equals("features")))
			{
				continue;
			}
			String groupName = Text.removeTags(entry.getOption()).trim();
			catalog.put(group, groupName);
			List<MenuEntry> retained = new ArrayList<>();
			boolean allRecognized = true;
			for (MenuEntry child : entry.getSubMenu().getMenuEntries())
			{
				boolean recognized = child.getSubMenu() == null && context.matches(child)
					&& (child.getType() == MenuAction.CC_OP || child.getType() == MenuAction.CC_OP_LOW_PRIORITY);
				String childName = Text.removeTags(child.getOption()).trim();
				String path = group + "/" + CapeMenuTransformer.key(childName);
				if (recognized && !childName.isEmpty() && path.length() <= 120)
				{
					catalog.put(path, groupName + " / " + childName);
					shortcuts.put(groupName + " / " + childName, child);
				}
				else
				{
					recognized = false;
					allRecognized = false;
				}
				if (!recognized || !layout.hiddenActions.contains(path)) { retained.add(child); }
			}
			if (allRecognized && !retained.isEmpty())
			{
				shortcuts.put(groupName, entry);
				submenus.put(groupName, retained.toArray(new MenuEntry[0]));
			}
			// Keep core's active click choice intact, including a submenu selected as default.
			if (entry == activeDefault) { continue; }
			if (layout.hiddenActions.contains(group) || retained.isEmpty())
			{
				hiddenParents.add(entry);
			}
			else if (retained.size() != entry.getSubMenu().getMenuEntries().length)
			{
				filteredChildren.put(entry.getSubMenu(), retained.toArray(new MenuEntry[0]));
			}
		}
	}

	void apply()
	{
		filteredChildren.forEach(Menu::setMenuEntries);
	}
}
