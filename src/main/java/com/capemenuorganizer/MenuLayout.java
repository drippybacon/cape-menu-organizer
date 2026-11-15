package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Persisted layout contains destination names, never executable game action data. */
final class MenuLayout
{
	static final Set<String> ORIGINALS = Set.of("inventory:teleports", "worn:guild teleports",
		"worn:skilling areas", "worn:poh portals");
	int version = 1;
	List<Entry> entries = new ArrayList<>();
	Set<String> hiddenOriginals = new LinkedHashSet<>(ORIGINALS);

	static final class Entry
	{
		String id = UUID.randomUUID().toString();
		String name;
		String destination;
		Integer color;
		List<Entry> children = new ArrayList<>();

		static Entry group(String name)
		{
			Entry entry = new Entry();
			entry.name = name;
			return entry;
		}

		boolean isGroup()
		{
			return destination == null;
		}

		@Override
		public String toString()
		{
			return isGroup() ? name : destination;
		}
	}

	static MenuLayout defaults()
	{
		MenuLayout layout = new MenuLayout();
		layout.addTeleport(null, "Home");
		Entry guilds = layout.addGroup("Guilds");
		for (String destination : Arrays.asList("Crafting Guild", "Farming Guild", "Fishing Guild", "Warriors' Guild"))
		{
			layout.addTeleport(guilds.id, destination);
		}
		return layout;
	}

	Entry addGroup(String name)
	{
		checkName(name);
		ensureCapacity();
		Entry entry = Entry.group(name.trim());
		entries.add(entry);
		return entry;
	}

	Entry addTeleport(String groupId, String destination)
	{
		checkDestination(destination);
		List<Entry> target = container(groupId);
		ensureCapacity();
		Entry entry = new Entry();
		entry.destination = destination;
		target.add(entry);
		return entry;
	}

	void rename(String id, String name)
	{
		checkName(name);
		Entry entry = find(id);
		if (!entry.isGroup())
		{
			throw new IllegalArgumentException("Only groups can be renamed.");
		}
		entry.name = name.trim();
	}

	void setColor(String id, Integer color)
	{
		checkColor(color);
		find(id).color = color;
	}

	void remove(String id)
	{
		List<Entry> parent = parent(id);
		parent.remove(find(id));
	}

	void move(String id, String groupId, int index)
	{
		Entry entry = find(id);
		List<Entry> target = container(groupId);
		if (entry.isGroup() && groupId != null)
		{
			throw new IllegalArgumentException("Groups cannot be nested.");
		}
		List<Entry> source = parent(id);
		source.remove(entry);
		target.add(Math.max(0, Math.min(index, target.size())), entry);
	}

	Entry find(String id)
	{
		return all().stream().filter(e -> e.id.equals(id)).findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Entry no longer exists."));
	}

	List<Entry> parent(String id)
	{
		for (Entry entry : entries)
		{
			if (entry.id.equals(id))
			{
				return entries;
			}
			if (entry.children.stream().anyMatch(e -> e.id.equals(id)))
			{
				return entry.children;
			}
		}
		throw new IllegalArgumentException("Entry no longer exists.");
	}

	private List<Entry> container(String groupId)
	{
		if (groupId == null)
		{
			return entries;
		}
		Entry group = find(groupId);
		if (!group.isGroup())
		{
			throw new IllegalArgumentException("Choose a group.");
		}
		return group.children;
	}

	List<Entry> all()
	{
		List<Entry> result = new ArrayList<>();
		for (Entry entry : entries)
		{
			result.add(entry);
			result.addAll(entry.children);
		}
		return result;
	}

	private void ensureCapacity()
	{
		if (all().size() >= 100)
		{
			throw new IllegalArgumentException("A layout can contain up to 100 groups and shortcuts.");
		}
	}

	void validate()
	{
		if (version != 1 || entries == null || hiddenOriginals == null || !ORIGINALS.containsAll(hiddenOriginals))
		{
			throw new IllegalArgumentException("Unsupported or invalid layout configuration.");
		}
		Set<String> ids = new HashSet<>();
		for (Entry entry : entries)
		{
			validateEntry(entry, false, ids);
		}
		if (ids.size() > 100)
		{
			throw new IllegalArgumentException("Layout is too large.");
		}
	}

	private static void validateEntry(Entry entry, boolean child, Set<String> ids)
	{
		if (entry == null || entry.id == null || entry.id.isEmpty() || entry.id.length() > 64
			|| !ids.add(entry.id) || entry.children == null)
		{
			throw new IllegalArgumentException("Invalid layout entry.");
		}
		checkColor(entry.color);
		if (entry.isGroup())
		{
			if (child)
			{
				throw new IllegalArgumentException("Groups cannot be nested.");
			}
			checkName(entry.name);
			for (Entry nested : entry.children)
			{
				validateEntry(nested, true, ids);
			}
		}
		else
		{
			checkDestination(entry.destination);
			if (entry.name != null || !entry.children.isEmpty())
			{
				throw new IllegalArgumentException("A shortcut cannot contain other entries.");
			}
		}
	}

	static void checkName(String name)
	{
		if (!plainText(name, 40) || "Other teleports".equalsIgnoreCase(name.trim()))
		{
			throw new IllegalArgumentException("Use a plain group name of 1–40 characters (except Other teleports).");
		}
	}

	private static void checkColor(Integer color)
	{
		if (color != null && (color < 0 || color > 0xffffff))
		{
			throw new IllegalArgumentException("Invalid entry color.");
		}
	}

	private static void checkDestination(String name)
	{
		if (!plainText(name, 100))
		{
			throw new IllegalArgumentException("Invalid teleport name.");
		}
	}

	private static boolean plainText(String value, int limit)
	{
		return value != null && !value.trim().isEmpty() && value.length() <= limit
			&& value.indexOf('<') < 0 && value.indexOf('>') < 0
			&& value.chars().noneMatch(Character::isISOControl);
	}
}
