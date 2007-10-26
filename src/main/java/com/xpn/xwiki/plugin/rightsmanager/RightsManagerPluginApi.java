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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.rightsmanager.utils.RequestLimit;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * API for managing rights, users and groups.
 * 
 * @version $Id: $
 * @since XWiki Core 1.1.2, XWiki Core 1.2M2
 */
public class RightsManagerPluginApi extends PluginApi
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = "lasterrorcode";

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = "lastexception";

    /**
     * Quote symbol.
     */
    public static final String QUOTE = "\"";

    /**
     * The logging toolkit.
     */
    protected static final Log LOG = LogFactory.getLog(RightsManagerPluginApi.class);

    /**
     * API for managing rights and inheritance.
     */
    private RightsManageRightsApi rightsApi;

    /**
     * Create an instance of the Rights Manager plugin user api.
     * 
     * @param plugin the entry point of the Rights Manager plugin.
     * @param context the XWiki context.
     */
    public RightsManagerPluginApi(RightsManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        rightsApi = new RightsManageRightsApi(context);
    }

    /**
     * @return the API for managing rights and inheritance.
     */
    public RightsManageRightsApi getRightsApi()
    {
        return rightsApi;
    }

    /**
     * Convert Map/List pattern matching parameter from Velocity to [][] used in
     * {@link RightsManager}.
     * 
     * @param map a map of list from Velocity.
     * @return a table of table for {@link RightsManager} methods.
     */
    private static Object[][] createMatchingTable(Map map)
    {
        if (map == null || map.size() == 0) {
            return null;
        }

        Object[][] table = new Object[map.size()][3];

        int i = 0;
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); ++i) {
            Map.Entry entry = (Map.Entry) it.next();

            table[i][0] = entry.getKey();

            if (entry.getValue() instanceof List) {
                List typeValue = (List) entry.getValue();
                table[i][1] = typeValue.get(0);
                if (typeValue.size() > 1) {
                    table[i][2] = typeValue.get(1);
                }
            } else {
                table[i][2] = entry.getValue();
            }
        }

        return table;
    }

    /**
     * Convert List/List order fields from Velocity to [][] used in {@link RightsManager}.
     * 
     * @param list a list of list from Velocity.
     * @return a table of table for {@link RightsManager} methods.
     */
    private static Object[][] createOrderTable(List list)
    {
        if (list == null || list.size() == 0) {
            return null;
        }

        Object[][] table = new Object[list.size()][3];

        int i = 0;
        for (Iterator it = list.iterator(); it.hasNext(); ++i) {
            Object entry = it.next();

            if (entry instanceof List) {
                List fieldParams = (List) entry;
                table[i][0] = fieldParams.get(0);
                if (fieldParams.size() > 1) {
                    table[i][1] = fieldParams.get(1);
                }
                if (fieldParams.size() > 2) {
                    table[i][2] = fieldParams.get(2);
                }
            } else {
                table[i][0] = entry.toString();
            }
        }

        return table;
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

    // Groups management

    /**
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllGroups() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllUsersOrGroups(false, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all groups", e);
        }

        return count;
    }

    /**
     * Get the number of groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search for groups.
     * @return the number of groups in the provided wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllWikiGroups(String wikiName) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllWikiUsersOrGroups(false, wikiName,
                    this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all groups in wiki " + QUOTE + wikiName + QUOTE, e);
        }

        return count;
    }

    /**
     * @return the number of groups in the main wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllGlobalGroups() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllGlobalUsersOrGroups(false, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all groups in main wiki", e);
        }

        return count;
    }

    /**
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllLocalGroups() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllLocalUsersOrGroups(false, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all groups in current wiki", e);
        }

        return count;
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGroupsNames() throws XWikiException
    {
        return getAllMatchedGroupsNames(null);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroupsNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedGroupsNames(matchFields, 0, 0, null);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedGroupsNames(matchFields, nb, start, null);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = Collections.EMPTY_LIST;

        try {
            groupList =
                RightsManager.getInstance().getAllMatchedUsersOrGroups(false,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names", e);
        }

        return groupList;
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGlobalGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGlobalGroupsNames() throws XWikiException
    {
        return getAllMatchedGlobalGroupsNames(null);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedGlobalGroupsNames(matchFields, 0, 0, null);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedGlobalGroupsNames(matchFields, nb, start, null);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = Collections.EMPTY_LIST;

        try {
            groupList =
                RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(false,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from global wiki", e);
        }

        return groupList;
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllWikiGroupsNames(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiGroupsNames(wikiName, null, nb, start);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllWikiGroupsNames(String wikiName) throws XWikiException
    {
        return getAllMatchedWikiGroupsNames(wikiName, null);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields)
        throws XWikiException
    {
        return getAllMatchedWikiGroupsNames(wikiName, matchFields, 0, 0, null);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedWikiGroupsNames(wikiName, matchFields, nb, start, null);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        List groupList = Collections.EMPTY_LIST;

        try {
            groupList =
                RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(false, wikiName,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from provided wiki", e);
        }

        return groupList;
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllLocalGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllLocalGroupsNames() throws XWikiException
    {
        return getAllMatchedLocalGroupsNames(null);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedLocalGroupsNames(matchFields, 0, 0, null);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedLocalGroupsNames(matchFields, nb, start, null);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = Collections.EMPTY_LIST;

        try {
            groupList =
                RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(false,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from local wiki", e);
        }

        return groupList;
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedGroups(null, nb, start);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGroups() throws XWikiException
    {
        return getAllMatchedGroups(null);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroups(Map matchFields) throws XWikiException
    {
        return getAllMatchedGroups(matchFields, 0, 0, null);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroups(Map matchFields, int nb, int start) throws XWikiException
    {
        return getAllMatchedGroups(matchFields, nb, start, null);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedUsersOrGroups(false,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                groupList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents", e);
        }

        return groupList;
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGlobalGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalGroups(null, nb, start);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllGlobalGroups() throws XWikiException
    {
        return getAllMatchedGlobalGroups(null);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroups(Map matchFields) throws XWikiException
    {
        return getAllMatchedGlobalGroups(matchFields, 0, 0, null);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroups(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedGlobalGroups(matchFields, nb, start, null);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedGlobalGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(false,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                groupList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from global wiki", e);
        }

        return groupList;
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllWikiGroups(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiGroups(wikiName, null, nb, start);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllWikiGroups(String wikiName) throws XWikiException
    {
        return getAllMatchedWikiGroups(wikiName, null);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields) throws XWikiException
    {
        return getAllMatchedWikiGroups(wikiName, matchFields, 0, 0, null);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedWikiGroups(wikiName, matchFields, nb, start, null);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        List groupList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(false, wikiName,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                groupList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from provided wiki", e);
        }

        return groupList;
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllLocalGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalGroups(null, nb, start);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllLocalGroups() throws XWikiException
    {
        return getAllMatchedLocalGroups(null);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroups(Map matchFields) throws XWikiException
    {
        return getAllMatchedLocalGroups(matchFields, 0, 0, null);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroups(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedLocalGroups(matchFields, nb, start, null);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List getAllMatchedLocalGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List groupList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(false,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                groupList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from local wiki", e);
        }

        return groupList;
    }

    // Users management

    /**
     * @return the number of users in the main wiki and the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllUsers() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllUsersOrGroups(true, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all users", e);
        }

        return count;
    }

    /**
     * Get the number of users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return the number of users in the provided wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllWikiUsers(String wikiName) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllWikiUsersOrGroups(true, wikiName,
                    this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all wiki users", e);
        }

        return count;
    }

    /**
     * @return the number of users in the main wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllGlobalUsers() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllGlobalUsersOrGroups(true, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all global users", e);
        }

        return count;
    }

    /**
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllLocalUsers() throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllLocalUsersOrGroups(true, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all local users", e);
        }

        return count;
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllUsersNames() throws XWikiException
    {
        return getAllMatchedUsersNames(null);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsersNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedUsersNames(matchFields, 0, 0, null);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsersNames(Map matchFields, int nb, int start) throws XWikiException
    {
        return getAllMatchedUsersNames(matchFields, nb, start, null);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllMatchedUsersOrGroups(true,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names", e);
        }

        return userList;
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllGlobalUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllGlobalUsersNames() throws XWikiException
    {
        return getAllMatchedGlobalUsersNames(null);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedGlobalUsersNames(matchFields, 0, 0, null);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedGlobalUsersNames(matchFields, nb, start, null);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(true,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from global wiki", e);
        }

        return userList;
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllWikiUsersNames(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiUsersNames(wikiName, null, nb, start);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllWikiUsersNames(String wikiName) throws XWikiException
    {
        return getAllMatchedWikiUsersNames(wikiName, null);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields)
        throws XWikiException
    {
        return getAllMatchedWikiUsersNames(wikiName, matchFields, 0, 0, null);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedWikiUsersNames(wikiName, matchFields, nb, start, null);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        List userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(true, wikiName,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from provided wiki", e);
        }

        return userList;
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllLocalUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllLocalUsersNames() throws XWikiException
    {
        return getAllMatchedLocalUsersNames(null);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields) throws XWikiException
    {
        return getAllMatchedLocalUsersNames(matchFields, 0, 0, null);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedLocalUsersNames(matchFields, nb, start, null);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     *            of {@link String} containing user names.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(true,
                    createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from local wiki", e);
        }

        return userList;
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedUsers(null, nb, start);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllUsers() throws XWikiException
    {
        return getAllMatchedUsers(null);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsers(Map matchFields) throws XWikiException
    {
        return getAllMatchedUsers(matchFields, 0, 0, null);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsers(Map matchFields, int nb, int start) throws XWikiException
    {
        return getAllMatchedUsers(matchFields, nb, start, null);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedUsersOrGroups(true,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                userList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched users", e);
        }

        return userList;
    }

    /**
     * Get all users in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllGlobalUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalUsers(null, nb, start);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllGlobalUsers() throws XWikiException
    {
        return getAllMatchedGlobalUsers(null);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsers(Map matchFields) throws XWikiException
    {
        return getAllMatchedGlobalUsers(matchFields, 0, 0, null);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsers(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedGlobalUsers(matchFields, nb, start, null);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedGlobalUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(true,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                userList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from global wiki", e);
        }

        return userList;
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllWikiUsers(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiUsers(wikiName, null, nb, start);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllWikiUsers(String wikiName) throws XWikiException
    {
        return getAllMatchedWikiUsers(wikiName, null);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields) throws XWikiException
    {
        return getAllMatchedWikiUsers(wikiName, matchFields, 0, 0, null);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return getAllMatchedWikiUsers(wikiName, matchFields, nb, start, null);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        List userList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(true, wikiName,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                userList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from provided wiki", e);
        }

        return userList;
    }

    /**
     * Get all users in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllLocalUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalUsers(null, nb, start);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllLocalUsers() throws XWikiException
    {
        return getAllMatchedLocalUsers(null);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsers(Map matchFields) throws XWikiException
    {
        return getAllMatchedLocalUsers(matchFields, 0, 0, null);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsers(Map matchFields, int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalUsers(matchFields, nb, start, null);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @param order the fields to order from. It is a List containing :
     *            <ul>
     *            <li>"field name" for document fields</li>
     *            <li>or ["filed name", "field type"] for object fields</li>
     *            </ul>
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List getAllMatchedLocalUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        List userList = new ArrayList();

        try {
            List list =
                RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(true,
                    createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    createOrderTable(order), this.context);

            for (Iterator it = list.iterator(); it.hasNext();) {
                userList.add(((XWikiDocument) it.next()).newDocument(context));
            }
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from local wiki", e);
        }

        return userList;
    }

    // Groups and users management

    /**
     * Get all groups containing provided user.
     * 
     * @param member the name of the member (user or group).
     * @return the {@link Collection} of {@link String} containing group name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection getAllGroupsNamesForMember(String member) throws XWikiException
    {
        Collection userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllGroupsNamesForMember(member, 0, 0, this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all groups containing user " + QUOTE + member + QUOTE
                + ") users from local wiki", e);
        }

        return userList;
    }

    /**
     * Get all members (users or groups) provided group contains.
     * 
     * @param group the name of the group.
     * @return the {@link Collection} of {@link String} containing member (user or group) name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection getAllMembersNamesForGroup(String group) throws XWikiException
    {
        return getAllMembersNamesForGroup(group, 0, 0);
    }

    /**
     * Get all members (users or groups) provided group contains.
     * 
     * @param group the name of the group.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return the {@link Collection} of {@link String} containing member (user or group) name.
     * @throws XWikiException error when browsing groups.
     */
    public Collection getAllMembersNamesForGroup(String group, int nb, int start)
        throws XWikiException
    {
        Collection userList = Collections.EMPTY_LIST;

        try {
            userList =
                RightsManager.getInstance().getAllMembersNamesForGroup(group, nb, start,
                    this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all user group " + QUOTE + group + QUOTE + ") contains", e);
        }

        return userList;
    }
}
