package top.jsoft.jguard.utils.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author Akumu
 * @date 10.01.14
 */
public final class LogFormatter extends Formatter
{
	private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

	@Override
	public String format(LogRecord record)
	{
		StringBuilder sb = new StringBuilder();

        // date
        sb.append("[").append(dateFormat.format(new Date(record.getMillis()))).append("]");

        // type
        sb.append("[").append(record.getLevel().getLocalizedName()).append("]");

        // message
		sb.append(" ").append(formatMessage(record)).append(LINE_SEPARATOR);

        // exception
		if (record.getThrown() != null)
		{
			try
			{
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				record.getThrown().printStackTrace(pw);
				pw.close();
				sb.append(sw.toString());
			}
			catch (Exception ex) {}
		}

		return sb.toString();
	}
}
