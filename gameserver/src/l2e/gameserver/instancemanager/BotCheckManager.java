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
package l2e.gameserver.instancemanager;

import java.io.File;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.Rnd;
import l2e.gameserver.Config;
import l2e.gameserver.ThreadPoolManager;
import l2e.gameserver.model.GameObjectsStorage;
import l2e.gameserver.model.World;
import l2e.gameserver.model.entity.events.cleft.AerialCleftEvent;
import l2e.gameserver.model.zone.ZoneId;

public class BotCheckManager extends LoggerObject
{
	public CopyOnWriteArrayList<BotCheckQuestion> _questions = new CopyOnWriteArrayList<>();
	
	protected BotCheckManager()
	{
		Document doc = null;
		final var file = new File(Config.DATAPACK_ROOT, "data/stats/chars/botQuestions.xml");
		if (!file.exists())
		{
			warn("botQuestions.xml file is missing.");
			return;
		}

		try
		{
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(false);
			factory.setIgnoringComments(true);
			doc = factory.newDocumentBuilder().parse(file);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}

		try
		{
			parseBotQuestions(doc);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}

	protected void parseBotQuestions(Document doc)
	{
		for (var n = doc.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (var d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("question".equalsIgnoreCase(d.getNodeName()))
					{
						final int id = Integer.parseInt(d.getAttributes().getNamedItem("id").getNodeValue());
						final String question_ru = d.getAttributes().getNamedItem("question_ru").getNodeValue();
						final String question_en = d.getAttributes().getNamedItem("question_en").getNodeValue();
						final boolean answer = Integer.parseInt(d.getAttributes().getNamedItem("answer").getNodeValue()) == 0 ? true : false;

						final var question_info = new BotCheckQuestion(id, question_ru, question_en, answer);
						_questions.add(question_info);
					}
				}
			}
		}
		info("Loaded " + _questions.size() + " bot questions.");
		ScheduleNextQuestion();
	}

	public class BotCheckQuestion
	{
		public final int _id;
		public final String _ruDescr;
		public final String _enDescr;
		public final boolean _answer;

		public BotCheckQuestion(int id, String ruDescr, String enDescr, boolean answer)
		{
			_id = id;
			_ruDescr = ruDescr;
			_enDescr = enDescr;
			_answer = answer;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getDescr(String lang)
		{
			return (lang != null && !lang.equalsIgnoreCase("en")) ? _ruDescr : _enDescr;
		}

		public boolean getAnswer()
		{
			return _answer;
		}
	}

	public CopyOnWriteArrayList<BotCheckQuestion> getAllAquisions()
	{
		if (_questions == null)
		{
			return null;
		}
		return _questions;
	}
	
	public boolean checkAnswer(int qId, boolean answer)
	{
		
		for (final var info : _questions)
		{
			if (info._id == qId)
			{
				return info.getAnswer() == answer;
			}
		}
		return true;
	}
	
	public BotCheckQuestion generateRandomQuestion()
	{
		return _questions.get(Rnd.get(0, _questions.size() - 1));
	}
	
	private void ScheduleNextQuestion()
	{
		ThreadPoolManager.getInstance().schedule(new BotQuestionAsked(), Rnd.get(Config.MINIMUM_TIME_QUESTION_ASK * 60000, Config.MAXIMUM_TIME_QUESTION_ASK * 60000));
	}
	
	private class BotQuestionAsked implements Runnable
	{
		@Override
		public void run()
		{
			for (final var player : GameObjectsStorage.getPlayers())
			{
				if (player == null)
				{
					continue;
				}
				
				if (player.isFakePlayer() || player.getFarmSystem().isAutofarming())
				{
					continue;
				}
				
				if (player.isInTownZone() || player.isInKrateisCube() || player.getUCState() > 0 || player.checkInTournament() || player.isInFightEvent() || player.inObserverMode() || player.isInsideZone(ZoneId.PVP) || player.isInSiege() || player.isInDuel() || player.isInStoreMode() || player.isInOfflineMode() || player.getPvpFlag() != 0 || player.isInOlympiadMode())
				{
					continue;
				}

				if (((AerialCleftEvent.getInstance().isStarted() || AerialCleftEvent.getInstance().isRewarding()) && AerialCleftEvent.getInstance().isPlayerParticipant(player.getObjectId())))
				{
					continue;
				}

				if (player.getBotRating() > Rnd.get(Config.MINIMUM_BOT_POINTS_TO_STOP_ASKING, Config.MAXIMUM_BOT_POINTS_TO_STOP_ASKING))
				{
					continue;
				}
				
				for (final var mob : World.getInstance().getAroundNpc(player, 1000, 600))
				{
					if (mob.isMonster())
					{
						player.requestCheckBot();
						break;
					}
				}
			}
			ScheduleNextQuestion();
		}
	}

	public static BotCheckManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final BotCheckManager _instance = new BotCheckManager();
	}
}