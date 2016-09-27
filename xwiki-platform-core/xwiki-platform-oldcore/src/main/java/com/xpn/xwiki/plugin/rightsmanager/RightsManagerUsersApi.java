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
 * API for managing users.
 *
 * @version $Id$
 * @since 1.1.2
 * @since 1.2M2
 */
public class RightsManagerUsersApi extends Api
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
    protected static final Logger LOGGER = LoggerFactory.getLogger(RightsManagerUsersApi.class);

    /**
     * Create an instance of RightsManageRightsApi.
     *
     * @param context the XWiki context.
     */
    public RightsManagerUsersApi(XWikiContext context)
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
    public int countAllMatchedUsers(Map<?, ?> matchFields) throws XWikiException
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
    public int countAllMatchedWikiUsers(String wikiName, Map<?, ?> matchFields) throws XWikiException
    {
        int count = 0;

        try {
            count =
                RightsManager.getInstance().countAllWikiUsersOrGroups(true, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), this.context);
        } catch (RightsManagerException e) {
            logError(MessageFormat.format("Try to count all users in wiki [{0}]", new Object[] { wikiName }), e);
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
    public int countAllMatchedGlobalUsers(Map<?, ?> matchFields) throws XWikiException
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
    public int countAllMatchedLocalUsers(Map<?, ?> matchFields) throws XWikiException
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

    /**
     * Get all users names in the main wiki and the current wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List<String> getAllUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the main wiki and the current wiki.
     *
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List<String> getAllUsersNames() throws XWikiException
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
    public List<String> getAllMatchedUsersNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedUsersNames(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
    public List<String> getAllMatchedUsersNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> userList;

        try {
            userList =
                (List<String>) RightsManager.getInstance().getAllMatchedUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names", e);

            userList = Collections.emptyList();
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
    public List<String> getAllGlobalUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the main wiki.
     *
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List<String> getAllGlobalUsersNames() throws XWikiException
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
    public List<String> getAllMatchedGlobalUsersNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedGlobalUsersNames(Map<?, ?> matchFields, int nb, int start)
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
    public List<String> getAllMatchedGlobalUsersNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> userList;

        try {
            userList =
                (List<String>) RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from global wiki", e);

            userList = Collections.emptyList();
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
    public List<String> getAllWikiUsersNames(String wikiName, int nb, int start) throws XWikiException
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
    public List<String> getAllWikiUsersNames(String wikiName) throws XWikiException
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
    public List<String> getAllMatchedWikiUsersNames(String wikiName, Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedWikiUsersNames(String wikiName, Map<?, ?> matchFields, int nb, int start)
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
    public List<String> getAllMatchedWikiUsersNames(String wikiName, Map<?, ?> matchFields, int nb, int start,
        List<?> order) throws XWikiException
    {
        List<String> userList;

        try {
            userList =
                (List<String>) RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(true, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from provided wiki", e);

            userList = Collections.emptyList();
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
    public List<String> getAllLocalUsersNames(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalUsersNames(null, nb, start);
    }

    /**
     * Get all users names in the current wiki.
     *
     * @return a {@link List} of {@link String} containing user names.
     * @throws XWikiException error when searching for users.
     */
    public List<String> getAllLocalUsersNames() throws XWikiException
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
    public List<String> getAllMatchedLocalUsersNames(Map<?, ?> matchFields) throws XWikiException
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
    public List<String> getAllMatchedLocalUsersNames(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
    public List<String> getAllMatchedLocalUsersNames(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<String> userList;

        try {
            userList =
                (List<String>) RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), false, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users names from local wiki", e);

            userList = Collections.emptyList();
        }

        return userList;
    }

    /**
     * Get all users in the main wiki and the current wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedUsers(null, nb, start);
    }

    /**
     * Get all users in the main wiki and the current wiki.
     *
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllUsers() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedUsers(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedUsers(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedUsers(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> userList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            userList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users", e);

            userList = Collections.emptyList();
        }

        return userList;
    }

    /**
     * Get all users in the main wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllGlobalUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedGlobalUsers(null, nb, start);
    }

    /**
     * Get all users in the main wiki.
     *
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllGlobalUsers() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedGlobalUsers(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedGlobalUsers(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedGlobalUsers(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> userList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedGlobalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            userList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from global wiki", e);

            userList = Collections.emptyList();
        }

        return userList;
    }

    /**
     * Get all users in the provided wiki.
     *
     * @param wikiName the wiki where to search for users.
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllWikiUsers(String wikiName, int nb, int start) throws XWikiException
    {
        return getAllMatchedWikiUsers(wikiName, null, nb, start);
    }

    /**
     * Get all users in the provided wiki.
     *
     * @param wikiName the wiki where to search for users.
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllWikiUsers(String wikiName) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedWikiUsers(String wikiName, Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedWikiUsers(String wikiName, Map<?, ?> matchFields, int nb, int start)
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedWikiUsers(String wikiName, Map<?, ?> matchFields, int nb, int start,
        List<?> order) throws XWikiException
    {
        List<Document> userList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedWikiUsersOrGroups(true, wikiName,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            userList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from provided wiki", e);

            userList = Collections.emptyList();
        }

        return userList;
    }

    /**
     * Get all users in the current wiki.
     *
     * @param nb the maximum number of result to return.
     * @param start the index of the first found user to return.
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllLocalUsers(int nb, int start) throws XWikiException
    {
        return getAllMatchedLocalUsers(null, nb, start);
    }

    /**
     * Get all users in the current wiki.
     *
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllLocalUsers() throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedLocalUsers(Map<?, ?> matchFields) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedLocalUsers(Map<?, ?> matchFields, int nb, int start) throws XWikiException
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
     * @return a {@link List} of {@link Document} containing user.
     * @throws XWikiException error when searching for users.
     */
    public List<Document> getAllMatchedLocalUsers(Map<?, ?> matchFields, int nb, int start, List<?> order)
        throws XWikiException
    {
        List<Document> userList;

        try {
            List<XWikiDocument> xdocList =
                (List<XWikiDocument>) RightsManager.getInstance().getAllMatchedLocalUsersOrGroups(true,
                    RightsManagerPluginApi.createMatchingTable(matchFields), true, new RequestLimit(nb, start),
                    RightsManagerPluginApi.createOrderTable(order), this.context);

            userList = convert(xdocList);
        } catch (RightsManagerException e) {
            logError("Try to get all matched users from local wiki", e);

            userList = Collections.emptyList();
        }

        return userList;
    }
}
