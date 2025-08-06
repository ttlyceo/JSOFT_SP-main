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
package l2e.gameserver.network;

import java.nio.ByteBuffer;

import org.nio.impl.IClientFactory;
import org.nio.impl.IMMOExecutor;
import org.nio.impl.IPacketHandler;
import org.nio.impl.MMOConnection;
import org.nio.impl.ReceivablePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.network.GameClient.GameClientState;
import l2e.gameserver.network.clientpackets.*;
import l2e.gameserver.network.clientpackets.Interface.RequestDynamicPacket;
import l2e.gameserver.network.clientpackets.pledge.RequestAnswerJoinPledge;
import l2e.gameserver.network.clientpackets.pledge.RequestCreatePledge;
import l2e.gameserver.network.clientpackets.pledge.RequestDismissPledge;
import l2e.gameserver.network.clientpackets.pledge.RequestExPledgeCrestLarge;
import l2e.gameserver.network.clientpackets.pledge.RequestExSetPledgeCrestLarge;
import l2e.gameserver.network.clientpackets.pledge.RequestJoinPledge;
import l2e.gameserver.network.clientpackets.pledge.RequestOustPledgeMember;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeCrest;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeExtendedInfo;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeInfo;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeMemberInfo;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeMemberList;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeMemberPowerInfo;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgePower;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgePowerGradeList;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeReorganizeMember;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeSetAcademyMaster;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeSetMemberPowerGrade;
import l2e.gameserver.network.clientpackets.pledge.RequestPledgeWarList;
import l2e.gameserver.network.clientpackets.pledge.RequestReplyStartPledgeWar;
import l2e.gameserver.network.clientpackets.pledge.RequestReplyStopPledgeWar;
import l2e.gameserver.network.clientpackets.pledge.RequestReplySurrenderPledgeWar;
import l2e.gameserver.network.clientpackets.pledge.RequestSurrenderPledgeWar;
import l2e.gameserver.network.clientpackets.pledge.RequestWithDrawalPledge;

public final class GamePacketHandler implements IPacketHandler<GameClient>, IClientFactory<GameClient>, IMMOExecutor<GameClient>
{
	private static final Logger _log = LoggerFactory.getLogger(GamePacketHandler.class);
	
