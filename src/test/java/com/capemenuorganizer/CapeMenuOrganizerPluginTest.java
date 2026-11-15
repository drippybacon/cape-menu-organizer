package com.capemenuorganizer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CapeMenuOrganizerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CapeMenuOrganizerPlugin.class);
		RuneLite.main(args);
	}
}