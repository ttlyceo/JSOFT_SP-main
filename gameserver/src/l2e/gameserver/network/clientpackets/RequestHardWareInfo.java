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
package l2e.gameserver.network.clientpackets;

import l2e.gameserver.model.actor.Player;

public class RequestHardWareInfo extends GameClientPacket
{
	private String mac;
	private String cpu;
	private String vgaName;
	private String driverVersion;
	private int windowsPlatformId;
	private int windowsMajorVersion;
	private int windowsMinorVersion;
	private int windowsBuildNumber;
	private int DXVersion;
	private int DXRevision;
	private int cpuSpeed;
	private int cpuCoreCount;
	private int unk8;
	private int unk9;
	private int PhysMemory1;
	private int PhysMemory2;
	private int unk12;
	private int videoMemory;
	private int unk14;
	private int vgaVersion;
	
	@Override
	protected void readImpl()
	{
		mac = readS();
		windowsPlatformId = readD();
		windowsMajorVersion = readD();
		windowsMinorVersion = readD();
		windowsBuildNumber = readD();
		DXVersion = readD();
		DXRevision = readD();
		cpu = readS();
		cpuSpeed = readD();
		cpuCoreCount = readD();
		unk8 = readD();
		unk9 = readD();
		PhysMemory1 = readD();
		PhysMemory2 = readD();
		unk12 = readD();
		videoMemory = readD();
		unk14 = readD();
		vgaVersion = readD();
		vgaName = readS();
		driverVersion = readS();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getActiveChar();
		if (player == null)
		{
			return;
		}
		
		final Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
		{
			return;
		}
		
		if (activeChar.isGM())
		{
			_log.info("Mac: {" + mac + "} WPI: {" + windowsPlatformId + "} WMV1: {" + windowsMajorVersion + "} WMV2: {" + windowsMinorVersion + "} WBN: {" + windowsBuildNumber + "} DXV: {" + DXVersion + "} DXR: {" + DXRevision + "} CPU: {" + cpu + "} CPUS: {" + cpuSpeed + "} CPUCC: {" + cpuCoreCount + "} PM1: {" + PhysMemory1 + "} PM2: {" + PhysMemory2 + "} VM: {" + videoMemory + "} VGAV: {" + vgaVersion + "} VGAN: {" + vgaName + "} DV: {" + driverVersion + "}");
			_log.info("UNK8: {" + unk8 + "} UNK9: {" + unk9 + "} UNK12: {" + unk12 + "} UNK14: {" + unk14 + "}");
		}
	}
}