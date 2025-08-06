package top.jsoft.phaseevent;

import l2e.gameserver.Config;
import l2e.gameserver.handler.admincommandhandlers.IAdminCommandHandler;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.network.serverpackets.NpcHtmlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jsoft.commons.time.Chronos;

/**
 * | Created by psygrammator
 * | @team jsoft
 */
public class AdminPhaseControl implements IAdminCommandHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminPhaseControl.class.getName());

    private static final String[] ADMIN_COMMANDS = {
            "admin_phasecontrol",
            "admin_setphase_day",
            "admin_setphase_night",
            "admin_setphase_redmoon"
    };

    @Override
    public boolean useAdminCommand(String command, Player activeChar)
    {
        if (activeChar == null || !activeChar.isGM())
        {
            return false;
        }

        PhaseManager phaseManager = PhaseManager.getInstance();
        String currentPhase = phaseManager.getCurrentPhase();
        long timeLeft = phaseManager.getPhaseEndTime() - System.currentTimeMillis();
        long minutesLeft = timeLeft / 60000;

        if (command.equalsIgnoreCase("admin_phasecontrol"))
        {
            showPhaseControlPanel(activeChar, currentPhase, minutesLeft);
        }
        else if (command.equalsIgnoreCase("admin_setphase_day"))
        {
            phaseManager.setPhaseImmediate("Day", Config.PHASEEVENT_DAY_DURATION * Chronos.MILLIS_PER_MINUTE);
            activeChar.sendMessage("Phase switched to Day.");
            showPhaseControlPanel(activeChar, "Day", Config.PHASEEVENT_DAY_DURATION);
            LOGGER.info("Admin {} switched phase to Day.", activeChar.getName(null));
        }
        else if (command.equalsIgnoreCase("admin_setphase_night"))
        {
            phaseManager.setPhaseImmediate("Night", Config.PHASEEVENT_NIGHT_DURATION * Chronos.MILLIS_PER_MINUTE);
            activeChar.sendMessage("Phase switched to Night.");
            showPhaseControlPanel(activeChar, "Night", Config.PHASEEVENT_NIGHT_DURATION);
            LOGGER.info("Admin {} switched phase to Night.", activeChar.getName(null));
        }
        else if (command.equalsIgnoreCase("admin_setphase_redmoon"))
        {
            phaseManager.setPhaseImmediate("RedMoon", Config.PHASEEVENT_DAY_DURATION * Chronos.MILLIS_PER_MINUTE);
            activeChar.sendMessage("Phase switched to RedMoon.");
            showPhaseControlPanel(activeChar, "RedMoon", Config.PHASEEVENT_DAY_DURATION);
            LOGGER.info("Admin {} switched phase to RedMoon.", activeChar.getName(null));
        }

        return true;
    }

    private void showPhaseControlPanel(Player activeChar, String currentPhase, long minutesLeft)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(0);
        html.setHtml(activeChar, "<html><body>" +
                "<center><font color=\"LEVEL\">Phase Control Panel</font></center><br>" +
                "<table width=\"300\">" +
                "<tr><td>Current Phase: " + currentPhase + "</td></tr>" +
                "<tr><td>Time Left: " + minutesLeft + " minutes</td></tr>" +
                "</table><br>" +
                "<table width=\"300\">" +
                "<tr><td><button value=\"Set Day Phase\" action=\"bypass -h admin_setphase_day\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
                "<tr><td><button value=\"Set Night Phase\" action=\"bypass -h admin_setphase_night\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
                "<tr><td><button value=\"Set RedMoon Phase\" action=\"bypass -h admin_setphase_redmoon\" width=100 height=20 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>" +
                "</table>" +
                "</body></html>");
        activeChar.sendPacket(html);
    }

    @Override
    public String[] getAdminCommandList()
    {
        return ADMIN_COMMANDS;
    }
}