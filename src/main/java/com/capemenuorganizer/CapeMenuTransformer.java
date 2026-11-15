package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.client.util.Text;

/** Copies current native actions only. No stored action IDs or synthetic dispatch. */
final class CapeMenuTransformer
{
	private static final Set<String> GROUP_NAMES = Set.of("teleports", "guild teleports", "skilling areas", "poh portals");
	private static final Set<String> ROOT_TELEPORTS = Set.of("home", "crafting guild");

	static final class Result
	{
		final boolean changed;
		final List<String> destinations;
		final Map<String, String> capeActions;
		final String status;

		Result(boolean changed, List<String> destinations, String status)
		{
			this(changed, destinations, Collections.emptyMap(), status);
		}

		Result(boolean changed, List<String> destinations, Map<String, String> capeActions, String status)
		{
			this.changed = changed;
			this.destinations = destinations;
			this.capeActions = capeActions;
			this.status = status;
		}
	}

	static String key(String text)
	{
		return Text.removeTags(text).trim().toLowerCase(Locale.ROOT);
	}

	static boolean isOriginalGroup(String option)
	{
		return GROUP_NAMES.contains(key(option));
	}

	Result transform(Menu menu, MenuLayout layout, CapeContext context)
	{
		MenuEntry[] original = menu.getMenuEntries();
		Map<String, Action> actions = new LinkedHashMap<>();
		Set<MenuEntry> teleportRoots = new HashSet<>();
		if (context == null || original.length == 0)
		{
			return unchanged("Open the max cape menu in inventory or equipment.");
		}
		for (MenuEntry entry : original)
		{
			// Leave core's Shift-right-click swap editor intact.
			if (key(entry.getOption()).startsWith("swap "))
			{
				return unchanged("Release Shift to see your organized menu.");
			}
			if (context.matches(entry) && entry.getSubMenu() != null && isOriginalGroup(entry.getOption()))
			{
				if (!collect(entry.getSubMenu(), context, actions, 0))
				{
					return unchanged("Unrecognized teleport action; original menu retained.");
				}
				teleportRoots.add(entry);
			}
		}
		if (actions.isEmpty())
		{
			return unchanged("No native cape teleport submenu found; original menu retained.");
		}
		for (MenuEntry entry : original)
		{
			if (context.matches(entry) && entry.getSubMenu() == null && nativeAction(entry)
				&& ROOT_TELEPORTS.contains(key(entry.getOption())))
			{
				actions.putIfAbsent(key(entry.getOption()), new Action(entry));
				teleportRoots.add(entry);
			}
		}
		layout.validate();
		MenuEntry defaultEntry = original[original.length - 1];
		CapeActionVisibility utilityVisibility = new CapeActionVisibility(original, context, layout);
		for (Map.Entry<String, MenuEntry> shortcut : utilityVisibility.shortcuts.entrySet())
		{
			List<Action> children = null;
			MenuEntry[] submenu = utilityVisibility.submenus.get(shortcut.getKey());
			if (submenu != null)
			{
				children = new ArrayList<>();
				for (MenuEntry child : submenu) { children.add(new Action(child)); }
			}
			actions.put(CapeActionNames.key(shortcut.getKey()), new Action(shortcut.getValue(), shortcut.getKey(), children));
		}
		// Count native submenu copies too: one reference may create several actual entries.
		int totalEntries = count(menu, 0);
		for (MenuLayout.Entry entry : layout.entries)
		{
			if (entry.isGroup())
			{
				int childCount = 0;
				for (MenuLayout.Entry child : entry.children)
				{
					Action action = actions.get(CapeActionNames.key(child.destination));
					if (action != null) { childCount += action.entryCount(); }
				}
				if (childCount > 0) { totalEntries += 1 + childCount; }
			}
			else
			{
				Action action = actions.get(CapeActionNames.key(entry.destination));
				if (action != null) { totalEntries += action.entryCount(); }
			}
		}
		if (totalEntries > 450) { return unchanged("Menu is too large; original menu retained."); }
		List<MenuEntry> retained = new ArrayList<>();
		for (MenuEntry entry : original)
		{
			boolean hide = teleportRoots.contains(entry) && (entry.getSubMenu() == null
				|| layout.hiddenOriginals.contains(context.name + ":" + key(entry.getOption())));
			hide |= utilityVisibility.hiddenParents.contains(entry);
			if (!hide || entry == defaultEntry)
			{
				retained.add(entry);
			}
		}
		List<MenuEntry> customDisplayOrder = new ArrayList<>();
		for (MenuLayout.Entry entry : layout.entries)
		{
			if (entry.isGroup())
			{
				List<Action> group = new ArrayList<>();
				for (MenuLayout.Entry child : entry.children)
				{
					Action action = actions.get(CapeActionNames.key(child.destination));
					if (action != null)
					{
						group.add(action);
					}
				}
				if (!group.isEmpty())
				{
					List<Integer> colors = new ArrayList<>();
					for (MenuLayout.Entry child : entry.children)
					{
						if (actions.containsKey(CapeActionNames.key(child.destination))) { colors.add(child.color); }
					}
					customDisplayOrder.add(addGroup(menu, colored(entry.name, entry.color), group, colors));
				}
			}
			else
			{
				Action action = actions.get(CapeActionNames.key(entry.destination));
				if (action != null)
				{
					customDisplayOrder.add(action.copyTo(menu, entry.color));
				}
			}
		}
		// Menu arrays are reversed. Insert custom entries below the untouched active default.
		Collections.reverse(customDisplayOrder);
		retained.addAll(Math.max(0, retained.size() - 1), customDisplayOrder);
		utilityVisibility.apply();
		menu.setMenuEntries(retained.toArray(new MenuEntry[0]));
		List<String> catalog = new ArrayList<>();
		for (Action action : actions.values())
		{
			catalog.add(action.reference);
		}
		catalog.sort(String.CASE_INSENSITIVE_ORDER);
		return new Result(true, catalog, utilityVisibility.catalog,
			"Layout applied to " + context.name + " cape (" + catalog.size() + " available actions).");
	}

