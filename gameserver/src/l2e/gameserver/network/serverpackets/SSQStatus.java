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
package l2e.gameserver.network.serverpackets;

import l2e.gameserver.Config;
import l2e.gameserver.SevenSigns;
import l2e.gameserver.SevenSignsFestival;
import l2e.gameserver.model.stats.StatsSet;
import l2e.gameserver.network.SystemMessageId;

public class SSQStatus extends GameServerPacket
{
	private final int _objectId;
	private final int _page;
	
	public SSQStatus(int objectId, int recordPage)
	{
		_objectId = objectId;
		_page = recordPage;
	}
	
	@Override
	protected final void writeImpl()
	{
		final int winningCabal = SevenSigns.getInstance().getCabalHighestScore();
		final int totalDawnMembers = SevenSigns.getInstance().getTotalMembers(SevenSigns.CABAL_DAWN);
		final int totalDuskMembers = SevenSigns.getInstance().getTotalMembers(SevenSigns.CABAL_DUSK);
		
		writeC(_page);
		writeC(SevenSigns.getInstance().getCurrentPeriod());
		
		long dawnPercent = 0;
		long duskPercent = 0;
		switch (_page)
		{
			case 1 :
				writeD(SevenSigns.getInstance().getCurrentCycle());
				final int currentPeriod = SevenSigns.getInstance().getCurrentPeriod();
				switch (currentPeriod)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING :
						writeD(SystemMessageId.INITIAL_PERIOD.getId());
						break;
					case SevenSigns.PERIOD_COMPETITION :
						writeD(SystemMessageId.SSQ_COMPETITION_UNDERWAY.getId());
						break;
					case SevenSigns.PERIOD_COMP_RESULTS :
						writeD(SystemMessageId.RESULTS_PERIOD.getId());
						break;
					case SevenSigns.PERIOD_SEAL_VALIDATION :
						writeD(SystemMessageId.VALIDATION_PERIOD.getId());
						break;
				}
				
				switch (currentPeriod)
				{
					case SevenSigns.PERIOD_COMP_RECRUITING :
					case SevenSigns.PERIOD_COMP_RESULTS :
						writeD(SystemMessageId.UNTIL_TODAY_6PM.getId());
						break;
					case SevenSigns.PERIOD_COMPETITION :
					case SevenSigns.PERIOD_SEAL_VALIDATION :
						writeD(SystemMessageId.UNTIL_MONDAY_6PM.getId());
						break;
				}
				writeC(SevenSigns.getInstance().getPlayerCabal(_objectId));
				writeC(SevenSigns.getInstance().getPlayerSeal(_objectId));
				writeQ(SevenSigns.getInstance().getPlayerStoneContrib(_objectId));
				writeQ(SevenSigns.getInstance().getPlayerAdenaCollect(_objectId));
				
				final double dawnStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DAWN);
				final int dawnFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(SevenSigns.CABAL_DAWN);
				final double duskStoneScore = SevenSigns.getInstance().getCurrentStoneScore(SevenSigns.CABAL_DUSK);
				final int duskFestivalScore = SevenSigns.getInstance().getCurrentFestivalScore(SevenSigns.CABAL_DUSK);
				final double totalStoneScore = duskStoneScore + dawnStoneScore;
				long duskStoneScoreProp = 0;
				long dawnStoneScoreProp = 0;
				
				if (totalStoneScore != 0)
				{
					duskStoneScoreProp = Math.round((duskStoneScore * 500 / totalStoneScore));
					dawnStoneScoreProp = Math.round((dawnStoneScore * 500 / totalStoneScore));
				}
				
				final long duskTotalScore = SevenSigns.getInstance().getCurrentScore(SevenSigns.CABAL_DUSK);
				final long dawnTotalScore = SevenSigns.getInstance().getCurrentScore(SevenSigns.CABAL_DAWN);
				
				final long totalOverallScore = duskTotalScore + dawnTotalScore;
				
				if (totalOverallScore != 0)
				{
					dawnPercent = Math.round((dawnTotalScore * 110. / totalOverallScore));
					duskPercent = Math.round((duskTotalScore * 110. / totalOverallScore));
				}
				
				if (Config.DEBUG)
				{
					_log.info("Dusk Stone Score: " + duskStoneScore + " - Dawn Stone Score: " + dawnStoneScore);
					_log.info("Dusk Festival Score: " + duskFestivalScore + " - Dawn Festival Score: " + dawnFestivalScore);
					_log.info("Dusk Score: " + duskTotalScore + " - Dawn Score: " + dawnTotalScore);
					_log.info("Overall Score: " + totalOverallScore);
					_log.info("");
					if (totalStoneScore == 0)
					{
						_log.info("Dusk Prop: 0 - Dawn Prop: 0");
					}
					else
					{
						_log.info("Dusk Prop: " + ((duskStoneScore / totalStoneScore) * 500) + " - Dawn Prop: " + ((dawnStoneScore / totalStoneScore) * 500));
					}
					_log.info("Dusk %: " + duskPercent + " - Dawn %: " + dawnPercent);
				}

				writeQ(duskStoneScoreProp);
				writeQ(duskFestivalScore);
				writeQ(duskTotalScore);
				writeC((int) duskPercent);
				writeQ(dawnStoneScoreProp);
				writeQ(dawnFestivalScore);
				writeQ(dawnTotalScore);
				writeC((int) dawnPercent);
				break;
			case 2 :
				writeH(0x01);
				writeC(0x05);
				
				for (int i = 0; i < 5; i++)
				{
					writeC(i + 1);
					writeD(SevenSignsFestival.FESTIVAL_LEVEL_SCORES[i]);
					
					final int duskScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DUSK, i);
					final int dawnScore = SevenSignsFestival.getInstance().getHighestScore(SevenSigns.CABAL_DAWN, i);
					
