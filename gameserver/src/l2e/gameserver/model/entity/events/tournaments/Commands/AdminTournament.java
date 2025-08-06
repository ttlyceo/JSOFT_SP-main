package l2e.gameserver.model.entity.events.tournaments.Commands;

import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;

/**
 * @author psygrammator
 */
public class AdminTournament implements IAdminCommandHandler
{
    @Override
    public boolean useAdminCommand(String command, Player player)
    {
        if (command.equalsIgnoreCase("admin_tour") || command.equalsIgnoreCase("admin_tournament"))
        {
            TournamentData.getInstance().onAction();
        }
        return true;
    }

    @Override
    public String[] getAdminCommandList()
    {
        return new String[]
        {
                "admin_tour", "admin_tournament"
        };
    }

}