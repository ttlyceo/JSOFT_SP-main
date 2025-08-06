package l2e.gameserver.model.entity.events.tournaments.Commands;

import l2e.gameserver.data.holder.CharNameHolder;
import l2e.gameserver.handler.bypasshandlers.IBypassHandler;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.actor.Creature;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.enums.RegisterType;
import l2e.gameserver.model.entity.events.tournaments.model.TournamentTeam;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.model.olympiad.OlympiadManager;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;

import java.util.Objects;
import java.util.StringTokenizer;

/**
 * @author psygrammator
 */
public class TournamentBypasses implements IBypassHandler
{

    @Override
    public boolean useBypass(String bypass, Player player, Creature target)
    {
        StringTokenizer st = new StringTokenizer(bypass, " ");
        st.nextToken();
        TournamentTeam team = player.getTournamentTeam();

        if (bypass.startsWith("bp_showTournamentPage"))
        {
            String page = st.hasMoreTokens() ? st.nextToken() : "main";
            TournamentData.getInstance().showHtml(player, page, TournamentUtil.getIntNameType(page), st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0);
            return true;
        }
        else if (bypass.startsWith("bp_checkTournamentPlayer"))
        {
            String playerName = st.nextToken();
            int type = TournamentUtil.getIntNameType(st.nextToken());
            int targetObjectId = CharNameHolder.getInstance().getIdByName(playerName);
            TournamentData.getInstance().showPlayerRankingData(player, targetObjectId, type);
            return true;
        }
        else if (bypass.startsWith("bp_tournamentRanking"))
        {
            int type = TournamentUtil.getIntNameType(st.nextToken());
            String rankType = st.nextToken();
            TournamentData.getInstance().showRanking(player, type, rankType);
            return true;
        }

        if(!player.isInPartyTournament())
        {
            if (bypass.startsWith("bp_tournamentTeamInfo"))
            {
                if (team != null)
                {
                    TournamentData.getInstance().showHtml(player, "createTeam", team.size());
                }
                else
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("First you must create a new Tournament Team.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "First you must create a new Tournament Team.");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                }
                return true;
            }
            if (bypass.startsWith("bp_leaveTournamentTeam"))
            {
                if(Objects.nonNull(player.getPartyTournament()))
                {
                    TournamentUtil.toPlayer(player, Say2.CLAN, "You cant't leave team now.");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                if (team != null)
                {
                    team.removeMember(player);
                }
                else
                {
                    TournamentUtil.toPlayer(player, Say2.CLAN, "You haven't a Team.");
                }
                TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                return true;
            }
            if (bypass.startsWith("bp_observerTournament"))
            {
                if (!TournamentData.getInstance().isEnabled())
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament isn't Running!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                boolean checkNpc = World.getInstance().getAroundNpc(player, 500, 200).stream().anyMatch(n -> n.getId() == TournamentUtil.TOURNAMENT_MAIN.getNpcId());
                if(!checkNpc)
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Can't use now!", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Can't use now!");
                    return true;
                }

                final Tournament tournament = TournamentData.getInstance().getTournament(Integer.parseInt(bypass.split(" ")[1]));
                if (Objects.isNull(tournament) || (!tournament.isPrepare() && !tournament.isRunning())) {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("This match is not active.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "This match is not active!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), Objects.nonNull(tournament) ? tournament.getType() : 0);
                    return true;
                }

                if (!TournamentUtil.TOURNAMENT_EVENTS.get(tournament.getType()).isObserver()) {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Currently observation is disabled.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Currently observation is disabled!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), tournament.getType());
                    return true;
                }

                if (player.isInTournamentTeam() && TournamentData.getInstance().isInLobby(player.getTournamentTeam())) {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("You cannot observe while participate in tournament.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "You cannot observe while participate in tournament!");
                    TournamentData.getInstance().showHtml(player, "observer/" + TournamentUtil.getStringNameType(tournament.getType()), tournament.getType());
                    return true;
                }

                TournamentUtil.addToList(player, tournament.getObservers(), s -> s.enterTournamentObserverMode(tournament));
                return true;
            }
            if (bypass.startsWith("bp_registerTournament"))
            {
                if (!TournamentData.getInstance().isEnabled())
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament isn't Running!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                boolean checkNpc = World.getInstance().getAroundNpc(player, 500, 200).stream().anyMatch(n -> n.getId() == TournamentUtil.TOURNAMENT_MAIN.getNpcId());
                if(!checkNpc)
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Can't use now!", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Can't use now!");
                    return true;
                }

                int type = TournamentUtil.getIntNameType(st.nextToken());
                if(TournamentUtil.TOURNAMENT_TYPES.stream().noneMatch(id -> id == type))
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament type wrong!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                RegisterType registerType = TournamentUtil.TOURNAMENT_EVENTS.get(type).getRegisterType();
                if(registerType == null)
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament registerType wrong!");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                if(registerType == RegisterType.TEAM)
                {
                    if(type == 1) //extra check 1x1
                    {
                        if (OlympiadManager.getInstance().isRegistered(player))
                        {
                            TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in Olympiad.", 3000));
                            TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in Olympiad.");
                            TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                            return true;
                        }

                        if (player.isRegisteredInFightEvent())
                        {
                            TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in another Fight Event.", 3000));
                            TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in another Fight Event.");
                            TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                            return true;
                        }

                        if (!player.isInTournamentTeam())
                        {
                            team = new TournamentTeam(player, null);
                        }
                    }

                    if(team == null)
                    {
                        TournamentUtil.toPlayer(player, Say2.CLAN, "Can't do it without a team!");
                        TournamentData.getInstance().showHtml(player, "fights/" + TournamentUtil.getStringNameType(type), type);
                        return true;
                    }

                    if(!TournamentData.getInstance().addToLobby(player, team, type, registerType))
                    {
                        TournamentData.getInstance().showHtml(player, "fights/" + TournamentUtil.getStringNameType(type), type);
                        return true;
                    }

                    TournamentData.getInstance().showHtml(player, "fights/" + TournamentUtil.getStringNameType(type), type);
                }
                else if(registerType == RegisterType.SOLO)
                {
                    if (OlympiadManager.getInstance().isRegistered(player))
                    {
                        TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in Olympiad.", 3000));
                        TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in Olympiad.");
                        TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                        return true;
                    }

                    if (player.isRegisteredInFightEvent())
                    {
                        TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in another Fight Event.", 3000));
                        TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in another Fight Event.");
                        TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                        return true;
                    }

                    if(!TournamentData.getInstance().addToLobby(player, null, type, registerType))
                    {
                        TournamentData.getInstance().showHtml(player, "fights/" + TournamentUtil.getStringNameType(type), type);
                        return true;
                    }
                }
                return true;
            }
            if (bypass.startsWith("bp_deleteTournamentTeam"))
            {
                boolean checkNpc = World.getInstance().getAroundNpc(player, 500, 200).stream().anyMatch(n -> n.getId() == TournamentUtil.TOURNAMENT_MAIN.getNpcId());
                if(!checkNpc)
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Can't use now!", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Can't use now!");
                    return true;
                }

