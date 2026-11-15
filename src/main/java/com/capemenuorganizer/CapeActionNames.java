package com.capemenuorganizer;

import java.util.Locale;
import net.runelite.client.util.Text;

/** Known naming differences between the inventory and worn cape's native menus. */
final class CapeActionNames
{
	private CapeActionNames()
	{
	}

	static String canonical(String name)
	{
		String plain = Text.removeTags(name).trim();
		switch (plain.toLowerCase(Locale.ROOT))
		{
			case "carnivorous chinchompas":
			case "feldip hills":
				return "Feldip Hills";
			case "warriors' guild":
			case "warrior's guild":
				return "Warrior's Guild";
			default:
				return plain;
		}
	}

	static String key(String name)
	{
		return canonical(name).toLowerCase(Locale.ROOT);
	}
}
