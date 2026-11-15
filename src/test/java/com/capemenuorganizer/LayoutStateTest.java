package com.capemenuorganizer;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutStateTest
{
	@Test
	public void freshEditorHasKnownCapeActionsBeforeAnyMenuWasOpened()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "");
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
		state.reload(1, "");
		LayoutState.Snapshot old = state.current();
		state.observe(old, List.of("Profile one observed action"));
		state.reload(2, "");
		assertFalse(state.catalog(state.current()).contains("Profile one observed action"));
		assertTrue(state.catalog(state.current()).contains("Feldip Hills"));
		try
		{
			state.prepareSave(old, MenuLayout.defaults(), 2, false);
			fail("Stale editor must be rejected");
		}
		catch (IllegalArgumentException expected) { }
	}

	@Test
	public void invalidSavedDataDisablesMenuAndRequiresExplicitReset()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "broken");
		LayoutState.Snapshot snapshot = state.current();
		assertNull(snapshot.layout);
		try
		{
			state.prepareSave(snapshot, MenuLayout.defaults(), 1, false);
			fail("Invalid data must not be overwritten automatically");
		}
		catch (IllegalArgumentException expected) { }
		assertFalse(state.prepareSave(snapshot, MenuLayout.defaults(), 1, true).isEmpty());
	}

	@Test
	public void observedCatalogSurvivesSameProfileEditsAndDiscardsStaleMenuObservations()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "");
		LayoutState.Snapshot old = state.current();
		state.observe(old, List.of("Session observed action"));
		state.reload(1, "");
		state.observe(old, List.of("Stale destination"));
		assertTrue(state.catalog(state.current()).contains("Session observed action"));
		assertFalse(state.catalog(state.current()).contains("Stale destination"));
	}
}
