package l2e.gameserver.model.skills;

import l2e.gameserver.data.parser.SkillsParser;
import l2e.gameserver.model.stats.StatsSet;

/**
 * @author psygrammator
 */
public class PcBuffTemplate {
    private final int skillId;
    private final int skillLevel;
    private final int buffTime;


    public PcBuffTemplate(StatsSet set) {
        this.skillId = set.getInteger("id");
        this.skillLevel = set.getInteger("lvl", SkillsParser.getInstance().getMaxLevel(skillId));
        this.buffTime = set.getInteger("time", -1);
    }

    public PcBuffTemplate(int skillId, int skillLevel, int buffTime) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
        this.buffTime = buffTime;
    }

    public PcBuffTemplate(int skillId, int skillLevel) {
        this.skillId = skillId;
        this.skillLevel = skillLevel;
        this.buffTime = -1;
    }

    public int getSkillId() {
        return skillId;
    }

    public int getSkillLevel() {
        return skillLevel;
    }

    public int getBuffTime() {
        return buffTime;
    }
}
