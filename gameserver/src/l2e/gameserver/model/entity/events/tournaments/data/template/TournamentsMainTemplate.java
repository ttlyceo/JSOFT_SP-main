package l2e.gameserver.model.entity.events.tournaments.data.template;

import l2e.commons.util.Rnd;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.stats.StatsSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author psygrammator
 */
public class TournamentsMainTemplate {
    private final boolean isEnable;
    private final int npcId;
    private final int minLvl;
    private final int maxLvl;
    private final List<Location> npcSpawnLocs;
    private final String schedule;
    private final int timerStart;
    private final int timerEnd;
    private final int matchFindTime;
    private final int battleDuration;
    private final int limitByIp;
    private final int limitByHwid;
    private final int summonToNpc;
    private final boolean givePvp;
    private final int[] announceCountdown;

    public TournamentsMainTemplate(StatsSet set) {
        this.isEnable = set.getBool("isEnable");
        this.npcId = set.getInteger("npcId");
        this.minLvl = set.getInteger("minLvl");
        this.maxLvl = set.getInteger("maxLvl");
        this.npcSpawnLocs = parseLocations(set.getString("npcSpawnLocs"));
        this.schedule = set.getString("schedule");
        this.timerStart = set.getInteger("timerStart");
        this.timerEnd = set.getInteger("timerEnd");
        this.matchFindTime = set.getInteger("matchFindTime");
        this.battleDuration = set.getInteger("battleDuration");
        this.limitByIp = set.getInteger("limitByIp");
        this.limitByHwid = set.getInteger("limitByHwid");
        this.summonToNpc = set.getInteger("summonToNpc");
        this.givePvp = set.getBool("givePvp");
        this.announceCountdown = parseIntArray(set);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public int getNpcId() {
        return npcId;
    }

    public int getMinLvl() {
        return minLvl;
    }

    public int getMaxLvl() {
        return maxLvl;
    }

    public List<Location> getNpcSpawnLocs() {
        return npcSpawnLocs;
    }

    public boolean isShedule() {
        return !schedule.equalsIgnoreCase("-1");
    }

    public String getSchedule() {
        return schedule;
    }

    public int getTimerStart() {
        return timerStart;
    }

    public int getTimerEnd() {
        return timerEnd;
    }

    public int getMatchFindTime() {
        return matchFindTime;
    }

    public int getBattleDuration() {
        return battleDuration;
    }

    public int getLimitByIp() {
        return limitByIp;
    }

    public int getLimitByHwid() {
        return limitByHwid;
    }

    public int getSummonToNpc() {
        return summonToNpc;
    }

    public boolean isGivePvp() {
        return givePvp;
    }

    public int[] getAnnounceCountdown() {
        return announceCountdown;
    }

    public int[] parseIntArray(StatsSet set)
    {
        String data = set.getString("announceCountdown", "off");
        if(data.equalsIgnoreCase("off"))
            return new int[0];

        return set.getIntegerArray("announceCountdown", ";");
    }

    private List<Integer> parseExcludedClasses(String classes)
    {
        if (classes.isEmpty())
        {
            return null;
        }

        final String[] classType = classes.split(";");

        final List<Integer> selected = new ArrayList<>(classType.length);
        for (final String classId : classType)
        {
            selected.add(Integer.parseInt(classId.trim()));
        }
        return selected;
    }

    private List<Location> parseLocations(String data)
    {
        if (data.isEmpty())
        {
            return null;
        }

        final String[] locs = data.split(";");

        final List<Location> selected = new ArrayList<>(locs.length);
        for (final String loc : locs)
        {
            final String[] pos = loc.split(",");
            selected.add(new Location(Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]), pos.length == 4 ? Integer.parseInt(pos[3]) : Rnd.nextInt(65536)));
        }
        return selected;
    }
}
