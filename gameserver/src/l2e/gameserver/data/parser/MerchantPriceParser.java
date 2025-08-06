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
package l2e.gameserver.data.parser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.InstanceListManager;
import l2e.gameserver.instancemanager.CastleManager;
import l2e.gameserver.model.actor.instance.MerchantInstance;
import l2e.gameserver.model.entity.Castle;

public class MerchantPriceParser extends LoggerObject implements InstanceListManager
{
	public static MerchantPriceParser getInstance()
	{
		return SingletonHolder._instance;
	}

	private final Map<Integer, MerchantPrice> _mpcs = new HashMap<>();
	private MerchantPrice _defaultMpc;

	protected MerchantPriceParser()
	{
	}

	public MerchantPrice getMerchantPrice(MerchantInstance npc)
	{
		for (final MerchantPrice mpc : _mpcs.values())
		{
			if ((npc.getWorldRegion() != null) && npc.containsZone(mpc.getZoneId()))
			{
				return mpc;
			}
		}
		return _defaultMpc;
	}

	public MerchantPrice getMerchantPrice(int id)
	{
		return _mpcs.get(id);
	}

	public void loadXML() throws SAXException, IOException, ParserConfigurationException
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setIgnoringComments(true);
		final File file = new File(Config.DATAPACK_ROOT + "/data/stats/regions/merchantPrice.xml");
		if (file.exists())
		{
			int defaultPriceConfigId;
			final Document doc = factory.newDocumentBuilder().parse(file);

			Node n = doc.getDocumentElement();
			final Node dpcNode = n.getAttributes().getNamedItem("defaultPrice");
			if (dpcNode == null)
			{
				throw new IllegalStateException("merchantPrice must define an 'defaultPriceConfig'");
			}
			defaultPriceConfigId = Integer.parseInt(dpcNode.getNodeValue());

			MerchantPrice mpc;
			for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			{
				mpc = parseMerchantPrice(n);
				if (mpc != null)
				{
					_mpcs.put(mpc.getId(), mpc);
				}
			}

			final MerchantPrice defaultMpc = this.getMerchantPrice(defaultPriceConfigId);
			if (defaultMpc == null)
			{
				throw new IllegalStateException("'defaultPriceConfig' points to an non-loaded priceConfig");
			}
			_defaultMpc = defaultMpc;
		}
	}

	private MerchantPrice parseMerchantPrice(Node n)
	{
		if (n.getNodeName().equals("price"))
		{
			final int id;
			final int baseTax;
			int castleId = -1;
			int zoneId = -1;
			final String name;

			Node node = n.getAttributes().getNamedItem("id");
			if (node == null)
			{
				throw new IllegalStateException("Must define the price 'id'");
			}
			id = Integer.parseInt(node.getNodeValue());

			node = n.getAttributes().getNamedItem("name");
			if (node == null)
			{
				throw new IllegalStateException("Must define the price 'name'");
			}
			name = node.getNodeValue();

			node = n.getAttributes().getNamedItem("baseTax");
			if (node == null)
			{
				throw new IllegalStateException("Must define the price 'baseTax'");
			}
			baseTax = Integer.parseInt(node.getNodeValue());

			node = n.getAttributes().getNamedItem("castleId");
			if (node != null)
			{
				castleId = Integer.parseInt(node.getNodeValue());
			}

			node = n.getAttributes().getNamedItem("zoneId");
			if (node != null)
			{
				zoneId = Integer.parseInt(node.getNodeValue());
			}

			return new MerchantPrice(id, name, baseTax, castleId, zoneId);
		}
		return null;
	}

	@Override
	public void loadInstances()
	{
		try
		{
			loadXML();
			info("Loaded " + _mpcs.size() + " merchant price configs.");
		}
		catch (final Exception e)
		{
			warn("Failed loading. Reason: " + e.getMessage(), e);
		}
	}

	@Override
	public void updateReferences()
	{
		for (final MerchantPrice mpc : _mpcs.values())
		{
			mpc.updateReferences();
		}
	}

	@Override
	public void activateInstances()
	{
	}

	public static final class MerchantPrice
	{
		private final int _id;
		private final String _name;
		private final int _baseTax;
		private final int _castleId;
		private Castle _castle;
		private final int _zoneId;

		public MerchantPrice(final int id, final String name, final int baseTax, final int castleId, final int zoneId)
		{
			_id = id;
			_name = name;
			_baseTax = baseTax;
			_castleId = castleId;
			_zoneId = zoneId;
		}

		public int getId()
		{
			return _id;
		}

		public String getName()
		{
			return _name;
		}

		public int getBaseTax()
		{
			return _baseTax;
		}

		public double getBaseTaxRate()
		{
			return _baseTax / 100.0;
		}

		public Castle getCastle()
		{
			return _castle;
		}

		public int getZoneId()
		{
			return _zoneId;
		}

		public boolean hasCastle()
		{
			return getCastle() != null;
		}

		public double getCastleTaxRate()
		{
			return hasCastle() ? getCastle().getTaxRate() : 0.0;
		}

		public int getTotalTax()
		{
			return hasCastle() ? (getCastle().getTaxPercent() + getBaseTax()) : getBaseTax();
		}

		public double getTotalTaxRate()
		{
			return getTotalTax() / 100.0;
		}

		public void updateReferences()
		{
			if (_castleId > 0)
			{
				_castle = CastleManager.getInstance().getCastleById(_castleId);
			}
		}
	}

	private static class SingletonHolder
	{
		protected static final MerchantPriceParser _instance = new MerchantPriceParser();
	}
}