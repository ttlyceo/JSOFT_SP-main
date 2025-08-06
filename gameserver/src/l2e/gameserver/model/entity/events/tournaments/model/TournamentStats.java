package l2e.gameserver.model.entity.events.tournaments.model;

import l2e.gameserver.model.entity.events.tournaments.enums.TournamentStatsType;
import l2e.gameserver.model.entity.events.tournaments.util.TournamentUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author psygrammator
 */
public class TournamentStats {
    private int tournamentMatchDamage;
    private final Map<Integer, Map<TournamentStatsType, Integer>> tournamentStats;
    private String lastPage = "main";

    public TournamentStats(Map<Integer, Map<TournamentStatsType, Integer>> tournamentStats) {
        this.tournamentStats = tournamentStats;
    }

    public int getTournamentTotalDamage()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(p -> getTournamentStats(p, TournamentStatsType.DAMAGE)).sum();
    }
    public int getTotalVictories()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(p -> getTournamentStats(p, TournamentStatsType.VICTORIES)).sum();
    }
    public int getTotalDefeats()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(p -> getTournamentStats(p, TournamentStatsType.DEFEATS)).sum();
    }
    public int getTotalTies()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(p -> getTournamentStats(p, TournamentStatsType.TIES)).sum();
    }
    public int getTournamentFightsDone(int type)
    {
        return getTournamentStats(type, TournamentStatsType.VICTORIES) + getTournamentStats(type, TournamentStatsType.DEFEATS) + getTournamentStats(type, TournamentStatsType.TIES);
    }
    public int getTotalTournamentFightsDone()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(this::getTournamentFightsDone).sum();
    }
    public int getTotalTournamentKills()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(p -> getTournamentStats(p, TournamentStatsType.KILLS)).sum();
    }
    public int getTournamentMatchDamage()
    {
        return tournamentMatchDamage;
    }
    public void addTournamentMatchDamage(int damage)
    {
        tournamentMatchDamage += damage;
    }
    public void setTournamentMatchDamage(int tournamentMatchDamage)
    {
        this.tournamentMatchDamage = tournamentMatchDamage;
    }
    public Map<TournamentStatsType, Integer> getTournamentStatsByType(int type)
    {
        return getTournamentStats().get(type);
    }
    public int getTournamentStats(int type, TournamentStatsType tournamentStatsType)
    {
        return getTournamentStats().get(type).get(tournamentStatsType);
    }
    public int getTournamentPoints()
    {
        return TournamentUtil.TOURNAMENT_TYPES.stream().mapToInt(id -> getTournamentStats(id, TournamentStatsType.VICTORIES)).sum();
    }

    public Map<Integer, Map<TournamentStatsType, Integer>> getTournamentStats() {
        return tournamentStats;
    }

    public void addTournamentTie(int type)
    {
        increment(getTournamentStatsByType(type), TournamentStatsType.TIES);
    }

    public void addTournamentDefeat(int type)
    {
        increment(getTournamentStatsByType(type), TournamentStatsType.DEFEATS);
    }

    public void addTournamentVictory(int type)
    {
        increment(getTournamentStatsByType(type), TournamentStatsType.VICTORIES);
    }

    public void addTournamentKill(int type)
    {
        increment(getTournamentStatsByType(type), TournamentStatsType.KILLS);
    }

    public static <K> void increment(Map<K, Integer> map, K key)
    {
        map.merge(key, 1, Integer::sum);

    }
    public void addTournamentDamage(int type, int damage)
    {
        increment(getTournamentStatsByType(type), TournamentStatsType.DAMAGE, damage);
    }

    public static <K> void increment(Map<K, Integer> map, K key, int toIncrement)
    {
        int val = map.get(key);
        map.put(key, val + toIncrement);
    }

    public String getLastPage() {
        return lastPage;
    }

    public void setLastPage(String lastPage) {
        this.lastPage = lastPage;
    }
}
