package l2e.gameserver.listener.player.impl;

import l2e.gameserver.Config;
import l2e.gameserver.listener.player.OnAnswerListener;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.actor.instance.RaidBossInstance;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class AskToTeleport implements OnAnswerListener {
    private final Player player;
    private final RaidBossInstance raidboss;

    public AskToTeleport(Player player, RaidBossInstance raidBoss) {
        this.player = player;
        this.raidboss = raidBoss;
    }

    @Override
    public void sayYes() {
        player.teleToLocation(Location.findPointToStay(raidboss, Config.SUMMON_TO_RB_COLLISION, false), true, player.getReflection());
    }

    @Override
    public void sayNo() {

    }
}
