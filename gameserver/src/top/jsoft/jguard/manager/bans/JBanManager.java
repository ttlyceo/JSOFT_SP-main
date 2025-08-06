package top.jsoft.jguard.manager.bans;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import top.jsoft.jguard.JGuardConfig;
import top.jsoft.jguard.manager.bans.model.JBan;
import top.jsoft.jguard.manager.session.JClientSessionManager;
import top.jsoft.jguard.manager.session.model.HWID;
import top.jsoft.jguard.manager.session.model.JClientSession;
import top.jsoft.jguard.model.TimedObject;
import top.jsoft.jguard.utils.log.GuardLog;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.security.InvalidParameterException;
import java.util.*;

/**
 * @author Akumu
 * @date 30.11.13
 */
public class JBanManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JBanManager.class);
	private static final HashMap<HWID, JBan> _bans = new HashMap<HWID, JBan>(100);
    private static final List<TimedObject<JBan>> _delayedBans = new ArrayList<TimedObject<JBan>>(100);
    private static final File banlistFile = new File(JGuardConfig.JGUARD_DIR, "bans.xml");

    private static long _lastStore = 0L;

    private static class BanWorker extends TimerTask
    {
        @Override
        public void run()
        {
            // отложенные баны
            try {
                synchronized (_delayedBans)
                {
                    Iterator<TimedObject<JBan>> it = _delayedBans.iterator();
                    while(it.hasNext())
                    {
                        TimedObject<JBan> to = it.next();

                        // пришло время забанить
                        if (to.time <= System.currentTimeMillis()) {
                            JBanManager.addBan(to.value, true);
                            it.remove();
                        }
                    }
                }
            }
            catch (Exception e)
            {
                GuardLog.logException(e);
            }

            // апдейт и анбан
            try {
                synchronized (_bans)
                {
                    boolean needsStore = false;

                    Iterator<Map.Entry<HWID, JBan>> it = _bans.entrySet().iterator();

                    while(it.hasNext())
                    {
                        Map.Entry<HWID, JBan> en = it.next();
                        JBan b = en.getValue();

                        if(b._lastUpdate > _lastStore)
                            needsStore = true;

                        if(b.isExpired()) {
                            LOGGER.info(String.format("Ban '%s' has expired and will be removed.", b));
                            it.remove();
                            needsStore = true;
                        }
                    }

                    if(needsStore)
                        store();
                }
            }
            catch (Exception e)
            {
                GuardLog.logException(e);
            }
        }
    }

    static
	{
        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new BanWorker(), 30000, 30000);

		reload();
		store();
	}

	public static Collection<JBan> getBans()
	{
        synchronized (_bans) {
            Set<JBan> res = new HashSet<JBan>(_bans.size());
            res.addAll(_bans.values());
            return res;
        }
	}

    public static JBan getBan(String hwid)
    {
        HWID hw = HWID.fromString(hwid);

        if(hw == null)
            return null;

        return getBan(hw);
    }

    public static JBan getBan(HWID hwid)
    {
    	JBan ban = null;

        synchronized (_bans) {

            for (Map.Entry<HWID, JBan> en : _bans.entrySet()) {
                HWID h = en.getKey();
                if (h.equalsForBan(hwid)) {
                    ban = en.getValue();
                    break;
                }
            }
        }

        if(ban != null)
        {
            if(ban.isExpired())
            {
                LOGGER.info(String.format("[BanManager]: Ban has expired and was removed: %s", ban));
                removeBan(ban.hwid);
                return null;
            }
        }

        return ban;
    }

    public static void addBan(JBan ban)
    {
        addBan(ban, JGuardConfig.BanlistAccountBan);
    }

	public static void addBan(JBan ban, boolean kickSession)
	{
        if(ban == null)
            return;

        synchronized (_bans) {
            _bans.put(ban.hwid, ban);
        }

        if(kickSession)
        {
        	JClientSession session = JClientSessionManager.getSession(ban.hwid);

            if(session != null)
                session.disconnect();
        }

        store();
	}

    public static void addDelayedBan(JBan ban, long delay)
    {
        if(ban == null)
            return;

        if(delay <= 0)
            throw new InvalidParameterException("Delay must be positive");

        synchronized (_delayedBans)
        {
            _delayedBans.add(new TimedObject<JBan>(ban, delay));
        }
    }

	public static void removeBan(String hwid)
	{
		HWID hw = HWID.fromString(hwid);

		if(hw == null)
			return;

		removeBan(hw);
	}

	public static void removeBan(HWID hwid)
	{
		if(hwid == null)
			return;

		JBan ban;

        synchronized (_bans) {
            ban = _bans.remove(hwid);
        }

		if(ban != null)
			store();
	}

    public static JBan checkAccount(String account)
    {
        if(account == null)
            return null;

        final String acc = account.toLowerCase();

        synchronized (_bans) {
            for (JBan ban : _bans.values())
                if (ban.findAccount(acc))
                    return ban;
        }

        return null;
    }

	public static boolean checkBan(String hwid)
	{
		HWID hw = HWID.fromString(hwid);
		return hw != null && checkBan(hw);
	}

	public static boolean checkBan(HWID hwid)
	{
		JBan ban = getBan(hwid);
        return ban != null;
    }

	public static void reload()
	{
		try
		{
            if(!banlistFile.exists())
                return;

            LOGGER.info("Reload ban list...");
            _bans.clear();


            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document doc = docBuilder.parse(banlistFile);

            NodeList nList = doc.getElementsByTagName("ban");

            for (int temp = 0; temp < nList.getLength(); temp++)
            {
                Node nNode = nList.item(temp);

                if (nNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                JBan ban = JBan.readFromXML((Element)nNode);

                if(ban != null) {
                    synchronized (_bans) {
                        _bans.put(ban.hwid, ban);
                    }
                }
            }
		}
		catch (Exception e)
		{
            LOGGER.error("Error reloading ban list!");
			GuardLog.logException(e);
		}
	}

	public static void store()
	{
        synchronized (_bans) {
            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

                Document doc = docBuilder.newDocument();
                Element rootElement = doc.createElement("banlist");
                doc.appendChild(rootElement);

                for (JBan b : _bans.values()) {
                    Element ban = doc.createElement("ban");

                    Attr hwid = doc.createAttribute("hwid");
                    hwid.setValue(b.hwid.plain);
                    ban.setAttributeNode(hwid);

                    Attr type = doc.createAttribute("type");
                    type.setValue(b.type.toString());
                    ban.setAttributeNode(type);
                    
                    Attr GMNAME = doc.createAttribute("gmname");
                    GMNAME.setValue(b.GMNAME.toString());
                    ban.setAttributeNode(GMNAME);

                    Attr time = doc.createAttribute("time");
                    time.setValue(String.valueOf(b.time));
                    ban.setAttributeNode(time);

                    if (b.type == JBan.Type.TEMP) {
                        Element endtime = doc.createElement("end_time");
                        endtime.appendChild(doc.createTextNode(String.valueOf(b.bannedUntil)));
                        ban.appendChild(endtime);
                    }

                    if (b.gmObjId > 0) {
                        Element gm = doc.createElement("gmObjId");
                        gm.appendChild(doc.createTextNode(String.valueOf(b.gmObjId)));
                        ban.appendChild(gm);
                    }

                    if (b.sessionAccounts.size() > 0) {
                        Element accounts = doc.createElement("accounts");

                        for (TimedObject<String> acc : b.sessionAccounts) {
                            if (acc == null)
                                continue;

                            Element account = doc.createElement("account");
                            account.appendChild(doc.createTextNode(acc.value.trim()));

                            Attr addtime = doc.createAttribute("time");
                            addtime.setValue(String.valueOf(acc.time));
                            account.setAttributeNode(addtime);

                            accounts.appendChild(account);
                        }

                        ban.appendChild(accounts);
                    }

                    if (b.sessionIPs.size() > 0) {
                        Element ipv4 = doc.createElement("ipv4");

                        for (TimedObject<String> ipp : b.sessionIPs) {
                            if (ipp == null)
                                continue;

                            Element ip = doc.createElement("ip");
                            ip.appendChild(doc.createTextNode(ipp.value.trim()));

                            Attr addtime = doc.createAttribute("time");
                            addtime.setValue(String.valueOf(ipp.time));
                            ip.setAttributeNode(addtime);

                            ipv4.appendChild(ip);
                        }

                        ban.appendChild(ipv4);
                    }

                    if (b.comment != null) {
                        Element firstname = doc.createElement("comment");
                        firstname.appendChild(doc.createTextNode(b.comment));
                        ban.appendChild(firstname);
                    }

                    rootElement.appendChild(ban);
                }

                Comment com = doc.createComment(" JGuard banlist file ");
                doc.insertBefore(com, rootElement);

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty(OutputKeys.METHOD, "xml");
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(banlistFile);
                transformer.transform(source, result);
            } catch (Exception e) {
                LOGGER.error("Error saving ban list!");
                GuardLog.logException(e);
            }

            _lastStore = System.currentTimeMillis();
        }
	}
}
