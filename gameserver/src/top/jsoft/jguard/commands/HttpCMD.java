package top.jsoft.jguard.commands;

import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.actor.Player;
import top.jsoft.jguard.network.HttpPacket;


public class HttpCMD implements IVoicedCommandHandler
{
	private final static String[] VOICED_COMMANDS = {"url_","url"};

    private static HttpCMD _instance;
	public static HttpCMD getInstance() {
        if (_instance == null) {
            _instance = new HttpCMD();
        }
        return _instance;
    }

    @Override
    public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (player == null)
			return false;
		
		if(command.startsWith(VOICED_COMMANDS[0]) && command.split("_")[0] != null && command.split("_")[1] != null)
		{
			target = command.split("_")[1];
			command = command.split("_")[0];
		}
		else
		if(command.split(" ")[0] != null && command.split(" ")[1] != null)
		{
			target = command.split(" ")[1];
			command = command.split(" ")[0];
		}
		else
		{
			return false;
		}

        if(command.equalsIgnoreCase("url") && !target.equalsIgnoreCase("") && target.length()<150 )
		{
        	String url = target;
        	player.sendMessage("Open: "+url);
        	player.sendPacket(new HttpPacket(url,true));
			return true;
		}
        else
        {
        	player.sendMessage("Usage: .url <url>");
        	//player.sendMessage("test "+target);
        }
       	return false;
	}
	
    @Override
    public String[] getVoicedCommandList()
    {
        return VOICED_COMMANDS;
    }
	
	public String getDescription(String command)
	{
		if(command.equals("url"))
			return "Open link .url <url>";
		return null;
	}
	
	
}