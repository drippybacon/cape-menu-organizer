package com.capemenuorganizer;

import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CapeContextTest
{
	@Test
	public void detectsInventoryCapeFromItsWidgetAndPreservesSlot()
	{
		MenuFixture menu = new MenuFixture();
		MenuFixture.Entry entry = parent(menu, "Teleports", InterfaceID.Inventory.ITEMS, 7);
		when(entry.widget.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX);
		CapeContext context = CapeContext.detect(menu.getMenuEntries());
		assertNotNull(context);
		assertEquals("inventory", context.name);
		assertEquals(7, context.slot);
		assertEquals(InterfaceID.Inventory.ITEMS, context.widgetId);
	}

	@Test
	public void detectsWornCapeThroughNativeEquipmentChild()
	{
		MenuFixture menu = new MenuFixture();
		MenuFixture.Entry entry = parent(menu, "Guild Teleports", InterfaceID.Wornitems.SLOT1, -1);
		Widget item = mock(Widget.class);
		when(entry.widget.getChild(1)).thenReturn(item);
		when(item.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX_WORN);
		CapeContext context = CapeContext.detect(menu.getMenuEntries());
		assertNotNull(context);
		assertEquals("worn", context.name);
		when(item.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX_FIRECAPE);
		assertNull(CapeContext.detect(menu.getMenuEntries()));
	}

	@Test
	public void ignoresBankUnsupportedItemsMissingWidgetsAndConflictingSlots()
	{
		MenuFixture menu = new MenuFixture();
		MenuFixture.Entry entry = parent(menu, "Teleports", InterfaceID.Bankmain.ITEMS, 7);
		when(entry.widget.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX);
		assertNull(CapeContext.detect(menu.getMenuEntries()));
		entry.setParam1(InterfaceID.Inventory.ITEMS);
		when(entry.widget.getId()).thenReturn(InterfaceID.Inventory.ITEMS);
		when(entry.widget.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX_FIRECAPE);
		assertNull(CapeContext.detect(menu.getMenuEntries()));
		entry.widget = null;
		assertNull(CapeContext.detect(menu.getMenuEntries()));
		menu = new MenuFixture();
		entry = parent(menu, "Teleports", InterfaceID.Inventory.ITEMS, 7);
		when(entry.widget.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX);
		MenuFixture.Entry conflicting = parent(menu, "Teleports", InterfaceID.Inventory.ITEMS, 8);
		when(conflicting.widget.getItemId()).thenReturn(ItemID.SKILLCAPE_MAX);
		assertNull(CapeContext.detect(menu.getMenuEntries()));
	}

	private static MenuFixture.Entry parent(MenuFixture menu, String name, int widgetId, int slot)
	{
		MenuFixture.Entry entry = (MenuFixture.Entry) menu.action(name, 2).setParam0(slot).setParam1(widgetId);
		entry.createSubMenu();
		entry.widget = mock(Widget.class);
		when(entry.widget.getId()).thenReturn(widgetId);
		return entry;
	}
}
