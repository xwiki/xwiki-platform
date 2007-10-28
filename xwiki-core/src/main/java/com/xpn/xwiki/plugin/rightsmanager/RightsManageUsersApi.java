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

package com.xpn.xwiki.plugin.rightsmanager;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

/**
 * API for managing users.
 * 
 * @version $Id: $
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
 */
public class RightsManageUsersApi extends Api
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE =
        RightsManagerPluginApi.CONTEXT_LASTERRORCODE;

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION =
        RightsManagerPluginApi.CONTEXT_LASTEXCEPTION;

    /**
     * Quote symbol.
     */
    public static final String QUOTE = RightsManagerPluginApi.QUOTE;

    /**
     * The logging toolkit.
     */
    protected static final Log LOG = LogFactory.getLog(RightsManageUsersApi.class);

    /**
     * Create an instance of RightsManageRightsApi.
     * 
     * @param context the XWiki context.
     */
    public RightsManageUsersApi(XWikiContext context)
    {
        super(context);
    }

    /**
     * Log error and register {@link #CONTEXT_LASTERRORCODE} and {@link #CONTEXT_LASTEXCEPTION}.
     * 
     * @param comment the comment to use with {@link #LOG}.
     * @param e the exception.
     */
    private void logError(String comment, XWikiException e)
    {
        LOG.error(comment, e);

        this.context.put(CONTEXT_LASTERRORCODE, new Integer(e.getCode()));
        this.context.put(CONTEXT_LASTEXCEPTION, e);
    }

    // Count

    /**
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllUsers() throws XWikiException
    {
        return countAllMatchedUsers(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllMatchedUsers(Map matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all users", e);
        }

        return count;
    }

    /**
     * Get the number of users in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search for users.
     * @return the number of users in the provided wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllWikiUsers(String wikiName) throws XWikiException
    {
        return countAllMatchedWikiUsers(wikiName, null);
    }

    /**
     * Get the number of users in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of users in the provided wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllMatchedWikiUsers(String wikiName, Map matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllWikiUsersOrGroups(true, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all users in wiki " + QUOTE + wikiName + QUOTE, e);
        }

        return count;
    }

    /**
     * @return the number of users in the main wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllGlobalUsers() throws XWikiException
    {
        return countAllMatchedGlobalUsers(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of users in the main wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllMatchedGlobalUsers(Map matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllGlobalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all users in main wiki", e);
        }

        return count;
    }

    /**
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllLocalUsers() throws XWikiException
    {
        return countAllMatchedLocalUsers(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllMatchedLocalUsers(Map matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllLocalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all users in current wiki", e);
        }

        return count;
    }
}
