package top.jsoft.jguard.manager.session.model;

import java.io.Serializable;

/**
 * @author Akumu
 * @date 27.10.13
 */
public class JClientData implements Serializable
{
	public JClientData(HWID hwid, String account, short langId)
	{
		this.hwid = hwid;
		this.account = account;
		this.langId = langId;
	}

	public JClientData(String hwid, String account, short langId)
	{
		this(HWID.fromString(hwid), account, langId);
	}

	public JClientData(byte[] hwid, String account, short langId)
	{
		this(HWID.fromData(hwid), account, langId);
	}

	public final String account;
	public final HWID hwid;
	public final short langId;

	@Override
	public String toString()
	{
		return "ClientData{" +
				"account='" + account + '\'' +
				", hwid=" + hwid +
				", langId=" + langId +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		JClientData that = (JClientData) o;

		if (langId != that.langId) return false;
		if (!account.equals(that.account)) return false;
		if (!hwid.equals(that.hwid)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = account.hashCode();
		result = 31 * result + hwid.hashCode();
		result = 31 * result + (int) langId;
		return result;
	}
}
