/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 *
 */
package l2e.gameserver.model.actor.instance.player;

import java.util.HashMap;
import java.util.Map;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.model.base.BypassType;

public class BypassStorage extends LoggerObject
{
	private final Map<Integer, String> _generalBypasses = new HashMap<>();
	private final Map<Integer, String> _bbsBypasses = new HashMap<>();

	public BypassType getBypassType(final String bypass)
    {
        switch(bypass.charAt(0))
        {
            case '0':
                return BypassType.ENCODED;
            case '1':
                return BypassType.ENCODED_BBS;
            default:
				if (Config.GENERAL_BYPASS_ENCODE_IGNORE.matcher(bypass).matches() || isIgnoreBypass(bypass))
				{
					return BypassType.SIMPLE;
				}
				if (Config.EXACT_BYPASS_ENCODE_IGNORE.contains(bypass))
				{
					return BypassType.SIMPLE_BBS;
				}
                return BypassType.SIMPLE_DIRECT;
        }
    }

    public String encodeBypasses(final String html, final boolean bbs)
    {
        if(html == null)
		{
			return null;
		}
		final var storage = bbs ? _bbsBypasses : _generalBypasses;
        storage.clear();

		final var m = Config.BYPASS_TEMPLATE.matcher(html);
        final StringBuffer sb = new StringBuffer();

        while(m.find())
        {
            final String bypass = m.group(2);
            String code = bypass;
            String params = "";
            final int i = bypass.indexOf(" $");
            final boolean use_params = i >= 0;
            if(use_params)
            {
                code = bypass.substring(0, i);
                params = bypass.substring(i).replace("$", "\\$");
            }
			
            if(bbs)
			{
				m.appendReplacement(sb, "\"bypass -h 1" + Integer.toHexString(encodeBypass(code, storage)) + params + "\"");
			}
			else
			{
				m.appendReplacement(sb, "\"bypass -h 0" + Integer.toHexString(encodeBypass(code, storage)) + params + "\"");
			}
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String decodeBypass(final String bypass, final boolean bbs)
    {
		final var storage = bbs ? _bbsBypasses : _generalBypasses;
        final String[] bypass_parsed = bypass.split(" ");
        String result = null;
        int idx;

        try
        {
            idx = Integer.parseInt(bypass_parsed[0], 16);
        }
        catch(final NumberFormatException e)
        {
            return null;
        }

        result = storage.get(idx);
        if(result != null && bypass_parsed.length > 1)
		{
			for(int i = 1; i < bypass_parsed.length; i++)
			{
				result += " " + bypass_parsed[i];
			}
		}
		
		if (result != null && !Config.REUSABLE_BYPASS_ENCODE.matcher(result).matches())
		{
			storage.remove(idx);
		}
        return result;
    }

    private int encodeBypass(final String bypass, final Map<Integer, String> storage)
    {
		final int key = Rnd.get((storage.size() * 1000), ((storage.size() + 1) * 1000 - 1));
        storage.put(key , bypass);
        return key;
    }
	
	private boolean isIgnoreBypass(String bypass)
	{
		final var list = Config.INITIAL_BYPASS_ENCODE_IGNORE;
		if (list == null || list.isEmpty())
		{
			return false;
		}
		for (final var cmd : list)
		{
			if (bypass.startsWith(cmd))
			{
				return true;
			}
		}
		return false;
	}
}
