package com.capemenuorganizer;

import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CapeMenusPanelTest
{
	@Test
	public void tabsRouteChangesToIndependentMenus() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			LayoutCodec codec = new LayoutCodec(new Gson());
			CapeMenusPanel panel = new CapeMenusPanel(codec, mock(ColorPickerManager.class));
			AtomicReference<MenuLayouts> saved = new AtomicReference<>(new MenuLayouts(new MenuLayout(), new MenuLayout()));
			panel.showLayouts(saved.get(), List.of("Home"), Map.of(), "Ready", true, (shift, value) ->
			{
				MenuLayouts before = saved.get();
				saved.set(new MenuLayouts(shift ? before.normal : value, shift ? value : before.shift));
			}, shift -> {});
			JTabbedPane tabs = find(panel, JTabbedPane.class, null);
			assertEquals("Normal", tabs.getTitleAt(0));
			assertEquals("Shift", tabs.getTitleAt(1));
			Container normal = (Container) tabs.getComponentAt(0);
			Container shift = (Container) tabs.getComponentAt(1);
			find(normal, JButton.class, "Add action").doClick();
			assertEquals(1, saved.get().normal.entries.size());
			assertEquals(0, saved.get().shift.entries.size());
			tabs.setSelectedIndex(1);
			find(shift, JButton.class, "Add action").doClick();
			assertEquals(1, saved.get().normal.entries.size());
			assertEquals(1, saved.get().shift.entries.size());
			find(shift, JCheckBox.class, "Show Features").doClick();
			assertFalse(saved.get().normal.hiddenActions.contains("features"));
			assertTrue(saved.get().shift.hiddenActions.contains("features"));
		});
	}

	private static <T extends Component> T find(Container root, Class<T> type, String text)
	{
		for (Component child : root.getComponents())
		{
			if (type.isInstance(child) && (text == null || (child instanceof javax.swing.AbstractButton
				&& text.equals(((javax.swing.AbstractButton) child).getText())))) { return type.cast(child); }
			if (child instanceof Container)
			{
				T result = find((Container) child, type, text);
				if (result != null) { return result; }
			}
		}
		return null;
	}
}
