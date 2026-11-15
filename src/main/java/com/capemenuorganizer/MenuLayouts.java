package com.capemenuorganizer;

/** The two layouts are saved together; published instances are never edited in place. */
final class MenuLayouts
{
	final MenuLayout normal;
	final MenuLayout shift;

	MenuLayouts(MenuLayout normal, MenuLayout shift)
	{
		this.normal = normal;
		this.shift = shift;
	}

	MenuLayout forShift(boolean shiftPressed)
	{
		return shiftPressed ? shift : normal;
	}
}
