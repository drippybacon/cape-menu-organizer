package com.capemenuorganizer;

import com.google.gson.Gson;
import java.awt.Component;
import java.awt.Container;
import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;
import javax.swing.JTree;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.components.colorpicker.RuneliteColorPicker;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CapeMenuPanelTest
{
	@Test
	public void nativePickerSavesSelectedEntryThroughOriginalEditorSession() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			ColorPickerManager manager = mock(ColorPickerManager.class);
			RuneliteColorPicker dialog = mock(RuneliteColorPicker.class);
			when(manager.create(any(Component.class), any(Color.class), anyString(), eq(true))).thenReturn(dialog);
			AtomicReference<Consumer<Color>> onClose = new AtomicReference<>();
			doAnswer(call -> { onClose.set(call.getArgument(0)); return null; }).when(dialog).setOnClose(any());
			AtomicReference<MenuLayout> oldSession = new AtomicReference<>();
			AtomicReference<MenuLayout> newSession = new AtomicReference<>();
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()), manager);
			MenuLayout before = new MenuLayout();
			MenuLayout.Entry home = before.addTeleport(null, "Home");
			panel.showLayout(before, List.of("Home"), "Ready", true, oldSession::set, () -> {});
			findTree(panel).setSelectionRow(1);
			button(panel, "Color…").doClick();
			assertNotNull(onClose.get());
			panel.showLayout(new MenuLayout(), List.of("Home"), "Profile changed", true, newSession::set, () -> {});
			onClose.get().accept(new Color(0x1256ab));
			assertNull(newSession.get());
			assertEquals(home.id, oldSession.get().entries.get(0).id);
			assertEquals(Integer.valueOf(0x1256ab), oldSession.get().entries.get(0).color);
		});
	}

	private static JTree findTree(Container root)
	{
		for (Component child : root.getComponents())
		{
			if (child instanceof JTree) { return (JTree) child; }
			if (child instanceof Container)
			{
				JTree tree = findTree((Container) child);
				if (tree != null) { return tree; }
			}
		}
		return null;
	}
	@Test
	public void individualCapeActionCheckboxSavesAndCanBeRestored() throws Exception
	{
		SwingUtilities.invokeAndWait(() ->
		{
			AtomicReference<MenuLayout> saved = new AtomicReference<>();
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()), mock(ColorPickerManager.class));
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
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()), mock(ColorPickerManager.class));
			MenuLayout original = new MenuLayout();
			panel.showLayout(original, List.of("Home"), "Ready", true, saved::set, () -> {});
			button(panel, "Add action").doClick();
			assertEquals(1, saved.get().entries.size());
			String firstId = saved.get().entries.get(0).id;
			button(panel, "Add action").doClick();
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
			CapeMenuPanel panel = new CapeMenuPanel(new LayoutCodec(new Gson()), mock(ColorPickerManager.class));
			panel.showLayout(new MenuLayout(), List.of("Home"), "Invalid configuration", false,
				layout -> fail("Invalid config must not be overwritten"), () -> {});
			assertFalse(button(panel, "Add action").isEnabled());
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
