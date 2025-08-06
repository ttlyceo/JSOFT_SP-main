package top.jsoft.jguard.network;


import l2e.gameserver.network.serverpackets.GameServerPacket;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class HttpPacket extends GameServerPacket implements IGlobalPacket
{
    private final char Subcode = 0x03;
    private char tray = 0x00;
    private String http = "https://l2wos.com/";

    public HttpPacket(String http, char tray)
    {
        this.http = http;
        this.tray = tray;
    }

    public HttpPacket(String http, boolean tray)
    {
        this.http = http;
        if(tray)
        {
            this.tray = 0x01;
        }
        else
        {
            this.tray = 0x00;
        }
    }

    public HttpPacket(String http)
    {
        this.http = http;
        //this.tray = 0x00;
    }

    @Override
    protected void writeImpl() {
        writeC(Opcode);
        writeC(Subcode); // 0xFF03
        //writeC(tray);
        writeS(http);
    }
}
