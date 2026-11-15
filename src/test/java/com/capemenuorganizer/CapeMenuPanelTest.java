package com.capemenuorganizer;

import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import org.junit.Test;
import static org.junit.Assert.*;

public class CapeMenuPanelTest
{
	@Test
	public void individualCapeActionCheckboxSavesAndCanBeRestored() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicReference<MenuLayout> saved = new AtomicReference<>();
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()));
			panel.showLayout(new MenuLayout(), List.of("Home"), Map.of("spellbook", "Spellbook",
				"spellbook/ancient", "Spellbook / Ancient"), "Ready", true, saved::set, () -> {});
			JCheckBox ancient = checkbox(panel, "Ancient");
			assertNotNull(ancient);
			assertTrue(ancient.isSelected());
			ancient.doClick();
			assertTrue(saved.get().hiddenActions.contains("spellbook/ancient"));
			ancient.doClick();
			assertFalse(saved.get().hiddenActions.contains("spellbook/ancient"));
		});
	}

	private static JCheckBox checkbox(Container root, String text)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JCheckBox && text.equals(((JCheckBox) child).getText())) { return (JCheckBox) child; }
			if (child instanceof Container)
			{
				JCheckBox nested = checkbox((Container) child, text);
				if (nested != null) { return nested; }
			}
		}
		return null;
	}
	@Test
	public void addingDuplicateShortcutsSavesIndependentCopiesAndUpdatesTree() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicReference<MenuLayout> saved = new AtomicReference<>();
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()));
			MenuLayout original = new MenuLayout();
			panel.showLayout(original, List.of("Home"), "Ready", true, saved::set, () -> {});
			button(panel, "Add teleport").doClick();
			assertEquals(1, saved.get().entries.size());
			String firstId = saved.get().entries.get(0).id;
			button(panel, "Add teleport").doClick();
			assertEquals(2, saved.get().entries.size());
			assertNotEquals(firstId, saved.get().entries.get(1).id);
			assertEquals(0, original.entries.size());
		});
	}

	@Test
	public void invalidConfigDisablesEditsButLeavesExplicitResetAvailable() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()));
			panel.showLayout(new MenuLayout(), List.of("Home"), "Invalid configuration", false,
				layout -> fail("Invalid config must not be overwritten"), () -> {});
			assertFalse(button(panel, "Add teleport").isEnabled());
			assertTrue(button(panel, "Reset layout").isEnabled());
		});
	}

	private static JButton button(Container root, String text)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JButton && text.equals(((JButton) child).getText()))
			{
				return (JButton) child;
			}
			if (child instanceof Container)
			{
				JButton nested = button((Container) child, text);
				if (nested != null) { return nested; }
			}
		}
		return null;
	}
}
