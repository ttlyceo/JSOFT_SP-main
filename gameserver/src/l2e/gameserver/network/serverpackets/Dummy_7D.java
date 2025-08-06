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
package l2e.gameserver.network.serverpackets;

public class Dummy_7D extends GameServerPacket
{
	private final ServerRequest _serverRequest;
	private String _drawText;
	private String _url;
	private WarnWindowType _warnWindowType;

	public Dummy_7D(final String drawText)
	{
		_serverRequest = ServerRequest.SC_SERVER_REQUEST_SET_DRAW_TEXT;
		_drawText = drawText;
	}

	public Dummy_7D(final String strValue, ServerRequest serverRequest)
	{
		_serverRequest = serverRequest;
		if(serverRequest == ServerRequest.SC_SERVER_REQUEST_OPEN_URL)
		{
			_url = strValue;
		}
		else if(serverRequest == ServerRequest.SC_SERVER_REQUEST_SET_DRAW_TEXT)
		{
			_drawText = strValue;
		}
	}

	public Dummy_7D(final WarnWindowType warnWindowType, final String warnMessage)
	{
		_serverRequest = ServerRequest.SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE;
		_warnWindowType = warnWindowType;
		_drawText = warnMessage;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(_serverRequest.ordinal());
		switch (_serverRequest)
		{
			case SC_SERVER_REQUEST_SET_DRAW_TEXT:
				writeS(_drawText);
				break;
			case SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE:
				writeC(_warnWindowType.ordinal());
				writeS(_drawText);
				break;
			case SC_SERVER_REQUEST_OPEN_URL:
				writeS(_url);
				break;
		}
	}

	public static enum ServerRequest
	{
		SC_SERVER_REQUEST_SET_DRAW_TEXT,
		SC_SERVER_REQUEST_SHOW_CUSTOM_WARN_MESSAGE,
		SC_SERVER_REQUEST_OPEN_URL,
	}

	public static enum WarnWindowType
	{
		UL2CW_DEFAULT,
		UL2CW_CLOSE_WINDOW,
	}
}