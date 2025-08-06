package top.jsoft.jguard.manager.session.model;


import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.regex.Pattern;

/**
 * @author Akumu
 * @date 05.06.2014 19:45
 */
@SuppressWarnings("SpellCheckingInspection")
public class HWID implements Serializable
{
    private static final long serialVersionUID = -1322322139926390329L;

	//public static final Pattern HWID_PATTERN = Pattern.compile("([a-fA-F0-9]{48})");
	public static final Pattern HWID_PATTERN = Pattern.compile("([a-fA-F0-9]{24})");
	//public static final int HWID_DATA_LENGTH = 24;
	public static final int HWID_DATA_LENGTH = 12;
    public static final int HWID_PLAIN_LENGTH = HWID_DATA_LENGTH * 2;

	public final String plain;
	//public final BigInteger HDD, MAC, CPU;
	//public final String HDD, MAC, CPU;

	private HWID(String hwid) throws InvalidParameterException
	{
        if(hwid == null)
            throw new InvalidParameterException("hwid string is null");

        if(hwid.length() != HWID_DATA_LENGTH)
            throw new InvalidParameterException("hwid string has invalid length");

		this.plain = hwid;

		/*this.HDD = new BigInteger(hwid.substring(0, 16), 16);
		this.MAC = new BigInteger(hwid.substring(16, 32), 16);
		this.CPU = new BigInteger(hwid.substring(32, 48), 16);*/
		
		//this.HDD = hwid.substring(0, 8);
		//this.MAC = hwid.substring(8, 16);
		//this.CPU = hwid.substring(16, 24);
		/*
		this.HDD = new BigInteger(hwid.substring(0, 8), 8);
		this.MAC = new BigInteger(hwid.substring(8, 16), 8);
		this.CPU = new BigInteger(hwid.substring(16, 24), 8);*/
	}

	private HWID(byte[] data)
	{
		StringBuilder sb = new StringBuilder();

		for (byte b : data)
			sb.append(String.format("%02x", b));

		this.plain = sb.toString();

		/*this.HDD = new BigInteger(this.plain.substring(0, 16), 16);
		this.MAC = new BigInteger(this.plain.substring(16, 32), 16);
		this.CPU = new BigInteger(this.plain.substring(32, 48), 16);
		*/

		
		//this.HDD = this.plain.substring(0, 8);
		//this.MAC = this.plain.substring(8, 16);
		//this.CPU = this.plain.substring(16, 24);
		
		/*
		this.HDD = new BigInteger(this.plain.substring(0, 8), 8);
		this.MAC = new BigInteger(this.plain.substring(8, 16), 8);
		this.CPU = new BigInteger(this.plain.substring(16, 24), 8);*/
		
	}

	@Override
	public String toString()
	{
		return plain;
	}

	public static HWID fromString(String hwid)
	{
		if(hwid == null || hwid.length() != HWID_DATA_LENGTH)
			return null;

		return new HWID(hwid);
		/*if(hwid == null)
			return null;

		Matcher m = HWID_PATTERN.matcher(hwid.toLowerCase());

        try
        {
            if (m.find())
                return new HWID(m.group());
        }
        catch (Exception e )
        {}

		return null;*/
	}

	public static HWID fromData(byte[] data)
	{
		if(data == null || data.length != HWID_DATA_LENGTH)
			return null;

		return new HWID(data);
	}
	
	public static HWID fromStringNew(String data)
	{
		if(data == null || data.length() != HWID_PLAIN_LENGTH)
			return null;

		return new HWID(data);
	}

	public boolean equalsForBan(HWID h)
	{
		//if(!HDD.equals(h.HDD) && (JGuardConfig.BanMask & HWIDParts.HDD.mask) == HWIDParts.HDD.mask)
		//	return false;

		if(!plain.equals(h.plain))
			return false;
		//if(!MAC.equals(h.MAC))
		//	return false;

		//if(!CPU.equals(h.CPU) && (JGuardConfig.BanMask & HWIDParts.CPU.mask) == HWIDParts.CPU.mask)
		//	return false;

		return true;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		HWID hwid = (HWID) o;

		//if (!CPU.equals(hwid.CPU)) return false;
		//if (!HDD.equals(hwid.HDD)) return false;
		//if (!MAC.equals(hwid.MAC)) return false;
		if (!plain.equals(hwid.plain)) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
        //int result = HDD.hashCode();
		//result = 31 * result + MAC.hashCode();
		//result = 31 * result + CPU.hashCode();
		return plain.hashCode();
	}
}
