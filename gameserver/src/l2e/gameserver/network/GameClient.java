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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.nio.impl.MMOClient;
import org.nio.impl.MMOConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.strixplatform.StrixPlatform;
import org.strixplatform.network.cipher.StrixGameCrypt;
import org.strixplatform.utils.StrixClientData;

import l2e.commons.dbutils.DbUtils;
import l2e.commons.util.FloodProtectorAction;
import l2e.commons.util.FloodProtectorConfig;
import l2e.commons.util.Util;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.data.holder.ClanHolder;
import l2e.gameserver.data.holder.SecPasswordHolder;
import l2e.gameserver.database.DatabaseFactory;
import l2e.gameserver.model.CharSelectInfoPackage;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.PcCondOverride;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.communication.AuthServerCommunication;
import l2e.gameserver.network.communication.SessionKey;
import l2e.gameserver.network.communication.gameserverpackets.PlayerLogout;
import l2e.gameserver.network.serverpackets.GameServerPacket;
import smartguard.integration.SmartClient;
import top.jsoft.jguard.JGuard;
import top.jsoft.jguard.crypt.JGameCrypt;

public final class GameClient extends MMOClient<MMOConnection<GameClient>>
{
	protected static final Logger _log = LoggerFactory.getLogger(GameClient.class);

	public static enum GameClientState
	{
		CONNECTED, AUTHED, ENTERING, IN_GAME
	}

	private GameClientState _state;
	
	private final InetAddress _addr;
	private SessionKey _sessionId;
	private Player _activeChar;
	private final ReentrantLock _activeCharLock = new ReentrantLock();
	private SecPasswordHolder _secondaryAuth;

	private boolean _isAuthedGG;
	private final long _connectionStartTime;
	private CharSelectInfoPackage[] _charSlotMapping = null;

	private final Map<String, FloodProtectorAction> _floodProtectors = new HashMap<>();

	public StrixGameCrypt _gameCrypt = null;
	private StrixClientData _clientData;
	private String _playerName = "";
	private int _playerId = 0;
	private int _revision = 0;
	private int _playerID;
	private String _login, _lockedHwid, _lockedIp;
	private LockType _lockType;
	private int _lang = 0;
	
	public static enum LockType
	{
		PLAYER_LOCK, ACCOUNT_LOCK, NONE
	}

	private boolean _isDetached = false;
	private int _failedPackets = 0;
	private String _realIpAddress;
	private String _hwid;
	private boolean _isCharCreation = false;
	private int _requestTd = Config.REQUEST_ID;
	
	public GameClient(MMOConnection<GameClient> con)
	{
		super(con);
		_state = GameClientState.CONNECTED;
		_connectionStartTime = System.currentTimeMillis();

		_gameCrypt = JGuard.isProtectEnabled() ? new JGameCrypt() : new StrixGameCrypt();
		
		try
		{
			_addr = con != null ? con.getSocket().getInetAddress() : InetAddress.getLocalHost();
		}
		catch (final UnknownHostException e)
		{
			throw new Error("Unable to determine localhost address.");
		}
		
		for (final FloodProtectorConfig config : Config.FLOOD_PROTECTORS)
		{
			_floodProtectors.put(config.FLOOD_PROTECTOR_TYPE, new FloodProtectorAction(this, config));
		}
	}

	public byte[] enableCrypt()
	{
		final byte[] key = BlowFishKeygen.getRandomKey();
		_gameCrypt.setKey(key);
		return key;
	}

	public GameClientState getState()
	{
		return _state;
	}

	public void setState(GameClientState state)
	{
		_state = state;
	}

