package l2e.gameserver;

import l2e.gameserver.instancemanager.SiegeManager;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.Siege;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.strings.server.ServerMessage;
import l2e.gameserver.model.zone.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class JSOFT {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSOFT.class);

    public static final String DEVELOPER = "psygrammator";
    public static final String COMPANY = "JSOFT";

    public static boolean checkCondition(Player player)
    {
        if (player == null)
        {
            return false;
        }

        if ( player.getUCState() > 0 || player.isInDuel() || player.isInPartyTournament() || player.isInKrateisCube() || player.isInCombat() || player.isRegisteredInFightEvent() || player.isOlympiadStart() || player.isFlying() || player.isJailed() || player.isInOlympiadMode() || player.inObserverMode() || player.isFestivalParticipant())
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        Siege siege = SiegeManager.getInstance().getSiege(player);
        if (siege != null && siege.getIsInProgress())
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        if ((player.isInSiege() || player.getSiegeState() != 0))
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        if (player.getKarma() > 0 || player.isCursedWeaponEquipped())
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        if (player.isAlikeDead() || player.isDead())
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        if (player.isInsideZone(ZoneId.SIEGE))
        {
            player.sendMessage((new ServerMessage("Community.ALL_DISABLE", player.getLang())).toString());
            return false;
        }

        return true;
    }

}
