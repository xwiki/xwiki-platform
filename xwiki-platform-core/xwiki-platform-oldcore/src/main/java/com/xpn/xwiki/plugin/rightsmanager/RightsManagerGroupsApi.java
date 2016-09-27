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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.rightsmanager.utils.RequestLimit;

/**
 * API for managing groups.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerGroupsApi extends Api
{
    /**
     * Field name of the last error code inserted in context.
     */
    public static final String CONTEXT_LASTERRORCODE = RightsManagerPluginApi.CONTEXT_LASTERRORCODE;

    /**
     * Field name of the last api exception inserted in context.
     */
    public static final String CONTEXT_LASTEXCEPTION = RightsManagerPluginApi.CONTEXT_LASTEXCEPTION;

    /**
     * The logging toolkit.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerGroupsApi.class);

    /**
     * Create an instance of RightsManageRightsApi.
     *
     * @param context the XWiki context.
     */
    public RightsManagerGroupsApi(XWikiContext context)
    {
        super(context);
    }

    /**
     * Log error and register {@link #CONTEXT_LASTERRORCODE} and {@link #CONTEXT_LASTEXCEPTION}.
     *
     * @param comment the comment to use with {@link #LOGGER}.
     * @param e the exception.
     */
    private void logError(String comment, XWikiException e)
    {
        LOGGER.error(comment, e);

        this.context.put(CONTEXT_LASTERRORCODE, e.getCode());
        this.context.put(CONTEXT_LASTEXCEPTION, e);
    }

    // Count

    /**
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllGroups() throws XWikiException
    {
        return countAllMatchedGroups(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMatchedGroups(Map<?, ?> matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
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
        return countAllMatchedWikiGroups(wikiName, null);
    }

    /**
     * Get the number of groups in the provided wiki.
     *
     * @param wikiName the name of the wiki where to search for groups.
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of groups in the provided wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMatchedWikiGroups(String wikiName, Map<?, ?> matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllWikiUsersOrGroups(false, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError(MessageFormat.format("Try to count all groups in wiki [{0}]", new Object[] { wikiName }), e);
        }

        return count;
    }

    /**
     * @return the number of groups in the main wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllGlobalGroups() throws XWikiException
    {
        return countAllMatchedGlobalGroups(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of groups in the main wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMatchedGlobalGroups(Map<?, ?> matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllGlobalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
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
        return countAllMatchedLocalGroups(null);
    }

    /**
     * @param matchFields the fields to match. It is a Map with field name as key and for value :
     *            <ul>
     *            <li>"matching string" for document fields</li>
     *            <li>or ["field type", "matching string"] for object fields</li>
     *            </ul>
     * @return the number of groups in the current wiki.
     * @throws XWikiException error when getting number of groups.
     */
    public int countAllMatchedLocalGroups(Map<?, ?> matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllLocalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
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
    public List<String> getAllGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the main wiki and the current wiki.
     *
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List<String> getAllGroupsNames() throws XWikiException
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
    public List<String> getAllMatchedGroupsNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedGroupsNames(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
    public List<String> getAllMatchedGroupsNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> groupList;

        try {
            groupList =
                (List<String>) RightsManager.getInstance().getAllMatchedUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names", e);

            groupList = Collections.emptyList();
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
    public List<String> getAllGlobalGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the main wiki.
     *
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List<String> getAllGlobalGroupsNames() throws XWikiException
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
    public List<String> getAllMatchedGlobalGroupsNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedGlobalGroupsNames(Map<?, ?> matchFields, int nb, int start)
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
    public List<String> getAllMatchedGlobalGroupsNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> groupList;

        try {
            groupList =
                (List<String>) RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from global wiki", e);

            groupList = Collections.emptyList();
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
    public List<String> getAllWikiGroupsNames(String wikiName, int nb, int start) throws XWikiException
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
    public List<String> getAllWikiGroupsNames(String wikiName) throws XWikiException
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
    public List<String> getAllMatchedWikiGroupsNames(String wikiName, Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedWikiGroupsNames(String wikiName, Map<?, ?> matchFields, int nb, int start)
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
    public List<String> getAllMatchedWikiGroupsNames(String wikiName, Map<?, ?> matchFields, int nb, int start,
        List<?> order) throws XWikiException
    {
        List<String> groupList;

        try {
            groupList =
                (List<String>) RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(false, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from provided wiki", e);

            groupList = Collections.emptyList();
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
    public List<String> getAllLocalGroupsNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalGroupsNames(null, nb, start);
    }

    /**
     * Get all groups names in the current wiki.
     *
     * @return a {@link List} of {@link String} containing group names.
     * @throws XWikiException error when searching for groups.
     */
    public List<String> getAllLocalGroupsNames() throws XWikiException
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
    public List<String> getAllMatchedLocalGroupsNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedLocalGroupsNames(Map<?, ?> matchFields, int nb, int start)
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
    public List<String> getAllMatchedLocalGroupsNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> groupList;

        try {
            groupList =
                (List<String>) RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups names from local wiki", e);

            groupList = Collections.emptyList();
        }

        return groupList;
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedGroups(null, nb, start);
    }

    /**
     * Get all groups in the main wiki and the current wiki.
     *
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllGroups() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGroups(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGroups(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGroups(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> groupList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            groupList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents", e);

            groupList = Collections.emptyList();
        }

        return groupList;
    }

    /**
     * Get all groups in the main wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllGlobalGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalGroups(null, nb, start);
    }

    /**
     * Get all groups in the main wiki.
     *
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllGlobalGroups() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGlobalGroups(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGlobalGroups(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedGlobalGroups(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> groupList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            groupList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from global wiki", e);

            groupList = Collections.emptyList();
        }

        return groupList;
    }

    /**
     * Get all groups in the provided wiki.
     *
     * @param wikiName the name of the wiki where to search.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllWikiGroups(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiGroups(wikiName, null, nb, start);
    }

    /**
     * Get all groups in the provided wiki.
     *
     * @param wikiName the name of the wiki where to search.
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllWikiGroups(String wikiName) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedWikiGroups(String wikiName, Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedWikiGroups(String wikiName, Map<?, ?> matchFields, int nb, int start)
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedWikiGroups(String wikiName, Map<?, ?> matchFields, int nb, int start,
        List<?> order) throws XWikiException
    {
        List<Document> groupList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(false, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            groupList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from provided wiki", e);

            groupList = Collections.emptyList();
        }

        return groupList;
    }

    /**
     * Get all groups in the current wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found group to return.
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllLocalGroups(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalGroups(null, nb, start);
    }

    /**
     * Get all groups in the current wiki.
     *
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllLocalGroups() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedLocalGroups(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedLocalGroups(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing group.
     * @throws XWikiException error when searching for groups.
     */
    public List<Document> getAllMatchedLocalGroups(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> groupList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(false,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            groupList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched groups documents from local wiki", e);

            groupList = Collections.emptyList();
        }

        return groupList;
    }
}
