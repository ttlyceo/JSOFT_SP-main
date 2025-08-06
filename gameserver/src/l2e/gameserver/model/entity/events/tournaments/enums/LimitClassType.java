package l2e.gameserver.model.entity.events.tournaments.enums;

/**
 * @author psygrammator
 */
public enum LimitClassType {
    FIGHTERS("Fighter(s)"),
    TANKS("Tank(s)"),
    ARCHERS("Archer(s)"),
    DAGGERS("Dagger(s)"),
    MAGES("Mage(s)"),
    SUMMONERS("Summoner(s)"),
    HEALERS("Healer(s)"),
    SUPPORTS("Support(s)"),
    KAMAELS("Kamael(s)");

    private final String name;
    LimitClassType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
