package com.capemenuorganizer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapeMenuTransformerTest
{
	private final CapeMenuTransformer transformer = new CapeMenuTransformer();
	private final CapeContext inventory = new CapeContext("inventory", 100, 3);
	private final CapeContext worn = new CapeContext("worn", 100, 3);

	@Test
	public void duplicateDestinationsAndGroupLabelsHaveIndependentColorsWithoutChangingActions()
	{
		MenuLayout layout = new MenuLayout();
		MenuLayout.Entry group = layout.addGroup("Farming");
		layout.setColor(group.id, 0xff8800);
		MenuLayout.Entry green = layout.addTeleport(group.id, "Farming Guild");
		MenuLayout.Entry blue = layout.addTeleport(group.id, "Farming Guild");
		layout.setColor(green.id, 0x00ff00);
		layout.setColor(blue.id, 0x0088ff);
		MenuFixture menu = fixture("Teleports");
		transformer.transform(menu, layout, inventory);
		MenuEntry parent = named(menu, "<col=ff8800>Farming</col>");
		assertNotNull(parent);
		MenuEntry[] children = parent.getSubMenu().getMenuEntries();
		assertEquals("<col=00ff00>Farming Guild</col>", children[1].getOption());
		assertEquals("<col=0088ff>Farming Guild</col>", children[0].getOption());
		assertEquals(65538, children[0].getIdentifier());
		assertEquals(65538, children[1].getIdentifier());
	}

	@Test
	public void sameCustomLayoutInInventoryAndEquipmentRetainsNativeActionData()
	{
		MenuLayout layout = new MenuLayout();
		layout.addTeleport(layout.addGroup("Clues").id, "Farming Guild");
		layout.addTeleport(layout.addGroup("Farming").id, "Farming Guild");
		MenuFixture inv = fixture("Teleports");
		MenuFixture equipped = fixture("Guild Teleports");
		AtomicInteger calls = new AtomicInteger();
		MenuEntry source = inv.getMenuEntries()[1].getSubMenu().getMenuEntries()[0];
		source.onClick(e -> calls.incrementAndGet()).setForceLeftClick(true).setDeprioritized(true);
		assertTrue(transformer.transform(inv, layout, inventory).changed);
		assertTrue(transformer.transform(equipped, layout, worn).changed);
		assertEquals("Wear,Clues,Farming,Other teleports,Cancel", names(inv));
		assertEquals(names(inv), names(equipped));
		MenuEntry duplicate = named(inv, "Clues").getSubMenu().getMenuEntries()[0];
		assertEquals(source.getIdentifier(), duplicate.getIdentifier());
		assertEquals(3, duplicate.getParam0());
		assertEquals(100, duplicate.getParam1());
		assertEquals(source.getItemId(), duplicate.getItemId());
		assertEquals(7, duplicate.getWorldViewId());
		assertEquals(source.getType(), duplicate.getType());
		assertEquals(source.isForceLeftClick(), duplicate.isForceLeftClick());
		assertEquals(source.isDeprioritized(), duplicate.isDeprioritized());
		duplicate.onClick().accept(duplicate);
		assertEquals(1, calls.get());
		assertNotSame(duplicate, named(inv, "Farming").getSubMenu().getMenuEntries()[0]);
		assertEquals("Home", names(named(inv, "Other teleports").getSubMenu()));
	}

	@Test
	public void originalSubmenuVisibilityIsIndependentAndDefaultEntryIsRetained()
	{
		MenuLayout layout = new MenuLayout();
		layout.hiddenOriginals.remove("inventory:teleports");
		MenuFixture menu = fixture("Teleports");
		MenuEntry defaultEntry = menu.getMenuEntries()[2];
		assertTrue(transformer.transform(menu, layout, inventory).changed);
		assertSame(defaultEntry, menu.getMenuEntries()[menu.getMenuEntries().length - 1]);
		assertNotNull(named(menu, "Teleports"));
		MenuFixture equipped = fixture("Guild Teleports");
		transformer.transform(equipped, layout, worn);
		assertFalse(names(equipped).contains("Guild Teleports"));
	}

	@Test
	public void unknownActionsAndDifferentSlotsLeaveWholeMenuUntouched()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry[] before = menu.getMenuEntries();
		before[1].getSubMenu().getMenuEntries()[0].setType(MenuAction.RUNELITE);
		assertFalse(transformer.transform(menu, new MenuLayout(), inventory).changed);
		assertArrayEquals(before, menu.getMenuEntries());
		MenuFixture otherSlot = fixture("Teleports");
		before = otherSlot.getMenuEntries();
		assertFalse(transformer.transform(otherSlot, new MenuLayout(), new CapeContext("inventory", 100, 5)).changed);
		assertArrayEquals(before, otherSlot.getMenuEntries());
	}

	@Test
	public void defaultTeleportSurvivesHiddenParentAndRepeatedOpenDoesNotDuplicate()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry originalDefault = menu.action("Home", 4);
		MenuLayout layout = new MenuLayout();
		transformer.transform(menu, layout, inventory);
		assertSame(originalDefault, menu.getMenuEntries()[menu.getMenuEntries().length - 1]);
		String first = names(menu);
		assertFalse(transformer.transform(menu, layout, inventory).changed);
		assertEquals(first, names(menu));
	}

	@Test
	public void spellbookAndFeaturesAreNeverTreatedAsTeleports()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry features = menu.action("Features", 8);
		((MenuFixture) features.createSubMenu()).action("Stamina", 9);
		transformer.transform(menu, new MenuLayout(), inventory);
		assertSame(features, named(menu, "Features"));
		assertEquals("Stamina", names(features.getSubMenu()));
	}

	private static MenuFixture fixture(String originalGroup)
	{
		MenuFixture menu = new MenuFixture();
		menu.createMenuEntry(-1).setOption("Cancel").setType(MenuAction.CANCEL);
		MenuFixture teleports = (MenuFixture) menu.action(originalGroup, 2).createSubMenu();
		teleports.action("Farming Guild", 65538);
		teleports.action("Home", 131074);
		menu.action("Wear", 1);
		return menu;
	}

	private static MenuEntry named(Menu menu, String option)
	{
		return Arrays.stream(menu.getMenuEntries()).filter(e -> e.getOption().equals(option)).findFirst().orElse(null);
	}

	private static String names(Menu menu)
	{
		MenuEntry[] entries = menu.getMenuEntries();
		StringBuilder result = new StringBuilder();
		for (int i = entries.length - 1; i >= 0; i--)
		{
			if (result.length() > 0) { result.append(','); }
			result.append(entries[i].getOption());
		}
		return result.toString();
	}
}
