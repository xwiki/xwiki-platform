/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * 
 * Created on 02.02.2005
 *
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/**
 * Sends out notifications.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class NotificationSender
{

	private static final Logger     LOG                        = Logger.getLogger (NotificationSender.class);
    final static String             ALL_SUBSCRIPTIONS          = "from BaseObject as obj where obj.className='"
                                                                       + EmailNotificationPlugin.SUBSCRIPTION_CLASS
                                                                       + "'";
    public static final String  XWIKI_PROPERTY_EMAILSUBJECT    = "xwiki.plugins.emailnotify.emailSubject";
    private static final String SUBSCRIPTION_DOCUMENTS_QUERY   = ", BaseObject as obj where obj.name=doc.fullName and obj.className='"+EmailNotificationPlugin.SUBSCRIPTION_CLASS+"'";
    
    private static final Comparator modificationDateComparator = new Comparator () {
                                                                   public int compare (Object o1, Object o2)
                                                                   {
                                                                       return ( ((PageModifications) o1)
                                                                               .getLastModification ()
                                                                               .getModificationDate ()
                                                                               .compareTo ( ((PageModifications) o2)
                                                                                       .getLastModification ()
                                                                                       .getModificationDate ()));
                                                                   }

                                                               };

    private XWiki                   xwiki;

    /**
     * holds all recorded page modifications. Key is the full name including
     * virtual wiki name: <code>wikiName:web.name</code>
     */
    private Map                     pageModifications;

    /**
     * holding the set of modifications for each web. The sets are ordered by
     * modification date.
     */
    private Map                     webModifications;
    /**
     * holding the set of modifications for each virtual wiki. The sets are
     * ordered by modification date.
     */
    private Map                     wikiModifications;

    /** names of existing virtual Wikis */
    private Collection              virtualWikiNames;

    /**
     * minimum time span in seconds between last modification and sending out a
     * notification, to prevent multiple notifications on minor edits
     */
    private int                     minModificationAge         = 0;

    /** checked in main loop, exit if true */
    private boolean                 exit                       = false;
    private final String            sender;

    /**
     * name of this instance, this is used to find out the subscriptions this
     * sender has to handle. There exists one instance of this class per
     * configured notification interval.
     */
    private final String            name;

    private String                  template;
    private static final String     TEMPLATE_NAME              = "notificationMail.vm";

    private final String            mailSubject;
    private final String            mailServer;
