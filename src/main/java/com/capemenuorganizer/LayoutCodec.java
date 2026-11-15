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
}
