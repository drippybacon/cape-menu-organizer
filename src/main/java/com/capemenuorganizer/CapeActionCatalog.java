package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/** Editor choices only. Execution always resolves a current native menu action. */
final class CapeActionCatalog
{
	private CapeActionCatalog()
	{
	}

	static Map<String, String> utilityActions()
	{
		Map<String, String> actions = new TreeMap<>();
		actions.put("spellbook", "Spellbook");
		actions.put("features", "Features");
		for (String name : List.of("Standard", "Ancient", "Lunar", "Arceuus", "Check"))
		{
			actions.put("spellbook/" + CapeMenuTransformer.key(name), "Spellbook / " + name);
		}
		for (String name : List.of("Search", "Ring of Life", "Commune", "Stamina Boost", "Rename boat"))
		{
			actions.put("features/" + CapeMenuTransformer.key(name), "Features / " + name);
		}
		return actions;
	}

	static List<String> knownActions()
	{
		TreeSet<String> actions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		actions.add("Home");
		for (MenuLayout.Entry entry : MenuLayout.defaults().all())
		{
			if (!entry.isGroup()) { actions.add(entry.destination); }
		}
		actions.add("Spellbook");
		actions.add("Features");
		for (Map.Entry<String, String> utility : utilityActions().entrySet())
		{
			if (utility.getKey().contains("/")) { actions.add(utility.getValue()); }
		}
		return new ArrayList<>(actions);
	}
}
