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
package l2e.gameserver.model.actor.templates.npc;

public class AnnounceTemplate
{
	private final NpcTemplate _tpl;
	private final long _delay;

	public AnnounceTemplate(NpcTemplate tpl, long delay)
	{
		_tpl = tpl;
		_delay = delay;
	}
	
	public NpcTemplate getTemplate()
	{
		return _tpl;
	}

	public long getDelay()
	{
		return _delay;
	}
}