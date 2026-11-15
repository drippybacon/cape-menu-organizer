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
	public void redundantRootHomeIsOmittedButSavedAndNestedCopiesArePreserved()
	{
		MenuFixture menu = fixture("Guild Teleports");
		MenuEntry nativeHome = menu.action("Home", 4);
		MenuLayout layout = new MenuLayout();
		MenuLayout.Entry customHome = layout.addTeleport(null, "Home");
		customHome.color = 0xff0000;
		String group = layout.addGroup("Favorites").id;
		layout.addTeleport(group, "Home");
		layout.addTeleport(group, "Home");
		transformer.transform(menu, layout, worn);
		assertEquals("Home,Favorites,Wear,Cancel", names(menu));
		assertSame(nativeHome, menu.getMenuEntries()[menu.getMenuEntries().length - 1]);
		assertEquals("Home,Home", names(named(menu, "Favorites").getSubMenu()));
		assertEquals("Home", layout.entries.get(0).destination);
		assertEquals(Integer.valueOf(0xff0000), layout.entries.get(0).color);
	}

	@Test
	public void rootHomeReturnsWhenAnotherActionIsDefaultAndCraftingDuplicateIsOmitted()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry crafting = menu.action("Crafting Guild", 4);
		MenuLayout layout = new MenuLayout();
		layout.addTeleport(null, "Home");
		layout.addTeleport(null, "Crafting Guild");
		transformer.transform(menu, layout, inventory);
		assertEquals("Crafting Guild,Home,Wear,Cancel", names(menu));
		assertSame(crafting, menu.getMenuEntries()[menu.getMenuEntries().length - 1]);
	}

	@Test
	public void matchingTextFromDifferentItemOrNonNativeCallbackDoesNotSuppressShortcut()
	{
		MenuFixture otherItem = fixture("Teleports");
		otherItem.action("Home", 4).setParam0(8);
		MenuLayout layout = new MenuLayout();
		layout.addTeleport(null, "Home");
		transformer.transform(otherItem, layout, inventory);
		assertEquals("Home,Home,Wear,Cancel", names(otherItem));
		MenuFixture otherCallback = fixture("Guild Teleports");
		otherCallback.action("Home", 4).onClick(entry -> {});
		transformer.transform(otherCallback, layout, worn);
		assertEquals("Home,Home,Wear,Cancel", names(otherCallback));
	}

	@Test
	public void hoverDiscoveryFindsUtilityActionsWithoutChangingNativeMenuOrDefault()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry spellbook = menu.action("Spellbook", 8);
		MenuFixture spells = (MenuFixture) spellbook.createSubMenu();
		spells.action("Ancient", 65544);
		MenuEntry[] before = menu.getMenuEntries();
		MenuEntry[] childBefore = spells.getMenuEntries();
		CapeMenuTransformer.Result observed = transformer.discover(menu, new MenuLayout(), inventory);
		assertTrue(observed.destinations.contains("Farming Guild"));
		assertTrue(observed.destinations.contains("Spellbook / Ancient"));
		assertFalse(observed.changed);
		assertArrayEquals(before, menu.getMenuEntries());
		assertArrayEquals(childBefore, spells.getMenuEntries());
		assertSame(spellbook, menu.getMenuEntries()[menu.getMenuEntries().length - 1]);
	}

	@Test
	public void repeatedNativeSubmenusRespectTotalMenuCapacityBeforeMutation()
	{
		MenuFixture menu = fixture("Teleports");
		MenuFixture spells = (MenuFixture) menu.action("Spellbook", 8).createSubMenu();
		for (int i = 0; i < 8; i++) { spells.action("Book " + i, 65544 + i); }
		menu.action("Home", 4);
		MenuEntry[] before = menu.getMenuEntries();
		MenuLayout layout = new MenuLayout();
		for (int i = 0; i < 60; i++) { layout.addTeleport(null, "Spellbook"); }
		assertFalse(transformer.transform(menu, layout, inventory).changed);
		assertArrayEquals(before, menu.getMenuEntries());
	}

	@Test
	public void utilityActionsAndWholeSubmenusCanBeMixedWithTeleportsEvenWhenOriginalIsHidden()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry spellbook = menu.action("Spellbook", 8);
		MenuFixture spells = (MenuFixture) spellbook.createSubMenu();
		spells.action("Ancient", 65544);
		spells.action("Check", 131080);
		MenuEntry features = menu.action("Features", 9);
		((MenuFixture) features.createSubMenu()).action("Stamina Boost", 65545);
		menu.action("Home", 4);
		MenuLayout layout = new MenuLayout();
		String group = layout.addGroup("Everything").id;
		layout.addTeleport(group, "Farming Guild");
		layout.addTeleport(group, "Spellbook / Ancient");
		layout.addTeleport(group, "Features / Stamina Boost");
		layout.addTeleport(group, "Spellbook");
		layout.hiddenActions.add("spellbook");
		layout.hiddenActions.add("spellbook/check");
		CapeMenuTransformer.Result result = transformer.transform(menu, layout, inventory);
		Menu mixed = named(menu, "Everything").getSubMenu();
		assertEquals("Farming Guild,Ancient,Stamina Boost,Spellbook", names(mixed));
		assertEquals(65544, named(mixed, "Ancient").getIdentifier());
		assertEquals(65545, named(mixed, "Stamina Boost").getIdentifier());
		assertEquals("Ancient", names(named(mixed, "Spellbook").getSubMenu()));
		assertNull(named(menu, "Spellbook"));
		assertTrue(result.destinations.contains("Spellbook / Ancient"));
		assertTrue(result.destinations.contains("Spellbook"));
	}

	@Test
	public void chinchompaAliasesResolveToCurrentNativeFeldipActionInBothContexts()
	{
		MenuLayout layout = new MenuLayout();
		layout.addTeleport(null, "Carnivorous chinchompas");
		MenuFixture inv = fixture("Teleports");
		((MenuFixture) inv.getMenuEntries()[1].getSubMenu()).action("Carnivorous chinchompas", 196610);
		MenuFixture equipped = fixture("Skilling Areas");
		((MenuFixture) equipped.getMenuEntries()[1].getSubMenu()).action("Feldip Hills", 262146);
		CapeMenuTransformer.Result invResult = transformer.transform(inv, layout, inventory);
		CapeMenuTransformer.Result wornResult = transformer.transform(equipped, layout, worn);
		assertNotNull(named(inv, "Feldip Hills"));
		assertNotNull(named(equipped, "Feldip Hills"));
		assertEquals(196610, named(inv, "Feldip Hills").getIdentifier());
		assertEquals(262146, named(equipped, "Feldip Hills").getIdentifier());
		assertTrue(invResult.destinations.contains("Feldip Hills"));
		assertFalse(invResult.destinations.contains("Carnivorous chinchompas"));
		assertEquals(invResult.destinations, wornResult.destinations);
	}

	@Test
	public void unassignedActionsDoNotProduceAutomaticFallbackMenu()
	{
		MenuFixture menu = fixture("Teleports");
		transformer.transform(menu, new MenuLayout(), inventory);
		assertEquals("Wear,Cancel", names(menu));
		assertNull(named(menu, "Other teleports"));
	}

	@Test
	public void selectivelyHidesSpellbooksAndFeaturesWithoutChangingRemainingActions()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry spellbook = menu.action("Spellbook", 8);
		MenuFixture spells = (MenuFixture) spellbook.createSubMenu();
		MenuEntry standard = spells.action("Standard", 65544);
		spells.action("Ancient", 131080);
		spells.action("Check", 196616);
		MenuEntry features = menu.action("Features", 9);
		MenuFixture perks = (MenuFixture) features.createSubMenu();
		MenuEntry stamina = perks.action("Stamina Boost", 65545);
		perks.action("Rename boat", 131081);
		menu.action("Home", 4);
		MenuLayout layout = new MenuLayout();
		layout.hiddenActions.add("spellbook/ancient");
		layout.hiddenActions.add("features/rename boat");
		CapeMenuTransformer.Result result = transformer.transform(menu, layout, worn);
		assertTrue(result.changed);
		assertEquals("Check,Standard", names(spellbook.getSubMenu()));
		assertSame(standard, spellbook.getSubMenu().getMenuEntries()[0]);
		assertEquals("Stamina Boost", names(features.getSubMenu()));
		assertSame(stamina, features.getSubMenu().getMenuEntries()[0]);
		assertEquals("Spellbook / Ancient", result.capeActions.get("spellbook/ancient"));
		assertEquals("Features / Rename boat", result.capeActions.get("features/rename boat"));
	}

	@Test
	public void hidingWholeUtilityGroupOrAllItsChildrenRemovesEmptyParentExceptActiveDefault()
	{
		MenuFixture menu = fixture("Teleports");
		MenuEntry spellbook = menu.action("Spellbook", 8);
		((MenuFixture) spellbook.createSubMenu()).action("Standard", 65544);
		MenuEntry features = menu.action("Features", 9);
		((MenuFixture) features.createSubMenu()).action("Search", 65545);
		menu.action("Home", 4);
		MenuLayout layout = new MenuLayout();
		layout.hiddenActions.add("spellbook");
		layout.hiddenActions.add("features/search");
		transformer.transform(menu, layout, inventory);
		assertNull(named(menu, "Spellbook"));
		assertNull(named(menu, "Features"));
		MenuFixture withDefault = fixture("Teleports");
		MenuEntry defaultEntry = withDefault.action("Spellbook", 8);
		((MenuFixture) defaultEntry.createSubMenu()).action("Standard", 65544);
		transformer.transform(withDefault, layout, inventory);
		assertSame(defaultEntry, withDefault.getMenuEntries()[withDefault.getMenuEntries().length - 1]);
		assertEquals("Standard", names(defaultEntry.getSubMenu()));
	}

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
		assertEquals("Wear,Clues,Farming,Cancel", names(inv));
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
		assertNull(named(inv, "Other teleports"));
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
