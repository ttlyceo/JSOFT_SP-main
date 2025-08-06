/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.handler.communityhandlers;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.handler.communityhandlers.impl.CommunityAcademy;
import l2e.gameserver.handler.communityhandlers.impl.CommunityAuction;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBalancer;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBalancerSkill;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.handler.communityhandlers.impl.CommunityCertification;
import l2e.gameserver.handler.communityhandlers.impl.CommunityClan;
import l2e.gameserver.handler.communityhandlers.impl.CommunityClassMaster;
import l2e.gameserver.handler.communityhandlers.impl.CommunityEvents;
import l2e.gameserver.handler.communityhandlers.impl.CommunityForge;
import l2e.gameserver.handler.communityhandlers.impl.CommunityFriend;
import l2e.gameserver.handler.communityhandlers.impl.CommunityGeneral;
import l2e.gameserver.handler.communityhandlers.impl.CommunityLink;
import l2e.gameserver.handler.communityhandlers.impl.CommunityNpcCalc;
import l2e.gameserver.handler.communityhandlers.impl.CommunityPunishment;
import l2e.gameserver.handler.communityhandlers.impl.CommunityRaidBoss;
import l2e.gameserver.handler.communityhandlers.impl.CommunityRanking;
import l2e.gameserver.handler.communityhandlers.impl.CommunityServices;
import l2e.gameserver.handler.communityhandlers.impl.CommunityTeleport;
import l2e.gameserver.handler.communityhandlers.impl.CommunityTopic;
import l2e.gameserver.model.entity.auction.AuctionsManager;

public class CommunityBoardHandler extends LoggerObject
{
	private final Map<String, ICommunityBoardHandler> _handlers;

	private CommunityBoardHandler()
	{
		_handlers = new HashMap<>();
		
		registerHandler(new CommunityAcademy());
		registerHandler(new CommunityGeneral());
		registerHandler(new CommunityForge());
		registerHandler(new CommunityRaidBoss());
		registerHandler(new CommunityBuffer());
		registerHandler(new CommunityClan());
		registerHandler(new CommunityClassMaster());
		registerHandler(new CommunityEvents());
		registerHandler(new CommunityFriend());
		registerHandler(new CommunityLink());
		registerHandler(new CommunityServices());
		registerHandler(new CommunityRanking());
		registerHandler(new CommunityTeleport());
		registerHandler(new CommunityTopic());
		registerHandler(new CommunityAuction());
		AuctionsManager.getInstance();
		registerHandler(new CommunityBalancer());
		registerHandler(new CommunityBalancerSkill());
		registerHandler(new CommunityCertification());
		registerHandler(new CommunityNpcCalc());
		registerHandler(new CommunityPunishment());
		
		info("Loaded " + _handlers.size() + " CommunityBoardHandlers.");
	}
	
	public void registerHandler(ICommunityBoardHandler commHandler)
	{
		for (final String bypass : commHandler.getBypassCommands())
		{
			if (_handlers.containsKey(bypass))
			{
				info("dublicate bypass registered! First handler: " + _handlers.get(bypass).getClass().getSimpleName() + " second: " + commHandler.getClass().getSimpleName());
				_handlers.remove(bypass);
			}
			_handlers.put(bypass, commHandler);
		}
	}
	
	public ICommunityBoardHandler getHandler(String bypass)
	{
		if (!Config.ALLOW_COMMUNITY || _handlers.isEmpty())
		{
			return null;
		}
		
		if (Config.DISABLE_COMMUNITY_BYPASSES.contains(bypass))
		{
			return null;
		}

		for (final Map.Entry<String, ICommunityBoardHandler> entry : _handlers.entrySet())
		{
			if (bypass.contains(entry.getKey()))
			{
				return entry.getValue();
			}
		}
		return null;
	}
	
	public int size()
	{
		return _handlers.size();
	}
	
	public static CommunityBoardHandler getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBoardHandler _instance = new CommunityBoardHandler();
	}
}