package com.capemenuorganizer;

import net.runelite.api.MenuEntry;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetUtil;

final class CapeContext
{
	final String name;
	final int widgetId;
	final int slot;

	CapeContext(String name, int widgetId, int slot)
	{
		this.name = name;
		this.widgetId = widgetId;
		this.slot = slot;
	}

	boolean matches(MenuEntry entry)
	{
		return entry.getParam1() == widgetId && entry.getParam0() == slot;
	}

	static CapeContext detect(MenuEntry[] entries)
	{
		CapeContext result = null;
		for (MenuEntry entry : entries)
		{
			if (entry.getSubMenu() == null || !CapeMenuTransformer.isOriginalGroup(entry.getOption()))
			{
				continue;
			}
			Widget widget = entry.getWidget();
			if (widget == null)
			{
				continue;
			}
			int group = WidgetUtil.componentToInterface(widget.getId());
			String context;
			int itemId;
			if (group == InterfaceID.INVENTORY)
			{
				context = "inventory";
				itemId = widget.getItemId();
			}
			else if (group == InterfaceID.WORNITEMS)
			{
				context = "worn";
				Widget item = widget.getChild(1);
				itemId = item == null ? -1 : item.getItemId();
			}
			else
			{
				continue;
			}
			if (itemId != ItemID.SKILLCAPE_MAX && itemId != ItemID.SKILLCAPE_MAX_WORN)
			{
				continue;
			}
			if (result != null && (!result.name.equals(context) || !result.matches(entry)))
			{
				return null;
			}
			result = new CapeContext(context, entry.getParam1(), entry.getParam0());
		}
		return result;
	}
}
