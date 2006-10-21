/*
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.ListProperty;

/**
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public class Utils
{
    private static final Logger LOG = Logger.getLogger (Utils.class);

    public static boolean isWikiSubscription (BaseObject subscriptionObj)
    {
        final IntegerProperty wikiSubscription = (IntegerProperty) subscriptionObj
                .getField (EmailNotificationPlugin.FIELD_SUBSCRIBED_WIKI);
        return (wikiSubscription != null && wikiSubscription.getValue () != null && "1"
                .equals (wikiSubscription.getValue ().toString ()));
    }

    /**
     * @param subscriptionObj
     * @param fieldName
     * @return
     */
    public static ListProperty getListProperty (BaseObject subscriptionObj, String fieldName)
    {
        ListProperty list = (ListProperty) subscriptionObj.getField (fieldName);
        if (list == null)
        {
            if (LOG.isDebugEnabled ()) LOG.debug ("creating new list property for field " + fieldName);
            list = new StringListProperty();
            list.setValue (new ArrayList ());
            subscriptionObj.safeput(fieldName, list);
        }
        return list;
    }

    /**
     * @param wiki
     * @return
     */
    public static Collection findWikiServers (XWiki wiki, XWikiContext context)
    {
        List retval = new ArrayList ();
        final String hql = ", BaseObject as obj, StringProperty as prop "
                + "where doc.fullName=obj.name and obj.className='XWiki.XWikiServerClass'"
                + " and prop.id.id = obj.id " + "and prop.id.name = 'server'";
        List result = null;
        try
        {
            result = wiki.getStore ().searchDocumentsNames (hql, context);
        } catch (Exception e)
        {
            LOG.error ("error getting list of wiki servers!");
        }
        if (result != null)
        {
            for (Iterator iter = result.iterator (); iter.hasNext ();)
            {
                String docname = (String) iter.next ();
                if (LOG.isDebugEnabled ())
                {
                    LOG.debug ("possible server name: " + docname);
                }
                if (docname.startsWith ("XWiki.XWikiServer"))
                {
                    retval.add (docname.substring ("XWiki.XWikiServer".length ()).toLowerCase ());
                }
            }
        }
        return retval;
    }

}
