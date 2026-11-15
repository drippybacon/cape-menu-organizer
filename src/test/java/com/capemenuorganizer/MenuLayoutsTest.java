package com.capemenuorganizer;

import com.google.gson.Gson;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuLayoutsTest
{
	private final LayoutCodec codec = new LayoutCodec(new Gson());

	@Test
	public void legacyLayoutMigratesIntoIndependentNormalAndShiftCopies()
	{
		MenuLayout legacy = new MenuLayout();
		MenuLayout.Entry home = legacy.addTeleport(null, "Home");
		home.color = 0x123456;
		legacy.hiddenActions.add("spellbook/check");
		MenuLayouts imported = codec.loadMenus(codec.encode(legacy), null);
		assertEquals(home.id, imported.normal.entries.get(0).id);
		assertEquals(home.id, imported.shift.entries.get(0).id);
		assertEquals(Integer.valueOf(0x123456), imported.shift.entries.get(0).color);
		imported.normal.entries.clear();
		imported.normal.hiddenActions.clear();
		assertEquals("Home", imported.shift.entries.get(0).destination);
		assertTrue(imported.shift.hiddenActions.contains("spellbook/check"));
	}

	@Test
	public void pairedMenusRoundTripAndTakePriorityOverLegacy()
	{
		MenuLayouts menus = codec.loadMenus("", "");
		menus.normal.entries.clear();
		menus.normal.addTeleport(null, "Home");
		menus.shift.entries.clear();
		menus.shift.addTeleport(null, "Crafting Guild");
		menus.shift.hiddenActions.add("features");
		MenuLayouts restored = codec.loadMenus("invalid legacy", codec.encodeMenus(menus));
		assertEquals("Home", restored.forShift(false).entries.get(0).destination);
		assertEquals("Crafting Guild", restored.forShift(true).entries.get(0).destination);
		assertFalse(restored.normal.hiddenActions.contains("features"));
		assertTrue(restored.shift.hiddenActions.contains("features"));
	}

	@Test
	public void invalidPairedDataDoesNotSilentlyFallBackToLegacy()
	{
		for (String invalid : new String[]{"{", "{}", "null", "{\"version\":2}",
			"{\"version\":1,\"normal\":null,\"shift\":null}"})
		{
			try
			{
				codec.loadMenus(codec.encode(MenuLayout.defaults()), invalid);
				fail("Invalid paired data accepted: " + invalid);
			}
			catch (IllegalArgumentException expected) { }
		}
	}
}
