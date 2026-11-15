package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;

/** Serializes profile/editor transitions; published layouts are never mutated. */
final class LayoutState
{
	static final class Snapshot
	{
		final long profileId;
		final MenuLayouts menus;
		final String error;

		Snapshot(long profileId, MenuLayouts menus, String error)
		{
			this.profileId = profileId;
			this.menus = menus;
			this.error = error;
		}
	}

	private final LayoutCodec codec;
	private final Set<String> catalog = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<String, String> capeActions = new TreeMap<>();
	private volatile Snapshot current;

	LayoutState(LayoutCodec codec)
	{
		this.codec = codec;
	}

	synchronized void reload(long profileId, String legacy, String paired)
	{
		if (current == null || current.profileId != profileId)
		{
			catalog.clear();
			catalog.addAll(CapeActionCatalog.knownActions());
			capeActions.clear();
			capeActions.putAll(CapeActionCatalog.utilityActions());
		}
		try
		{
			current = new Snapshot(profileId, codec.loadMenus(legacy, paired), null);
			for (MenuLayout layout : new MenuLayout[]{current.menus.normal, current.menus.shift})
			{
				for (MenuLayout.Entry entry : layout.all())
				{
					if (!entry.isGroup()) { catalog.add(entry.destination); }
				}
			}
		}
		catch (IllegalArgumentException ex)
		{
			current = new Snapshot(profileId, null, "Saved menus could not be read. Native menus retained. Reset explicitly to restore both layouts.");
		}
	}

	Snapshot current()
	{
		return current;
	}

	synchronized String prepareSave(Snapshot expected, MenuLayout value, long activeProfileId, boolean reset, boolean shift)
	{
		if (current != expected || expected.profileId != activeProfileId)
		{
			throw new IllegalArgumentException("The active profile or layout changed. Try your edit again.");
		}
		if (expected.menus == null && !reset)
		{
			throw new IllegalArgumentException("Reset explicitly to replace the unreadable saved layout.");
		}
		MenuLayouts source = expected.menus == null ? codec.loadMenus(null, null) : expected.menus;
		return codec.encodeMenus(new MenuLayouts(codec.copy(shift ? source.normal : value), codec.copy(shift ? value : source.shift)));
	}

	synchronized boolean observe(Snapshot expected, List<String> destinations)
	{
		return expected == current && catalog.addAll(destinations);
	}

	synchronized boolean observeActions(Snapshot expected, Map<String, String> observed)
	{
		if (expected != current) { return false; }
		boolean changed = false;
		for (Map.Entry<String, String> action : observed.entrySet())
		{
			if (capeActions.containsKey(action.getKey()) || capeActions.size() < 100)
			{
				String previous = capeActions.put(action.getKey(), action.getValue());
				changed |= !action.getValue().equals(previous);
			}
		}
		return changed;
	}

	synchronized Map<String, String> capeActions(Snapshot expected)
	{
		return current == expected ? new TreeMap<>(capeActions) : new TreeMap<>();
	}

	synchronized List<String> catalog(Snapshot expected)
	{
		return current == expected ? new ArrayList<>(catalog) : new ArrayList<>();
	}
}