//    private final int            	mailServerPort;
    
    /**
     * Predicate for filtering delayed modifications and such made by the given
     * user
     */
    static final class ModificationPredicate implements Predicate
    {
        private final String userName;

        public ModificationPredicate (String uName)
        {
            this.userName = uName;
        }

        /* Returns true if provided PageModifications set contains
         * at least 1 valid modification not by userName
         */
        public boolean evaluate(Object arg0)
        {
        	boolean retval;
        	PageModifications mods =(PageModifications) arg0;
        	PageModification mod = mods.getLastModificationNotBy(userName);
        	retval = (mod!=null);
        	if (LOG.isDebugEnabled()) LOG.debug ("evaluated modification "+mod+" as "+retval); 
            return retval;
        }

    }

    /**
     * 
     */
    public NotificationSender (XWiki xw, XWikiContext context, String name, Collection virtualWikiNames)
    {
        this.xwiki = xw;
        this.name = name;
        initMaps ();
        this.virtualWikiNames = virtualWikiNames;

        this.sender = xw.getXWikiPreference ("admin_email", context);
        this.template = readTemplate ();
        
        XWikiConfig config = context.getWiki().getConfig();
        this.mailSubject = config.getProperty(XWIKI_PROPERTY_EMAILSUBJECT, "XWiki change notification");
        this.mailServer  = xwiki.getXWikiPreference("smtp_server", context);
        //this.mailServerPort = Integer.parseInt(xwiki.getXWikiPreference("smtp_port", context));        
    }

    /**
     * @return velocity template for notification mails as a String
     */
    protected String readTemplate ()
    {
        StringBuffer retval = new StringBuffer ();
        try
        {
            Reader reader = new InputStreamReader (getClass ().getClassLoader ()
                    .getResourceAsStream (TEMPLATE_NAME), "UTF-8");
            final char[] buffer = new char[1024];
            int bytesRead = 0;
            while ( (bytesRead = reader.read (buffer)) >= 0)
            {
                retval.append (buffer, 0, bytesRead);
            }
        } catch (Exception e)
        {
            LOG.error ("error reading mail template " + TEMPLATE_NAME, e);
            e.printStackTrace ();
        }
        return retval.toString ();
    }

    /**
     * 
     */
    private synchronized void initMaps ()
    {
        pageModifications = new HashMap ();
        webModifications = new HashMap ();
        wikiModifications = new HashMap ();
    }

    public void doExit ()
    {
        exit = true;
    }

    /**
     * Main Method, to be called regularly by a scheduled job. Checks for
     * present changes. If there are any,
     * <ul>
     * <li>loops through all virtual wikis</li>
     * <li>loops through all subscriptions of the virtual wiki</li>
     * <li>processes each subscription, that is, collect all modifications of
     * pages that subscriber is subscribed through, including indirect
     * subscriptions of pages through web and wiki subscription</li>
     * </ul>
     */
    public void run ()
    {
        MDC.put ("url", "email notification sender: " + name);
        LOG.info ("email notification sender thread up and running.");
        if (!haveModifications ())
        {
            if (LOG.isDebugEnabled ()) LOG.debug ("nothing to do, exiting");
            return;
        }
        final Map oldPageModifications = pageModifications;
        final Map oldWebModifications = webModifications;
        final Map oldWikiModifications = wikiModifications;
        initMaps ();
        if (LOG.isDebugEnabled ()) LOG.debug ("have " + oldPageModifications.size () + " modifications...");
        
        if (minModificationAge>0) {
          checkForTooNewModifications (oldPageModifications);
        }
        
        for (Iterator iter = virtualWikiNames.iterator (); iter.hasNext ();)
        {
            final String wikiName = (String) iter.next ();
            if (LOG.isDebugEnabled()) LOG.debug("Processing " + oldPageModifications.size () + " mods on wiki " + wikiName);            
            final XWikiContext wikiContext = new XWikiContext ();
            wikiContext.setWiki (xwiki);
            wikiContext.put ("vcontext", new VelocityContext ());
            wikiContext.setUser ("XWiki.Admin");
            wikiContext.setDatabase (wikiName);
            final Collection subscriptions = getAllSubscriptions (wikiContext);
            if (LOG.isDebugEnabled ())
                LOG.debug (subscriptions.size () + " subscriptions for wiki " + wikiName);
            for (Iterator iterator = subscriptions.iterator (); iterator.hasNext ();)
            {
                final BaseObject subscObj = (BaseObject) iterator.next ();
                processSubscription (subscObj, wikiContext, oldPageModifications, oldWebModifications,
                                     oldWikiModifications);
            }
        }
        MDC.remove ("url");
    }

    /**
     * Marks pageModifications still too new as not to send out now, and re-adds
     * them to the new pageModifications Map using
     * {@link #addOldModification(PageModification)}.
     * @param oldPageModifications
     */
    protected synchronized void checkForTooNewModifications (Map oldPageModifications)
    {
        final Date lastChangeDateForNotification = calculateLastModificationDateToSend ();
        if (LOG.isDebugEnabled ())
            LOG.debug ("lastChangeDateForNotification: " + lastChangeDateForNotification);
        for (Iterator iter = oldPageModifications.values ().iterator (); iter.hasNext ();)
        {
            PageModification mod = ((PageModifications) iter.next ()).getLastModification ();
            if (mod.getModificationDate ().after (lastChangeDateForNotification))
            {
                mod.setDontSendNow (true);
                addOldModification (new PageModification (mod));
            }
        }
    }

    /**
     * @return the date we use to filter out page modifications being too new
     *         for being sent out.
     */
    protected Date calculateLastModificationDateToSend ()
    {
        final Calendar cal = Calendar.getInstance ();
        cal.add (Calendar.SECOND, -1 * minModificationAge);
        final Date lastChangeDateForNotification = cal.getTime ();
        return lastChangeDateForNotification;
    }

    /**
     * @return true when there are modifications to be processed
     */
    private boolean haveModifications ()
    {
        return (pageModifications.size () != 0);
    }

    /**
     * collects the relevant modification for a user's subscription data and
     * triggers sending out the mail.
     * @param subscObj
     * @param oldPageModifications
     * @param oldWebModifications
     * @param oldWikiModifications
     */
    private void processSubscription (BaseObject subscObj, XWikiContext context, Map oldPageModifications,
                                      Map oldWebModifications, Map oldWikiModifications)
    {
        final String userName = subscObj.getStringValue (EmailNotificationPlugin.FIELD_USERNAME);
        final Predicate modificationPredicate = new ModificationPredicate (userName);
        String wikiName = context.getDatabase ();
        if (LOG.isDebugEnabled ()) LOG.debug ("wiki: " + wikiName + "\nuser:" + userName);
        final Set modifications = new TreeSet (modificationDateComparator);
        if (Utils.isWikiSubscription (subscObj))
        {
            // whole wiki subscribed, so nothing more to do with subscribed
            // pages/webs

            final Set thisWikiModifications = (Set) oldWikiModifications.get (wikiName);
            if (thisWikiModifications != null)
            {
                if (LOG.isDebugEnabled ())
                    LOG.debug ("wiki subscription, " + thisWikiModifications.size ()
                            + " total modified pages");
                // find out modifications done by others
                final Collection foreignWikiModifications = CollectionUtils.select (thisWikiModifications,
                                                                                    modificationPredicate);
                if (LOG.isDebugEnabled ())
                    LOG.debug (foreignWikiModifications.size () + " pages modified by other users");
                modifications.addAll (foreignWikiModifications);
            } else
            {
                if (LOG.isDebugEnabled ()) LOG.debug ("no modifications for wiki " + wikiName);
            }
        } else
        {
            // prepare for appending web names
            wikiName += ":";

            List subscribedWebs = Utils.getListProperty (subscObj,
                                                         EmailNotificationPlugin.FIELD_SUBSCRIBED_WEBS)
                    .getList ();

            if (subscribedWebs != null)
            {
                if (LOG.isDebugEnabled ()) LOG.debug (subscribedWebs.size () + " web subscriptions");
                for (Iterator iter = subscribedWebs.iterator (); iter.hasNext ();)
                {
                    final String fullWebName = new StringBuffer (wikiName).append ((String) iter.next ())
                            .toString ();
                    if (LOG.isDebugEnabled ()) LOG.debug ("web: " + fullWebName);
                    final Set allModifiedPagesInWeb = (Set) oldWebModifications.get (fullWebName);
                    if (LOG.isDebugEnabled ()) LOG.debug ("modified in web: " + allModifiedPagesInWeb);
                    if (allModifiedPagesInWeb != null)
                    {
                        if (LOG.isDebugEnabled ())
                            LOG.debug (allModifiedPagesInWeb.size () + " pages in web modified");
                        final Collection modifiedPagesInThisWeb = CollectionUtils
                                .select (allModifiedPagesInWeb, modificationPredicate);
                        if (LOG.isDebugEnabled ())
                            LOG.debug (modifiedPagesInThisWeb.size ()
                                    + "  pages in web modified by other users");
                        modifications.addAll (modifiedPagesInThisWeb);
                    }
                }
            }
            List subscribedPages = Utils.getListProperty (subscObj,
                                                          EmailNotificationPlugin.FIELD_SUBSCRIBED_PAGES)
                    .getList ();

            if (subscribedPages != null)
            {
                if (LOG.isDebugEnabled ()) LOG.debug (subscribedPages.size () + " page subscriptions");
                for (Iterator iter = subscribedPages.iterator (); iter.hasNext ();)
                {
                    final String pageName = new StringBuffer (wikiName).append ((String) iter.next ())
                            .toString ();
                    if (LOG.isDebugEnabled ()) LOG.debug ("check pagename: " + pageName);
                    final PageModifications mod = (PageModifications) oldPageModifications.get (pageName);
                    if (LOG.isDebugEnabled ()) LOG.debug ("got mods: " + mod);
                    if (mod != null && modificationPredicate.evaluate (mod))
                    {
                        if (LOG.isDebugEnabled ()) LOG.debug ("modified page: " + pageName);
                        modifications.add (mod);
                    }
                }
            }
        }
        if (modifications.size () > 0)
        {
            sendChangeNotification (userName, context, modifications);
        } else
        {
            if (LOG.isDebugEnabled ()) LOG.debug ("no relevant modifications found for user " + userName);
        }
    }

    /**
     * @param userName
     * @param context
     * @param modifications
     *           set of PageModifications instances each containing the full
     *           change history of a single document
     */
    private void sendChangeNotification (String userName, XWikiContext context, Set modifications)
    {
        XWikiDocument docuser;
        try
        {
            docuser = xwiki.getDocument (userName, context);
        } catch (XWikiException e)
        {
            LOG.error ("error getting user object: " + userName, e);
            e.printStackTrace ();
            return;
        }
        BaseObject userobj = docuser.getObject ("XWiki.XWikiUsers", 0);
        String userEmail = userobj.getStringValue ("email");
        if (LOG.isDebugEnabled ())
            LOG.debug ("sending mail with " + modifications.size () + " modified docs to " + userEmail);
        VelocityContext vcontext = new VelocityContext ();
        vcontext.put ("pagemodifications", modifications);
        vcontext.put ("currentUser", userobj.getName());        
        vcontext.put ("firstname", userobj.getStringValue ("first_name"));
        vcontext.put ("lastname", userobj.getStringValue ("last_name"));
        vcontext.put ("server", context.getWikiServer ());
        vcontext.put ("wikiName", context.getDatabase ());
        
        String content = XWikiVelocityRenderer.evaluate (template, "emailnotify", vcontext);
        if (LOG.isDebugEnabled()) LOG.debug ("sending mail \n" + content + " \n to " + userEmail);
        SimpleEmail email = new SimpleEmail();
        email.setHostName(mailServer);
        //email.setSmtpPort(mailServerPort);
        email.setSubject(mailSubject);
        try {
            email.addTo(userEmail);
            email.setFrom(this.sender);
            email.setMsg(content);              	
            email.send();
        } catch (EmailException e) {
        	LOG.error("Error sending email: "+e);
            e.printStackTrace();
        }        	
    }

    /**
     * @param wikiName
     * @return
     * @todo TODO: later the return value should be limited to those
     *       subscriptions having the notification interval set this instance is
     *       responsible for.
     */
    protected Collection getAllSubscriptions (XWikiContext context)
    {
        List retval = new ArrayList ();
        try {        
            Collection userDocs = xwiki.getStore ().searchDocuments(SUBSCRIPTION_DOCUMENTS_QUERY, context);
            for (Iterator iter = userDocs.iterator (); iter.hasNext ();)
            {
            
                // XWikiDocument doc = xwiki.getDocument ((String) iter.next (),
                // context);
                XWikiDocument doc = (XWikiDocument) iter.next ();
                if (doc != null)
                {
                    BaseObject obj = doc.getObject (EmailNotificationPlugin.SUBSCRIPTION_CLASS);
                    if (obj != null)
                    {
                    	if (LOG.isDebugEnabled()) LOG.debug("Subscription object for user " + doc.getFullName() + " : " + obj);
                        String schedulerName = obj
                                .getStringValue (EmailNotificationPlugin.FIELD_SCHEDULER_NAME);
                        if (name.equals (schedulerName))
                        {
                            retval.add (obj);
                            if (LOG.isDebugEnabled ())
                                LOG.debug ("adding subscription of user " + doc.getName ());
                        } else
                        {
                            if (LOG.isDebugEnabled ())
                                LOG.debug ("This scheduler is : " + name + ". Skipping subscription of user " + doc.getName ()
                                        + " since not our scheduler name: " + schedulerName);
                        }
                    }
                }
            }

        } catch (XWikiException e)
        {
            LOG.error ("error retrieving subscriptions of wiki " + context.getDatabase () + ": " + e);
            e.printStackTrace ();
            retval = new ArrayList ();
        }
        return retval;
      
    }

    /**
     * Re-Adds this item to the modification lists, if there hasn't been a more
     * recent notification of the same page already
     * @param item
     */
    protected synchronized void addOldModification (PageModification item)
    {
        if (LOG.isDebugEnabled ())
            LOG.debug ("modification too new " + item.getPageData ().toString () + ", keeping for next run");
        add (item);
    }

    /**
     * @param item
     */
    protected synchronized void add (PageModification item)
    {
        PageModifications pMods = (PageModifications) pageModifications.get (item.getPageData ()
                .getFullPageName ());
        if (pMods != null)
        {
            pMods.addModification (item);
        } else
        {
            pMods = new PageModifications (item);
            pageModifications.put (item.getPageData ().getFullPageName (), pMods);
        }
        getWebModifications (item.getPageData ().getWikiAndWebName ()).add (pMods);
        getWikiModifications (item.getPageData ().getWikiName ()).add (pMods);
    }

    protected Set getWikiModifications (String wikiName)
    {
        Set retval = (Set) wikiModifications.get (wikiName);
        if (retval == null)
        {
            retval = new HashSet ();
            wikiModifications.put (wikiName, retval);
        }
        return retval;
    }

    protected Set getWebModifications (String wikiAndWebName)
    {
        Set retval = (Set) webModifications.get (wikiAndWebName);
        if (retval == null)
        {
            retval = new HashSet ();
            webModifications.put (wikiAndWebName, retval);
        }
        return retval;
    }

}
