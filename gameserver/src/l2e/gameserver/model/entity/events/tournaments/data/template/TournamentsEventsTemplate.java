package l2e.gameserver.model.entity.events.tournaments.data.template;

import l2e.commons.util.Rnd;
import l2e.gameserver.model.Location;
import l2e.gameserver.model.entity.events.tournaments.enums.LimitClassType;
import l2e.gameserver.model.entity.events.tournaments.enums.RegisterType;
import l2e.gameserver.model.holders.ItemHolder;
import l2e.gameserver.model.stats.StatsSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author psygrammator
 */
public class TournamentsEventsTemplate {
    private final int id;
    private final String type;
    private final RegisterType registerType;
    private final int typeById;
    private final boolean isEnable;

    private final Map<LimitClassType, Integer> limitClassesByType;
    private final Map<Integer, Integer> limitClassesById;
    private final List<Integer> restrictedClass;
    private final List<Integer> restrictedItems;
    private final List<Integer> restrictedSkills;
    private final List<Integer> doors;

    private final boolean communityBuffer;
    private final boolean observer;
    private final boolean potion;
    private final boolean scroll;
    private final boolean resurrection;
    private final boolean escape;
    private final boolean itemSummon;
    private final boolean canTeamTarget;
    private final boolean summons;
    private final boolean removeBuffEnter;
    private final boolean removeBuffExit;
    private final boolean resetReuseSkillsEnter;
    private final boolean resetReuseSkillsExit;
    private final List<ItemHolder> winner;
    private final List<ItemHolder> looser;
    private final List<ItemHolder> tie;
    private final List<Location> teamBlueLoc;
    private final List<Location> teamRedLoc;
    private final List<Location> teamObserverLoc;

    public TournamentsEventsTemplate(StatsSet set) {
        this.id = set.getInteger("id");
        this.type = set.getString("type");
        this.registerType = set.getEnum("registerType", RegisterType.class);
        this.typeById = set.getInteger("typeById");
        this.isEnable = set.getBool("isEnable", false);
        this.limitClassesByType = parseLimitedClassesByType(set.getString("limitClassesByType", ""));
        this.limitClassesById = parseLimitedClassesById(set.getString("limitClassesById", ""));
        this.restrictedClass = parseRestricted(set.getString("restrictedClass", ""));
        this.restrictedItems = parseRestricted(set.getString("restrictedItems", ""));
        this.restrictedSkills = parseRestricted(set.getString("restrictedSkills", ""));
        this.doors = parseRestricted(set.getString("doors", ""));
        this.communityBuffer = set.getBool("communityBuffer", false);
        this.observer = set.getBool("observer", false);
        this.potion = set.getBool("potion", true);
        this.scroll = set.getBool("scroll", false);
        this.resurrection = set.getBool("resurrection", false);
        this.escape = set.getBool("escape", false);
        this.itemSummon = set.getBool("itemSummon", false);
        this.canTeamTarget = set.getBool("canTeamTarget", true);
        this.winner = parseRewards(set.getString("winner", ""));
        this.looser = parseRewards(set.getString("looser", ""));
        this.tie = parseRewards(set.getString("tie", ""));
        this.teamBlueLoc = parseLocations(set.getString("teamBlueLoc"));
        this.teamRedLoc = parseLocations(set.getString("teamRedLoc"));
        this.teamObserverLoc = parseLocations(set.getString("teamObserverLoc"));
        this.summons = set.getBool("summons", false);
        this.removeBuffExit = set.getBool("removeBuffExit", false);
        this.removeBuffEnter = set.getBool("removeBuffEnter", false);
        this.resetReuseSkillsEnter = set.getBool("resetReuseSkillsEnter", false);
        this.resetReuseSkillsExit = set.getBool("resetReuseSkillsExit", false);
    }

    public int getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public RegisterType getRegisterType() {
        return registerType;
    }

