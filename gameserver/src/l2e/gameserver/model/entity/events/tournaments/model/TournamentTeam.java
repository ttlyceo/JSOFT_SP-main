package l2e.gameserver.model.entity.events.tournaments.model;

import l2e.gameserver.handler.communityhandlers.CommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.ICommunityBoardHandler;
import l2e.gameserver.handler.communityhandlers.impl.CommunityBuffer;
import l2e.gameserver.model.Party;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.base.ClassId;
import l2e.gameserver.model.entity.events.tournaments.Tournament;
import l2e.gameserver.model.entity.events.tournaments.TournamentData;
import l2e.gameserver.model.entity.events.tournaments.enums.LimitClassType;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;
import l2e.gameserver.network.clientpackets.Say2;
import l2e.gameserver.network.serverpackets.ExShowScreenMessage;
import l2e.gameserver.network.serverpackets.GameServerPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author psygrammator
 */
public class TournamentTeam
{
    private Player leader;
    private Player target;
    private List<Player> members = new ArrayList<>();
    private Party party;

    public TournamentTeam(Player leader, Player target)
    {
        this.leader = leader;
        this.target = target;

        members.add(0, leader);
        leader.setTournamentTeam(this);
        TournamentUtil.toPlayer(leader, Say2.CLAN, "Your Tournament fight has been created.");

        if (target != null)
        {
            TournamentUtil.toPlayer(leader, Say2.CLAN, target.getName(null) + " entered your Tournament Team.");
            TournamentUtil.toPlayer(target, Say2.CLAN, leader.getName(null) + " entered your Tournament Team.");
            members.add(1, target);
            target.setTournamentTeam(this);
        }
    }

    public List<Player> getMembers()
    {
        return members;
    }

    public void setMembers(List<Player> members)
    {
        this.members = members;
    }

    public Player getLeader()
    {
        return leader;
    }

    public void setLeader(Player leader)
    {
        this.leader = leader;
    }

    public Player getTarget()
    {
        return target;
    }

    public void setTarget(Player target)
    {
        this.target = target;
    }

    public String getName()
    {
        return "[" + leader.getName(null) + " Team]";
    }

    public void addMember(Player player)
    {
        if (player == null || getMembers().contains(player))
            return;

        getMembers().add(player);
        player.setTournamentTeam(this);
        TournamentUtil.toPlayer(player, Say2.CLAN, "You entered " + leader.getName(null) + "'s Tournament Team.");
        TournamentUtil.toTeam(this, Say2.CLAN, "Player " + player.getName(null) + " joined your Tournament Team.");
    }

    public void removeMember(Player member)
    {
        if (member == null || !getMembers().contains(member))
            return;

        getMembers().remove(member);
        member.setTournamentTeam(null);
        member.setTournamentTeamBeingInvited(false);
        TournamentUtil.toPlayer(member, Say2.CLAN, "Your tournament Team has dispersed.");
        if (party != null)
            party.removePartyMember(member, Party.messageType.Left);
        if (members.isEmpty())
        {
            disbandTeam();
        }
    }

    public void disbandTeam()
    {
        if (TournamentData.getInstance().getLobbies().contains(this))
        {
            TournamentData.getInstance().removeFromLobby(this);
        }
        for (Player member : getMembers())
        {
            member.setTournamentTeam(null);
            member.setTournamentTeamBeingInvited(false);
            TournamentUtil.toPlayer(member, Say2.CLAN, "Your tournament Team has dispersed.");
        }
        if (getParty() != null)
            getParty().disbandParty();
        setParty(null);
    }

    public void createParty()
    {
        getMembers().forEach(Player::leaveParty);
        if(getMembers().size() > 1)
        {
            getMembers().forEach(p -> {
                Party party = getLeader().getParty();
                if(party == null)
                {
                    party = new Party(getLeader(), Party.ITEM_LOOTER);
                    setParty(party);
                }

                if(!p.isInParty())
                    p.joinParty(party);
            });
        }
    }

    public void callBuffer()
    {
        getMembers().forEach(p ->
        {
            final ICommunityBoardHandler handler = CommunityBoardHandler.getInstance().getHandler(CommunityBuffer.BYPASS_BUFFER);
            if (handler != null)
            {
                handler.onBypassCommand(CommunityBuffer.BYPASS_BUFFER, p);
            }
        });
    }

    public void setPartyTournament(Tournament partyTournament)
    {
        getMembers().forEach(p -> p.setPartyTournament(partyTournament));
    }
    public Party getParty()
    {
        return party;
    }

    public void setParty(Party party)
    {
        this.party = party;
    }
    public boolean isLeader(Player player)
    {
        return player == leader;
    }
    public void addTotalDamageToPlayers(int type)
    {
        getMembers().forEach(s -> s.getTournamentStats().addTournamentDamage(type, s.getTournamentStats().getTournamentMatchDamage()));
    }
    public void resetTeamMatchDamage()
    {
        getMembers().forEach(s -> s.getTournamentStats().setTournamentMatchDamage(0));
    }

    public int getCountOfClass(int classId)
    {
        return (int) getMembers().stream().filter(p -> p.getClassId().getId() == classId).count();
    }

    public int getClassTypeCount(LimitClassType limitClassType)
    {
        return (int) getMembers().stream().filter(p -> TournamentUtil.isLimitClassByType(p, limitClassType)).count();
    }

    public void sendPacket(GameServerPacket packet)
    {
        getMembers().forEach(p -> p.sendPacket(packet));
    }

    public int size()
    {
        return members.size();
    }
}