					writeQ(duskScore);
					
					StatsSet highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DUSK, i);
					String[] partyMembers = highScoreData.getString("members").split(",");
					
					if (partyMembers != null)
					{
						writeC(partyMembers.length);
						
						for (final String partyMember : partyMembers)
						{
							writeS(partyMember);
						}
					}
					else
					{
						writeC(0x00);
					}
					writeQ(dawnScore);
					
					highScoreData = SevenSignsFestival.getInstance().getHighestScoreData(SevenSigns.CABAL_DAWN, i);
					partyMembers = highScoreData.getString("members").split(",");
					
					if (partyMembers != null)
					{
						writeC(partyMembers.length);
						for (final String partyMember : partyMembers)
						{
							writeS(partyMember);
						}
					}
					else
					{
						writeC(0x00);
					}
				}
				break;
			case 3 :
				writeC(0x10);
				writeC(0x35);
				writeC(0x03);
				
				for (int i = 1; i < 4; i++)
				{
					final int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					final int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
					
					if (Config.DEBUG)
					{
						_log.info(SevenSigns.getSealName(i, true) + " = Dawn Prop: " + dawnProportion + "(" + ((dawnProportion / totalDawnMembers) * 100) + "%)" + ", Dusk Prop: " + duskProportion + "(" + ((duskProportion / totalDuskMembers) * 100) + "%)");
					}
					
					writeC(i);
					writeC(SevenSigns.getInstance().getSealOwner(i));
					
					if (totalDuskMembers == 0)
					{
						if (totalDawnMembers == 0)
						{
							writeC(0x00);
							writeC(0x00);
						}
						else
						{
							writeC(0x00);
							writeC(Math.round(((float) dawnProportion / (float) totalDawnMembers) * 100));
						}
					}
					else
					{
						if (totalDawnMembers == 0)
						{
							writeC(Math.round(((float) duskProportion / (float) totalDuskMembers) * 100));
							writeC(0x00);
						}
						else
						{
							writeC(Math.round(((float) duskProportion / (float) totalDuskMembers) * 100));
							writeC(Math.round(((float) dawnProportion / (float) totalDawnMembers) * 100));
						}
					}
				}
				break;
			case 4 :
				writeC(winningCabal);
				writeC(0x03);
				for (int i = 1; i < 4; i++)
				{
					final int dawnProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DAWN);
					final int duskProportion = SevenSigns.getInstance().getSealProportion(i, SevenSigns.CABAL_DUSK);
					dawnPercent = Math.round((dawnProportion / (totalDawnMembers == 0 ? 1 : (float) totalDawnMembers)) * 100);
					duskPercent = Math.round((duskProportion / (totalDuskMembers == 0 ? 1 : (float) totalDuskMembers)) * 100);
					final int sealOwner = SevenSigns.getInstance().getSealOwner(i);
					
					writeC(i);
					
					switch (sealOwner)
					{
						case SevenSigns.CABAL_NULL :
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL :
									writeC(SevenSigns.CABAL_NULL);
									writeD(SystemMessageId.COMPETITION_TIE_SEAL_NOT_AWARDED.getId());
									break;
								case SevenSigns.CABAL_DAWN :
									if (dawnPercent >= 35)
									{
										writeC(SevenSigns.CABAL_DAWN);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_LESS_VOTED.getId());
									}
									break;
								case SevenSigns.CABAL_DUSK :
									if (duskPercent >= 35)
									{
										writeC(SevenSigns.CABAL_DUSK);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_LESS_VOTED.getId());
									}
									break;
							}
							break;
						case SevenSigns.CABAL_DAWN :
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL :
									if (dawnPercent >= 10)
									{
										writeC(SevenSigns.CABAL_DAWN);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.COMPETITION_TIE_SEAL_NOT_AWARDED.getId());
									}
									break;
								case SevenSigns.CABAL_DAWN :
									if (dawnPercent >= 10)
									{
										writeC(sealOwner);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_OWNED_10_LESS_VOTED.getId());
									}
									break;
								case SevenSigns.CABAL_DUSK :
									if (duskPercent >= 35)
									{
										writeC(SevenSigns.CABAL_DUSK);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_MORE_VOTED.getId());
									}
									else if (dawnPercent >= 10)
									{
										writeC(SevenSigns.CABAL_DAWN);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_OWNED_10_LESS_VOTED.getId());
									}
									break;
							}
							break;
						case SevenSigns.CABAL_DUSK :
							switch (winningCabal)
							{
								case SevenSigns.CABAL_NULL :
									if (duskPercent >= 10)
									{
										writeC(SevenSigns.CABAL_DUSK);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.COMPETITION_TIE_SEAL_NOT_AWARDED.getId());
									}
									break;
								case SevenSigns.CABAL_DAWN :
									if (dawnPercent >= 35)
									{
										writeC(SevenSigns.CABAL_DAWN);
										writeD(SystemMessageId.SEAL_NOT_OWNED_35_MORE_VOTED.getId());
									}
									else if (duskPercent >= 10)
									{
										writeC(sealOwner);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_OWNED_10_LESS_VOTED.getId());
									}
									break;
								case SevenSigns.CABAL_DUSK :
									if (duskPercent >= 10)
									{
										writeC(sealOwner);
										writeD(SystemMessageId.SEAL_OWNED_10_MORE_VOTED.getId());
									}
									else
									{
										writeC(SevenSigns.CABAL_NULL);
										writeD(SystemMessageId.SEAL_OWNED_10_LESS_VOTED.getId());
									}
									break;
							}
							break;
					}
				}
				break;
		}
	}
}