package com.capemenuorganizer;

import com.google.gson.Gson;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutStateTest
{
	@Test
	public void staleEditorCannotSaveAfterProfileChange()
	{
		LayoutState state = new LayoutState(new LayoutCodec(new Gson()));
		state.reload(1, "");
		LayoutState.Snapshot old = state.current();
		state.observe(old, List.of("Home", "Crafting Guild"));
		state.reload(2, "");
		assertTrue(state.catalog(state.current()).isEmpty());
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
		state.observe(old, List.of("Home"));
		state.reload(1, "");
		state.observe(old, List.of("Stale destination"));
		assertEquals(List.of("Home"), state.catalog(state.current()));
	}
}
