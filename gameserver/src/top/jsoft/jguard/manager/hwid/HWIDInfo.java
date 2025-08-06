package top.jsoft.jguard.manager.hwid;

import top.jsoft.jguard.manager.HWIDBanManager.BanType;

/**
 * Created by psygrammator
 * group jsoft.top
 */
public class HWIDInfo {
    private final int id;
    private String HWID;
    private String login;
    private BanType banType;
    private String name;

    public HWIDInfo(final int id) {
        this.id = id;
    }

    public int getIdInList() {
        return id;
    }

    public String getHWID() {
        return HWID;
    }

    public void setHWID(final String HWID) {
        this.HWID = HWID;
    }

    public void setHWIDBanned(final String HWID) {
        this.HWID = HWID;
    }

    public BanType getBanType() {
        return banType;
    }

    public void setBanType(final BanType banType) {
        this.banType = banType;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(final String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
