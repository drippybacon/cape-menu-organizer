package com.capemenuorganizer;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutStateTest
{
	@Test
	public void corruptPairedMenusDoNotReimportLegacyAndExplicitResetRestoresBoth()
	{
		LayoutCodec codec = new LayoutCodec(new Gson());
		MenuLayout legacy = new MenuLayout();
		legacy.addTeleport(null, "Home");
		LayoutState state = new LayoutState(codec);
		state.reload(1, codec.encode(legacy), "broken");
		assertNull(state.current().menus);
		try
		{
			state.prepareSave(state.current(), legacy, 1, false, true);
			fail("Unreadable paired data must remain protected");
		}
		catch (IllegalArgumentException expected) { }
		MenuLayouts reset = codec.loadMenus(null,
			state.prepareSave(state.current(), MenuLayout.defaults(), 1, true, true));
		assertEquals("Poh", reset.normal.entries.get(0).name);
		assertEquals("Poh", reset.shift.entries.get(0).name);
	}

	@Test
	public void firstNormalSavePersistsInitialShiftCopyAndLaterShiftEditPreservesNormal()
	{
		LayoutCodec codec = new LayoutCodec(new Gson());
		MenuLayout legacy = new MenuLayout();
		legacy.addTeleport(null, "Home");
		String oldJson = codec.encode(legacy);
		LayoutState state = new LayoutState(codec);
		state.reload(1, oldJson, null);
		MenuLayout normal = codec.copy(state.current().menus.normal);
		normal.entries.clear();
		normal.addTeleport(null, "Farming Guild");
		String firstSave = state.prepareSave(state.current(), normal, 1, false, false);
		state.reload(1, oldJson, firstSave);
		assertEquals("Farming Guild", state.current().menus.normal.entries.get(0).destination);
		assertEquals("Home", state.current().menus.shift.entries.get(0).destination);
		MenuLayout shift = codec.copy(state.current().menus.shift);
		shift.hiddenActions.add("spellbook");
		shift.entries.get(0).color = 0x123456;
		String secondSave = state.prepareSave(state.current(), shift, 1, false, true);
		state.reload(1, oldJson, secondSave);
		assertFalse(state.current().menus.normal.hiddenActions.contains("spellbook"));
		assertTrue(state.current().menus.shift.hiddenActions.contains("spellbook"));
		assertEquals("Farming Guild", state.current().menus.normal.entries.get(0).destination);
		assertEquals(Integer.valueOf(0x123456), state.current().menus.shift.entries.get(0).color);
	}

	@Test
	public void resettingValidShiftLayoutDoesNotResetNormal()
	{
		LayoutCodec codec = new LayoutCodec(new Gson());
		MenuLayout old = new MenuLayout();
		old.addTeleport(null, "Home");
		LayoutState state = new LayoutState(codec);
		state.reload(1, codec.encode(old), null);
		String reset = state.prepareSave(state.current(), MenuLayout.defaults(), 1, true, true);
		MenuLayouts saved = codec.loadMenus(null, reset);
		assertEquals("Home", saved.normal.entries.get(0).destination);
		assertEquals("Poh", saved.shift.entries.get(0).name);
	}
	@Test
	public void freshEditorHasKnownCapeActionsBeforeAnyMenuWasOpened()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "", null);
		List<String> choices = state.catalog(state.current());
		assertTrue(choices.contains("Feldip Hills"));
		assertTrue(choices.contains("Home"));
		assertTrue(choices.contains("Spellbook"));
		assertTrue(choices.contains("Spellbook / Ancient"));
		assertTrue(choices.contains("Features / Stamina Boost"));
		assertEquals("Spellbook / Ancient", state.capeActions(state.current()).get("spellbook/ancient"));
	}
	@Test
	public void staleEditorCannotSaveAfterProfileChange()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "", null);
		LayoutState.Snapshot old = state.current();
		state.observe(old, List.of("Profile one observed action"));
		state.reload(2, "", null);
		assertFalse(state.catalog(state.current()).contains("Profile one observed action"));
		assertTrue(state.catalog(state.current()).contains("Feldip Hills"));
		try
		{
			state.prepareSave(old, MenuLayout.defaults(), 2, false, false);
			fail("Stale editor must be rejected");
		}
		catch (IllegalArgumentException expected) { }
	}

	@Test
	public void invalidSavedDataDisablesMenuAndRequiresExplicitReset()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "broken", null);
		LayoutState.Snapshot snapshot = state.current();
		assertNull(snapshot.menus);
		try
		{
			state.prepareSave(snapshot, MenuLayout.defaults(), 1, false, false);
			fail("Invalid data must not be overwritten automatically");
		}
		catch (IllegalArgumentException expected) { }
		assertFalse(state.prepareSave(snapshot, MenuLayout.defaults(), 1, true, false).isEmpty());
	}

	@Test
	public void observedCatalogSurvivesSameProfileEditsAndDiscardsStaleMenuObservations()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "", null);
		LayoutState.Snapshot old = state.current();
		state.observe(old, List.of("Session observed action"));
		state.reload(1, "", null);
		state.observe(old, List.of("Stale destination"));
		assertTrue(state.catalog(state.current()).contains("Session observed action"));
		assertFalse(state.catalog(state.current()).contains("Stale destination"));
	}
}
