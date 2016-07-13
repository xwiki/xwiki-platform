/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.plugin.watchlist;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Manager for WatchList jobs.
 * 
 * @version $Id$
 */
@Deprecated
public class WatchListJobManager
{
    /**
     * WatchList Job class.
     */
    public static final String WATCHLIST_JOB_CLASS = "XWiki.WatchListJobClass";

    /**
     * WatchList Job email template property name.
     */
    public static final String WATCHLIST_JOB_EMAIL_PROP = "template";

    /**
     * WatchList Job last fire time property name.
     */
    public static final String WATCHLIST_JOB_LAST_FIRE_TIME_PROP = "last_fire_time";

    /**
     * Name of the groups property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_GROUPS_PROPERTY = "groups";

    /**
     * Name of the levels property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_LEVELS_PROPERTY = "levels";

    /**
     * Name of the allow property in the XWiki rights class.
     */
    public static final String XWIKI_RIGHTS_CLASS_ALLOW_PROPERTY = "allow";

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListJobManager.class);

    /**
     * Get the list of available jobs (list of {@link XWikiDocument}).
     * 
     * @param context Context of the request
     * @return the list of available jobs
     */
    public List<Document> getJobs(XWikiContext context)
    {
        String oriDatabase = context.getWikiId();
        List<Object> params = new ArrayList<Object>();
        List<Document> results = new ArrayList<Document>();

        try {
            context.setWikiId(context.getMainXWiki());
            params.add(WATCHLIST_JOB_CLASS);
            List<String> docNames =
                context
                    .getWiki()
                    .getStore()
                    .searchDocumentsNames(", BaseObject obj where doc.fullName=obj.name and obj.className=?", 0, 0,
                        params, context);
            for (String docName : docNames) {
                XWikiDocument doc = context.getWiki().getDocument(docName, context);
                results.add(new Document(doc, context));
            }
        } catch (Exception e) {
            LOGGER.error("error getting list of available watchlist jobs", e);
        } finally {
            context.setWikiId(oriDatabase);
        }

        return results;
    }

    /**
     * Create default WatchList jobs in the wiki.
     * 
     * @param context Context of the request
     * @throws XWikiException When a job creation fails
     */
    public void init(XWikiContext context) throws XWikiException
    {
    }
}