    public int getTypeById() {
        return typeById;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public Map<LimitClassType, Integer> getLimitClassesByType() {
        return limitClassesByType;
    }

    public Map<Integer, Integer> getLimitClassesById() {
        return limitClassesById;
    }

    public List<Integer> getRestrictedClass() {
        return restrictedClass;
    }

    public List<Integer> getRestrictedItems() {
        return restrictedItems;
    }

    public List<Integer> getRestrictedSkills() {
        return restrictedSkills;
    }

    public List<Integer> getDoors() {
        return doors;
    }

    public boolean isCommunityBuffer() {
        return communityBuffer;
    }

    public boolean isObserver() {
        return observer;
    }

    public boolean isPotion() {
        return potion;
    }

    public boolean isScroll() {
        return scroll;
    }

    public boolean isResurrection() {
        return resurrection;
    }

    public boolean isEscape() {
        return escape;
    }

    public boolean isItemSummon() {
        return itemSummon;
    }

    public boolean isCanTeamTarget() {
        return canTeamTarget;
    }

    public List<ItemHolder> getWinner() {
        return winner;
    }

    public List<ItemHolder> getLooser() {
        return looser;
    }

    public List<ItemHolder> getTie() {
        return tie;
    }

    public List<Location> getTeamBlueLoc() {
        return teamBlueLoc;
    }

    public List<Location> getTeamRedLoc() {
        return teamRedLoc;
    }

    public List<Location> getTeamObserverLoc() {
        return teamObserverLoc;
    }

    public boolean isSummons() {
        return summons;
    }

    public boolean isRemoveBuffEnter() {
        return removeBuffEnter;
    }

    public boolean isRemoveBuffExit() {
        return removeBuffExit;
    }

    public boolean isResetReuseSkillsEnter() {
        return resetReuseSkillsEnter;
    }

    public boolean isResetReuseSkillsExit() {
        return resetReuseSkillsExit;
    }

    private Map<LimitClassType, Integer> parseLimitedClassesByType(String classes)
    {
        if (classes.isEmpty())
        {
            return new HashMap<>();
        }

        final String[] classType = classes.split(";");

        final Map<LimitClassType, Integer> selected = new HashMap<>(classType.length);
        for (final String classIds : classType)
        {
            final String[] delimiter = classIds.split(",");
            selected.put(LimitClassType.valueOf(delimiter[0]), Integer.valueOf(delimiter[1]));
        }
        return selected;
    }

    private Map<Integer, Integer> parseLimitedClassesById(String classes)
    {
        if (classes.isEmpty())
        {
            return new HashMap<>();
        }

        final String[] classType = classes.split(";");

        final Map<Integer, Integer> selected = new HashMap<>(classType.length);
        for (final String classIds : classType)
        {
            final String[] delimiter = classIds.split(",");
            selected.put(Integer.valueOf(delimiter[0]), Integer.valueOf(delimiter[1]));
        }
        return selected;
    }

    private List<Integer> parseRestricted(String restricteds)
    {
        if (restricteds.isEmpty())
        {
            return new ArrayList<>();
        }

        final String[] delimiter = restricteds.split(";");

        final List<Integer> selected = new ArrayList<>(delimiter.length);
        for (final String restricted : delimiter)
        {
            selected.add(Integer.parseInt(restricted.trim()));
        }
        return selected;
    }

    private List<Location> parseLocations(String data)
    {
        if (data.isEmpty())
        {
            return new ArrayList<>();
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
    private List<ItemHolder> parseRewards(String data)
    {
        if (data.isEmpty())
        {
            return new ArrayList<>();
        }

        final String[] items = data.split(";");

        final List<ItemHolder> selected = new ArrayList<>(items.length);
        for (final String item : items)
        {
            final String[] delimiter = item.split(",");
            if(delimiter.length == 2)
                selected.add(new ItemHolder(Integer.parseInt(delimiter[0]), Long.parseLong(delimiter[1])));
            else if(delimiter.length == 3)
                selected.add(new ItemHolder(Integer.parseInt(delimiter[0]), Long.parseLong(delimiter[1]), Double.parseDouble(delimiter[2])));
            else if(delimiter.length == 4)
                selected.add(new ItemHolder(Integer.parseInt(delimiter[0]), Long.parseLong(delimiter[1]), Long.parseLong(delimiter[2]), Double.parseDouble(delimiter[3]), 0));
            else if(delimiter.length == 5)
                selected.add(new ItemHolder(Integer.parseInt(delimiter[0]), Long.parseLong(delimiter[1]), Long.parseLong(delimiter[2]), Double.parseDouble(delimiter[3]), Integer.parseInt(delimiter[4])));
        }
        return selected;
    }
}