                if (OlympiadManager.getInstance().isRegistered(player))
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in Olympiad.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in Olympiad.");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                if (player.isRegisteredInFightEvent())
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in another Fight Event.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in another Fight Event.");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                if (player.isInPartyTournament())
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage(player.getName(null) + " is already participant in Tournament Event.", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, player.getName(null) + " is already participant in another Tournament Event.");
                    TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                    return true;
                }

                if (team != null)
                {
                    team.disbandTeam();
                }
                else
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "You haven't a Tournament Team.");
                }
                TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                return true;
            }
            if (bypass.startsWith("bp_inviteTournamentMember"))
            {
                TournamentData tManager = TournamentData.getInstance();
                if (!tManager.isEnabled()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament isn't Running!");
                    return false;
                }
                if (tManager.isTournamentTeleporting()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament is teleporting players, wait " + TournamentUtil.TOURNAMENT_MAIN.getSummonToNpc() + " seconds to invite someone.");
                    return false;
                }
                if (!st.hasMoreTokens()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Invalid command format. Player name missing.");
                    return false;
                }
                String nextMemberName = st.nextToken();
                Player nextMember = GameObjectsStorage.getPlayer(nextMemberName);
                if (nextMember == null || nextMember.isGM()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Player " + nextMemberName + " doesn't exist or is not online!");
                    return false;
                }
                if (nextMember == player) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "You can't invite yourself!");
                    return false;
                }
                if (nextMember.isInOfflineMode()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Player is in offline shop!");
                    return false;
                }
                if (nextMember.isInTournamentTeam() || nextMember.isInPartyTournament()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "This player is already in a Tournament Team.");
                    return false;
                }
                if (nextMember.isInOlympiadMode() || nextMember.isOlympiadStart()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Player is in Olympiad mode!");
                    return false;
                }
                if (!nextMember.isOnline()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Player is currently offline. You can't invite offline players.");
                    return false;
                }
                if (nextMember.getLevel() < TournamentUtil.TOURNAMENT_MAIN.getMinLvl()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN,  "Player is at level lower " + TournamentUtil.TOURNAMENT_MAIN.getMinLvl());
                    return false;
                }
                if (nextMember.getLevel() > TournamentUtil.TOURNAMENT_MAIN.getMaxLvl()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN,  "Player is at level high " + TournamentUtil.TOURNAMENT_MAIN.getMaxLvl());
                    return false;
                }
                if (nextMember.isRegisteredInFightEvent()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN,  "Player is in Fight Event!");
                    return false;
                }
                if (nextMember.isInParty()) {
                    TournamentUtil.toPlayer(player,Say2.CLAN,  "You can't invite players in a party. Don't worry, a party will be automatically created!");
                    return false;
                }
                team = player.getTournamentTeam();
                if (team != null && team.getLeader() != player) {
                    String leaderName = team.getLeader().getName(null);
                    TournamentUtil.toPlayer(player,Say2.CLAN,  "Only the leader [ " + leaderName + " ] can invite players.");
                    return false;
                }
                tManager.askJoinTeam(player, nextMember);
                return true;
            }
            if (bypass.startsWith("bp_removeTournamentParticipation"))
            {
                if (!TournamentData.getInstance().isEnabled())
                {
                    TournamentUtil.toPlayer(player, Say2.CLAN,  "Tournament isn't Running!");
                    return false;
                }
                boolean checkNpc = World.getInstance().getAroundNpc(player, 500, 200).stream().anyMatch(n -> n.getId() == TournamentUtil.TOURNAMENT_MAIN.getNpcId());
                if(!checkNpc)
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Can't use now!", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Can't use now!");
                    return true;
                }
                if (team != null)
                {
                    if (TournamentData.getInstance().isInLobby(team))
                    {
                        TournamentData.getInstance().removeFromLobby(team);
                    }
                    else
                    {
                        TournamentUtil.toPlayer(player,Say2.CLAN,  "Your team isn't registered.");
                        TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
                        return false;
                    }
                }
                else
                {
                    if (TournamentData.getInstance().isInLobbySolo(player))
                    {
                        TournamentData.getInstance().removeFromLobbySolo(player);
                    }
                    else
                        TournamentUtil.toPlayer(player,Say2.CLAN,  "You haven't a Tournament Team.");
                }
                TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);

            }
            if (bypass.startsWith("bp_createTournamentTeam"))
            {
                if (!TournamentData.getInstance().isEnabled())
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "Tournament isn't Running!");
                    return false;
                }
                boolean checkNpc = World.getInstance().getAroundNpc(player, 500, 200).stream().anyMatch(n -> n.getId() == TournamentUtil.TOURNAMENT_MAIN.getNpcId());
                if(!checkNpc)
                {
                    TournamentUtil.toPlayer(player, new ExShowScreenMessage("Can't use now!", 3000));
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Can't use now!");
                    return true;
                }
                if (player.getTournamentTeam() != null)
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "You can't create a new Tournament Team.");
                    return false;
                }
                if (player.isInPartyTournament())
                {
                    TournamentUtil.toPlayer(player,Say2.CLAN, "You can't create a team in Tournament.");
                    return false;
                }
                if (player.isRegisteredInFightEvent()) {
                    TournamentUtil.toPlayer(player, Say2.CLAN,  "Player is in Fight Event!");
                    return false;
                }
                if (team == null)
                {
                    team = new TournamentTeam(player, null);
                }
                else
                {
                    TournamentUtil.toPlayer(player, Say2.CLAN, "Your Tournament Team has been already created, try to invite someone.");
                    return false;
                }
                TournamentData.getInstance().showHtml(player, player.getTournamentStats().getLastPage(), 0);
            }
        }
        return true;
    }

    @Override
    public String[] getBypassList()
    {
        return new String[]
        {
            "bp_checkTournamentPlayer",
            "bp_showTournamentPage",
            "bp_registerTournament",
            "bp_removeTournamentParticipation",
            "bp_createTournamentTeam",
            "bp_inviteTournamentMember",
            "bp_deleteTournamentTeam",
            "bp_tournamentTeamInfo",
            "bp_inviteTournamentPage",
            "bp_tournamentRanking",
            "bp_leaveTournamentTeam",
            "bp_observerTournament",
            "bp_showObserverPage",
        };
    }
}