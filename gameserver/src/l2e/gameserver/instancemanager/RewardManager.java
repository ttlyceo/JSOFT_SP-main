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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.ExPCCafePointInfo;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class RewardManager extends LoggerObject
{
	private boolean _allowPvpReward = false;
	private boolean _allowFortPvpReward = false;
	private boolean _allowClanWarReward = false;
	private boolean _allowTerritoryWarReward = false;
	
	private boolean _allowCaptureFortReward = false;
	private boolean _allowDefenceFortReward = false;
	
	private boolean _allowPvpRewardForParty = false;
	private boolean _allowFortPvpRewardForParty = false;
	private boolean _allowClanWarPvpRewardForParty = false;
	private boolean _allowTerritoryWarPvpRewardForParty = false;
	private boolean _allowCaptureFortRewardForLeader = false;
	private boolean _allowDefenceFortRewardForLeader = false;
	
	private boolean _allowOlyRewardByWinner = false;
	private boolean _allowOlyRewardByLoser = false;

	private final List<ItemHolder> _pvpRewards = new ArrayList<>();
	private final List<ItemHolder> _fortPvpRewards = new ArrayList<>();
	private final Map<Integer, List<ItemHolder>> _fortCaptureRewards = new HashMap<>();
	private final Map<Integer, List<ItemHolder>> _fortDefenceRewards = new HashMap<>();
	private final List<ItemHolder> _clanWarRewards = new ArrayList<>();
	private final List<ItemHolder> _territoryWarRewards = new ArrayList<>();
	private final List<ItemHolder> _olyByWinnerRewards = new ArrayList<>();
	private final List<ItemHolder> _olyByLoserRewards = new ArrayList<>();
	
	public RewardManager()
	{
		_pvpRewards.clear();
		_fortPvpRewards.clear();
		_fortCaptureRewards.clear();
		_fortDefenceRewards.clear();
		_clanWarRewards.clear();
		_territoryWarRewards.clear();
		_olyByWinnerRewards.clear();
		_olyByLoserRewards.clear();
		loadRewards();
	}
	
	public void reload()
	{
		_pvpRewards.clear();
		_fortPvpRewards.clear();
		_fortCaptureRewards.clear();
		_fortDefenceRewards.clear();
		_clanWarRewards.clear();
		_territoryWarRewards.clear();
		_olyByWinnerRewards.clear();
		_olyByLoserRewards.clear();
		loadRewards();
	}

	private void loadRewards()
	{
		try
		{
			final var file = new File(Config.DATAPACK_ROOT + "/config/mods/customRewards.xml");
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			final var doc1 = factory.newDocumentBuilder().parse(file);

			int counter = 0;
			for (var n1 = doc1.getFirstChild(); n1 != null; n1 = n1.getNextSibling())
			{
				if ("list".equalsIgnoreCase(n1.getNodeName()))
				{
					for (var d1 = n1.getFirstChild(); d1 != null; d1 = d1.getNextSibling())
					{
						if ("pvp".equalsIgnoreCase(d1.getNodeName()))
						{
							_allowPvpReward = Boolean.parseBoolean(d1.getAttributes().getNamedItem("allowReward").getNodeValue());
							_allowPvpRewardForParty = Boolean.parseBoolean(d1.getAttributes().getNamedItem("allowForParty").getNodeValue());
							for (var e1 = d1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
							{
								if ("reward".equalsIgnoreCase(e1.getNodeName()))
								{
									final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
									final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
									final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
									_pvpRewards.add(new ItemHolder(itemId, count, chance));
									counter++;
								}
							}
						}
						else if ("forts".equalsIgnoreCase(d1.getNodeName()))
						{
							for (var s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("pvp".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowFortPvpReward = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									_allowFortPvpRewardForParty = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowForParty").getNodeValue());
									for (var e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											_fortPvpRewards.add(new ItemHolder(itemId, count, chance));
											counter++;
										}
									}
								}
								else if ("capture".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowCaptureFortReward = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									_allowCaptureFortRewardForLeader = Boolean.parseBoolean(s1.getAttributes().getNamedItem("onlyForLeader").getNodeValue());
									for (Node e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											for (final var fort : FortManager.getInstance().getForts())
											{
												if (fort != null)
												{
													if (!_fortCaptureRewards.containsKey(fort.getId()))
													{
														_fortCaptureRewards.put(fort.getId(), new ArrayList<>());
													}
													_fortCaptureRewards.get(fort.getId()).add(new ItemHolder(itemId, count, chance));
													counter++;
												}
											}
										}
										else if ("fort".equalsIgnoreCase(e1.getNodeName()))
										{
											final var castleId = Integer.parseInt(e1.getAttributes().getNamedItem("id").getNodeValue());
											for (var r1 = e1.getFirstChild(); r1 != null; r1 = r1.getNextSibling())
											{
												if ("reward".equalsIgnoreCase(r1.getNodeName()))
												{
													final var itemId1 = Integer.parseInt(r1.getAttributes().getNamedItem("itemId").getNodeValue());
													final var count1 = Long.parseLong(r1.getAttributes().getNamedItem("count").getNodeValue());
													final var chance1 = Double.parseDouble(r1.getAttributes().getNamedItem("chance").getNodeValue());
													for (final var fort : FortManager.getInstance().getForts())
													{
														if (fort != null && fort.getId() == castleId)
														{
															if (!_fortCaptureRewards.containsKey(castleId))
															{
																_fortCaptureRewards.put(castleId, new ArrayList<>());
															}
															_fortCaptureRewards.get(castleId).add(new ItemHolder(itemId1, count1, chance1));
															counter++;
														}
													}
												}
											}
										}
									}
								}
								else if ("defence".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowDefenceFortReward = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									_allowDefenceFortRewardForLeader = Boolean.parseBoolean(s1.getAttributes().getNamedItem("onlyForLeader").getNodeValue());
									for (var e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											for (final var fort : FortManager.getInstance().getForts())
											{
												if (fort != null)
												{
													if (!_fortDefenceRewards.containsKey(fort.getId()))
													{
														_fortDefenceRewards.put(fort.getId(), new ArrayList<>());
													}
													_fortDefenceRewards.get(fort.getId()).add(new ItemHolder(itemId, count, chance));
													counter++;
												}
											}
										}
										else if ("fort".equalsIgnoreCase(e1.getNodeName()))
										{
											final var castleId = Integer.parseInt(e1.getAttributes().getNamedItem("id").getNodeValue());
											for (var r1 = e1.getFirstChild(); r1 != null; r1 = r1.getNextSibling())
											{
												if ("reward".equalsIgnoreCase(r1.getNodeName()))
												{
													final var itemId1 = Integer.parseInt(r1.getAttributes().getNamedItem("itemId").getNodeValue());
													final var count1 = Long.parseLong(r1.getAttributes().getNamedItem("count").getNodeValue());
													final var chance1 = Double.parseDouble(r1.getAttributes().getNamedItem("chance").getNodeValue());
													for (final var fort : FortManager.getInstance().getForts())
													{
														if (fort != null && fort.getId() == castleId)
														{
															if (!_fortDefenceRewards.containsKey(castleId))
															{
																_fortDefenceRewards.put(castleId, new ArrayList<>());
															}
															_fortDefenceRewards.get(castleId).add(new ItemHolder(itemId1, count1, chance1));
															counter++;
														}
													}
												}
											}
										}
									}
								}
							}
						}
						else if ("clanWar".equalsIgnoreCase(d1.getNodeName()))
						{
							_allowClanWarReward = Boolean.parseBoolean(d1.getAttributes().getNamedItem("allowReward").getNodeValue());
							_allowClanWarPvpRewardForParty = Boolean.parseBoolean(d1.getAttributes().getNamedItem("allowForParty").getNodeValue());
							for (var e1 = d1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
							{
								if ("reward".equalsIgnoreCase(e1.getNodeName()))
								{
									final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
									final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
									final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
									_clanWarRewards.add(new ItemHolder(itemId, count, chance));
									counter++;
								}
							}
						}
						else if ("territoryWar".equalsIgnoreCase(d1.getNodeName()))
						{
							for (var s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("pvp".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowTerritoryWarReward = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									_allowTerritoryWarPvpRewardForParty = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowForParty").getNodeValue());
									for (var e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											_territoryWarRewards.add(new ItemHolder(itemId, count, chance));
											counter++;
										}
									}
								}
							}
						}
						else if ("olympiad".equalsIgnoreCase(d1.getNodeName()))
						{
							for (var s1 = d1.getFirstChild(); s1 != null; s1 = s1.getNextSibling())
							{
								if ("winner".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowOlyRewardByWinner = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									for (var e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											_olyByWinnerRewards.add(new ItemHolder(itemId, count, chance));
											counter++;
										}
									}
								}
								else if ("loser".equalsIgnoreCase(s1.getNodeName()))
								{
									_allowOlyRewardByLoser = Boolean.parseBoolean(s1.getAttributes().getNamedItem("allowReward").getNodeValue());
									for (var e1 = s1.getFirstChild(); e1 != null; e1 = e1.getNextSibling())
									{
										if ("reward".equalsIgnoreCase(e1.getNodeName()))
										{
											final var itemId = Integer.parseInt(e1.getAttributes().getNamedItem("itemId").getNodeValue());
											final var count = Long.parseLong(e1.getAttributes().getNamedItem("count").getNodeValue());
											final var chance = Double.parseDouble(e1.getAttributes().getNamedItem("chance").getNodeValue());
											_olyByLoserRewards.add(new ItemHolder(itemId, count, chance));
											counter++;
										}
									}
								}
							}
						}
					}
				}
			}
			info("Loaded " + counter + " custom rewards.");
		}
		catch (
		    NumberFormatException | DOMException | ParserConfigurationException | SAXException e)
		{
			warn("customRewards.xml could not be initialized.", e);
		}
		catch (
		    IOException | IllegalArgumentException e)
		{
			warn("IOException or IllegalArgumentException.", e);
		}
	}
	
	public void checkPvpReward(Player killer, Player target)
	{
		if (!_allowPvpReward || _pvpRewards.isEmpty())
		{
			return;
		}
		
		if (_allowPvpRewardForParty && killer.isInParty())
		{
			if (target.isInParty() && (target.getParty() == killer.getParty()))
			{
				return;
			}
			
			if (killer != null && killer.getParty() != null)
			{
				killer.getParty().getMembers().stream().filter(p -> p != null).forEach(pm -> _pvpRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(pm, i.getId(), i.getCount())));
			}
		}
		else
		{
			if (killer != null)
			{
				_pvpRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(killer, i.getId(), i.getCount()));
			}
		}
	}
	
	public void checkTerritoryWarReward(Player killer, Player target)
	{
		if (!_allowTerritoryWarReward || _territoryWarRewards.isEmpty())
		{
			return;
		}
		
		if (_allowTerritoryWarPvpRewardForParty && killer.isInParty())
		{
			if (target.isInParty() && (target.getParty() == killer.getParty()))
			{
				return;
			}
			
			if (killer != null && killer.getParty() != null)
			{
				killer.getParty().getMembers().stream().filter(p -> p != null).forEach(pm -> _territoryWarRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(pm, i.getId(), i.getCount())));
			}
		}
		else
		{
			if (killer != null)
			{
				_territoryWarRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(killer, i.getId(), i.getCount()));
			}
		}
	}
	
	public void checkClanWarReward(Player killer, Player target)
	{
		if (!_allowClanWarReward || _clanWarRewards.isEmpty())
		{
			return;
		}
		
		if (_allowClanWarPvpRewardForParty && killer.isInParty())
		{
			if (target.isInParty() && (target.getParty() == killer.getParty()))
			{
				return;
			}
			
			if (killer != null && killer.getParty() != null)
			{
				killer.getParty().getMembers().stream().filter(p -> p != null).forEach(pm -> _clanWarRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(pm, i.getId(), i.getCount())));
			}
		}
		else
		{
			if (killer != null)
			{
				_clanWarRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(killer, i.getId(), i.getCount()));
			}
		}
	}

	public void checkFortPvpReward(Player killer, Player target)
	{
		if (!_allowFortPvpReward || _fortPvpRewards.isEmpty())
		{
			return;
		}
		
		if (_allowFortPvpRewardForParty && killer.isInParty())
		{
			if (target.isInParty() && (target.getParty() == killer.getParty()))
			{
				return;
			}
			
			if (killer != null && killer.getParty() != null)
			{
				killer.getParty().getMembers().stream().filter(p -> p != null).forEach(pm -> _fortPvpRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(pm, i.getId(), i.getCount())));
			}
		}
		else
		{
			if (killer != null)
			{
				_fortPvpRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(killer, i.getId(), i.getCount()));
			}
		}
	}
	
	public void checkFortCaptureReward(Clan clan, int fortId)
	{
		if (!_allowCaptureFortReward || _fortCaptureRewards.isEmpty() || clan == null)
		{
			return;
		}
		
		final var rewards = _fortCaptureRewards.get(fortId);
		if (rewards == null || rewards.isEmpty())
		{
			return;
		}
		
		if (_allowCaptureFortRewardForLeader)
		{
			final var leader = GameObjectsStorage.getPlayer(clan.getLeaderId());
			if (leader != null)
			{
				rewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(leader, i.getId(), i.getCount()));
			}
		}
		else
		{
			for (final var member : clan.getMembers())
			{
				if (member != null && member.isOnline())
				{
					for (final var tr : rewards)
					{
						if (Rnd.chance(tr.getChance()))
						{
							getReward(member.getPlayerInstance(), tr.getId(), tr.getCount());
						}
					}
				}
			}
		}
	}
	
	public void checkFortDefenceReward(Clan clan, int fortId)
	{
		if (!_allowDefenceFortReward || _fortDefenceRewards.isEmpty() || clan == null)
		{
			return;
		}
		
		final var rewards = _fortDefenceRewards.get(fortId);
		if (rewards == null || rewards.isEmpty())
		{
			return;
		}
		
		if (_allowDefenceFortRewardForLeader)
		{
			final var leader = GameObjectsStorage.getPlayer(clan.getLeaderId());
			if (leader != null)
			{
				rewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(leader, i.getId(), i.getCount()));
			}
		}
		else
		{
			for (final var member : clan.getMembers())
			{
				if (member != null && member.isOnline())
				{
					for (final var tr : rewards)
					{
						if (Rnd.chance(tr.getChance()))
						{
							getReward(member.getPlayerInstance(), tr.getId(), tr.getCount());
						}
					}
				}
			}
		}
	}
	
	public void checkOlyRewards(Player winner, Player loser)
	{
		if (_allowOlyRewardByWinner)
		{
			if (_olyByWinnerRewards.isEmpty())
			{
				return;
			}
			
			if (winner != null)
			{
				_olyByWinnerRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(winner, i.getId(), i.getCount()));
			}
		}
		
		if (_allowOlyRewardByLoser)
		{
			if (_olyByLoserRewards.isEmpty())
			{
				return;
			}
			
			if (winner != null)
			{
				_olyByLoserRewards.stream().filter(r -> (r != null) && Rnd.chance(r.getChance())).forEach(i -> getReward(loser, i.getId(), i.getCount()));
			}
		}
	}
	
	private void getReward(Player player, int itemId, long amount)
	{
		if (itemId == -100)
		{
			if (player.getPcBangPoints() >= Config.MAX_PC_BANG_POINTS)
			{
				final var sm = SystemMessage.getSystemMessage(SystemMessageId.THE_MAXMIMUM_ACCUMULATION_ALLOWED_OF_PC_CAFE_POINTS_HAS_BEEN_EXCEEDED);
				player.sendPacket(sm);
				return;
			}
			
			if ((player.getPcBangPoints() + amount) > Config.MAX_PC_BANG_POINTS)
			{
				amount = Config.MAX_PC_BANG_POINTS - player.getPcBangPoints();
			}
			
			player.setPcBangPoints((int) (player.getPcBangPoints() + amount));
			final var smsg = SystemMessage.getSystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_PC_CAFE_POINTS);
			smsg.addNumber((int) amount);
			player.sendPacket(smsg);
			player.sendPacket(new ExPCCafePointInfo(player.getPcBangPoints(), (int) amount, false, false, 1));
		}
		else if (itemId == -200)
		{
			player.getClan().addReputationScore((int) amount, true);
		}
		else if (itemId == -300)
		{
			player.setFame((int) (player.getFame() + amount));
			player.sendUserInfo();
		}
		else if (itemId > 0)
		{
			player.addItem("Reward", itemId, amount, player, true);
		}
	}
	
	public static final RewardManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final RewardManager _instance = new RewardManager();
	}
}