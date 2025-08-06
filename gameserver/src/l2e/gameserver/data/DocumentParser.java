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
package l2e.gameserver.data;

import java.io.File;
import java.io.FileFilter;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import l2e.commons.log.LoggerObject;
import l2e.commons.util.file.filter.XMLFilter;
import l2e.gameserver.Config;

public abstract class DocumentParser extends LoggerObject
{
	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

	private static final XMLFilter xmlFilter = new XMLFilter();

	private File _currentFile;

	private Document _currentDocument;

	private FileFilter _currentFilter = null;

	public abstract void load();

	protected void parseDatapackFile(String path)
	{
		final var oriPath = path;
		if (!Config.SERVER_STAGE.isEmpty())
		{
			if (path.contains(".xml"))
			{
				path = path.replace(".xml", "-" + Config.SERVER_STAGE + ".xml");
				final var file = new File(Config.DATAPACK_ROOT, path);
				if (file.exists())
				{
					parseFile(file, false);
					return;
				}
			}
		}
		parseFile(new File(Config.DATAPACK_ROOT, oriPath), false);
	}

	protected void parseFile(File f, boolean isReload)
	{
		if (!getCurrentFileFilter().accept(f))
		{
			if (Config.DEBUG)
			{
				warn("Could not parse " + f.getName() + " is not a file or it doesn't exist!");
			}
			return;
		}

		final var dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(true);
		dbf.setIgnoringComments(true);
		_currentDocument = null;
		_currentFile = f;
		try
		{
			dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			final var db = dbf.newDocumentBuilder();
			db.setErrorHandler(new XMLErrorHandler());
			_currentDocument = db.parse(f);
		}
		catch (final Exception e)
		{
			warn("Could not parse " + f.getName() + " file: " + e.getMessage());
			return;
		}
		
		if (isReload)
		{
			reloadDocument();
		}
		else
		{
			parseDocument();
		}
	}

	public File getCurrentFile()
	{
		return _currentFile;
	}

	protected Document getCurrentDocument()
	{
		return _currentDocument;
	}

	protected boolean parseDirectory(File file)
	{
		return parseDirectory(file, false, false);
	}

	protected boolean parseDirectory(String path, boolean isReload)
	{
		if (!Config.SERVER_STAGE.isEmpty())
		{
			final var file = new File(Config.DATAPACK_ROOT, path + "-" + Config.SERVER_STAGE);
			if (file.exists())
			{
				return parseDirectory(file, false, isReload);
			}
		}
		return parseDirectory(new File(path), false, isReload);
	}

	protected boolean parseDirectory(String path, boolean recursive, boolean isReload)
	{
		return parseDirectory(new File(path), recursive, isReload);
	}

	protected boolean parseDirectory(File dir, boolean recursive, boolean isReload)
	{
		if (!dir.exists())
		{
			if (Config.DEBUG)
			{
				warn("Folder " + dir.getAbsolutePath() + " doesn't exist!");
			}
			return false;
		}

		final File[] listOfFiles = dir.listFiles();
		for (final var f : listOfFiles)
		{
			if (recursive && f.isDirectory())
			{
				parseDirectory(f, recursive, isReload);
			}
			else if (getCurrentFileFilter().accept(f))
			{
				parseFile(f, isReload);
			}
		}
		return true;
	}
	
	protected void parseDocument(Document doc)
	{
	}

	protected abstract void parseDocument();
	protected abstract void reloadDocument();

	protected static int parseInt(NamedNodeMap n, String name)
	{
		return Integer.parseInt(n.getNamedItem(name).getNodeValue());
	}

	protected static Integer parseInteger(NamedNodeMap n, String name)
	{
		return Integer.valueOf(n.getNamedItem(name).getNodeValue());
	}

	protected static int parseInt(Node n)
	{
		return Integer.parseInt(n.getNodeValue());
	}

	protected static Integer parseInteger(Node n)
	{
		return Integer.valueOf(n.getNodeValue());
	}

	protected Integer parseInteger(NamedNodeMap attrs, String name, Integer defaultValue)
	{
		return parseInteger(attrs.getNamedItem(name), defaultValue);
	}

	protected Integer parseInteger(Node node, Integer defaultValue)
	{
		return node != null ? Integer.valueOf(node.getNodeValue()) : defaultValue;
	}

	protected static Long parseLong(NamedNodeMap n, String name)
	{
		return Long.valueOf(n.getNamedItem(name).getNodeValue());
	}

	protected static float parseFloat(NamedNodeMap n, String name)
	{
		return Float.parseFloat(n.getNamedItem(name).getNodeValue());
	}

	protected static Double parseDouble(NamedNodeMap n, String name)
	{
		return Double.valueOf(n.getNamedItem(name).getNodeValue());
	}

	protected static boolean parseBoolean(NamedNodeMap n, String name)
	{
		final var b = n.getNamedItem(name);
		return (b != null) && Boolean.parseBoolean(b.getNodeValue());
	}
	
	protected static boolean parseBoolean(Node node, Boolean defaultValue)
	{
		return node != null ? Boolean.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	protected static boolean parseBoolean(Node node)
	{
		return parseBoolean(node, null);
	}
	
	protected static boolean parseBoolean(NamedNodeMap attrs, String name, Boolean defaultValue)
	{
		return parseBoolean(attrs.getNamedItem(name), defaultValue);
	}

	protected static String parseString(NamedNodeMap n, String name)
	{
		final var b = n.getNamedItem(name);
		return (b == null) ? "" : b.getNodeValue();
	}
	
	protected static <T extends Enum<T>> T parseEnum(Node node, Class<T> clazz, T defaultValue)
	{
		if (node == null)
		{
			return defaultValue;
		}
		
		try
		{
			return Enum.valueOf(clazz, node.getNodeValue());
		}
		catch (final IllegalArgumentException e)
		{
			return defaultValue;
		}
	}
	
	protected static <T extends Enum<T>> T parseEnum(Node node, Class<T> clazz)
	{
		return parseEnum(node, clazz, null);
	}
	
	protected static <T extends Enum<T>> T parseEnum(NamedNodeMap attrs, Class<T> clazz, String name)
	{
		return parseEnum(attrs.getNamedItem(name), clazz);
	}
	
	protected static <T extends Enum<T>> T parseEnum(NamedNodeMap attrs, Class<T> clazz, String name, T defaultValue)
	{
		return parseEnum(attrs.getNamedItem(name), clazz, defaultValue);
	}

	public void setCurrentFileFilter(FileFilter filter)
	{
		_currentFilter = filter;
	}

	public FileFilter getCurrentFileFilter()
	{
		return _currentFilter != null ? _currentFilter : xmlFilter;
	}

	protected class XMLErrorHandler implements ErrorHandler
	{
		@Override
		public void warning(SAXParseException e) throws SAXParseException
		{
			throw e;
		}

		@Override
		public void error(SAXParseException e) throws SAXParseException
		{
			throw e;
		}

		@Override
		public void fatalError(SAXParseException e) throws SAXParseException
		{
			throw e;
		}
	}
}