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
package l2e.gameserver.model.entity.mods.votereward;

import l2e.commons.dao.JdbcEntity;
import l2e.commons.dao.JdbcEntityState;
import l2e.gameserver.data.holder.VoteRewardHolder;

public class VoteRewardRecord implements JdbcEntity
{
	private static final long serialVersionUID = 8665903675445841610L;
	
	private final String _site;
	private final String _identifier;
	
	private int _votes;
	private int _lastVoteTime;
	
	private JdbcEntityState _jdbcEntityState = JdbcEntityState.CREATED;
	
	public VoteRewardRecord(String site, String identifier, int votes, int lastVoteTime)
	{
		_site = site;
		_identifier = identifier;
		_votes = votes;
		_lastVoteTime = lastVoteTime;
	}
	
	public String getSite()
	{
		return _site;
	}
	
	public String getIdentifier()
	{
		return _identifier;
	}
	
	public int getVotes()
	{
		return _votes;
	}
	
	public int getLastVoteTime()
	{
		return _lastVoteTime;
	}
	
	public void onReceiveReward(int votes, long voteTime)
	{
		_votes += votes;
		_lastVoteTime = (int) (voteTime / 1000);
		setJdbcState(JdbcEntityState.UPDATED);
		update();
	}
	
	@Override
	public void setJdbcState(JdbcEntityState state)
	{
		_jdbcEntityState = state;
	}
	
	@Override
	public JdbcEntityState getJdbcState()
	{
		return _jdbcEntityState;
	}
	
	@Override
	public void save()
	{
		VoteRewardHolder.getInstance().save(this);
	}
	
	@Override
	public void delete()
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void update()
	{
		VoteRewardHolder.getInstance().update(this);
	}
}