	public InetAddress getConnectionAddress()
	{
		return _addr;
	}

	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}

	@Override
	public boolean decrypt(ByteBuffer buf, int size)
	{
		_gameCrypt.decrypt(buf.array(), buf.position(), size);
		return true;
	}

	@Override
	public boolean encrypt(final ByteBuffer buf, final int size)
	{
		_gameCrypt.encrypt(buf.array(), buf.position(), size);
		buf.position(buf.position() + size);
		return true;
	}

	public Player getActiveChar()
	{
		return _activeChar;
	}

	public void setActiveChar(Player pActiveChar)
	{
		_activeChar = pActiveChar;
	}

	public ReentrantLock getActiveCharLock()
	{
		return _activeCharLock;
	}

	public void setGameGuardOk(boolean val)
	{
		_isAuthedGG = val;
	}

	public boolean isAuthedGG()
	{
		return _isAuthedGG;
	}

	public void setSessionId(SessionKey sk)
	{
		_sessionId = sk;
	}

	public SessionKey getSessionId()
	{
		return _sessionId;
	}

	public void sendPacket(GameServerPacket gsp)
	{
		if (_isDetached || (gsp == null))
		{
			return;
		}

		if (gsp.isInvisible() && (getActiveChar() != null) && !getActiveChar().canOverrideCond(PcCondOverride.SEE_ALL_PLAYERS))
		{
			return;
		}
		
		if (isConnected())
		{
			getConnection().sendPacket(gsp);
		}
	}

	public boolean isDetached()
	{
		return _isDetached;
	}

	public void setDetached(boolean b)
	{
		_isDetached = b;
	}

	public byte markToDeleteChar(int charslot)
	{
		final int objid = getObjectIdForSlot(charslot);

		if (objid < 0)
		{
			return -1;
		}

		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("SELECT clanId FROM characters WHERE charId=?");
			statement.setInt(1, objid);
			byte answer = 0;
			rset = statement.executeQuery();
			final int clanId = rset.next() ? rset.getInt(1) : 0;
			if (clanId != 0)
			{
				final Clan clan = ClanHolder.getInstance().getClan(clanId);
				
				if (clan == null)
				{
					answer = 0;
				}
				else if (clan.getLeaderId() == objid)
				{
					answer = 2;
				}
				else
				{
					answer = 1;
				}
			}
			
			statement.close();
			if (answer == 0)
			{
				if (Config.DELETE_DAYS == 0)
				{
					deleteCharByObjId(objid);
				}
				else
				{
					statement = con.prepareStatement("UPDATE characters SET deletetime=? WHERE charId=?");
					statement.setLong(1, System.currentTimeMillis() + (Config.DELETE_DAYS * 86400000L));
					statement.setInt(2, objid);
					statement.execute();
				}
			}
			return answer;
		}
		catch (final Exception e)
		{
			_log.warn("Error updating delete time of character.", e);
			return -1;
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
	}

	public void markRestoredChar(int charslot)
	{
		final int objid = getObjectIdForSlot(charslot);
		if (objid < 0)
		{
			return;
		}

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("UPDATE characters SET deletetime=0 WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
		}
		catch (final Exception e)
		{
			_log.warn("Error restoring character.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public static void deleteCharByObjId(int objid)
	{
		if (objid < 0)
		{
			return;
		}

		CharNameHolder.getInstance().removeName(objid);

		Connection con = null;
		PreparedStatement statement = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement("DELETE FROM character_contacts WHERE charId=? OR contactId=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_friends WHERE charId=? OR friendId=?");
			statement.setInt(1, objid);
			statement.setInt(2, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_hennas WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_macroses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_quests WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_quest_global_data WHERE charId=?");
			statement.setInt(1, objid);
			statement.executeUpdate();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_recipebook WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_skills_save WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_subclasses WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM heroes WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM seven_signs WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM pets WHERE item_obj_id IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM item_attributes WHERE itemId IN (SELECT object_id FROM items WHERE items.owner_id=?)");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM items WHERE owner_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM raidboss_points WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_instance_time WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM character_variables WHERE obj_id=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			statement = con.prepareStatement("DELETE FROM characters WHERE charId=?");
			statement.setInt(1, objid);
			statement.execute();
			statement.close();

			if (Config.ALLOW_WEDDING)
			{
				statement = con.prepareStatement("DELETE FROM mods_wedding WHERE player1Id = ? OR player2Id = ?");
				statement.setInt(1, objid);
				statement.setInt(2, objid);
				statement.execute();
			}
		}
		catch (final Exception e)
		{
			_log.warn("Error deleting character.", e);
		}
		finally
		{
			DbUtils.closeQuietly(con, statement);
		}
	}

	public Player loadCharFromDisk(int charslot)
	{
		final int objId = getObjectIdForSlot(charslot);
		if (objId < 0)
		{
			return null;
		}

		Player character = null;
		final Player oldPlayer = GameObjectsStorage.getPlayer(objId);
		if (oldPlayer != null)
		{
			if (oldPlayer.isInOfflineMode() || oldPlayer.isLogoutStarted())
			{
				oldPlayer.kick();
			}
			else
			{
				oldPlayer.sendPacket(SystemMessageId.ANOTHER_LOGIN_WITH_ACCOUNT);
				final GameClient oldClient = oldPlayer.getClient();
				if (oldClient != null)
				{
					oldClient.setActiveChar(null);
					oldClient.closeNow(false);
				}
				oldPlayer.setClient(this);
				oldPlayer.getPersonalTasks().removeTask(11, true);
				character = oldPlayer;
			}
		}
		
		if (character == null)
		{
			character = Player.load(objId);
		}
		
		if (character != null)
		{
			setActiveChar(character);
			character.setRunning();
			character.standUp();
			character.refreshOverloaded();
			character.refreshExpertisePenalty();
			character.setOnlineStatus(true, false);
		}
		else
		{
			_log.error("could not restore in slot: " + charslot);
		}
		return character;
	}

	public void setCharSelection(CharSelectInfoPackage[] chars)
	{
		_charSlotMapping = chars;
	}

	public CharSelectInfoPackage getCharSelection(int charslot)
	{
		if ((_charSlotMapping == null) || (charslot < 0) || (charslot >= _charSlotMapping.length))
		{
			return null;
		}
		return _charSlotMapping[charslot];
	}
	
	public int getSlotForObjectId(int objectId)
	{
		for (int slotIdx = 0; slotIdx < _charSlotMapping.length; slotIdx++)
		{
			final CharSelectInfoPackage p = _charSlotMapping[slotIdx];
			if (p != null && p.getObjectId() == objectId)
			{
				return slotIdx;
			}
		}
		return -1;
	}

	public SecPasswordHolder getSecondaryAuth()
	{
		return _secondaryAuth;
	}

	public void close(GameServerPacket gsp)
	{
		final var con = getConnection();
		if (con != null)
		{
			con.close(gsp);
		}
	}

	private int getObjectIdForSlot(int charslot)
	{
		final CharSelectInfoPackage info = getCharSelection(charslot);
		if (info == null)
		{
			_log.warn(toString() + " tried to delete Character in slot " + charslot + " but no characters exits at that slot.");
			return -1;
		}
		return info.getObjectId();
	}

	@Override
	protected void onDisconnection()
	{
		final var player = getActiveChar();
		setActiveChar(null);
		if (player != null)
		{
			player.setClient(null);
			player.scheduleDelete();
		}
		
		if (getSessionId() != null)
		{
			if (isAuthed())
			{
				AuthServerCommunication.getInstance().removeAuthedClient(getLogin());
				AuthServerCommunication.getInstance().sendPacket(new PlayerLogout(getLogin()));
			}
			else
			{
				AuthServerCommunication.getInstance().removeWaitingClient(getLogin());
			}
		}
	}

	@Override
	public String toString()
	{
		try
		{
			final InetAddress address = getConnection().getSocket().getInetAddress();
			switch (getState())
			{
				case CONNECTED :
					return "[IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				case AUTHED :
					return "[Account: " + getLogin() + " - IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				case ENTERING :
				case IN_GAME :
					return "[Character: " + (getActiveChar() == null ? "disconnected" : getActiveChar().getName(null) + "[" + getActiveChar().getObjectId() + "]") + " - Account: " + getLogin() + " - IP: " + (address == null ? "disconnected" : address.getHostAddress()) + "]";
				default :
					throw new IllegalStateException("Missing state on switch");
			}
		}
		catch (final NullPointerException e)
		{
			return "[Character read failed due to disconnect]";
		}
	}

	public boolean handleCheat(String punishment)
	{
		if (_activeChar != null)
		{
			Util.handleIllegalPlayerAction(_activeChar, toString() + ": " + punishment);
			return true;
		}
		closeNow(false);
		return false;
	}

	public void onBufferUnderflow()
	{
		if (_failedPackets++ >= 10)
		{
			if (_state == GameClientState.CONNECTED)
			{
				if (Config.CLIENT_PACKET_HANDLER_DEBUG)
				{
					_log.error("Client " + toString() + " - Disconnected, too many buffer underflows in non-authed state.");
				}
				closeNow(true);
			}
		}
	}

	public void onUnknownPacket()
	{
		if (_state == GameClientState.CONNECTED)
		{
			if (Config.CLIENT_PACKET_HANDLER_DEBUG)
			{
				_log.error("Client " + toString() + " - Disconnected, too many unknown packets in non-authed state.");
			}
			closeNow(true);
		}
	}

	public int getRevision()
	{
		return _revision;
	}

	public void setRevision(int revision)
	{
		_revision = revision;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public void setPlayerId(int plId)
	{
		_playerId = plId;
	}

	public final String getPlayerName()
	{
		return _playerName;
	}

	public void setPlayerName(final String name)
	{
		_playerName = name;
	}

	public LockType getLockType()
	{
		return _lockType;
	}

	public void setLockType(LockType lockType)
	{
		_lockType = lockType;
	}

	public String getLogin()
	{
		return _login;
	}

	public void setLogin(String login)
	{
		_login = login;
		if (Config.SECOND_AUTH_ENABLED)
		{
			_secondaryAuth = new SecPasswordHolder(this);
		}
	}

	public int getPlayerID()
	{
		return _playerID;
	}

	public void setPlayerID(int playerID)
	{
		_playerID = playerID;
	}
	
	public void setStrixClientData(final StrixClientData clientData)
	{
		_clientData = clientData;
	}
	
	public StrixClientData getStrixClientData()
	{
		return _clientData;
	}
	
	public void updateHWID()
	{
		switch (Config.PROTECTION)
		{
			case "STRIX" :
				if (StrixPlatform.getInstance().isPlatformEnabled() && getStrixClientData() != null)
				{
					_hwid = getStrixClientData().getClientHWID();
				}
				break;
			case "SMART" :
				_hwid = SmartClient.getHwid(this);
				break;
			case "SGUARD" :
			case "ANTICHEAT" :
			case "JGUARD" :
				break;
			case "NONE" :
				_hwid = "N/A";
				break;
			default :
				_hwid = "N/A";
				break;
		}
	}
	
	public void setHWID(String hwid)
	{
		_hwid = hwid;
	}
	
	public String getHWID()
	{
		return _hwid;
	}
	
	public String getIPAddress()
	{
		return getRealIpAddress() != null && !getRealIpAddress().isEmpty() ? getRealIpAddress() : getIpAddr();
	}
	
	public String getDefaultAddress()
	{
		final var defaultAddress = getConnectionAddress().getHostAddress();
		return !defaultAddress.equals(getIPAddress()) ? defaultAddress : "";
	}
	
	public String getRealIpAddress()
	{
		return _realIpAddress;
	}
	
	public void setRealIpAddress(String realIpAddress)
	{
		_realIpAddress = realIpAddress;
	}
	
	public int getRequestId()
	{
		return _requestTd;
	}
	
	public void setRequestId(int requestTd)
	{
		if (requestTd <= 0)
		{
			return;
		}
		_requestTd = requestTd;
	}
	
	public boolean checkFloodProtection(String type, String command)
	{
		final FloodProtectorAction floodProtector = _floodProtectors.get(type.toUpperCase());
		return floodProtector == null || floodProtector.tryPerformAction(command);
	}
	
	public void setCharCreation(boolean isCharCreation)
	{
		_isCharCreation = isCharCreation;
	}
	
	public boolean isCharCreation()
	{
		return _isCharCreation;
	}
	
	public void setLang(int val)
	{
		_lang = val;
	}
	
	public int getLang()
	{
		return _lang;
	}
	
	public void setLockedHwid(String hwid)
	{
		_lockedHwid = hwid;
	}
	
	public String getLockedHwid()
	{
		return _lockedHwid;
	}
	
	public void setLockedIp(String ip)
	{
		_lockedIp = ip;
	}
	
	public String getLockedIp()
	{
		return _lockedIp;
	}
}