	@Override
	public ReceivablePacket<GameClient> handlePacket(ByteBuffer buf, GameClient client)
	{
		int id3;
		
		ReceivablePacket<GameClient> msg = null;
		final int opcode = buf.get() & 0xFF;
		final GameClientState state = client.getState();
		
		switch (state)
		{
			case CONNECTED :
				switch (opcode)
				{
					case 0x0E :
						msg = new SendProtocolVersion();
						break;
					case 0x2B :
						msg = new RequestLogin();
						break;
					case 0xCB :
						msg = new ReplyGameGuardQuery();
						break;
					default :
						printDebug(opcode, buf, state, client);
						break;
				}
				break;
			case AUTHED :
				switch (opcode)
				{
					case 0x00 :
						msg = new SendLogOut();
						break;
					case 0x0C :
						msg = new RequestCharacterCreate();
						break;
					case 0x0D :
						msg = new RequestCharacterDelete();
						break;
					case 0x12 :
						msg = new RequestGameStart();
						break;
					case 0x13 :
						msg = new RequestNewCharacter();
						break;
					case 0x7B :
						msg = new RequestCharacterRestore();
						break;
					case 0xCB :
						msg = new ReplyGameGuardQuery();
						break;
					case 0xD0 :
						int id2 = -1;
						if (buf.remaining() >= 2)
						{
							id2 = buf.getShort() & 0xffff;
						}
						else
						{
							if (Config.CLIENT_PACKET_HANDLER_DEBUG)
							{
								_log.warn("Client: " + client.toString() + " sent a 0xd0 without the second opcode.");
							}
							break;
						}
						
						switch (id2)
						{
							case 0x21 :
								msg = new RequestKeyMapping();
								break;
							case 0x36 :
								msg = new RequestGotoLobby();
								break;
							case 0x3B :
								msg = new RequestExChangeName();
								break;
							case 0x93 :
								msg = new RequestEx2ndPasswordCheck();
								break;
							case 0x94 :
								msg = new RequestEx2ndPasswordVerify();
								break;
							case 0x95 :
								msg = new RequestEx2ndPasswordReq();
								break;
							default :
								printDebugDoubleOpcode(opcode, id2, buf, state, client);
						}
						break;
					default :
						printDebug(opcode, buf, state, client);
						break;
				}
				break;
			case ENTERING :
				switch (opcode)
				{
					case 0x11 :
						msg = new RequestEnterWorld();
						break;
					case 0xD0 :
						int id2 = -1;
						if (buf.remaining() >= 2)
						{
							id2 = buf.getShort() & 0xffff;
						}
						else
						{
							if (Config.CLIENT_PACKET_HANDLER_DEBUG)
							{
								_log.warn("Client: " + client.toString() + " sent a 0xd0 without the second opcode.");
							}
							break;
						}
						
						switch (id2)
						{
							case 0x01 :
								msg = new RequestManorList();
								break;
							case 0x21 :
								msg = new RequestKeyMapping();
								break;
							case 0x83 :
								int id99 = 0;
								if (buf.remaining() >= 4)
								{
									id99 = buf.getInt();
								}
								switch(id99)
								{
									case 0x10 :
										if (Config.ALLOW_CUSTOM_INTERFACE)
										{
											msg = new RequestKeyPacket();
										}
										break;
								}
								break;
							default :
								printDebugDoubleOpcode(opcode, id2, buf, state, client);
								break;
							
						}
						break;
					
					default :
						printDebug(opcode, buf, state, client);
						break;
				}
				break;
			case IN_GAME :
				switch (opcode)
				{
					case 0x00 :
						msg = new SendLogOut();
						break;
					case 0x01 :
						msg = new RequestAttack();
						break;
					case 0x03 :
						msg = new RequestStartPledgeWar();
						break;
					case 0x04 :
						msg = new RequestReplyStartPledgeWar();
						break;
					case 0x05 :
						msg = new RequestStopPledgeWar();
						break;
					case 0x06 :
						msg = new RequestReplyStopPledgeWar();
						break;
					case 0x07 :
						msg = new RequestSurrenderPledgeWar();
						break;
					case 0x08 :
						msg = new RequestReplySurrenderPledgeWar();
						break;
					case 0x09 :
						msg = new RequestSetPledgeCrest();
						break;
					case 0x0B :
						msg = new RequestGiveNickName();
						break;
					case 0x0F :
						msg = new MoveBackwardToLocation();
						break;
					case 0x10 :
						msg = new Say();
						break;
					case 0x14 :
						msg = new RequestItemList();
						break;
					case 0x15 :
						msg = new RequestEquipItem();
						break;
					case 0x16 :
						msg = new RequestUnEquipItem();
						break;
					case 0x17 :
						msg = new RequestDropItem();
						break;
					case 0x19 :
						msg = new RequestUseItem();
						break;
					case 0x1A :
						msg = new RequestTrade();
						break;
					case 0x1B :
						msg = new RequestAddTradeItem();
						break;
					case 0x1C :
						msg = new TradeDone();
						break;
					case 0x1F :
						msg = new Action();
						break;
					case 0x22 :
						msg = new RequestLinkHtml();
						break;
					case 0x23 :
						msg = new RequestBypassToServer();
						break;
					case 0x24 :
						msg = new RequestBBSwrite();
						break;
					case 0x25 :
						msg = new RequestCreatePledge();
						break;
					case 0x26 :
						msg = new RequestJoinPledge();
						break;
					case 0x27 :
						msg = new RequestAnswerJoinPledge();
						break;
					case 0x28 :
						msg = new RequestWithDrawalPledge();
						break;
					case 0x29 :
						msg = new RequestOustPledgeMember();
						break;
					case 0x2C :
						msg = new RequestGetItemFromPet();
						break;
					case 0x2E :
						msg = new RequestAllyInfo();
						break;
					case 0x2F :
						msg = new RequestCrystallizeItem();
						break;
					case 0x30 :
						msg = new RequestPrivateStoreSellManageList();
						break;
					case 0x31 :
						msg = new SetPrivateStoreSellList();
						break;
					case 0x33 :
						msg = new RequestTeleport();
						break;
					case 0x34 :
						msg = new SocialAction();
						break;
					case 0x35 :
						msg = new ChangeMoveType();
						break;
					case 0x36 :
						msg = new ChangeWaitType();
						break;
					case 0x37 :
						msg = new RequestSellItem();
						break;
					case 0x38 :
						msg = new UserAck();
						break;
					case 0x39 :
						msg = new RequestMagicSkillUse();
						break;
					case 0x3A :
						msg = new SendAppearing();
						break;
					case 0x3B :
						if (Config.ALLOW_WAREHOUSE)
						{
							msg = new SendWareHouseDepositList();
						}
						break;
					case 0x3C :
						msg = new SendWareHouseWithDrawList();
						break;
					case 0x3D :
						msg = new RequestShortCutReg();
						break;
					case 0x3E :
						msg = new RequestShortCutUse();
						break;
					case 0x3F :
						msg = new RequestShortCutDel();
						break;
					case 0x40 :
						msg = new RequestBuyItem();
						break;
					case 0x41 :
						msg = new RequestDismissPledge();
						break;
					case 0x42 :
						msg = new RequestJoinParty();
						break;
					case 0x43 :
						msg = new RequestAnswerJoinParty();
						break;
					case 0x44 :
						msg = new RequestWithDrawalParty();
						break;
					case 0x45 :
						msg = new RequestOustPartyMember();
						break;
					case 0x46 :
						msg = new RequestDismissParty();
						break;
					case 0x47 :
						msg = new CanNotMoveAnymore();
						break;
					case 0x48 :
						msg = new RequestTargetCancel();
						break;
					case 0x49 :
						msg = new Say2();
						break;
					case 0x4D :
						msg = new RequestPledgeMemberList();
						break;
					case 0x4F :
						msg = new RequestMagicList();
						break;
					case 0x50 :
						msg = new RequestSkillList();
						break;
					case 0x52 :
						msg = new MoveWithDelta();
						break;
					case 0x53 :
						msg = new RequestGetOnVehicle();
						break;
					case 0x54 :
						msg = new RequestGetOffVehicle();
						break;
					case 0x55 :
						msg = new AnswerTradeRequest();
						break;
					case 0x56 :
						msg = new RequestActionUse();
						break;
					case 0x57 :
						msg = new RequestRestart();
						break;
					case 0x58 :
						msg = new RequestSiegeInfo();
						break;
					case 0x59 :
						msg = new ValidatePosition();
						break;
					case 0x5A :
						msg = new RequestSEKCustom();
						break;
					case 0x5B :
						msg = new StartRotating();
						break;
					case 0x5C :
						msg = new FinishRotating();
						break;
					case 0x5E :
						msg = new RequestShowBoard();
						break;
					case 0x5F :
						msg = new RequestEnchantItem();
						break;
					case 0x60 :
						msg = new RequestDestroyItem();
						break;
					case 0x62 :
						msg = new RequestQuestList();
						break;
					case 0x63 :
						msg = new RequestDestroyQuest();
						break;
					case 0x65 :
						msg = new RequestPledgeInfo();
						break;
					case 0x66 :
						msg = new RequestPledgeExtendedInfo();
						break;
					case 0x67 :
						msg = new RequestPledgeCrest();
						break;
					case 0x69 :
						msg = new RequestSurrenderPersonally();
						break;
					case 0x6B :
						msg = new RequestSendL2FriendSay();
						break;
					case 0x6C :
						msg = new RequestOpenMinimap();
						break;
					case 0x6D :
						msg = new RequestSendMsnChatLog();
						break;
					case 0x6E :
						msg = new RequestReload();
						break;
					case 0x6F :
						msg = new RequestHennaEquip();
						break;
					case 0x70 :
						msg = new RequestHennaUnequipList();
						break;
					case 0x71 :
						msg = new RequestHennaUnequipInfo();
						break;
					case 0x72 :
						msg = new RequestHennaUnequip();
						break;
					case 0x73 :
						msg = new RequestAcquireSkillInfo();
						break;
					case 0x74 :
						msg = new SendBypassBuildCmd();
						break;
					case 0x75 :
						msg = new RequestMoveToLocationInVehicle();
						break;
					case 0x76 :
						msg = new CanNotMoveAnymoreInVehicle();
						break;
					case 0x77 :
						msg = new RequestFriendInvite();
						break;
					case 0x78 :
						msg = new RequestFriendAddReply();
						break;
					case 0x79 :
						msg = new RequestFriendInfoList();
						break;
					case 0x7A :
						msg = new RequestFriendDel();
						break;
					case 0x7C :
						msg = new RequestAcquireSkill();
						break;
					case 0x7D :
						msg = new RequestRestartPoint();
						break;
					case 0x7E :
						msg = new RequestGMCommand();
						break;
					case 0x7F :
						msg = new RequestListPartyWaiting();
						break;
					case 0x80 :
						msg = new RequestManagePartyRoom();
						break;
					case 0x81 :
						msg = new RequestJoinPartyRoom();
						break;
					case 0x83 :
						msg = new SendPrivateStoreBuyList();
						break;
					case 0x84 :
						msg = new RequestReviveReply();
						break;
					case 0x85 :
						msg = new RequestTutorialLinkHtml();
						break;
					case 0x86 :
						msg = new RequestTutorialPassCmdToServer();
						break;
					case 0x87 :
						msg = new RequestTutorialQuestionMarkPressed();
						break;
					case 0x88 :
						msg = new RequestTutorialClientEvent();
						break;
					case 0x89 :
						msg = new RequestPetition();
						break;
					case 0x8A :
						msg = new RequestPetitionCancel();
						break;
					case 0x8B :
						msg = new RequestGmList();
						break;
					case 0x8C :
						msg = new RequestJoinAlly();
						break;
					case 0x8D :
						msg = new RequestAnswerJoinAlly();
						break;
					case 0x8E :
						msg = new RequestWithdrawAlly();
						break;
					case 0x8F :
						msg = new RequestOustAlly();
						break;
					case 0x90 :
						msg = new RequestDismissAlly();
						break;
					case 0x91 :
						msg = new RequestSetAllyCrest();
						break;
					case 0x92 :
						msg = new RequestAllyCrest();
						break;
					case 0x93 :
						msg = new RequestChangePetName();
						break;
					case 0x94 :
						msg = new RequestPetUseItem();
						break;
					case 0x95 :
						msg = new RequestGiveItemToPet();
						break;
					case 0x96 :
						msg = new RequestPrivateStoreSellQuit();
						break;
					case 0x97 :
						msg = new SetPrivateStoreSellMsg();
						break;
					case 0x98 :
						msg = new RequestPetGetItem();
						break;
					case 0x99 :
						msg = new RequestPrivateStoreBuyManageList();
						break;
					case 0x9A :
						msg = new SetPrivateStoreBuyList();
						break;
					case 0x9B :
						msg = new ReplyStopAllianceWar();
						break;
					case 0x9C :
						msg = new RequestPrivateStoreBuyManageQuit();
						break;
					case 0x9D :
						msg = new SetPrivateStoreBuyMsg();
						break;
					case 0x9F :
						msg = new SendPrivateStoreSellList();
						break;
					case 0xA0 :
						msg = new SendTimeCheck();
						break;
					case 0xA6 :
						msg = new RequestSkillCoolTime();
						break;
					case 0xA7 :
						msg = new RequestPackageSendableItemList();
						break;
					case 0xA8 :
						msg = new RequestPackageSend();
						break;
					case 0xA9 :
						msg = new RequestBlock();
						break;
					case 0xAA :
						msg = new RequestCastleSiegeInfo();
						break;
					case 0xAB :
						msg = new RequestCastleSiegeAttackerList();
						break;
					case 0xAC :
						msg = new RequestCastleSiegeDefenderList();
						break;
					case 0xAD :
						msg = new RequestJoinCastleSiege();
						break;
					case 0xAE :
						msg = new RequestConfirmCastleSiegeWaitingList();
						break;
					case 0xAF :
						msg = new RequestSetCastleSiegeTime();
						break;
					case 0xB0 :
						msg = new RequestMultiSellChoose();
						break;
					case 0xB1 :
						msg = new NetPing();
						break;
					case 0xB2 :
						msg = new RequestRemainTime();
						break;
					case 0xB3 :
						msg = new BypassUserCmd();
						break;
					case 0xB4 :
						msg = new GMSnoopEnd();
						break;
					case 0xB5 :
						msg = new RequestRecipeBookOpen();
						break;
					case 0xB6 :
						msg = new RequestRecipeItemDelete();
						break;
					case 0xB7 :
						msg = new RequestRecipeItemMakeInfo();
						break;
					case 0xB8 :
						msg = new RequestRecipeItemMakeSelf();
						break;
					case 0xB9 :
						msg = new RequestRecipeShopManageList();
						break;
					case 0xBA :
						msg = new RequestRecipeShopMessageSet();
						break;
					case 0xBB :
						msg = new RequestRecipeShopListSet();
						break;
					case 0xBC :
						msg = new RequestRecipeShopManageQuit();
						break;
					case 0xBD :
						msg = new RequestRecipeShopManageCancel();
						break;
					case 0xBE :
						msg = new RequestRecipeShopMakeInfo();
						break;
					case 0xBF :
						msg = new RequestRecipeShopMakeDo();
						break;
					case 0xC0 :
						msg = new RequestRecipeShopSellList();
						break;
					case 0xC1 :
						msg = new RequestObserverEnd();
						break;
					case 0xC2 :
						msg = new VoteSociality();
						break;
					case 0xC3 :
						msg = new RequestHennaItemList();
						break;
					case 0xC4 :
						msg = new RequestHennaItemInfo();
						break;
					case 0xC5 :
						msg = new RequestBuySeed();
						break;
					case 0xC6 :
						msg = new ConfirmDlg();
						break;
					case 0xC7 :
						// msg = new RequestPreviewItem();
						break;
					case 0xC8 :
						msg = new RequestSSQStatus();
						break;
					case 0xC9 :
						msg = new PetitionVote();
						break;
					case 0xCB :
						msg = new ReplyGameGuardQuery();
						break;
					case 0xCC :
						msg = new RequestPledgePower();
						break;
					case 0xCD :
						msg = new RequestMakeMacro();
						break;
					case 0xCE :
						msg = new RequestDeleteMacro();
						break;
					case 0xCF :
						msg = new RequestProcureCrop();
						break;
					case 0xD0 :
						int id2 = -1;
						if (buf.remaining() >= 2)
						{
							id2 = buf.getShort() & 0xffff;
						}
						else
						{
							if (Config.CLIENT_PACKET_HANDLER_DEBUG)
							{
								_log.warn("Client: " + client.toString() + " sent a 0xd0 without the second opcode.");
							}
							break;
						}
						
						switch (id2)
						{
							case 0x01 :
								msg = new RequestManorList();
								break;
							case 0x02 :
								msg = new RequestProcureCropList();
								break;
							case 0x03 :
								msg = new RequestSetSeed();
								break;
							case 0x04 :
								msg = new RequestSetCrop();
								break;
							case 0x05 :
								msg = new RequestWriteHeroWords();
								break;
							case 0x06 :
								msg = new RequestExAskJoinMPCC();
								break;
							case 0x07 :
								msg = new RequestExAcceptJoinMPCC();
								break;
							case 0x08 :
								msg = new RequestExOustFromMPCC();
								break;
							case 0x09 :
								msg = new RequestOustFromPartyRoom();
								break;
							case 0x0A :
								msg = new RequestDismissPartyRoom();
								break;
							case 0x0B :
								msg = new RequestWithdrawPartyRoom();
								break;
							case 0x0C :
								msg = new RequestHandOverPartyMaster();
								break;
							case 0x0D :
								msg = new RequestAutoSoulShot();
								break;
							case 0x0E :
								msg = new RequestExEnchantSkillInfo();
								break;
							case 0x0F :
								msg = new RequestExEnchantSkill();
								break;
							case 0x10 :
								msg = new RequestExPledgeCrestLarge();
								break;
							case 0x11 :
								msg = new RequestExSetPledgeCrestLarge();
								break;
							case 0x12 :
								msg = new RequestPledgeSetAcademyMaster();
								break;
							case 0x13 :
								msg = new RequestPledgePowerGradeList();
								break;
							case 0x14 :
								msg = new RequestPledgeMemberPowerInfo();
								break;
							case 0x15 :
								msg = new RequestPledgeSetMemberPowerGrade();
								break;
							case 0x16 :
								msg = new RequestPledgeMemberInfo();
								break;
							case 0x17 :
								msg = new RequestPledgeWarList();
								break;
							case 0x18 :
								msg = new RequestExFishRanking();
								break;
							case 0x19 :
								msg = new RequestPCCafeCouponUse();
								break;
							case 0x1A :
								msg = new RequestExOrcMove();
								break;
							case 0x1B :
								msg = new RequestDuelStart();
								break;
							case 0x1C :
								msg = new RequestDuelAnswerStart();
								break;
							case 0x1D :
								msg = new RequestExSetTutorial();
								break;
							case 0x1E :
								msg = new RequestExRqItemLink();
								break;
							case 0x1F :
								msg = new RequestCannotMoveAnymoreAirShip();
								break;
							case 0x20 :
								msg = new RequestMoveToLocationInAirShip();
								break;
							case 0x21 :
								msg = new RequestKeyMapping();
								break;
							case 0x22 :
								msg = new RequestSaveKeyMapping();
								break;
							case 0x23 :
								msg = new RequestExRemoveItemAttribute();
								break;
							case 0x24 :
								msg = new RequestSaveInventoryOrder();
								break;
							case 0x25 :
								msg = new RequestExitPartyMatchingWaitingRoom();
								break;
							case 0x26 :
								msg = new RequestConfirmTargetItem();
								break;
							case 0x27 :
								msg = new RequestConfirmRefinerItem();
								break;
							case 0x28 :
								msg = new RequestConfirmGemStone();
								break;
							case 0x29 :
								msg = new RequestOlympiadObserverEnd();
								break;
							case 0x2A :
								msg = new RequestCursedWeaponList();
								break;
							case 0x2B :
								msg = new RequestCursedWeaponLocation();
								break;
							case 0x2C :
								msg = new RequestPledgeReorganizeMember();
								break;
							case 0x2D :
								msg = new RequestExMPCCShowPartyMembersInfo();
								break;
							case 0x2E :
								msg = new RequestOlympiadMatchList();
								break;
							case 0x2F :
								msg = new RequestAskJoinPartyRoom();
								break;
							case 0x30 :
								msg = new AnswerJoinPartyRoom();
								break;
							case 0x31 :
								msg = new RequestListPartyMatchingWaitingRoom();
								break;
							case 0x32 :
								msg = new RequestExEnchantSkillSafe();
								break;
							case 0x33 :
								msg = new RequestExEnchantSkillUntrain();
								break;
							case 0x34 :
								msg = new RequestExEnchantSkillChange();
								break;
							case 0x35 :
								msg = new RequestExEnchantItemAttribute();
								break;
							case 0x36 :
								msg = new RequestExGetOnAirShip();
								break;
							case 0x37 :
								msg = new RequestExGetOffAirShip();
								break;
							case 0x38 :
								msg = new RequestMoveToLocationAirShip();
								break;
							case 0x39 :
								msg = new RequestBidItemAuction();
								break;
							case 0x3A :
								msg = new RequestInfoItemAuction();
								break;
							case 0x3B :
								msg = new RequestExChangeName();
								break;
							case 0x3C :
								msg = new RequestAllCastleInfo();
								break;
							case 0x3D :
								msg = new RequestAllFortressInfo();
								break;
							case 0x3E :
								msg = new RequestAllAgitInfo();
								break;
							case 0x3F :
								msg = new RequestFortressSiegeInfo();
								break;
							case 0x40 :
								msg = new RequestGetBossRecord();
								break;
							case 0x41 :
								msg = new RequestRefine();
								break;
							case 0x42 :
								msg = new RequestConfirmCancelItem();
								break;
							case 0x43 :
								msg = new RequestRefineCancel();
								break;
							case 0x44 :
								msg = new RequestExMagicSkillUseGround();
								break;
							case 0x45 :
								msg = new RequestDuelSurrender();
								break;
							case 0x46 :
								msg = new RequestExEnchantSkillInfoDetail();
								break;
							case 0x48 :
								msg = new RequestFortressMapInfo();
								break;
							case 0x49 :
								msg = new RequestPVPMatchRecord();
								break;
							case 0x4A :
								msg = new SetPrivateStoreWholeMsg();
								break;
							case 0x4B :
								msg = new RequestDispel();
								break;
							case 0x4C :
								msg = new RequestExTryToPutEnchantTargetItem();
								break;
							case 0x4D :
								msg = new RequestExTryToPutEnchantSupportItem();
								break;
							case 0x4E :
								msg = new RequestExCancelEnchantItem();
								break;
							case 0x4F :
								msg = new RequestChangeNicknameColor();
								break;
							case 0x50 :
								msg = new RequestResetNickname();
								break;
							case 0x51 :
								id3 = 0;
								if (buf.remaining() >= 4)
								{
									id3 = buf.getInt();
								}
								else
								{
									if (Config.CLIENT_PACKET_HANDLER_DEBUG)
									{
										_log.warn("Client: " + client.toString() + " sent a 0xd0:0x51 without the third opcode.");
									}
									break;
								}
								switch (id3)
								{
									case 0x00 :
										msg = new RequestBookMarkSlotInfo();
										break;
									case 0x01 :
										msg = new RequestSaveBookMarkSlot();
										break;
									case 0x02 :
										msg = new RequestModifyBookMarkSlot();
										break;
									case 0x03 :
										msg = new RequestDeleteBookMarkSlot();
										break;
									case 0x04 :
										msg = new RequestTeleportBookMark();
										break;
									case 0x05 :
										msg = new RequestChangeBookMarkSlot();
										break;
									default :
										printDebugDoubleOpcode(opcode, id3, buf, state, client);
										break;
								}
								break;
							case 0x52 :
								msg = new RequestWithDrawPremiumItem();
								break;
							case 0x53 :
								msg = new RequestJump();
								break;
							case 0x54 :
								msg = new RequestStartShowCrataeCubeRank();
								break;
							case 0x55 :
								msg = new RequestStopShowCrataeCubeRank();
								break;
							case 0x56 :
								msg = new RequestNotifyStartMiniGame();
								break;
							case 0x57 :
								msg = new RequestJoinDominionWar();
								break;
							case 0x58 :
								msg = new RequestDominionInfo();
								break;
							case 0x59 :
								msg = new RequestExCleftEnter();
								break;
							case 0x5A :
								int id4 = 0;
								if (buf.remaining() >= 4)
								{
									id4 = buf.getInt();
								}
								else
								{
									if (Config.CLIENT_PACKET_HANDLER_DEBUG)
									{
										_log.warn("Client: " + client.toString() + " sent a 0xd0:0x5A without the third opcode.");
									}
									break;
								}
								switch (id4)
								{
									case 0x00 :
										msg = new RequestExBlockGameEnter();
										break;
								}
								break;
							case 0x5B :
								msg = new RequestEndScenePlayer();
								break;
							case 0x5C :
								msg = new RequestExBlockGameVote();
								break;
							case 0x5D :
								msg = new RequestListMpccWaiting();
								break;
							case 0x5E :
								msg = new RequestManageMpccRoom();
								break;
							case 0x5F :
								msg = new RequestJoinMpccRoom();
								break;
							case 0x60 :
								msg = new RequestOustFromMpccRoom();
								break;
							case 0x61 :
								msg = new RequestDismissMpccRoom();
								break;
							case 0x62 :
								msg = new RequestWithdrawMpccRoom();
								break;
							case 0x63 :
								msg = new RequestSeedPhase();
								break;
							case 0x64 :
								msg = new RequestMpccPartymasterList();
								break;
							case 0x65 :
								msg = new RequestPostItemList();
								break;
							case 0x66 :
								msg = new RequestSendPost();
								break;
							case 0x67 :
								msg = new RequestReceivedPostList();
								break;
							case 0x68 :
								msg = new RequestDeleteReceivedPost();
								break;
							case 0x69 :
								msg = new RequestReceivedPost();
								break;
							case 0x6A :
								msg = new RequestReceivePost();
								break;
							case 0x6B :
								msg = new RequestRejectPost();
								break;
							case 0x6C :
								msg = new RequestSentPostList();
								break;
							case 0x6D :
								msg = new RequestDeleteSentPost();
								break;
							case 0x6E :
								msg = new RequestSentPost();
								break;
							case 0x6F :
								msg = new RequestCancelSentPost();
								break;
							case 0x70 :
								msg = new RequestShowNewUserPetition();
								break;
							case 0x71 :
								msg = new RequestExShowStepTwo();
								break;
							case 0x72 :
								msg = new RequestExShowStepThree();
								break;
							case 0x73 :
								msg = new RequestExConnectToRaidServer();
								break;
							case 0x74 :
								msg = new RequestExReturnFromRaidServer();
								break;
							case 0x75 :
								msg = new RequestRefundItem();
								break;
							case 0x76 :
								msg = new RequestBuySellUIClose();
								break;
							case 0x77 :
								msg = new RequestEventMatchObserverEnd();
								break;
							case 0x78 :
								msg = new RequestPartyLootingModify();
								break;
							case 0x79 :
								msg = new RequestAnswerPartyLootingModify();
								break;
							case 0x7A :
								msg = new RequestAnswerCoupleAction();
								break;
							case 0x7B :
								msg = new RequestExBrEventRankerList();
								break;
							case 0x7C :
								msg = new RequestAskMemberShip();
								break;
							case 0x7D :
								msg = new RequestAddExpandQuestAlarm();
								break;
							case 0x7E :
								msg = new RequestNewVoteSystem();
								break;
							case 0x83 :
								int id5 = 0;
								if (buf.remaining() >= 4)
								{
									id5 = buf.getInt();
								}
								else
								{
									if (Config.CLIENT_PACKET_HANDLER_DEBUG)
									{
										_log.warn("Client: " + client.toString() + " sent a 0xd0:0x83 without the third opcode.");
									}
									break;
								}
								switch (id5)
								{
									case 0x01 :
										msg = new RequestExAgitInitialize();
										break;
									case 0x02 :
										msg = new RequestExAgitDetailInfo();
										break;
									case 0x03 :
										msg = new RequestExMyAgitState();
										break;
									case 0x04 :
										msg = new RequestExRegisterAgitForBidStep1();
										break;
									case 0x05 :
										msg = new RequestExRegisterAgitForBidStep3();
										break;
									case 0x07 :
										msg = new RequestExConfirmCancelRegisteringAgit();
										break;
									case 0x08 :
										msg = new RequestExProceedCancelRegisteringAgit();
										break;
									case 0x09 :
										msg = new RequestExConfirmCancelAgitLot();
										break;
									case 0x0A :
										msg = new RequestExProceedCancelAgitLot();
										break;
									case 0x0D :
										msg = new RequestExApplyForBidStep1();
										break;
									case 0x0E :
										msg = new RequestExApplyForBidStep2();
										break;
									case 0x0F :
										msg = new RequestExApplyForBidStep3();
										break;
									case 0x10 :
										if(Config.ALLOW_CUSTOM_INTERFACE)
										{
											msg = new RequestKeyPacket();
										}
										else
										{
											msg = new RequestExReBid();
										}
										break;
									case 0x11 :
										msg = new RequestExAgitListForLot();
										break;
									case 0x12 :
										msg = new RequestExApplyForAgitLotStep1();
										break;
									case 0x13 :
										msg = new RequestExApplyForAgitLotStep2();
										break;
									case 0x14 :
										msg = new RequestExAgitListForBid();
										break;
									case 0x80 :
										if (Config.ALLOW_CUSTOM_INTERFACE)
										{
											msg = new RequestDynamicPacket();
										}
										break;
								}
								break;
							case 0x84 :
								msg = new RequestExAddPostFriendForPostBox();
								break;
							case 0x85 :
								msg = new RequestExDeletePostFriendForPostBox();
								break;
							case 0x86 :
								msg = new RequestExShowPostFriendListForPostBox();
								break;
							case 0x87 :
								msg = new RequestExFriendListForPostBox();
								break;
							case 0x88 :
								msg = new RequestExOlympiadMatchListRefresh();
								break;
							case 0x89 :
								msg = new RequestBrGamePoint();
								break;
							case 0x8A :
								msg = new RequestBrProductList();
								break;
							case 0x8B :
								msg = new RequestBrProductInfo();
								break;
							case 0x8C :
								msg = new RequestBrBuyProduct();
								break;
							case 0x8D :
								msg = new RequestBrRecentProductList();
								break;
							case 0x8E :
								msg = new RequestBrMiniGameLoadScores();
								break;
							case 0x8F :
								msg = new RequestBrMiniGameInsertScore();
								break;
							case 0x90 :
								msg = new RequestBrLectureMark();
								break;
							case 0x91 :
								msg = new RequestGoodsInventoryInfo();
								break;
							case 0x92 :
								msg = new RequestUseGoodsInventoryItem();
								break;
							case 0x96 :
								msg = new RequestHardWareInfo();
								break;
							default :
								printDebugDoubleOpcode(opcode, id2, buf, state, client);
								break;
						}
						break;
					default :
						printDebug(opcode, buf, state, client);
						break;
				}
				break;
		}
		
		if (msg != null && Config.CLIENT_PACKET_HANDLER_DEBUG)
		{
			_log.info("Client packet: " + msg.getClass().getSimpleName());
		}
		return msg;
	}
	
	private void printDebug(int opcode, ByteBuffer buf, GameClientState state, GameClient client)
	{
		client.onUnknownPacket();
		if (Config.CLIENT_PACKET_HANDLER_DEBUG)
		{
			_log.warn("Unknown client packet! State: " + state.name() + ", packet ID: " + Integer.toHexString(opcode).toUpperCase());
		}
	}
	
	private void printDebugDoubleOpcode(int opcode, int id2, ByteBuffer buf, GameClientState state, GameClient client)
	{
		client.onUnknownPacket();
		if (Config.CLIENT_PACKET_HANDLER_DEBUG)
		{
			_log.warn("Unknown client packet! State: " + state.name() + ", packet ID: " + Integer.toHexString(opcode).toUpperCase() + ":" + Integer.toHexString(id2).toUpperCase());
		}
	}
	
	@Override
	public GameClient create(MMOConnection<GameClient> con)
	{
		return new GameClient(con);
	}
	
	@Override
	public void execute(Runnable r)
	{
		ThreadPoolManager.getInstance().execute(r);
	}
}
