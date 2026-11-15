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
		final MenuLayout layout;
		final String error;

		Snapshot(long profileId, MenuLayout layout, String error)
		{
			this.profileId = profileId;
			this.layout = layout;
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

	synchronized void reload(long profileId, String json)
	{
		if (current == null || current.profileId != profileId)
		{
			catalog.clear();
			capeActions.clear();
		}
		try
		{
			current = new Snapshot(profileId, json == null || json.isEmpty() ? MenuLayout.defaults() : codec.decode(json), null);
		}
		catch (IllegalArgumentException ex)
		{
			current = new Snapshot(profileId, null, "Saved layout could not be read. Native menus retained. Reset explicitly to replace it.");
		}
	}

	Snapshot current()
	{
		return current;
	}

	synchronized String prepareSave(Snapshot expected, MenuLayout value, long activeProfileId, boolean reset)
	{
		if (current != expected || expected.profileId != activeProfileId)
		{
			throw new IllegalArgumentException("The active profile or layout changed. Try your edit again.");
		}
		if (expected.layout == null && !reset)
		{
			throw new IllegalArgumentException("Reset explicitly to replace the unreadable saved layout.");
		}
		return codec.encode(value);
	}

	synchronized boolean observe(Snapshot expected, List<String> destinations)
	{
		return expected == current && catalog.addAll(destinations);
	}

	synchronized void observeActions(Snapshot expected, Map<String, String> observed)
	{
		if (expected == current && capeActions.size() + observed.size() <= 100)
		{
			capeActions.putAll(observed);
		}
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
