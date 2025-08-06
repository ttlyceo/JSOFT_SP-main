package top.jsoft.phaseevent;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class PhaseEventConfig extends LoggerObject
{
    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseEventConfig.class.getName());

    private static final String CONFIG_FILE = "./config/jsoft/phase/phase_event_spawns.xml";

    private final Map<String, List<NpcSpawnConfig>> phaseSpawns = new HashMap<>();

    private static PhaseEventConfig instance;

    private PhaseEventConfig()
    {
        loadConfig();
    }

    public static PhaseEventConfig getInstance()
    {
        if (instance == null)
        {
            instance = new PhaseEventConfig();
        }
        return instance;
    }

    private void loadConfig()
    {
        phaseSpawns.clear();
        try
        {
            final File file = new File(Config.DATAPACK_ROOT, CONFIG_FILE);
            if (!file.exists())
            {
                LOGGER.warn("[PhaseEventConfig] Config file not found: " + file.getAbsolutePath());
                return;
            }

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringComments(true);
            final Document doc = factory.newDocumentBuilder().parse(file);

            for (Node phaseNode = doc.getFirstChild(); phaseNode != null; phaseNode = phaseNode.getNextSibling())
            {
                if ("phases".equalsIgnoreCase(phaseNode.getNodeName()))
                {
                    for (Node phase = phaseNode.getFirstChild(); phase != null; phase = phase.getNextSibling())
                    {
                        if ("phase".equalsIgnoreCase(phase.getNodeName()))
                        {
                            String phaseName = phase.getAttributes().getNamedItem("name").getNodeValue().toLowerCase();
                            List<NpcSpawnConfig> spawns = new ArrayList<>();

                            for (Node npcNode = phase.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
                            {
                                if ("npc".equalsIgnoreCase(npcNode.getNodeName()))
                                {
                                    try
                                    {
                                        int npcId = Integer.parseInt(npcNode.getAttributes().getNamedItem("id").getNodeValue());
                                        int x = Integer.parseInt(npcNode.getAttributes().getNamedItem("x").getNodeValue());
                                        int y = Integer.parseInt(npcNode.getAttributes().getNamedItem("y").getNodeValue());
                                        int z = Integer.parseInt(npcNode.getAttributes().getNamedItem("z").getNodeValue());
                                        int heading = Integer.parseInt(npcNode.getAttributes().getNamedItem("heading").getNodeValue());
                                        int count = Integer.parseInt(npcNode.getAttributes().getNamedItem("count").getNodeValue());
                                        int respawnDelay = Integer.parseInt(npcNode.getAttributes().getNamedItem("respawnDelay").getNodeValue());

                                        spawns.add(new NpcSpawnConfig(npcId, x, y, z, heading, count, respawnDelay));
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        LOGGER.error("[PhaseEventConfig] Invalid number format in NPC config for phase " + phaseName, e);
                                    }
                                }
                            }
                            phaseSpawns.put(phaseName, spawns);
                            LOGGER.info("[PhaseEventConfig] Loaded " + spawns.size() + " mobs for phase: " + phaseName);
                        }
                    }
                }
            }
        }
        catch (ParserConfigurationException | SAXException | IOException e)
        {
            LOGGER.error("[PhaseEventConfig] Could not load config file: " + CONFIG_FILE, e);
        }
    }

    public List<NpcSpawnConfig> getSpawnsForPhase(String phase)
    {
        return phaseSpawns.getOrDefault(phase.toLowerCase(), Collections.emptyList());
    }

    public void reload() {
        loadConfig();
    }

    public static class NpcSpawnConfig
    {
        public final int npcId;
        public final int x;
        public final int y;
        public final int z;
        public final int heading;
        public final int count;
        public final int respawnDelay;

        public NpcSpawnConfig(int npcId, int x, int y, int z, int heading, int count, int respawnDelay)
        {
            this.npcId = npcId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.heading = heading;
            this.count = count;
            this.respawnDelay = respawnDelay;
        }
    }
}