	private static Result unchanged(String reason)
	{
		return new Result(false, Collections.emptyList(), reason);
	}

	private static boolean nativeAction(MenuEntry entry)
	{
		return entry.getType() == MenuAction.CC_OP || entry.getType() == MenuAction.CC_OP_LOW_PRIORITY;
	}

	private static boolean collect(Menu menu, CapeContext context, Map<String, Action> actions, int depth)
	{
		if (depth > 3 || menu.getMenuEntries().length == 0)
		{
			return false;
		}
		for (MenuEntry entry : menu.getMenuEntries())
		{
			if (entry.getSubMenu() != null)
			{
				if (!collect(entry.getSubMenu(), context, actions, depth + 1))
				{
					return false;
				}
				continue;
			}
			if (!context.matches(entry) || !nativeAction(entry) || entry.getIdentifier() <= 0)
			{
				return false;
			}
			String name = Text.removeTags(entry.getOption()).trim();
			if (name.isEmpty() || name.length() > 100 || name.indexOf('<') >= 0 || name.indexOf('>') >= 0)
			{
				return false;
			}
			Action action = new Action(entry);
			Action previous = actions.putIfAbsent(CapeActionNames.key(name), action);
			if (previous != null && !previous.sameOperation(action))
			{
				return false;
			}
			if (actions.size() > 100)
			{
				return false;
			}
		}
		return true;
	}

	private static int count(Menu menu, int depth)
	{
		if (depth > 5)
		{
			return 1000;
		}
		int count = 0;
		for (MenuEntry entry : menu.getMenuEntries())
		{
			count++;
			if (entry.getSubMenu() != null)
			{
				count += count(entry.getSubMenu(), depth + 1);
			}
		}
		return count;
	}

	private static String colored(String name, Integer color)
	{
		return color == null ? name : String.format(Locale.ROOT, "<col=%06x>%s</col>", color, name);
	}

	private static MenuEntry addGroup(Menu menu, String name, List<Action> actions, List<Integer> colors)
	{
		MenuEntry parent = menu.createMenuEntry(-1).setOption(name).setTarget("").setType(MenuAction.RUNELITE);
		Menu sub = parent.createSubMenu();
		for (int i = actions.size() - 1; i >= 0; i--)
		{
			actions.get(i).copyTo(sub, colors.get(i));
		}
		return parent;
	}

	private static final class Action
	{
		private final String option, target, reference;
		private final List<Action> children;
		private final int identifier, param0, param1, itemId, worldViewId;
		private final MenuAction type;
		private final boolean forceLeftClick, deprioritized;
		private final Consumer<MenuEntry> callback;

		Action(MenuEntry source)
		{
			this(source, CapeActionNames.canonical(source.getOption()), null);
		}

		Action(MenuEntry source, String reference, List<Action> children)
		{
			option = CapeActionNames.canonical(source.getOption());
			this.reference = reference;
			this.children = children;
			target = source.getTarget();
			identifier = source.getIdentifier();
			param0 = source.getParam0();
			param1 = source.getParam1();
			itemId = source.getItemId();
			worldViewId = source.getWorldViewId();
			type = source.getType();
			forceLeftClick = source.isForceLeftClick();
			deprioritized = source.isDeprioritized();
			callback = source.onClick();
		}

		boolean sameOperation(Action other)
		{
			return identifier == other.identifier && param0 == other.param0 && param1 == other.param1
				&& itemId == other.itemId && type == other.type && callback == other.callback;
		}

		int entryCount()
		{
			int count = 1;
			if (children != null)
			{
				for (Action child : children) { count += child.entryCount(); }
			}
			return count;
		}

		MenuEntry copyTo(Menu menu, Integer color)
		{
			MenuEntry copy = menu.createMenuEntry(-1).setOption(colored(option, color)).setTarget(target).setIdentifier(identifier)
				.setParam0(param0).setParam1(param1).setItemId(itemId).setWorldViewId(worldViewId)
				.setType(type).setForceLeftClick(forceLeftClick).setDeprioritized(deprioritized).onClick(callback);
			if (children != null)
			{
				copy.setType(MenuAction.RUNELITE).onClick(null);
				Menu submenu = copy.createSubMenu();
				for (Action child : children) { child.copyTo(submenu, null); }
			}
			return copy;
		}
	}
}
