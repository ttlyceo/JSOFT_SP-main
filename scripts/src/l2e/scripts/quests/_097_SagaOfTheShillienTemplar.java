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
package l2e.scripts.quests;

import l2e.gameserver.model.Location;

/**
 * Rework by LordWinter 12.12.2021
 */
public class _097_SagaOfTheShillienTemplar extends SagasSuperClass
{
	public _097_SagaOfTheShillienTemplar(int id, String name, String descr)
	{
		super(id, name, descr);
		
		_npcs = new int[]
		{
		        31580, 31623, 31285, 31285, 31610, 31646, 31648, 31652, 31654, 31655, 31659, 31285
		};
		
		_items = new int[]
		{
		        7080, 7526, 7081, 7512, 7295, 7326, 7357, 7388, 7419, 7450, 7091, 0
		};
		
		_mobs = new int[]
		{
		        27271, 27246, 27273
		};
		
		_classId = new int[]
		{
		        106
		};
		
		_prevClass = new int[]
		{
		        0x21
		};
		
		_npcSpawnLoc = new Location[]
		{
		        new Location(161719, -92823, -1893), new Location(124355, 82155, -2803), new Location(124376, 82127, -2796)
		};
		
		_msgs = new String[]
		{
				"PLAYERNAME! Pursued to here! However, I jumped out of the Banshouren boundaries! You look at the giant as the sign of power!",
				"... Oh ... good! So it was ... let's begin!",
				"I do not have the patience ..! I have been a giant force ...! Cough chatter ah ah ah!",
				"Paying homage to those who disrupt the orderly will be PLAYERNAME's death!",
				"Now, my soul freed from the shackles of the millennium, Halixia, to the back side I come ...",
				"Why do you interfere others' battles?",
				"This is a waste of time.. Say goodbye...!",
				"...That is the enemy",
				"...Goodness! PLAYERNAME you are still looking?",
				"PLAYERNAME ... Not just to whom the victory. Only personnel involved in the fighting are eligible to share in the victory.",
				"Your sword is not an ornament. Don't you think, PLAYERNAME?",
				"Goodness! I no longer sense a battle there now.",
				"let...",
				"Only engaged in the battle to bar their choice. Perhaps you should regret.",
				"The human nation was foolish to try and fight a giant's strength.",
				"Must...Retreat... Too...Strong.",
				"PLAYERNAME. Defeat...by...retaining...and...Mo...Hacker",
		        "....! Fight...Defeat...It...Fight...Defeat...It..."
		};
		registerNPCs();
	}
	
	public static void main(String[] args)
	{
		new _097_SagaOfTheShillienTemplar(97, _097_SagaOfTheShillienTemplar.class.getSimpleName(), "");
	}
}
