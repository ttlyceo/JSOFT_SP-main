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
package l2e.gameserver.model.service.buffer;

public class RequestItems
{
	private final int[][] _requestItems;
	private final boolean _needAllItems;
	private final boolean _removeItems;
	private final boolean _isBuffForItems;
	
	public RequestItems(int[][] requestItems, boolean needAllItems, boolean removeItems)
	{
		_requestItems = requestItems;
		_needAllItems = needAllItems;
		_removeItems = removeItems;
		_isBuffForItems = _requestItems != null;
	}
	
	public int[][] getRequestItems()
	{
		return _requestItems;
	}
	
	public boolean needAllItems()
	{
		return _needAllItems;
	}
	
	public boolean isBuffForItems()
	{
		return _isBuffForItems;
	}
	
	public boolean isRemoveItems()
	{
		return _removeItems;
	}
}