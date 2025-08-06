package top.jsoft.jguard.manager.bans.model;


import l2e.gameserver.network.GameClient;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import top.jsoft.jguard.JGuardConfig;
import top.jsoft.jguard.manager.session.JClientSessionManager;
import top.jsoft.jguard.manager.session.model.HWID;
import top.jsoft.jguard.manager.session.model.JClientSession;
import top.jsoft.jguard.model.TimedObject;
import top.jsoft.jguard.utils.log.GuardLog;

import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.HashSet;
import java.util.Set;

/**
 * @autgor Breeze
 * @date 07.01.2015.
 */
public class JBan implements Serializable
{
    private static final long serialVersionUID = -1322322139926390329L;

    public enum Type
    {
        TEMP, NORMAL
    }

    public final HWID hwid;
    public final Type type;
    public final long bannedUntil, time;
    public final String comment;
    public final String GMNAME;

    // additional information
    public int gmObjId;
    public final Set<TimedObject<String>> sessionAccounts        = new HashSet<TimedObject<String>>(JGuardConfig.JGUARD_LIMIT_LOGIN > 0 ? JGuardConfig.JGUARD_LIMIT_LOGIN : 10);
    public final Set<TimedObject<String>> sessionIPs             = new HashSet<TimedObject<String>>(JGuardConfig.JGUARD_LIMIT_LOGIN > 0 ? JGuardConfig.JGUARD_LIMIT_LOGIN : 10);

    // internal
    public long _lastUpdate = 0L;

    
    
    public JBan(HWID hwid, String comment, String GMNAME)
    {
        this(hwid, Type.NORMAL, comment, 0L, 0L, GMNAME);
    }

    public JBan(HWID hwid, long bannedUntil, String comment, String GMNAME)
    {
        this(hwid, Type.TEMP, comment, bannedUntil, 0L, GMNAME);
    }
    
    
    /*
    public IOBan(HWID hwid, String comment)
    {
        this(hwid, Type.NORMAL, comment, 0L, 0L);
    }

    public IOBan(HWID hwid, long bannedUntil, String comment)
    {
        this(hwid, Type.TEMP, comment, bannedUntil, 0L);
    }*/

    private JBan(HWID hwid, Type type, String comment, long bannedUntil, long time, String GMNAME)
    {
        if(hwid == null)
            throw new InvalidParameterException("HWID can not be null");

        this.hwid = hwid;
        this.bannedUntil = bannedUntil;
        this.comment = comment;
        this.type = type;
        this.GMNAME = GMNAME;

        this.time = time <= 0 ? System.currentTimeMillis() : time;

        JClientSession sess = JClientSessionManager.getSession(hwid);

        
        if(sess != null)
        {
            long timeNow = System.currentTimeMillis();

            for(GameClient client : sess.getClients())
            {
                if(client.getLogin() != null)
                    sessionAccounts.add(new TimedObject<String>(client.getLogin(), timeNow));

                sessionIPs.add(new TimedObject<String>(client.getIPAddress(), timeNow));
            }
        }


        _lastUpdate = System.currentTimeMillis();
    }

    public void addAccount(String acc)
    {
        if(acc == null)
            return;

        sessionAccounts.add(new TimedObject<String>(acc.toLowerCase(), System.currentTimeMillis()));
        _lastUpdate = System.currentTimeMillis();
    }

    public boolean findAccount(String acc)
    {
        for(TimedObject<String> ts : sessionAccounts)
            if(acc.equals(ts.value))
                return true;

        return false;
    }

    public String[] getAccountNames()
    {
        String[] res = new String[sessionAccounts.size()];

        int i = 0;
        for (TimedObject<String> sessionAccount : sessionAccounts) {
            res[i++] = sessionAccount.value;
        }

        return res;
    }

    public boolean isExpired()
    {
        return type == Type.TEMP && bannedUntil <= System.currentTimeMillis();
    }

    public static JBan readFromXML(Element xmle)
    {
        if(xmle == null)
            return null;

        try
        {
            HWID hwid = HWID.fromString(xmle.getAttribute("hwid"));
            Type type = Type.valueOf(xmle.getAttribute("type").toUpperCase());
            
            String GMNAME = "NONE";
            if(xmle.getAttribute("gmname")!=null && !xmle.getAttribute("gmname").isEmpty())
            {
                GMNAME = xmle.getAttribute("gmname");
            }

            long time = Long.parseLong(xmle.getAttribute("time"));
            long bannedUntil = 0L;
            String comment = null;

            // срок бана
            if(type == Type.TEMP)
                bannedUntil = Long.parseLong(xmle.getElementsByTagName("end_time").item(0).getTextContent());

            // комментарий
            NodeList commentNodes = xmle.getElementsByTagName("comment");
            if(commentNodes.getLength() > 0)
                comment = commentNodes.item(0).getTextContent().trim();

            JBan b = new JBan(hwid, type, comment, bannedUntil, time, GMNAME);

            // gm objid
            NodeList gmNodes = xmle.getElementsByTagName("gmObjId");
            if(gmNodes.getLength() > 0)
                b.gmObjId = Integer.parseInt(gmNodes.item(0).getTextContent().trim());

            // список аккаунтов
            NodeList accountListNode = xmle.getElementsByTagName("accounts");
            if(accountListNode.getLength() > 0)
            {
                for (int i = 0; i < accountListNode.getLength(); i++)
                {
                    Node accountsNode = accountListNode.item(i);

                    if (accountsNode.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    Element accountsElement = (Element) accountsNode;
                    NodeList aNode = accountsElement.getElementsByTagName("account");

                    for (int j = 0; j < aNode.getLength(); j++)
                    {
                        Node n = aNode.item(j);

                        if (n.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        Element accountElement = (Element) n;

                        String accountName = accountElement.getTextContent().trim();
                        long accountTime = Long.parseLong(accountElement.getAttribute("time").trim());

                        b.sessionAccounts.add(new TimedObject<String>(accountName, accountTime));
                    }
                }
            }

            // список IP адресов
            NodeList ipv4ListNode = xmle.getElementsByTagName("ipv4");
            if(ipv4ListNode.getLength() > 0)
            {
                for (int i = 0; i < ipv4ListNode.getLength(); i++)
                {
                    Node ipNode = ipv4ListNode.item(i);

                    if (ipNode.getNodeType() != Node.ELEMENT_NODE)
                        continue;

                    Element accountsElement = (Element) ipNode;
                    NodeList iNode = accountsElement.getElementsByTagName("ip");

                    for (int j = 0; j < iNode.getLength(); j++)
                    {
                        Node n = iNode.item(j);

                        if (n.getNodeType() != Node.ELEMENT_NODE)
                            continue;

                        Element ipElement = (Element) n;

                        String ip = ipElement.getTextContent().trim();
                        long ipTime = Long.parseLong(ipElement.getAttribute("time").trim());

                        b.sessionIPs.add(new TimedObject<String>(ip, ipTime));
                    }
                }
            }

            return b;
        }
        catch (Exception e)
        {
            GuardLog.logException(e);
        }

        return null;
    }

    @Override
    public String toString()
    {
        return "Ban{" +
                "hwid=" + hwid +
                ", type=" + type +
                ", bannedUntil=" + bannedUntil +
                ", comment='" + comment + '\'' +
                '}';
    }
}
