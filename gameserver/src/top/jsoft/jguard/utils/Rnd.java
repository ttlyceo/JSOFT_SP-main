package top.jsoft.jguard.utils;

import java.util.Random;

/**
 * @author Akumu
 * @date 22.01.14
 */
public class Rnd
{
	private static final Random _rnd = new Random();

	/**
	 * @return random byte
	 */
	public static byte get()
	{
		return (byte)get(255);
	}

	/**
	 *
	 * @param max maximum value of generated int
	 * @return random int between 0 and max
	 */
	public static int get(int max)
	{
		return get(0, max);
	}

	/**
	 *
	 * @param min minimum value of generated int
	 * @param max maximum value of generated int
	 * @return random int between min and max
	 */
	public static int get(int min, int max)
	{
   		return _rnd.nextInt(max - min) + min;
	}
}
