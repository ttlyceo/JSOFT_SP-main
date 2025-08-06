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
package l2e.gameserver.listener.other;

import l2e.commons.listener.Listener;
import l2e.gameserver.model.Clan;
import l2e.gameserver.model.entity.FortSiege;

public interface OnFortSiegeStatusListener extends Listener<FortSiege>
{
	public void onStart(FortSiege siege);

	public void onEnd(FortSiege siege, Clan winClan, Clan defClan);
}
