package com.capemenuorganizer;

import com.google.gson.Gson;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public class MenuLayoutTest
{
	@Test
	public void existingSavedLayoutLoadsWithAllUtilityActionsVisible()
	{
		MenuLayout layout = new LayoutCodec(new Gson()).decode("{\"version\":1,\"entries\":[],\"hiddenOriginals\":[]}");
		assertTrue(layout.hiddenActions.isEmpty());
		layout.hiddenActions.add("unrelated/action");
		assertRejected(layout::validate);
	}
	@Test
	public void duplicatesAreIndependentAndMovesPreserveIdentity()
	{
		MenuLayout layout = new MenuLayout();
		MenuLayout.Entry a = layout.addGroup("Guilds");
		MenuLayout.Entry b = layout.addGroup("Farming");
		MenuLayout.Entry first = layout.addTeleport(a.id, "Farming Guild");
		MenuLayout.Entry second = layout.addTeleport(b.id, "Farming Guild");
		assertNotEquals(first.id, second.id);
		layout.move(first.id, null, 0);
		assertEquals(first.id, layout.entries.get(0).id);
		layout.remove(first.id);
		assertEquals("Farming Guild", b.children.get(0).destination);
		assertEquals(second.id, b.children.get(0).id);
	}

	@Test
	public void codecRoundTripsLayoutAndVisibilityWithoutSharingMutableState()
	{
		MenuLayout original = new MenuLayout();
		original.addTeleport(original.addGroup("Clues").id, "Crafting Guild");
		original.setColor(original.entries.get(0).children.get(0).id, 0x00ff00);
		original.hiddenOriginals.add("inventory:teleports");
		original.hiddenActions.add("spellbook/check");
		LayoutCodec codec = new LayoutCodec(new Gson());
		MenuLayout restored = codec.decode(codec.encode(original));
		assertEquals("Crafting Guild", restored.entries.get(0).children.get(0).destination);
		assertEquals(Integer.valueOf(0x00ff00), restored.entries.get(0).children.get(0).color);
		assertTrue(restored.hiddenOriginals.contains("inventory:teleports"));
		assertTrue(restored.hiddenActions.contains("spellbook/check"));
		restored.entries.clear();
		assertEquals(1, original.entries.size());
	}

	@Test
	public void invalidConfigurationIsRejectedRatherThanSilentlyReset()
	{
		LayoutCodec codec = new LayoutCodec(new Gson());
		for (String invalid : Arrays.asList("{", "null", "{}", "{\"version\":2,\"entries\":[]}",
			"{\"version\":1,\"entries\":null}", "{\"version\":1,\"entries\":[],\"hiddenOriginals\":[\"bad\"]}"))
		{
			try
			{
				codec.decode(invalid);
				fail("Accepted invalid configuration: " + invalid);
			}
			catch (IllegalArgumentException expected)
			{
				// Keep the original stored configuration for recovery.
			}
		}
	}

	@Test
	public void rejectsNestedGroupsDuplicateIdsAndFormattingTags()
	{
		MenuLayout layout = new MenuLayout();
		MenuLayout.Entry group = layout.addGroup("Clues");
		assertRejected(() -> layout.setColor(group.id, -1));
		assertRejected(() -> layout.setColor(group.id, 0x1000000));
		assertRejected(() -> layout.rename(group.id, "<col=ff0000>Clues"));
		assertRejected(() -> layout.move(group.id, group.id, 0));
		group.children.add(MenuLayout.Entry.group("Nested"));
		assertRejected(layout::validate);
		group.children.clear();
		layout.entries.add(group);
		assertRejected(layout::validate);
	}

	@Test
	public void validationBoundsNamesAndLayoutSize()
	{
		MenuLayout layout = new MenuLayout();
		assertRejected(() -> layout.addGroup("   "));
		assertRejected(() -> layout.addGroup("x".repeat(41)));
		for (int i = 0; i < 100; i++)
		{
			layout.addTeleport(null, "Home");
		}
		assertRejected(() -> layout.addTeleport(null, "Home"));
		assertEquals(100, layout.entries.size());
	}

	private static void assertRejected(Runnable action)
	{
		try
		{
			action.run();
			fail("Expected validation failure");
		}
		catch (IllegalArgumentException expected)
		{
			// Expected.
		}
	}
}
