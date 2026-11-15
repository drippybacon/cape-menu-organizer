package com.capemenuorganizer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

final class LayoutCodec
{
	private final Gson gson;

	LayoutCodec(Gson gson)
	{
		this.gson = gson;
	}

	MenuLayout decode(String json)
	{
		if (json == null || json.length() > 64000)
		{
			throw new IllegalArgumentException("Layout is missing or too large.");
		}
		try
		{
			JsonElement parsed = gson.fromJson(json, JsonElement.class);
			if (parsed == null || !parsed.isJsonObject())
			{
				throw new IllegalArgumentException("Invalid layout configuration.");
			}
			JsonObject object = parsed.getAsJsonObject();
			if (!object.has("version") || !object.has("entries"))
			{
				throw new IllegalArgumentException("Layout version or entries are missing.");
			}
			MenuLayout layout = gson.fromJson(object, MenuLayout.class);
			layout.validate();
			for (MenuLayout.Entry entry : layout.all())
			{
				if (!entry.isGroup()) { entry.destination = CapeActionNames.canonical(entry.destination); }
			}
			return layout;
		}
		catch (JsonParseException | IllegalStateException ex)
		{
			throw new IllegalArgumentException("Cannot read the saved layout.", ex);
		}
	}

	String encode(MenuLayout layout)
	{
		layout.validate();
		return gson.toJson(layout);
	}

	MenuLayout copy(MenuLayout layout)
	{
		return decode(encode(layout));
	}

	MenuLayouts loadMenus(String legacy, String paired)
	{
		if (paired == null || paired.isEmpty())
		{
			MenuLayout original = legacy == null || legacy.isEmpty() ? MenuLayout.defaults() : decode(legacy);
			return new MenuLayouts(original, copy(original));
		}
		if (paired.length() > 128000) { throw new IllegalArgumentException("Saved menus are too large."); }
		try
		{
			JsonElement parsed = gson.fromJson(paired, JsonElement.class);
			if (parsed == null || !parsed.isJsonObject()) { throw new IllegalArgumentException("Invalid saved menus."); }
			JsonObject object = parsed.getAsJsonObject();
			JsonElement version = object.get("version");
			if (version == null || !version.isJsonPrimitive() || !version.getAsJsonPrimitive().isNumber()
				|| !"1".equals(version.getAsString()) || !object.has("normal") || !object.has("shift"))
			{
				throw new IllegalArgumentException("Unsupported or incomplete saved menus.");
			}
			return new MenuLayouts(decode(gson.toJson(object.get("normal"))), decode(gson.toJson(object.get("shift"))));
		}
		catch (JsonParseException | IllegalStateException ex)
		{
			throw new IllegalArgumentException("Cannot read saved menus.", ex);
		}
	}

	String encodeMenus(MenuLayouts menus)
	{
		JsonObject object = new JsonObject();
		object.addProperty("version", 1);
		object.add("normal", gson.fromJson(encode(menus.normal), JsonElement.class));
		object.add("shift", gson.fromJson(encode(menus.shift), JsonElement.class));
		String json = gson.toJson(object);
		if (json.length() > 128000) { throw new IllegalArgumentException("Saved menus are too large."); }
		return json;
	}
}
