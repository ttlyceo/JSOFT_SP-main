package l2e.gameserver.model.entity.events.tournaments.Commands;

import l2e.gameserver.Config;
import l2e.gameserver.handler.voicedcommandhandlers.IVoicedCommandHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.network.clientpackets.Say2;

/**
 * @author psygrammator
 */
public class VoiceTournament implements IVoicedCommandHandler {

    @Override
    public boolean useVoicedCommand(String command, Player player, String params) {

        if ("mytour".equalsIgnoreCase(command) || "tournamentstats".equalsIgnoreCase(command) || "ts".equalsIgnoreCase(command)) {
            showMyTournamentInfo(player);
            return true;
        }

        if (player.getLevel() < TournamentUtil.TOURNAMENT_MAIN.getMinLvl()) {
            TournamentUtil.toPlayer(player,Say2.CLAN,  "You is at level lower " + TournamentUtil.TOURNAMENT_MAIN.getMinLvl());
            return false;
        }
        if (player.getLevel() > TournamentUtil.TOURNAMENT_MAIN.getMaxLvl()) {
            TournamentUtil.toPlayer(player,Say2.CLAN,  "You is at level high " + TournamentUtil.TOURNAMENT_MAIN.getMaxLvl());
            return false;
        }

        if(player.isInPartyTournament())
        {
            TournamentUtil.toPlayer(player, Say2.CLAN, "Cannot be used on an event.");
            return false;
        }

        if ("tournamentinvite".equalsIgnoreCase(command)) {
            if (params == null || params.isEmpty()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "SYS - Usage: .tournamentinvite <playerName>");
                return false;
            }
            handleTournamentInvite(player, params);
            return true;
        }

        return false;
    }

    private void showMyTournamentInfo(Player player) {
        TournamentData.getInstance().showHtml(player, "myTour", 0);
    }

    private void handleTournamentInvite(Player player, String params) {
        if (!TournamentData.getInstance().isEnabled()) {
            TournamentUtil.toPlayer(player, Say2.CLAN, "Tournament isn't running!");
            return;
        }

        if (TournamentData.getInstance().isTournamentTeleporting()) {
            TournamentUtil.toPlayer(player, Say2.CLAN, "Tournament is teleporting players, please wait " + TournamentUtil.TOURNAMENT_MAIN.getSummonToNpc() + " seconds to invite someone.");
            return;
        }

        Player nextMember = null;

        if (!params.isEmpty()) {
            nextMember = GameObjectsStorage.getPlayer(params);

            if (nextMember == null) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player " + params + " doesn't exist or is not online!");
                return;
            }

            if (nextMember.isGM()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player " + params + " doesn't exist or is not online!");
                return;
            }

            if (!nextMember.isOnline()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player " + params + " is currently offline. You can't invite offline players.");
                return;
            }

            if (nextMember == player) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "You can't invite yourself!");
                return;
            }

            if (nextMember.isInOfflineMode()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player is in offline shop!");
                return;
            }

            if (nextMember.getLevel() < TournamentUtil.TOURNAMENT_MAIN.getMinLvl()) {
                TournamentUtil.toPlayer(player,Say2.CLAN,  "Player is at level lower " + TournamentUtil.TOURNAMENT_MAIN.getMinLvl());
                return;
            }
            if (nextMember.getLevel() > TournamentUtil.TOURNAMENT_MAIN.getMaxLvl()) {
                TournamentUtil.toPlayer(player,Say2.CLAN,  "Player is at level high " + TournamentUtil.TOURNAMENT_MAIN.getMaxLvl());
                return;
            }

            if (nextMember.isRegisteredInFightEvent()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player is in Fight Event!");
                return;
            }

            if (nextMember.isInTournamentTeam()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "This player is already in a Tournament Team.");
                return;
            }

            if (nextMember.isDead() || nextMember.isAlikeDead()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player is in dead.");
                return;
            }

            if (nextMember.isInParty()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "You can't invite players in a party. Don't worry, a party will be automatically created!");
                return;
            }
            if (nextMember.isInOlympiadMode() || nextMember.isOlympiadStart()) {
                TournamentUtil.toPlayer(player, Say2.CLAN, "Player is in Olympiad mode!");
                return;
            }

            TournamentTeam team = player.getTournamentTeam();
            if (team != null && team.getLeader() != player) {
                String leaderName = team.getLeader().getName(null);
                TournamentUtil.toPlayer(player, Say2.CLAN, "Only the leader [ " + leaderName + " ] can invite players.");
                return;
            }
        }
        if(nextMember == null)
            return;

        TournamentData.getInstance().askJoinTeam(player, nextMember);
    }

    @Override
    public String[] getVoicedCommandList() {
        return new String[]{
                "mytour",
                "tournamentinvite",
                "tournamentstats",
                "ts",
        };
    }
}