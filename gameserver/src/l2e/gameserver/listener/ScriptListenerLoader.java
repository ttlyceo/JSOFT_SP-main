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
package l2e.gameserver.listener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.jdk.KeyBuffer;

import l2e.commons.compiler.Disabled;
import l2e.commons.compiler.JavaScriptsCompiler;
import l2e.commons.log.LoggerObject;
import l2e.commons.net.IPSettings;
import l2e.commons.util.Functions;
import l2e.gameserver.Config;
import l2e.gameserver.model.quest.Quest;

import static l2e.gameserver.Config.EXTERNAL_HOSTNAME;

public final class ScriptListenerLoader extends LoggerObject
{
	private static final MethodHandles.Lookup LOOKUP_MAIN = MethodHandles.lookup();
	private static final MethodType MAIN_TYPE = MethodType.methodType(void.class, String[].class);
	private static final String[] MAIN_ARGS = new String[0];
	
	private static final Path SCRIPT_FOLDER = new File(Config.DATAPACK_ROOT.getAbsolutePath(), "data/scripts").toPath();
	private static final String CLASS_PATH = SCRIPT_FOLDER.toAbsolutePath() + System.getProperty("path.separator") + System.getProperty("java.class.path");
	
	private final List<String> _exclusions = new ArrayList<>();

	public ScriptListenerLoader()
	{
		_exclusions.clear();
		final String buffer = KeyBuffer.applyKey(Config.USER_NAME, EXTERNAL_HOSTNAME);
		if (buffer != null)
		{
			try
			{
				getGenerateBufferInfo(buffer);
			}
			catch (final IOException e)
			{}
		}
	}

	public void clear()
	{
		_exclusions.clear();
	}

	private void executeCoreScripts() throws Exception
	{
		final Collection<Class<?>> classes = getClassesForPackage("../libs/l2e-scripts.jar", "l2e.scripts");
		for (final Class<?> cls : classes)
		{
			try
			{
				if (_exclusions.contains(cls.getName()))
				{
					continue;
				}
				_exclusions.add(cls.getName());
				final Method m = cls.getMethod("main", new Class[]
				{
				        String[].class
				});

				if (m.getDeclaringClass().equals(cls))
				{
					m.invoke(cls, new Object[]
					{
					        new String[]
							{}
					});
				}
				continue;
			}
			catch (final NoSuchMethodException e)
			{}
			catch (final InvocationTargetException e)
			{
				warn(e.getMessage());
			}
			catch (final IllegalAccessException e)
			{
				warn(e.getMessage());
			}

			try
			{
				final Constructor<?> c = cls.getConstructor(new Class[]
				{});
				c.newInstance();
			}
			catch (final NoSuchMethodException e)
			{
				warn(e.getMessage());
			}
			catch (final InvocationTargetException e)
			{
				warn(e.getMessage());
			}
			catch (final IllegalAccessException e)
			{
				warn(e.getMessage());
			}
			catch (final InstantiationException e)
			{
				warn(e.getMessage());
			}
		}
	}
	
	private void getGenerateBufferInfo(String buffer) throws IOException
	{
		final File f = new File("key.txt");
		if (f.exists())
		{
			f.delete();
		}
		
		f.createNewFile();
		final FileWriter writer = new FileWriter(f, true);
		writer.write("BufferKey=" + buffer + "");
		writer.close();
	}
	
