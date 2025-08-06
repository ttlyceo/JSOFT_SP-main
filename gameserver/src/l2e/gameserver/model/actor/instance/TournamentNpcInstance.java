package l2e.gameserver.model.actor.instance;

import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.enums.RegisterType;

/**
 * @author psygrammator
 */
public class TournamentNpcInstance extends Npc
{
    public TournamentNpcInstance(final int objectId, final NpcTemplate template)
    {
        super(objectId, template);
    }

    @Override
    public void showChatWindow(Player player)
    {
        TournamentData.getInstance().showHtml(player, "main", 0);
    }

    @Override
    public String getHtmlPath(final int npcId, final int val)
    {
        String pom = "";
        if (val == 0)
        {
            pom = "" + npcId;
        }
        else
        {
            pom = npcId + "-" + val;
        }
        return "data/html/mods/tournament/" + pom + ".htm";
    }

    @Override
    public boolean isInvul() {
        return true;
    }
}