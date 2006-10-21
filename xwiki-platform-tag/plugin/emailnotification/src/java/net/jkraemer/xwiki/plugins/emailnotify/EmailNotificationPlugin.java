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
 * Created on 27.01.2005
 *
 */
package net.jkraemer.xwiki.plugins.emailnotify;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class EmailNotificationPlugin extends XWikiDefaultPlugin implements
        XWikiDocChangeNotificationInterface, XWikiActionNotificationInterface
{
    /**
     * Property which names the default scheduler name used for newly created
     * subscription objects (of users subscribing to a document the first time)
     */
    public static final String  XWIKI_PROPERTY_DEFAULTSCHEDULERNAME = "xwiki.plugins.emailnotify.defaultSchedulerName";

    public static final String  TYPE_PAGE                           = "page";
    public static final String  TYPE_WEB                            = "web";
    public static final String  TYPE_WIKI                           = "wiki";

    public static final String  FIELD_USERNAME                      = "Username";
    public static final String  FIELD_SUBSCRIBED_PAGES              = "subscribedDocuments";
    public static final String  FIELD_SUBSCRIBED_WEBS               = "subscribedWebs";
    public static final String  FIELD_SUBSCRIBED_WIKI               = "subscribedWiki";
    public static final String  FIELD_SCHEDULER_NAME                = "schedulerName";

    private static final String SUBSCRIPTION_PAGE_NAME              = "EmailSubscriptions";
    private static final String SUBSCRIPTION_PAGE_WEB               = "XWiki";
    public static final String  SUBSCRIPTION_CLASS                  = SUBSCRIPTION_PAGE_WEB + "."
                                                                            + SUBSCRIPTION_PAGE_NAME;

    private static final String SQL_FULL_PAGE_NAMES                 = "select distinct doc.fullName from XWikiDocument as doc order by doc.web, doc.name";
    private static final String SQL_WEB_NAMES                       = "select distinct doc.web from XWikiDocument as doc order by doc.web";
    private static final Logger LOG                                 = Logger.getLogger (EmailNotificationPlugin.class);

    private boolean             didAlreadyTryToUpdateClass          = false;

    private Scheduler           scheduler;
    private Map                 notificationSenders;
    private Collection          virtualWikiNames;
    private String              defaultSchedulerName;

    public EmailNotificationPlugin (final String name, final String className, final XWikiContext context)
    {
        super (name, className, context);
        init (context);
    }

    public synchronized void init (final XWikiContext context)
    {
        super.init (context);
        XWikiConfig config = context.getWiki ().getConfig ();
        this.defaultSchedulerName = config.getProperty (XWIKI_PROPERTY_DEFAULTSCHEDULERNAME, "hourly");

        initVirtualWikiNames (context);
        initNotificationJobs (context);
        context.getWiki ().getNotificationManager ().addGeneralRule (new DocChangeRule (this));
        context.getWiki ().getNotificationManager ().addGeneralRule (new XWikiActionRule (this));
    }

    /**
     * 
     */
    private synchronized void initVirtualWikiNames (final XWikiContext context)
    {
        final com.xpn.xwiki.XWiki xw = context.getWiki ();
        if (xw.isVirtual ())
        {

	    String wikilist = xw.Param("xwiki.plugins.emailnotify.wikilist");
            if ((wikilist!=null)&&(!wikilist.equals (""))) {
               String[] wikilistarray = StringUtils.split (wikilist, ", ");
               if (wikilistarray!=null) {
                 for (int i=0;i<wikilistarray.length;i++) {
                    this.virtualWikiNames = new ArrayList ();
                    this.virtualWikiNames.add (wikilistarray[i]);
                 }
               }
            } else {

            this.virtualWikiNames = Utils.findWikiServers (xw, context);
            if (LOG.isDebugEnabled ()) LOG.debug (this.virtualWikiNames.size () + " virtual wikis found");
            }
        } else
        {
            this.virtualWikiNames = new ArrayList (1);
            this.virtualWikiNames.add ("xwiki");
        }
    }

    /**
     * @throws SchedulerException
     * @throws ParseException
     * @throws
     */
    private synchronized void initNotificationJobs (final XWikiContext context)
    {
        // start quartz scheduler
        try
        {
            scheduler = StdSchedulerFactory.getDefaultScheduler ();
            scheduler.start ();
        } catch (SchedulerException se)
        {
            se.printStackTrace ();
        }

        // create jobs for the configured notification intervals
        notificationSenders = new HashMap ();
        final Config notificationConfig = new Config ("emailnotification");

        NotifierThreadConfig cfg;
        NotificationSender notifier;
        JobDetail jobDetail;
        CronTrigger trigger;
        for (Iterator iter = notificationConfig.getThreadConfig ().iterator (); iter.hasNext ();)
        {
            cfg = (NotifierThreadConfig) iter.next ();
            notifier = new NotificationSender (context.getWiki (), context, cfg.getName (), virtualWikiNames);
            try
            {
                jobDetail = new JobDetail ("emailnotify_" + cfg.getName (), Scheduler.DEFAULT_GROUP,
                                           NotifierJob.class);
                jobDetail.getJobDataMap ().put (NotifierJob.NOTIFIER_KEY, notifier);
                trigger = new CronTrigger ("emailnotify_" + cfg.getName (), Scheduler.DEFAULT_GROUP, cfg
                        .getCrontab ());
                scheduler.scheduleJob (jobDetail, trigger);
                notificationSenders.put (cfg.getName (), notifier);
            } catch (Exception e)
            {
                LOG.error ("error scheduling quartz job " + cfg.getName() + " (" + cfg.getCrontab() + ")", e);
            }
        }
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize () throws Throwable
    {
        if (scheduler != null) scheduler.shutdown ();
        super.finalize ();
    }

    /**
     * Toggles subscription
     * synchronized to avoid colliding edits of the subscriptions document when
     * two users decide to subscribe at the same time.
     * @todo TODO: check if this synchronization is necessary
     * @param pageName
     * @param wiki
     * @param context
     */
    public synchronized void processSubscription (final String type, final Document docToSubscribe,
                                                  final XWiki wiki, final XWikiContext context)
    {
        XWikiDocument doc = getUserDocument (wiki, context);
        BaseObject subscObj = getSubscriptionObject (wiki, context);
        if (subscObj == null)
        {
            subscObj = createSubscriptionObject (wiki, context);
            doc.addObject(SUBSCRIPTION_CLASS, subscObj);
        }
        toggleSubscription (subscObj, type, docToSubscribe);
        saveDocument (wiki, context, doc);
    }


	/**
	 * Sets subscription
	 * @param enable
	 * @param type
	 * @param docToSubscribe
	 * @param wiki
	 * @param context
	 */
	public synchronized void processSubscription(final boolean enable, final String type,
			 						final Document docToSubscribe, final XWiki wiki, final XWikiContext context) {
        XWikiDocument doc = getUserDocument (wiki, context);
        BaseObject subscObj = getSubscriptionObject (wiki, context);
        if (subscObj == null)
        {
            subscObj = createSubscriptionObject (wiki, context);
            doc.addObject(SUBSCRIPTION_CLASS, subscObj);
        }
        setSubscription (enable, subscObj, type, docToSubscribe);
        saveDocument (wiki, context, doc);
	}   
    
    /**
     * @param type
     * @param doc
     * @param wiki
     * @return true if the user already has the specified type of Subscription
     *         for this page/this pages web/the whole wiki
     * @todo TODO: cache the information on a per user basis (at least for web
     *       and wiki subscriptions this should make sense)
     */
    public boolean isSubscribed (final String type, final Document doc, final XWiki wiki,
                                 final XWikiContext context)
    {
        BaseObject obj = getSubscriptionObject (wiki, context);
        if (obj == null) return false;
        if (TYPE_WIKI.equals (type))
        {
            if (obj.getIntValue (FIELD_SUBSCRIBED_WIKI) == 1) return true;
            return false;
        }

        ListProperty list;
        String name;
        if (TYPE_WEB.equals (type))
        {
            list = Utils.getListProperty (obj, FIELD_SUBSCRIBED_WEBS);
            name = doc.getWeb ();
        } else
            if (TYPE_PAGE.equals (type))
            {
                list = Utils.getListProperty (obj, FIELD_SUBSCRIBED_PAGES);
                name = doc.getFullName ();
            } else
            {
                LOG.error ("invalid subscription type " + type);
                return false;
            }

        final List theList = list.getList ();
        if (LOG.isDebugEnabled ())
            LOG.debug ("name: " + name + ", subscribed items: "
                    + new ToStringBuilder (theList).append (theList).toString ());
        return theList != null && theList.contains (name);
    }

    protected void saveDocument (final XWiki wiki, final XWikiContext context, final XWikiDocument doc)
    {
        try
        {
            context.getWiki().saveDocument(doc, context);
        } catch (XWikiException e)
        {
            LOG.error ("error saving subscriptions document!", e);
            e.printStackTrace ();
        }
    }

    /**
     * Toggles the subscription of the given type,
     * @param subscriptionObj
     * @param type
     * @param docToSubscribe
     */
    protected void toggleSubscription (final BaseObject subscriptionObj, final String type,
                                       final Document docToSubscribe)
    {
        if (TYPE_WIKI.equals (type))
        {
            IntegerProperty prop = (IntegerProperty) subscriptionObj.getField (FIELD_SUBSCRIBED_WIKI);
            if ("1".equals (prop.getValue ().toString ()))
            {
                prop.setValue (new Integer (0));
                if (LOG.isDebugEnabled ()) LOG.debug ("turned wiki-wide notifications off");
            } else
            {
                prop.setValue (new Integer (1));
                if (LOG.isDebugEnabled ()) LOG.debug ("turned wiki-wide notifications on");
            }
        } else
        {
            String name;
            ListProperty list;
            if (TYPE_PAGE.equals (type))
            {
                list = Utils.getListProperty (subscriptionObj, FIELD_SUBSCRIBED_PAGES);
                name = docToSubscribe.getFullName ();
            } else
                if (TYPE_WEB.equals (type))
                {
                    list = Utils.getListProperty (subscriptionObj, FIELD_SUBSCRIBED_WEBS);
                    name = docToSubscribe.getWeb ();
                } else
                {
                    LOG.error ("invalid subscription type " + type);
                    return;
                }

            if (LOG.isDebugEnabled ())
            {
                LOG.debug ("processing " + type + " subscription for " + name);
            }
            // can be either full document names or web names, depending on type
            List subscribedEntities = list.getList ();
            if (LOG.isDebugEnabled ())
            {
                LOG.debug (subscribedEntities.size () + " subscribed " + type + "s");
            }
            if (subscribedEntities.contains (name))
            {
                if (LOG.isDebugEnabled ())
                {
                    LOG.debug ("remove subscription");
                }
                subscribedEntities.remove (name);
            } else
            {
                if (LOG.isDebugEnabled ())
                {
                    LOG.debug ("add subscription");
                }
                subscribedEntities.add (name);
            }
        }
    }

    /**
     * Like toggleSubscription, except action is defined explicitly by "enable". 
     * This is to prevent accidentally toggling subscription when 
     * hitting the back button in the browser.
     * @param enable : true to subscribe, false to unsubscribe.
     * @param subscriptionObj
     * @param type
     * @param docToSubscribe
     */
    protected void setSubscription(boolean enable, final BaseObject subscriptionObj,
    							   final String type, final Document docToSubscribe) {
		if (TYPE_WIKI.equals(type)) {
			IntegerProperty prop = (IntegerProperty) subscriptionObj
					.getField(FIELD_SUBSCRIBED_WIKI);
			if (enable) {
				prop.setValue(new Integer(1));
				if (LOG.isDebugEnabled())
					LOG.debug("turned wiki-wide notifications on");				
			} else {
				prop.setValue(new Integer(0));
				if (LOG.isDebugEnabled())
					LOG.debug("turned wiki-wide notifications off");
			}
		} else {
			String name;
			ListProperty list;
			if (TYPE_PAGE.equals(type)) {
				list = Utils.getListProperty(subscriptionObj,
						FIELD_SUBSCRIBED_PAGES);
				name = docToSubscribe.getFullName();
			} else if (TYPE_WEB.equals(type)) {
				list = Utils.getListProperty(subscriptionObj,
						FIELD_SUBSCRIBED_WEBS);
				name = docToSubscribe.getWeb();
			} else {
				LOG.error("invalid subscription type " + type);
				return;
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("processing " + type + " subscription for " + name);
			}
			// can be either full document names or web names, depending on type
			List subscribedEntities = list.getList();
			if (LOG.isDebugEnabled()) {
				LOG.debug(subscribedEntities.size() + " subscribed " + type
						+ "s");
			}
			if (enable) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("add subscription");
				}
				subscribedEntities.add(name);				
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("remove subscription");
				}
				subscribedEntities.remove(name);
			}
		}
	}

    
    
    protected XWikiDocument getUserDocument (final XWiki wiki, final XWikiContext context)
    {
        try
        {
            return wiki.getDocument (context.getUser ()).getDocument();
        } catch (XWikiException e)
        {
            LOG.error ("error getting user document for user" + context.getUser (), e);
            e.printStackTrace ();
            return null;
        }
    }

    /**
     * @param wiki
     * @param user
     * @param objects
     */
    private BaseObject getSubscriptionObject (final XWiki wiki, final XWikiContext context)
    {
        XWikiDocument userDocument = getUserDocument (wiki, context);
        if (userDocument==null)
         return null;
        
        List objects = userDocument.getObjects (SUBSCRIPTION_CLASS);
        if (LOG.isDebugEnabled ())
        {
            LOG.debug ("found " + (objects != null ? objects.size () + "" : "0") + " subscription objects");
        }
        if (objects != null && objects.size () > 0)
        {
            return (BaseObject) objects.get (0);
        }
        return null;
    }

    /**
     * @param wiki
     * @param context
     * @param user
     * @return
     */
    private BaseObject createSubscriptionObject (final XWiki wiki, final XWikiContext context)
    {
        final String user = context.getUser ();
        if (LOG.isDebugEnabled ())
        {
            LOG.debug ("creating new Subscription object for user " + user);
        }
        BaseObject subscriptionObj;
        // no subscription object for this user, so let's create one.
        // TODO: this cast is only safe as long as nobody changes
        // BaseClass#newObject()
        BaseClass objClass = getSubscriptionClass (wiki, context);
        subscriptionObj = (BaseObject) objClass.newObject ();
        subscriptionObj.setName(user);
        subscriptionObj.setClassName(objClass.getName());
        subscriptionObj.setStringValue (FIELD_USERNAME, user);
        
        ListProperty subscPages = new StringListProperty ();
        subscPages.setValue(new ArrayList());
        subscriptionObj.safeput(FIELD_SUBSCRIBED_PAGES, subscPages);
        
        ListProperty subscWebs = new StringListProperty ();
        subscWebs.setValue(new ArrayList ());
        subscriptionObj.safeput(FIELD_SUBSCRIBED_WEBS, subscWebs);
        
        subscriptionObj.setIntValue(FIELD_SUBSCRIBED_WIKI, 0);
        subscriptionObj.setStringValue(FIELD_SCHEDULER_NAME, defaultSchedulerName);
        
        return subscriptionObj;
    }

    private BaseClass getSubscriptionClass (final XWiki wiki, final XWikiContext context)
    {
    	if (LOG.isDebugEnabled()) LOG.debug("In getSubscriptionClass");
        XWikiDocument doc;
        boolean needsUpdate = false;

        final com.xpn.xwiki.XWiki xwiki = wiki.getXWiki ();
        try
        {
            doc = xwiki.getDocument (SUBSCRIPTION_CLASS, context);
        } catch (Exception e)
        {
            doc = new XWikiDocument ();
            doc.setWeb (SUBSCRIPTION_PAGE_WEB);
            doc.setName (SUBSCRIPTION_PAGE_NAME);
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass ();
        if (didAlreadyTryToUpdateClass)
        {
            // shortcut, after first run we return here.
            return bclass;
        }
        bclass.setName (SUBSCRIPTION_CLASS);

        needsUpdate |= bclass.addTextField (FIELD_USERNAME, FIELD_USERNAME, 50);
        if (bclass.get (FIELD_SUBSCRIBED_PAGES) == null)
        {
            needsUpdate = true;
            DBListClass dbList = new DBListClass ();
            dbList.setName (FIELD_SUBSCRIBED_PAGES);
            dbList.setPrettyName ("Subscribed Documents");
            dbList.setSize (15);
            dbList.setMultiSelect (true);
            dbList.setSql (SQL_FULL_PAGE_NAMES);
            dbList.setObject (bclass);
            bclass.put (FIELD_SUBSCRIBED_PAGES, dbList);
        }
        if (bclass.get (FIELD_SUBSCRIBED_WEBS) == null)
        {
            needsUpdate = true;
            DBListClass dbList = new DBListClass ();
            dbList.setName (FIELD_SUBSCRIBED_WEBS);
            dbList.setPrettyName ("Subscribed Webs");
            dbList.setSize (15);
            dbList.setMultiSelect (true);
            dbList.setSql (SQL_WEB_NAMES);
            dbList.setObject (bclass);
            bclass.put (FIELD_SUBSCRIBED_WEBS, dbList);
        }
        if (bclass.get (FIELD_SCHEDULER_NAME) == null)
        {
            needsUpdate = true;
            StaticListClass list = new StaticListClass ();
            list.setName (FIELD_SCHEDULER_NAME);
            list.setPrettyName ("Notification frequency");
            list.setSize (1);
            list.setMultiSelect (false);
            list.setValues (getSchedulerNamesAsString ());
            list.setObject (bclass);
            bclass.put (FIELD_SCHEDULER_NAME, list);
        }
        needsUpdate |= bclass.addBooleanField (FIELD_SUBSCRIBED_WIKI, "All Documents", "yesno");

        if (doc.isNew())
        {
            needsUpdate = true;
            doc.setContent ("1 EMail Notification Plugin\n "
                    + "This class is used for storing Subscriptions to Pages, Webs or to the whole Wiki.");
        }

        if (needsUpdate) try
        {
            if (LOG.isDebugEnabled ())
            {
                LOG.debug ("now saving PageSubscriptions document and class");
            }
            xwiki.saveDocument (doc, context);
        } catch (XWikiException e)
        {
            LOG.error ("error creating page subscriptions class", e);
            e.printStackTrace ();
        }
        // enable the short cut for all following calls to this method
        didAlreadyTryToUpdateClass = true;
        return bclass;
    }

    /**
     * @return
     */
    private String getSchedulerNamesAsString ()
    {
        final Set schedulerNames = notificationSenders.keySet ();
        if (schedulerNames.size () == 0) return "";
        StringBuffer values = new StringBuffer();
        for (Iterator iter = schedulerNames.iterator (); iter.hasNext ();)
        {
            String name = (String) iter.next ();
            values.append (name).append ("|");
        }
        return values.substring (0, values.length () - 1);
    }

    public String getName ()
    {
        return "emailnotify";
    }

    public Api getPluginApi (final XWikiPluginInterface plugin, final XWikiContext context)
    {
        return new EmailNotificationPluginApi ((EmailNotificationPlugin) plugin, context);
    }

    private final WeakHashMap pageData = new WeakHashMap ();

    /**
     * @see com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.doc.XWikiDocument,
     *      int, com.xpn.xwiki.XWikiContext)
     */
    public void notify (final XWikiNotificationRule rule, final XWikiDocument newdoc,
                        final XWikiDocument olddoc, final int event, final XWikiContext context)
    {
    	if (LOG.isDebugEnabled())
            LOG.debug(String.format("Detected change type %d to document %s", event, newdoc.getFullName()));
        for (Iterator iter = notificationSenders.values ().iterator (); iter.hasNext ();)
        {
            final NotificationSender sender = (NotificationSender) iter.next ();
            PageData pd = getPageData(newdoc, context);
            sender.add (new PageModification (newdoc, olddoc, pd));
        }

    }
    
    protected PageData getPageData (XWikiDocument document, XWikiContext context)
    {
        final String key = PageData.buildKey (document, context);
        PageData retval = (PageData) pageData.get (key);
        if (retval == null)
        {
            retval = new PageData (document, context);
            synchronized (pageData)
            {
                pageData.put (key, retval);
            }
        }
        return retval;
    }

    /**
     * @see com.xpn.xwiki.notify.XWikiActionNotificationInterface#notify(com.xpn.xwiki.notify.XWikiNotificationRule,
     *      com.xpn.xwiki.doc.XWikiDocument, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public void notify (final XWikiNotificationRule rule, final XWikiDocument doc, final String action,
                        final XWikiContext context)
    {
    	if (LOG.isDebugEnabled()) LOG.debug(String.format("Detected action type %s to document %s", action, doc.getFullName()));
/*        if ("upload".equals (action))
        {
            // TODO: notify on uploads
        }
*/        
    }


}
