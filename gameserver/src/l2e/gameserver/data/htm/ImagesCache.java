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
package l2e.gameserver.data.htm;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import l2e.commons.files.FilenameUtils;
import l2e.commons.log.LoggerObject;
import l2e.gameserver.Config;
import l2e.gameserver.data.holder.CrestHolder;
import l2e.gameserver.idfactory.IdFactory;
import l2e.gameserver.model.actor.Player;
import l2e.gameserver.model.entity.mods.DDSConverter;
import l2e.gameserver.network.serverpackets.pledge.PledgeCrest;

public class ImagesCache extends LoggerObject
{
	private static final int[] SIZES =
	{
	        1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024
	};
	private static final int MAX_SIZE = SIZES[(SIZES.length - 1)];
	public static final Pattern HTML_PATTERN = Pattern.compile("%image:(.*?)%", 32);
	
	private final Map<Integer, byte[]> _images = new HashMap<>();
	private final Map<String, Integer> _imagesId = new HashMap<>();
	
	public ImagesCache()
	{
		loadImages();
	}
	
	private void loadImages()
	{
		final Map<Integer, File> imagesToLoad = getImagesToLoad();
		
		for (final Map.Entry<Integer, File> image : imagesToLoad.entrySet())
		{
			final var file = image.getValue();
			final byte[] data = DDSConverter.convertToDDS(file).array();
			_images.put(image.getKey(), data);
			_imagesId.put(file.getName().toLowerCase(), image.getKey());
		}
		
		info("Loaded " + imagesToLoad.size() + " images!");
	}
	
	private Map<Integer, File> getImagesToLoad()
	{
		final Map<Integer, File> files = new HashMap<>();
		
		final var folder = new File(Config.DATAPACK_ROOT + "/data/images");
		if (!folder.exists())
		{
			warn("Path \"./data/images\" doesn't exist!");
			return files;
		}
		
		for (final var file : folder.listFiles())
		{
			for (var newFile : (file.isDirectory() ? file.listFiles() : new File[]
			{
			        file
			}))
			{
				if (checkImageFormat(newFile))
				{
					newFile = resizeImage(newFile);
					
					var id = -1;
					try
					{
						final var name = FilenameUtils.getBaseName(newFile.getName());
						id = Integer.parseInt(name);
					}
					catch (final Exception e)
					{
						id = IdFactory.getInstance().getNextId();
					}
					
					if (id != -1)
					{
						files.put(id, newFile);
					}
				}
			}
		}
		
		return files;
	}
	
	private File resizeImage(File file)
	{
		BufferedImage image;
		try
		{
			image = ImageIO.read(file);
		}
		catch (final IOException e)
		{
			warn("Error while resizing " + file.getName() + " image.", e);
			return null;
		}
		
		if (image == null)
		{
			return null;
		}
		final var width = image.getWidth();
		final var height = image.getHeight();
		
		var resizeWidth = true;
		if (width > MAX_SIZE)
		{
			image = image.getSubimage(0, 0, MAX_SIZE, height);
			resizeWidth = false;
		}
		
		var resizeHeight = true;
		if (height > MAX_SIZE)
		{
			image = image.getSubimage(0, 0, width, MAX_SIZE);
			resizeHeight = false;
		}
		
		var resizedWidth = width;
		if (resizeWidth)
		{
			for (final var size : SIZES)
			{
				if (size >= width)
				{
					resizedWidth = size;
					break;
				}
			}
		}
		int resizedHeight = height;
		if (resizeHeight)
		{
			for (final var size : SIZES)
			{
				if (size >= height)
				{
					resizedHeight = size;
					break;
				}
			}
		}
		if ((resizedWidth != width) || (resizedHeight != height))
		{
			for (int x = 0; x < resizedWidth; x++)
			{
				for (int y = 0; y < resizedHeight; y++)
				{
					image.setRGB(x, y, Color.BLACK.getRGB());
				}
			}
			final var filename = file.getName();
			final var format = filename.substring(filename.lastIndexOf("."));
			try
			{
				ImageIO.write(image, format, file);
			}
			catch (final IOException e)
			{
				warn("Error while resizing " + file.getName() + " image.", e);
				return null;
			}
		}
		return file;
	}
	
	public void sendImageToPlayer(Player player, int imageId)
	{
		if (!Config.ALLOW_SENDING_IMAGES)
		{
			return;
		}
		
		if (player.wasImageLoaded(imageId))
		{
			return;
		}
		
		if (_images.containsKey(imageId))
		{
			player.addLoadedImage(imageId);
			player.sendPacket(new PledgeCrest(imageId, _images.get(imageId)));
			return;
		}
		
		final var crest = CrestHolder.getInstance().getCrest(imageId);
		if (crest != null)
		{
			player.addLoadedImage(imageId);
			player.sendPacket(new PledgeCrest(imageId, crest.getData()));
		}
	}

	private static boolean checkImageFormat(File file)
	{
		final var filename = file.getName();
		final var dotPos = filename.lastIndexOf(".");
		final var format = filename.substring(dotPos);
		if (format.equalsIgnoreCase(".jpg") || format.equalsIgnoreCase(".png") || format.equalsIgnoreCase(".bmp"))
		{
			return true;
		}
		return false;
	}
	
	public static ImagesCache getInstance()
	{
		return ImagesCacheHolder.instance;
	}
	
	private static class ImagesCacheHolder
	{
		protected static final ImagesCache instance = new ImagesCache();
	}
}
