package top.jsoft.phaseevent;


import l2e.gameserver.data.holder.SpawnHolder;
import l2e.gameserver.data.parser.NpcsParser;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.actor.Npc;
import l2e.gameserver.model.actor.templates.npc.NpcTemplate;
import l2e.gameserver.model.spawn.Spawner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class PhaseMobSpawner
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseMobSpawner.class.getName());
    private final List<Spawner> _activeSpawns = new ArrayList<>();

    private static final PhaseMobSpawner INSTANCE = new PhaseMobSpawner();

    public static PhaseMobSpawner getInstance()
    {
        return INSTANCE;
    }

    public void spawnMobsForPhase(String phase)
    {
        removeCurrentMobs();

        List<PhaseEventConfig.NpcSpawnConfig> configs = PhaseEventConfig.getInstance().getSpawnsForPhase(phase);

        for (PhaseEventConfig.NpcSpawnConfig c : configs)
        {
            spawnNpc(c);
        }

        LOGGER.info("[PhaseMobSpawner] Spawned mobs for phase: " + phase);
    }

    private void spawnNpc(PhaseEventConfig.NpcSpawnConfig config)
    {
        final NpcTemplate template = NpcsParser.getInstance().getTemplate(config.npcId);
        if (template == null)
        {
            LOGGER.warn("[PhaseMobSpawner] NPC template not found for ID: " + config.npcId);
            return;
        }

        try
        {
            for (int i = 0; i < config.count; i++)
            {
                final Spawner spawn = new Spawner(template);
                spawn.setXYZ(Location.findAroundPosition(config.x, config.y, config.z, 50, 100, spawn.getReflection()));
                spawn.setHeading(config.heading);
                spawn.setAmount(1);
                spawn.setRespawnDelay(config.respawnDelay);

                SpawnHolder.getInstance().addNewSpawn(spawn, false);
                spawn.init();

                _activeSpawns.add(spawn);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("[PhaseMobSpawner] Error spawning NPC: ", e);
        }
    }

    public void removeCurrentMobs()
    {
        for (Spawner spawn : _activeSpawns)
        {
            Npc npc = spawn.getLastSpawn();
            if (npc != null)
            {
                spawn.stopRespawn();
                npc.deleteMe();
            }
            SpawnHolder.getInstance().deleteSpawn(spawn, false);
        }
        _activeSpawns.clear();
        LOGGER.info("[PhaseMobSpawner] Cleared mobs of previous phase.");
    }
}