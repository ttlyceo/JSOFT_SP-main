package top.jsoft.jguard.network;


import l2e.gameserver.network.serverpackets.GameServerPacket;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class DataPacket extends GameServerPacket implements IGlobalPacket
{
    private final char Subcode = 0x01;
    private String data = "";

    public DataPacket(String data)
    {
        this.data = data;
    }

    @Override
    protected void writeImpl() {
        writeC(Opcode);
        writeC(Subcode); // 0xFF01
        writeS(data);
    }
}
