package top.jsoft.jguard.utils;

import top.jsoft.jguard.JGuard;
import top.jsoft.jguard.manager.session.model.HWID;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class Strings
{
	protected static final Logger _log = Logger.getLogger(Strings.class.getName());
	
	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 * @param startIdx - начальный индекс, если указать отрицательный то он отнимется от количества строк
	 * @param maxCount - мескимум элементов, если 0 - вернутся пустая строка, если отрицательный то учитыватся не будет
	 */
	
	
	public static String joinStrings(String glueStr, String[] strings, int startIdx, int maxCount)
	{
		String result = "";
		if(startIdx < 0)
		{
			startIdx += strings.length;
			if(startIdx < 0)
				return result;
		}
		while(startIdx < strings.length && maxCount != 0)
		{
			if(!result.isEmpty() && glueStr != null && !glueStr.isEmpty())
				result += glueStr;
			result += strings[startIdx++];
			maxCount--;
		}
		return result;
	}

	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 * @param startIdx - начальный индекс, если указать отрицательный то он отнимется от количества строк
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx)
	{
		return joinStrings(glueStr, strings, startIdx, -1);
	}

	/***
	 * Склеивалка для строк
	 * @param glueStr - строка разделитель, может быть пустой строкой или null
	 * @param strings - массив из строк которые надо склеить
	 */
	public static String joinStrings(String glueStr, String[] strings)
	{
		return joinStrings(glueStr, strings, 0);
	}

	public static String stripToSingleLine(String s)
	{
		if(s.isEmpty())
			return s;
		s = s.replaceAll("\\\\n", "\n");
		int i = s.indexOf("\n");
		if(i > -1)
			s = s.substring(0, i);
		return s;
	}

	public static String getStringFromWCHARArray(byte[] data)
	{
		ByteBuffer buf = ByteBuffer.wrap(data);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		StringBuilder sb = new StringBuilder();

		char c;
		while((c = buf.getChar()) != 0)
			sb.append(c);

		buf.clear();
		buf = null;

		return sb.toString();
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
    
    public static String byteArrayToHexString(byte[] b) {
    	  int len = b.length;
    	  String data = new String();

    	  for (int i = 0; i < len; i++){
    	  data += Integer.toHexString((b[i] >> 4) & 0xf);
    	  data += Integer.toHexString(b[i] & 0xf);
    	  }
    	  return data;
    }
    
    public static void printSection(String s)
	{
		int maxlength = 79;
		s = "-[ " + s + " ]";
		int slen = s.length();
		if (slen > maxlength)
		{
			System.out.println(s);
			return;
		}
		int i;
		for (i = 0; i < (maxlength - slen); i++)
			s = "=" + s;
		System.out.println(s);
	}
    
    public static boolean isSameHWID(String hwid1, String hwid2) {


        if (JGuard.isProtectEnabled())
        {
            //return hwid1.equals(hwid2);
        	return equ(hwid1,hwid2);
            //return com.lameguard.HWID.equals(hwid1, hwid2, com.lameguard.HWID.HWID_HDD | com.lameguard.HWID.HWID_CPU | com.lameguard.HWID.HWID_BIOS);
        }
        
        return true;
    }
    
    public static boolean equ (String hwid, String hwid2)
    {
      HWID hw = HWID.fromString(hwid);
      if (hw == null) {
        return true;
      }
  	HWID hw2 = HWID.fromString(hwid2);
      if (hw2 == null) {
        return true;
      }
      return equ(hw,hw2);
    }
    
    
    public static boolean equ(HWID hwid,HWID hwid2)
    {
  		if (hwid.equalsForBan(hwid2))
          {
            return true;
          }
      return false;
    }
    
    /*public static boolean checkHwId(L2GameClient cl) {
        if (cl.getActiveChar() != null) {
        	//cl.getActiveChar().setPhone(cl.getPhone());
            if (cl.getHWIDStr() != null && cl.getHWIDStr().length() != 0) {
                try {
                    //getActiveChar().setPhone(getPhone());
                	/*
                    if (Config.SMS_PHONE && Config.SMS_HWID_NOTIFY && !getPhone().isEmpty()) {
                        if (containsHWID(getHWid())) {
                            return true;
                        }

                        getActiveChar().notifyHwidLock();
                        return true;
                    }*/
					/*
                    //String hw = cl.getActiveChar().getAccountData().getString("hwbind");
                    String hw = cl.getMyHWID();
                    if (hw.length() > 0 && !hw.toLowerCase().equalsIgnoreCase("none")) {
                        if (!isSameHWID(hw, cl.getHWIDStr())) {
                        	Log.add("Wrong HWID '" + hw + "' for: " + cl.getActiveChar().getFingerPrints(), "hwbind");
                        	_log.info("Wrong HWID '" + hw + "' for: " + cl.getActiveChar().getFingerPrints());
                            //new Disconnection(cl).deleteMe().defaultSequence(false);
                        	cl.getActiveChar().kick();
                        	//cl.closeNow();
                            return false;
                        }
                    }
                } catch (IllegalArgumentException e) {
                }
            }
        }
        return true;
    }*/
    
    public static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(input.getBytes());
            input = bytesToHex(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            //
        }
        return input;
    }
    public static String bytesToHex(byte[] b) {
        char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        StringBuilder buf = new StringBuilder();
        for (int j = 0; j < b.length; j++) {
            buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
            buf.append(hexDigit[b[j] & 0x0f]);
        }
        return buf.toString();
    }
    
}