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
package l2e.gameserver.model.actor.templates.player;

/**
 * Created by LordWinter
 */
public class PvpColorTempate
{
	private final int _id;
	private final int _request;
	private final int _color;
	private final int _titleColor;
	private final String _title;
	private int _nextCheck = 0;
	private boolean _isLast = false;
	
	public PvpColorTempate(int id, int request, int color, int titleColor, String title)
	{
		_id = id;
		_request = request;
		_color = color;
		_titleColor = titleColor;
		_title = title;
	}
	
	public int getId()
	{
		return _id;
	}
	
	public int getRequest()
	{
		return _request;
	}
	
	public int getColor()
	{
		return _color;
	}
	
	public int getTitleColor()
	{
		return _titleColor;
	}
	
	public String getTitle()
	{
		return _title;
	}
	
	public void setNextCheck(int value)
	{
		_nextCheck = value;
	}
	
	public int getLimit()
	{
		return _nextCheck;
	}
	
	public void setLast()
	{
		_isLast = true;
	}
	
	public boolean isLast()
	{
		return _isLast;
	}
}