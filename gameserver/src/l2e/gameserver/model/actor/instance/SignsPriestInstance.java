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
package l2e.gameserver.model.actor.instance;

import java.util.StringTokenizer;

import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.data.htm.HtmCache;
import l2e.gameserver.instancemanager.ReflectionManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.items.instance.ItemInstance;
import l2e.gameserver.network.SystemMessageId;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import l2e.gameserver.network.serverpackets.SystemMessage;

public class SignsPriestInstance extends NpcInstance
{
	public SignsPriestInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
		setInstanceType(InstanceType.SignsPriestInstance);
	}
	
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if ((player.getLastFolkNPC() == null) || (player.getLastFolkNPC().getObjectId() != getObjectId()))
		{
			return;
		}
		
		if (command.startsWith("SevenSignsDesc"))
		{
			final int val = Integer.parseInt(command.substring(15));
			showChatWindow(player, val, null, true);
		}
		else if (command.startsWith("SevenSigns"))
		{
			SystemMessage sm;

			String path;

			int cabal = SevenSigns.CABAL_NULL;
			int stoneType = 0;

			final long ancientAdenaAmount = player.getAncientAdena();

			int val = Integer.parseInt(command.substring(11, 12).trim());

			if (command.length() > 12)
			{
				val = Integer.parseInt(command.substring(11, 13).trim());
			}

			if (command.length() > 13)
			{
				try
				{
					cabal = Integer.parseInt(command.substring(14, 15).trim());
				}
				catch (final Exception e)
				{
					try
					{
						cabal = Integer.parseInt(command.substring(13, 14).trim());
					}
					catch (final Exception e2)
					{
						try
						{
							final StringTokenizer st = new StringTokenizer(command.trim());
							st.nextToken();
							cabal = Integer.parseInt(st.nextToken());
						}
						catch (final Exception e3)
						{
							_log.warn("Failed to retrieve cabal from bypass command. NpcId: " + getId() + "; Command: " + command);
						}
					}
				}
			}

			switch (val)
			{
				case 2 :
					if (!player.getInventory().validateCapacity(1))
					{
						player.sendPacket(SystemMessageId.SLOTS_FULL);
						break;
					}

					if (!player.reduceAdena("SevenSigns", SevenSigns.RECORD_SEVEN_SIGNS_COST, this, true))
					{
						player.sendPacket(SystemMessageId.YOU_NOT_ENOUGH_ADENA);
						break;
					}
					player.getInventory().addItem("SevenSigns", SevenSigns.RECORD_SEVEN_SIGNS_ID, 1, player, this);

					sm = SystemMessage.getSystemMessage(SystemMessageId.EARNED_ITEM_S1);
					sm.addItemName(SevenSigns.RECORD_SEVEN_SIGNS_ID);
					player.sendPacket(sm);

					if (this instanceof DawnPriestInstance)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				case 33 :
					final int oldCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());

					if (oldCabal != SevenSigns.CABAL_NULL)
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, val, "dawn_member", false);
						}
						else
						{
							showChatWindow(player, val, "dusk_member", false);
						}
						return;
					}
					else if (player.getClassId().level() == 0)
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, val, "dawn_firstclass", false);
						}
						else
						{
							showChatWindow(player, val, "dusk_firstclass", false);
						}
						return;
					}
					else if ((cabal == SevenSigns.CABAL_DUSK) && Config.ALT_GAME_CASTLE_DUSK)
					{
						if ((player.getClan() != null) && (player.getClan().getCastleId() > 0))
						{
							showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
							break;
						}
					}
					else if ((cabal == SevenSigns.CABAL_DAWN) && Config.ALT_GAME_CASTLE_DAWN)
					{
						if ((player.getClan() == null) || (player.getClan().getCastleId() == 0))
						{
							showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_fee.htm");
							break;
						}
					}

					if (this instanceof DawnPriestInstance)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				case 34 :
					if ((player.getClassId().level() > 0) && ((player.getAdena() >= Config.SSQ_JOIN_DAWN_ADENA_FEE) || (player.getInventory().getInventoryItemCount(Config.SSQ_MANORS_AGREEMENT_ID, -1) > 0)))
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn.htm");
					}
					else
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_no.htm");
					}
					break;
				case 3 :
				case 8 :
					showChatWindow(player, val, SevenSigns.getCabalShortName(cabal), false);
					break;
				case 4 :
					final int newSeal = Integer.parseInt(command.substring(15));

					if (player.getClassId().level() >= 1)
					{
						if ((cabal == SevenSigns.CABAL_DUSK) && Config.ALT_GAME_CASTLE_DUSK)
						{
							if ((player.getClan() != null) && (player.getClan().getCastleId() > 0))
							{
								showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dusk_no.htm");
								return;
							}
						}

						if (Config.ALT_GAME_CASTLE_DAWN && (cabal == SevenSigns.CABAL_DAWN))
						{
							boolean allowJoinDawn = false;

							if ((player.getClan() != null) && (player.getClan().getCastleId() > 0))
							{
								allowJoinDawn = true;
							}
							else if (player.destroyItemByItemId("SevenSigns", Config.SSQ_MANORS_AGREEMENT_ID, 1, this, true))
							{
								allowJoinDawn = true;
							}
							else if (player.reduceAdena("SevenSigns", Config.SSQ_JOIN_DAWN_ADENA_FEE, this, true))
							{
								allowJoinDawn = true;
							}

							if (!allowJoinDawn)
							{
								showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_33_dawn_fee.htm");
								return;
							}
						}
					}
					SevenSigns.getInstance().setPlayerInfo(player.getObjectId(), cabal, newSeal);

					if (cabal == SevenSigns.CABAL_DAWN)
					{
						player.sendPacket(SystemMessageId.SEVENSIGNS_PARTECIPATION_DAWN);
					}
					else
					{
						player.sendPacket(SystemMessageId.SEVENSIGNS_PARTECIPATION_DUSK);
					}

					switch (newSeal)
					{
						case SevenSigns.SEAL_AVARICE :
							player.sendPacket(SystemMessageId.FIGHT_FOR_AVARICE);
							break;
						case SevenSigns.SEAL_GNOSIS :
							player.sendPacket(SystemMessageId.FIGHT_FOR_GNOSIS);
							break;
						case SevenSigns.SEAL_STRIFE :
							player.sendPacket(SystemMessageId.FIGHT_FOR_STRIFE);
							break;
					}
					showChatWindow(player, 4, SevenSigns.getCabalShortName(cabal), false);
					break;
				case 5 :
					if (this instanceof DawnPriestInstance)
					{
						if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == SevenSigns.CABAL_NULL)
						{
							showChatWindow(player, val, "dawn_no", false);
						}
						else
						{
							showChatWindow(player, val, "dawn", false);
						}
					}
					else
					{
						if (SevenSigns.getInstance().getPlayerCabal(player.getObjectId()) == SevenSigns.CABAL_NULL)
						{
							showChatWindow(player, val, "dusk_no", false);
						}
						else
						{
							showChatWindow(player, val, "dusk", false);
						}
					}
					break;
				case 21 :
					final int contribStoneId = Integer.parseInt(command.substring(14, 18));

					final ItemInstance contribBlueStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
					final ItemInstance contribGreenStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
					final ItemInstance contribRedStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);

					final long contribBlueStoneCount = contribBlueStones == null ? 0 : contribBlueStones.getCount();
					final long contribGreenStoneCount = contribGreenStones == null ? 0 : contribGreenStones.getCount();
					final long contribRedStoneCount = contribRedStones == null ? 0 : contribRedStones.getCount();

					long score = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
					long contributionCount = 0;

					boolean contribStonesFound = false;

					long redContrib = 0;
					long greenContrib = 0;
					long blueContrib = 0;

					try
					{
						contributionCount = Long.parseLong(command.substring(19).trim());
					}
					catch (final Exception NumberFormatException)
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 6, "dawn_failure", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk_failure", false);
						}
						break;
					}

					switch (contribStoneId)
					{
						case SevenSigns.SEAL_STONE_BLUE_ID :
							blueContrib = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.BLUE_CONTRIB_POINTS;
							if (blueContrib > contribBlueStoneCount)
							{
								blueContrib = contributionCount;
							}
							break;
						case SevenSigns.SEAL_STONE_GREEN_ID :
							greenContrib = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.GREEN_CONTRIB_POINTS;
							if (greenContrib > contribGreenStoneCount)
							{
								greenContrib = contributionCount;
							}
							break;
						case SevenSigns.SEAL_STONE_RED_ID :
							redContrib = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - score) / SevenSigns.RED_CONTRIB_POINTS;
							if (redContrib > contribRedStoneCount)
							{
								redContrib = contributionCount;
							}
							break;
					}

					if (redContrib > 0)
					{
						if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_RED_ID, redContrib, this, false))
						{
							contribStonesFound = true;
							final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
							msg.addItemName(SevenSigns.SEAL_STONE_RED_ID);
							msg.addItemNumber(redContrib);
							player.sendPacket(msg);
						}
					}
					if (greenContrib > 0)
					{
						if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_GREEN_ID, greenContrib, this, false))
						{
							contribStonesFound = true;
							final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
							msg.addItemName(SevenSigns.SEAL_STONE_GREEN_ID);
							msg.addItemNumber(greenContrib);
							player.sendPacket(msg);
						}
					}
					if (blueContrib > 0)
					{
						if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_BLUE_ID, blueContrib, this, false))
						{
							contribStonesFound = true;
							final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
							msg.addItemName(SevenSigns.SEAL_STONE_BLUE_ID);
							msg.addItemNumber(blueContrib);
							player.sendPacket(msg);
						}
					}

					if (!contribStonesFound)
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 6, "dawn_low_stones", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk_low_stones", false);
						}
					}
					else
					{
						score = SevenSigns.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContrib, greenContrib, redContrib);
						sm = SystemMessage.getSystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_S1);
						sm.addItemNumber(score);
						player.sendPacket(sm);

						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 6, "dawn", false);
						}
						else
						{
							showChatWindow(player, 6, "dusk", false);
						}
					}
					break;
				case 6 :
					stoneType = Integer.parseInt(command.substring(13));

					final ItemInstance blueStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
					final ItemInstance greenStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
					final ItemInstance redStones = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);

					final long blueStoneCount = blueStones == null ? 0 : blueStones.getCount();
					final long greenStoneCount = greenStones == null ? 0 : greenStones.getCount();
					final long redStoneCount = redStones == null ? 0 : redStones.getCount();

					long contribScore = SevenSigns.getInstance().getPlayerContribScore(player.getObjectId());
					boolean stonesFound = false;

					if (contribScore == Config.ALT_MAXIMUM_PLAYER_CONTRIB)
					{
						player.sendPacket(SystemMessageId.CONTRIB_SCORE_EXCEEDED);
					}
					else
					{
						long redContribCount = 0;
						long greenContribCount = 0;
						long blueContribCount = 0;

						String contribStoneColor = null;
						String stoneColorContr = null;

						long stoneCountContr = 0;
						int stoneIdContr = 0;

						switch (stoneType)
						{
							case 1 :
								contribStoneColor = "Blue";
								stoneColorContr = "blue";
								stoneIdContr = SevenSigns.SEAL_STONE_BLUE_ID;
								stoneCountContr = blueStoneCount;
								break;
							case 2 :
								contribStoneColor = "Green";
								stoneColorContr = "green";
								stoneIdContr = SevenSigns.SEAL_STONE_GREEN_ID;
								stoneCountContr = greenStoneCount;
								break;
							case 3 :
								contribStoneColor = "Red";
								stoneColorContr = "red";
								stoneIdContr = SevenSigns.SEAL_STONE_RED_ID;
								stoneCountContr = redStoneCount;
								break;
							case 4 :
								long tempContribScore = contribScore;
								redContribCount = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.RED_CONTRIB_POINTS;
								if (redContribCount > redStoneCount)
								{
									redContribCount = redStoneCount;
								}

								tempContribScore += redContribCount * SevenSigns.RED_CONTRIB_POINTS;
								greenContribCount = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.GREEN_CONTRIB_POINTS;
								if (greenContribCount > greenStoneCount)
								{
									greenContribCount = greenStoneCount;
								}

								tempContribScore += greenContribCount * SevenSigns.GREEN_CONTRIB_POINTS;
								blueContribCount = (Config.ALT_MAXIMUM_PLAYER_CONTRIB - tempContribScore) / SevenSigns.BLUE_CONTRIB_POINTS;
								if (blueContribCount > blueStoneCount)
								{
									blueContribCount = blueStoneCount;
								}

								if (redContribCount > 0)
								{
									if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_RED_ID, redContribCount, this, false))
									{
										stonesFound = true;
										final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
										msg.addItemName(SevenSigns.SEAL_STONE_RED_ID);
										msg.addItemNumber(redContribCount);
										player.sendPacket(msg);
									}
								}
								if (greenContribCount > 0)
								{
									if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_GREEN_ID, greenContribCount, this, false))
									{
										stonesFound = true;
										final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
										msg.addItemName(SevenSigns.SEAL_STONE_GREEN_ID);
										msg.addItemNumber(greenContribCount);
										player.sendPacket(msg);
									}
								}
								if (blueContribCount > 0)
								{
									if (player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_BLUE_ID, blueContribCount, this, false))
									{
										stonesFound = true;
										final SystemMessage msg = SystemMessage.getSystemMessage(SystemMessageId.S2_S1_DISAPPEARED);
										msg.addItemName(SevenSigns.SEAL_STONE_BLUE_ID);
										msg.addItemNumber(blueContribCount);
										player.sendPacket(msg);
									}
								}

								if (!stonesFound)
								{
									if (this instanceof DawnPriestInstance)
									{
										showChatWindow(player, val, "dawn_no_stones", false);
									}
									else
									{
										showChatWindow(player, val, "dusk_no_stones", false);
									}
								}
								else
								{
									contribScore = SevenSigns.getInstance().addPlayerStoneContrib(player.getObjectId(), blueContribCount, greenContribCount, redContribCount);
									sm = SystemMessage.getSystemMessage(SystemMessageId.CONTRIB_SCORE_INCREASED_S1);
									sm.addItemNumber(contribScore);
									player.sendPacket(sm);

									if (this instanceof DawnPriestInstance)
									{
										showChatWindow(player, 6, "dawn", false);
									}
									else
									{
										showChatWindow(player, 6, "dusk", false);
									}
								}
								return;
						}

						if (this instanceof DawnPriestInstance)
						{
							path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_6_dawn_contribute.htm";
						}
						else
						{
							path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_6_dusk_contribute.htm";
						}

						String contentContr = HtmCache.getInstance().getHtm(player, player.getLang(), path);

						if (contentContr != null)
						{
							contentContr = contentContr.replaceAll("%contribStoneColor%", contribStoneColor);
							contentContr = contentContr.replaceAll("%stoneColor%", stoneColorContr);
							contentContr = contentContr.replaceAll("%stoneCount%", String.valueOf(stoneCountContr));
							contentContr = contentContr.replaceAll("%stoneItemId%", String.valueOf(stoneIdContr));
							contentContr = contentContr.replaceAll("%objectId%", String.valueOf(getObjectId()));

							final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
							html.setHtml(player, contentContr);
							player.sendPacket(html);
						}
						else
						{
							_log.warn("Problem with HTML text " + path);
						}
					}
					break;
				case 7 :
					long ancientAdenaConvert = 0;

					try
					{
						ancientAdenaConvert = Long.parseLong(command.substring(13).trim());
					}
					catch (final NumberFormatException e)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}
					catch (final StringIndexOutOfBoundsException e)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}

					if (ancientAdenaConvert < 1)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_3.htm");
						break;
					}
					if (ancientAdenaAmount < ancientAdenaConvert)
					{
						showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_4.htm");
						break;
					}

					player.reduceAncientAdena("SevenSigns", ancientAdenaConvert, this, true);
					player.addAdena("SevenSigns", ancientAdenaConvert, this, true);

					showChatWindow(player, SevenSigns.SEVEN_SIGNS_HTML_PATH + "blkmrkt_5.htm");
					break;
				case 9 :
					final int playerCabal = SevenSigns.getInstance().getPlayerCabal(player.getObjectId());
					final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();

					if (SevenSigns.getInstance().isSealValidationPeriod() && (playerCabal == winningCabal))
					{
						final int ancientAdenaReward = SevenSigns.getInstance().getAncientAdenaReward(player.getObjectId(), true);

						if (ancientAdenaReward < 3)
						{
							if (this instanceof DawnPriestInstance)
							{
								showChatWindow(player, 9, "dawn_b", false);
							}
							else
							{
								showChatWindow(player, 9, "dusk_b", false);
							}
							break;
						}

						player.addAncientAdena("SevenSigns", ancientAdenaReward, this, true);

						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 9, "dawn_a", false);
						}
						else
						{
							showChatWindow(player, 9, "dusk_a", false);
						}
					}
					break;
				case 11 :
					try
					{
						final String portInfo = command.substring(14).trim();
						final StringTokenizer st = new StringTokenizer(portInfo);

						final int x = Integer.parseInt(st.nextToken());
						final int y = Integer.parseInt(st.nextToken());
						final int z = Integer.parseInt(st.nextToken());

						final long ancientAdenaCost = Long.parseLong(st.nextToken());

						if (ancientAdenaCost > 0)
						{
							if (!player.reduceAncientAdena("SevenSigns", ancientAdenaCost, this, true))
							{
								break;
							}
						}

						player.teleToLocation(x, y, z, true, ReflectionManager.DEFAULT);
					}
					catch (final Exception e)
					{
						_log.warn("SevenSigns: Error occurred while teleporting player: " + e.getMessage(), e);
					}
					break;
				case 16 :
					if (this instanceof DawnPriestInstance)
					{
						showChatWindow(player, val, "dawn", false);
					}
					else
					{
						showChatWindow(player, val, "dusk", false);
					}
					break;
				case 17 :
					stoneType = Integer.parseInt(command.substring(14));

					int stoneId = 0;
					long stoneCount = 0;
					int stoneValue = 0;

					String stoneColor = null;

					switch (stoneType)
					{
						case 1 :
							stoneColor = "blue";
							stoneId = SevenSigns.SEAL_STONE_BLUE_ID;
							stoneValue = SevenSigns.SEAL_STONE_BLUE_VALUE;
							break;
						case 2 :
							stoneColor = "green";
							stoneId = SevenSigns.SEAL_STONE_GREEN_ID;
							stoneValue = SevenSigns.SEAL_STONE_GREEN_VALUE;
							break;
						case 3 :
							stoneColor = "red";
							stoneId = SevenSigns.SEAL_STONE_RED_ID;
							stoneValue = SevenSigns.SEAL_STONE_RED_VALUE;
							break;
						case 4 :
							final ItemInstance blueStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_BLUE_ID);
							final ItemInstance greenStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_GREEN_ID);
							final ItemInstance redStonesAll = player.getInventory().getItemByItemId(SevenSigns.SEAL_STONE_RED_ID);

							final long blueStoneCountAll = blueStonesAll == null ? 0 : blueStonesAll.getCount();
							final long greenStoneCountAll = greenStonesAll == null ? 0 : greenStonesAll.getCount();
							final long redStoneCountAll = redStonesAll == null ? 0 : redStonesAll.getCount();
							long ancientAdenaRewardAll = 0;

							ancientAdenaRewardAll = SevenSigns.calcAncientAdenaReward(blueStoneCountAll, greenStoneCountAll, redStoneCountAll);

							if (ancientAdenaRewardAll == 0)
							{
								if (this instanceof DawnPriestInstance)
								{
									showChatWindow(player, 18, "dawn_no_stones", false);
								}
								else
								{
									showChatWindow(player, 18, "dusk_no_stones", false);
								}
								return;
							}

							if (blueStoneCountAll > 0)
							{
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_BLUE_ID, blueStoneCountAll, this, true);
							}
							if (greenStoneCountAll > 0)
							{
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_GREEN_ID, greenStoneCountAll, this, true);
							}
							if (redStoneCountAll > 0)
							{
								player.destroyItemByItemId("SevenSigns", SevenSigns.SEAL_STONE_RED_ID, redStoneCountAll, this, true);
							}

							player.addAncientAdena("SevenSigns", ancientAdenaRewardAll, this, true);

							if (this instanceof DawnPriestInstance)
							{
								showChatWindow(player, 18, "dawn", false);
							}
							else
							{
								showChatWindow(player, 18, "dusk", false);
							}
							return;
					}

					final ItemInstance stoneInstance = player.getInventory().getItemByItemId(stoneId);

					if (stoneInstance != null)
					{
						stoneCount = stoneInstance.getCount();
					}

					if (this instanceof DawnPriestInstance)
					{
						path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17_dawn.htm";
					}
					else
					{
						path = SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17_dusk.htm";
					}

					String content = HtmCache.getInstance().getHtm(player, player.getLang(), path);

					if (content != null)
					{
						content = content.replaceAll("%stoneColor%", stoneColor);
						content = content.replaceAll("%stoneValue%", String.valueOf(stoneValue));
						content = content.replaceAll("%stoneCount%", String.valueOf(stoneCount));
						content = content.replaceAll("%stoneItemId%", String.valueOf(stoneId));
						content = content.replaceAll("%objectId%", String.valueOf(getObjectId()));

						final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
						html.setHtml(player, content);
						player.sendPacket(html);
					}
					else
					{
						_log.warn("Problem with HTML text " + SevenSigns.SEVEN_SIGNS_HTML_PATH + "signs_17.htm: " + path);
					}
					break;
				case 18 :
					final int convertStoneId = Integer.parseInt(command.substring(14, 18));
					long convertCount = 0;

					try
					{
						convertCount = Long.parseLong(command.substring(19).trim());
					}
					catch (final Exception NumberFormatException)
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 18, "dawn_failed", false);
						}
						else
						{
							showChatWindow(player, 18, "dusk_failed", false);
						}
						break;
					}

					final ItemInstance convertItem = player.getInventory().getItemByItemId(convertStoneId);

					if (convertItem != null)
					{
						long ancientAdenaReward = 0;
						final long totalCount = convertItem.getCount();

						if ((convertCount <= totalCount) && (convertCount > 0))
						{
							switch (convertStoneId)
							{
								case SevenSigns.SEAL_STONE_BLUE_ID :
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(convertCount, 0, 0);
									break;
								case SevenSigns.SEAL_STONE_GREEN_ID :
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, convertCount, 0);
									break;
								case SevenSigns.SEAL_STONE_RED_ID :
									ancientAdenaReward = SevenSigns.calcAncientAdenaReward(0, 0, convertCount);
									break;
							}

							if (player.destroyItemByItemId("SevenSigns", convertStoneId, convertCount, this, true))
							{
								player.addAncientAdena("SevenSigns", ancientAdenaReward, this, true);

								if (this instanceof DawnPriestInstance)
								{
									showChatWindow(player, 18, "dawn", false);
								}
								else
								{
									showChatWindow(player, 18, "dusk", false);
								}
							}
						}
						else
						{
							if (this instanceof DawnPriestInstance)
							{
								showChatWindow(player, 18, "dawn_low_stones", false);
							}
							else
							{
								showChatWindow(player, 18, "dusk_low_stones", false);
							}
							break;
						}
					}
					else
					{
						if (this instanceof DawnPriestInstance)
						{
							showChatWindow(player, 18, "dawn_no_stones", false);
						}
						else
						{
							showChatWindow(player, 18, "dusk_no_stones", false);
						}
						break;
					}
					break;
				case 19 :
					final int chosenSeal = Integer.parseInt(command.substring(16));

					final String fileSuffix = SevenSigns.getSealName(chosenSeal, true) + "_" + SevenSigns.getCabalShortName(cabal);

					showChatWindow(player, val, fileSuffix, false);
					break;
				case 20 :
					final StringBuilder contentBuffer = new StringBuilder();
					if (this instanceof DawnPriestInstance)
					{
						contentBuffer.append("<html><body>Priest of Dawn:<br><font color=\"LEVEL\">[ Seal Status ]</font><br>");
					}
					else
					{
						contentBuffer.append("<html><body>Dusk Priestess:<br><font color=\"LEVEL\">[ Status of the Seals ]</font><br>");
					}

					for (int i = 1; i < 4; i++)
					{
						final int sealOwner = SevenSigns.getInstance().getSealOwner(i);

						if (sealOwner != SevenSigns.CABAL_NULL)
						{
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": " + SevenSigns.getCabalName(sealOwner) + "]<br>");
						}
						else
						{
							contentBuffer.append("[" + SevenSigns.getSealName(i, false) + ": Nothingness]<br>");
						}
					}

					contentBuffer.append("<a action=\"bypass -h npc_" + getObjectId() + "_Chat 0\">Go back.</a></body></html>");

					final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
					html.setHtml(player, contentBuffer.toString());
					player.sendPacket(html);
					break;
				default :
					showChatWindow(player, val, null, false);
					break;
			}
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}

	private void showChatWindow(Player player, int val, String suffix, boolean isDescription)
	{
		String filename = SevenSigns.SEVEN_SIGNS_HTML_PATH;

		filename += (isDescription) ? "desc_" + val : "signs_" + val;
		filename += (suffix != null) ? "_" + suffix + ".htm" : ".htm";

		showChatWindow(player, filename);
	}
}