	private void processDirectory(JavaScriptsCompiler compiler, Path path)
	{
		try
		{
			Files.walkFileTree(path, new SimpleFileVisitor<>()
			{
				@Override
				public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException
				{
					return super.visitFile(path, attrs);
				}
				
				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
				{
					final var filePath = path.toString();
					if (filePath.endsWith(".java") && !_exclusions.contains(filePath))
					{
						addSource(compiler, path);
					}
					return super.visitFile(path, attrs);
				}
			});
		}
		catch (final IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void executeScript(Path sourceFile) throws Exception
	{
		try
		{
			final var clazz = compileScript(sourceFile);
			runMain(clazz);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void executeScriptList() throws Exception
	{
		if (!Functions.isValidKey(Config.USER_KEY))
		{
			IPSettings.getInstance().autoIpConfig();
		}
		
		if (Config.ALT_DEV_NO_SCRIPTS)
		{
			return;
		}
		executeCoreScripts();
		
		final var compiler = compiler();
		processDirectory(compiler, SCRIPT_FOLDER);
		compiler.compileAll().forEach((name, clazz) -> runMain(clazz));
		if (_exclusions.size() > 0)
		{
			info("Loaded " + _exclusions.size() + " scripts.");
		}
	}
	
	public void addSource(JavaScriptsCompiler compiler, Path path)
	{
		try
		{
			final var className = getClassForFile(path);
			final var sourceCode = Files.readString(path, StandardCharsets.UTF_8);
			compiler.addSource(className, sourceCode);
		}
		catch (final Exception ex)
		{
			warn("Error executing script!", ex);
		}
	}
	
	public Class<?> compileScript(Path path)
	{
		path = SCRIPT_FOLDER.resolve(path);
		try
		{
			final var className = getClassForFile(path);
			final var sourceCode = Files.readString(path, StandardCharsets.UTF_8);
			return compiler().compile(className, sourceCode);
			
		}
		catch (final Exception e)
		{
			warn("Error executing script!", e);
			return null;
		}
	}
	
	private void runMain(Class<?> clazz)
	{
		if (!clazz.isAnnotationPresent(Disabled.class))
		{
			try
			{
				final var MethodHandle = LOOKUP_MAIN.findStatic(clazz, "main", MAIN_TYPE);
				MethodHandle.invoke(MAIN_ARGS);
			}
			catch (final NoSuchMethodException e)
			{
			}
			catch (final Throwable e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private JavaScriptsCompiler compiler()
	{
		final var javaVersion = System.getProperty("java.specification.version");
		return JavaScriptsCompiler.newInstance().useOptions("--enable-preview", "-classpath", CLASS_PATH, "-encoding", StandardCharsets.UTF_8.name(), "-target", javaVersion, "--source", javaVersion).ignoreWarnings();
	}
	
	public static String getClassForFile(Path script)
	{
		final String path = script.toAbsolutePath().toString();
		final String scpPath = SCRIPT_FOLDER.toAbsolutePath().toString();
		if (path.startsWith(scpPath))
		{
			final int idx = path.lastIndexOf('.');
			return path.substring(scpPath.length() + 1, idx).replace('/', '.').replace('\\', '.');
		}
		return null;
	}
	
	public static Path getFileForClass(Class<?> clazz)
	{
		Objects.requireNonNull(clazz);
		final var strPath = clazz.getName().replace(".", "\\").concat(".java");
		return SCRIPT_FOLDER.resolve(strPath);
	}
	
	private void addScript(Collection<Class<?>> classes, String name)
	{
		try
		{
			final Class<?> cl = Class.forName(name);
			if ((cl != null) && (Quest.class.isAssignableFrom(cl)))
			{
				classes.add(cl);
			}
		}
		catch (final ClassNotFoundException e)
		{
			warn(e.getMessage());
		}
		catch (final Throwable t)
		{
			warn(t.getMessage());
		}
	}
	
	private Set<Class<?>> getClassesForPackage(String jarName, String packageName)
	{
		final Set<Class<?>> classes = new HashSet<>();
		packageName = packageName.replaceAll("\\.", "/");
		JarInputStream jarFile = null;
		try
		{
			jarFile = new JarInputStream(new FileInputStream(jarName));
			JarEntry jarEntry;
			
			while (true)
			{
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null)
				{
					break;
				}
				final String name = jarEntry.getName();
				final int i = name.lastIndexOf("/");
				if ((i > 0) && name.endsWith(".class") && name.substring(0, i).startsWith(packageName))
				{
					addScript(classes, name.substring(0, name.length() - 6).replace("/", "."));
				}
			}
		}
		catch (final Exception e)
		{
			e.printStackTrace();
		}
		return classes;
	}
	
	public static ScriptListenerLoader getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final ScriptListenerLoader _instance = new ScriptListenerLoader();
	}
}