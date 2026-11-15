package com.capemenuorganizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.runelite.api.Actor;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

final class MenuFixture implements Menu
{
	private List<MenuEntry> entries = new ArrayList<>();

	@Override public MenuEntry createMenuEntry(int index)
	{
		MenuEntry entry = new Entry();
		entries.add(index < 0 ? entries.size() + index + 1 : index, entry);
		return entry;
	}
	@Override public MenuEntry[] getMenuEntries() { return entries.toArray(new MenuEntry[0]); }
	@Override public void setMenuEntries(MenuEntry[] values) { entries = new ArrayList<>(Arrays.asList(values)); }
	@Override public void removeMenuEntry(MenuEntry entry) { entries.remove(entry); }
	@Override public int getMenuX() { return 0; }
	@Override public int getMenuY() { return 0; }
	@Override public int getMenuWidth() { return 0; }
	@Override public int getMenuHeight() { return 0; }

	MenuEntry action(String name, int identifier)
	{
		return createMenuEntry(-1).setOption(name).setTarget("<col=ff9040>Max cape</col>")
			.setType(MenuAction.CC_OP).setIdentifier(identifier).setParam0(3).setParam1(100)
			.setItemId(ItemID.SKILLCAPE_MAX).setWorldViewId(7);
	}

	static final class Entry implements MenuEntry
	{
		private String option = "", target = "";
		private int identifier, param0, param1, itemId = -1, worldViewId;
		private MenuAction type = MenuAction.RUNELITE;
		private boolean force, deprioritized;
		private Consumer<MenuEntry> callback;
		private Menu sub;
		@Override public String getOption() { return option; }
		@Override public MenuEntry setOption(String v) { option = v; return this; }
		@Override public String getTarget() { return target; }
		@Override public MenuEntry setTarget(String v) { target = v; return this; }
		@Override public int getIdentifier() { return identifier; }
		@Override public MenuEntry setIdentifier(int v) { identifier = v; return this; }
		@Override public MenuAction getType() { return type; }
		@Override public MenuEntry setType(MenuAction v) { type = v; return this; }
		@Override public int getParam0() { return param0; }
		@Override public MenuEntry setParam0(int v) { param0 = v; return this; }
		@Override public int getParam1() { return param1; }
		@Override public MenuEntry setParam1(int v) { param1 = v; return this; }
		@Override public boolean isForceLeftClick() { return force; }
		@Override public MenuEntry setForceLeftClick(boolean v) { force = v; return this; }
		@Override public int getWorldViewId() { return worldViewId; }
		@Override public MenuEntry setWorldViewId(int v) { worldViewId = v; return this; }
		@Override public boolean isDeprioritized() { return deprioritized; }
		@Override public MenuEntry setDeprioritized(boolean v) { deprioritized = v; return this; }
		@Override public MenuEntry onClick(Consumer<MenuEntry> v) { callback = v; return this; }
		@Override public Consumer<MenuEntry> onClick() { return callback; }
		@Override public boolean isItemOp() { return type == MenuAction.CC_OP; }
		@Override public int getItemOp() { return identifier & 65535; }
		@Override public int getItemId() { return itemId; }
		@Override public MenuEntry setItemId(int v) { itemId = v; return this; }
		@Override public Widget getWidget() { return null; }
		@Override public NPC getNpc() { return null; }
		@Override public Player getPlayer() { return null; }
		@Override public Actor getActor() { return null; }
		@Override public Menu getSubMenu() { return sub; }
		@Override public Menu createSubMenu() { sub = new MenuFixture(); return sub; }
		@Override public void deleteSubMenu() { sub = null; }
	}
}
