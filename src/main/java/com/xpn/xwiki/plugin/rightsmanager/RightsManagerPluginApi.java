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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.plugin.PluginApi;
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
     * The name of the property in {@link com.xpn.xwiki.XWikiConfig} indicating the user interface
     * to use for rights management.
     */
    public static final String RIGHTS_UI_PROPERTY = "xwiki.rights.defaultui";

    /**
     * The value of the {@link #RIGHTS_UI_PROPERTY} that indicate to use the stable/basic user
     * interface.
     */
    public static final String RIGHTS_UI_VALUE_STABLE = "stable";

    /**
     * The value of the {@link #RIGHTS_UI_PROPERTY} that indicate to use the new experimental ajax
     * user interface.
     */
    public static final String RIGHTS_UI_VALUE_NEW = "new";

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
     * API for managing users.
     */
    private RightsManageUsersApi usersApi;

    /**
     * API for managing groups.
     */
    private RightsManageGroupsApi groupsApi;

    /**
     * Create an instance of the Rights Manager plugin user api.
     * 
     * @param plugin the entry point of the Rights Manager plugin.
     * @param context the XWiki context.
     */
    public RightsManagerPluginApi(RightsManagerPlugin plugin, XWikiContext context)
    {
        super(plugin, context);

        this.rightsApi = new RightsManageRightsApi(context);
        this.usersApi = new RightsManageUsersApi(context);
        this.groupsApi = new RightsManageGroupsApi(context);
    }

    /**
     * @return the API for managing rights and inheritance.
     */
    public RightsManageRightsApi getRightsApi()
    {
        return this.rightsApi;
    }

    /**
     * @return the API for managing rights and inheritance.
     */
    public RightsManageUsersApi getUsersApi()
    {
        return this.usersApi;
    }

    /**
     * @return the API for managing rights and inheritance.
     */
    public RightsManageGroupsApi getGroupsApi()
    {
        return this.groupsApi;
    }

    /**
     * Convert Map/List pattern matching parameter from Velocity to [][] used in
     * {@link RightsManager}.
     * 
     * @param map a map of list from Velocity.
     * @return a table of table for {@link RightsManager} methods.
     */
    static Object[][] createMatchingTable(Map map)
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
    static Object[][] createOrderTable(List list)
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

    /**
     * @return the user interface to use for rights management. Can be "stable" or "new".
     */
    public String getDefaultUi()
    {
        return this.context.getWiki().getConfig().getProperty(RIGHTS_UI_PROPERTY,
            RIGHTS_UI_VALUE_NEW);
    }

    /**
     * Modify the user interface to use for rights management.
     * 
     * @param ui "stable" or "new".
     */
    public void setDefaultUi(String ui)
    {
        this.context.getWiki().getConfig().setProperty(RIGHTS_UI_PROPERTY, ui);
    }

    // Groups management

    /**
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public int countAllGroups() throws XWikiException
    {
        return this.groupsApi.countAllGroups();
    }

    /**
     * Get the number of groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search for groups.
     * @return the number of groups in the provided wiki.
     * @throws XWikiException error when getting number of groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public int countAllWikiGroups(String wikiName) throws XWikiException
    {
        return this.groupsApi.countAllWikiGroups(wikiName);
    }

    /**
     * @return the number of groups in the main wiki.
     * @throws XWikiException error when getting number of groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public int countAllGlobalGroups() throws XWikiException
    {
        return this.groupsApi.countAllGlobalGroups();
    }

    /**
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public int countAllLocalGroups() throws XWikiException
    {
        return this.groupsApi.countAllLocalGroups();
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGroupsNames(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllGroupsNames(nb, start);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGroupsNames() throws XWikiException
    {
        return this.groupsApi.getAllGroupsNames();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroupsNames(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroupsNames(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroupsNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroupsNames(matchFields, nb, start, order);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGlobalGroupsNames(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllGlobalGroupsNames(nb, start);
    }

    /**
     * Get all groups names in the main wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGlobalGroupsNames() throws XWikiException
    {
        return this.groupsApi.getAllGlobalGroupsNames();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroupsNames(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroupsNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroupsNames(matchFields, nb, start, order);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllWikiGroupsNames(String wikiName, int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllWikiGroupsNames(wikiName, nb, start);
    }

    /**
     * Get all groups names in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllWikiGroupsNames(String wikiName) throws XWikiException
    {
        return this.groupsApi.getAllWikiGroupsNames(wikiName);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroupsNames(wikiName, matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroupsNames(wikiName, matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroupsNames(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroupsNames(wikiName, matchFields, nb, start,
            order);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllLocalGroupsNames(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllLocalGroupsNames(nb, start);
    }

    /**
     * Get all groups names in the current wiki.
     * 
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllLocalGroupsNames() throws XWikiException
    {
        return this.groupsApi.getAllLocalGroupsNames();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroupsNames(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroupsNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroupsNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroupsNames(matchFields, nb, start, order);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGroups(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllGroups(nb, start);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGroups() throws XWikiException
    {
        return this.groupsApi.getAllGroups();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroups(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroups(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroups(Map matchFields, int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroups(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGroups(matchFields, nb, start, order);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGlobalGroups(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllGlobalGroups(nb, start);
    }

    /**
     * Get all groups in the main wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllGlobalGroups() throws XWikiException
    {
        return this.groupsApi.getAllGlobalGroups();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroups(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroups(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroups(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroups(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedGlobalGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedGlobalGroups(matchFields, nb, start, order);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllWikiGroups(String wikiName, int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllWikiGroups(wikiName, nb, start);
    }

    /**
     * Get all groups in the provided wiki.
     * 
     * @param wikiName the name of the wiki where to search.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllWikiGroups(String wikiName) throws XWikiException
    {
        return this.groupsApi.getAllWikiGroups(wikiName);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroups(wikiName, matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroups(wikiName, matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedWikiGroups(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        return this.groupsApi.getAllMatchedWikiGroups(wikiName, matchFields, nb, start, order);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllLocalGroups(int nb, int start) throws XWikiException
    {
        return this.groupsApi.getAllLocalGroups(nb, start);
    }

    /**
     * Get all groups in the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing group.
     * @throws XWikiException error when searching for groups.
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllLocalGroups() throws XWikiException
    {
        return this.groupsApi.getAllLocalGroups();
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroups(Map matchFields) throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroups(matchFields);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroups(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroups(matchFields, nb, start);
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
     * @deprecated Use {@link #getGroupsApi()} to get {@link RightsManageGroupsApi}.
     */
    public List getAllMatchedLocalGroups(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.groupsApi.getAllMatchedLocalGroups(matchFields, nb, start, order);
    }

    // Users management

    /**
     * @return the number of users in the main wiki and the current wiki.
     * @throws XWikiException error when getting number of users.
     * @deprecated Use {@link #getUsersApi()}: {@link RightsManageUsersApi#countAllUsers()}.
     */
    public int countAllUsers() throws XWikiException
    {
        return this.usersApi.countAllUsers();
    }

    /**
     * Get the number of users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return the number of users in the provided wiki.
     * @throws XWikiException error when getting number of users.
     * @deprecated Use {@link #getUsersApi()}:
     *             {@link RightsManageUsersApi#countAllWikiUsers(String wikiName)}.
     */
    public int countAllWikiUsers(String wikiName) throws XWikiException
    {
        return this.usersApi.countAllWikiUsers(wikiName);
    }

    /**
     * @return the number of users in the main wiki.
     * @throws XWikiException error when getting number of users.
     * @deprecated Use {@link #getUsersApi()}: {@link RightsManageUsersApi#countAllGlobalUsers()}.
     */
    public int countAllGlobalUsers() throws XWikiException
    {
        return this.usersApi.countAllGlobalUsers();
    }

    /**
     * @return the number of users in the current wiki.
     * @throws XWikiException error when getting number of users.
     * @deprecated Use {@link #getUsersApi()}: {@link RightsManageUsersApi#countAllLocalUsers()}.
     */
    public int countAllLocalUsers() throws XWikiException
    {
        return this.usersApi.countAllLocalUsers();
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllUsersNames(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllUsersNames(nb, start);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllUsersNames() throws XWikiException
    {
        return this.usersApi.getAllUsersNames();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsersNames(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedUsersNames(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsersNames(Map matchFields, int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllMatchedUsersNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedUsersNames(matchFields, nb, start, order);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllGlobalUsersNames(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllGlobalUsersNames(nb, start);
    }

    /**
     * Get all users names in the main wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllGlobalUsersNames() throws XWikiException
    {
        return this.usersApi.getAllGlobalUsersNames();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsersNames(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsersNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsersNames(matchFields, nb, start, order);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllWikiUsersNames(String wikiName, int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllWikiUsersNames(wikiName, nb, start);
    }

    /**
     * Get all users names in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllWikiUsersNames(String wikiName) throws XWikiException
    {
        return this.usersApi.getAllWikiUsersNames(wikiName);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsersNames(wikiName, matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsersNames(wikiName, matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsersNames(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsersNames(wikiName, matchFields, nb, start, order);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllLocalUsersNames(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllLocalUsersNames(nb, start);
    }

    /**
     * Get all users names in the current wiki.
     * 
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllLocalUsersNames() throws XWikiException
    {
        return this.usersApi.getAllLocalUsersNames();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsersNames(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsersNames(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsersNames(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsersNames(matchFields, nb, start, order);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllUsers(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllUsers(nb, start);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllUsers() throws XWikiException
    {
        return this.usersApi.getAllUsers();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsers(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedUsers(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsers(Map matchFields, int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllMatchedUsers(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedUsers(matchFields, nb, start, order);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllGlobalUsers(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllGlobalUsers(nb, start);
    }

    /**
     * Get all users in the main wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllGlobalUsers() throws XWikiException
    {
        return this.usersApi.getAllGlobalUsers();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsers(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsers(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsers(Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsers(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedGlobalUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedGlobalUsers(matchFields, nb, start, order);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllWikiUsers(String wikiName, int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllWikiUsers(wikiName, nb, start);
    }

    /**
     * Get all users in the provided wiki.
     * 
     * @param wikiName the wiki where to search for users.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllWikiUsers(String wikiName) throws XWikiException
    {
        return this.usersApi.getAllWikiUsers(wikiName);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsers(wikiName, matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields, int nb, int start)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsers(wikiName, matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedWikiUsers(String wikiName, Map matchFields, int nb, int start,
        List order) throws XWikiException
    {
        return this.usersApi.getAllMatchedWikiUsers(wikiName, matchFields, nb, start, order);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllLocalUsers(int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllLocalUsers(nb, start);
    }

    /**
     * Get all users in the current wiki.
     * 
     * @return a {@link List} of {@link com.xpn.xwiki.api.Document} containing user.
     * @throws XWikiException error when searching for users.
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllLocalUsers() throws XWikiException
    {
        return this.usersApi.getAllLocalUsers();
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsers(Map matchFields) throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsers(matchFields);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsers(Map matchFields, int nb, int start) throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsers(matchFields, nb, start);
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
     * @deprecated Use {@link #getUsersApi()} to get {@link RightsManageUsersApi}.
     */
    public List getAllMatchedLocalUsers(Map matchFields, int nb, int start, List order)
        throws XWikiException
    {
        return this.usersApi.getAllMatchedLocalUsers(matchFields, nb, start, order);
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
                RightsManager.getInstance()
                    .getAllGroupsNamesForMember(member, 0, 0, this.context);
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

    /**
     * Return the number of groups containing provided member.
     * 
     * @param member the name of the member (user or group).
     * @return the number of groups.
     * @throws XWikiException error when getting number of users.
     */
    public int countAllGroupsNamesForMember(String member) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllGroupsNamesForMember(member, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count groups containing provided user", e);
        }

        return count;
    }

    /**
     * Return the number of members provided group contains.
     * 
     * @param group the name of the group.
     * @return the number of members.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMembersNamesForGroup(String group) throws XWikiException
    {
        int count = 0;

        try {
            count = RightsManager.getInstance().countAllMembersNamesForGroup(group, this.context);
        } catch (RightsManagerException e) {
            logError("Try to count all members provided group contains", e);
        }

        return count;
    }
}
