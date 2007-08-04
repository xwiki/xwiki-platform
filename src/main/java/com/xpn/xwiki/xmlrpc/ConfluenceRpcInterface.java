/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;

import java.util.Map;

/**
 * Implement the <a href="http://confluence.atlassian.com/display/DOC/Remote+API+Specification">
 * Confluence XML-RPC interface</a>. Provides all the operations that can be done remotely on a
 * XWiki instance using the XML-RPC protocol.
 * 
 * <p>
 * Note: Lots of the Javadoc comments below are borrowed from the <a
 * href="http://www.atlassian.com/software/jira/docs/api/rpc-jira-plugin/latest/index.html?com/atlassian/jira/rpc/xmlrpc/XmlRpcService.html">
 * Confluence Javadoc</a>.
 * </p>
 * 
 * @version $Id: $
 * @todo rename deletePage to removePage
 * @todo add missing functionality to fully support version 2.0 of Remote API Specification
 */
public interface ConfluenceRpcInterface
{
    // General

    /**
     * Retrieve some basic information about the server being connected to. Useful for clients that
     * need to turn certain features on or off depending on the version of the server.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return the Server information such as Server base URL and Server version. The returned map
     *         contains the fields from {@linkServerInfo}.
     * @throws XWikiException in case of error
     */
    Map getServerInfo(String token) throws XWikiException;

    // Authentication Methods

    /**
     * Logs the user into XWiki. The security token which is returned is used in all subsequent
     * method calls.
     * 
     * @param username the username of the person logged in as
     * @param password the appropriate password
     * @return A string which is a security token to be used in all subsequent calls
     * @throws XWikiException in case of error
     */
    String login(String username, String password) throws XWikiException;

    /**
     * Logs the user out of XWiki.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return whether the logging out was successful or not
     * @throws XWikiException in case of error
     */
    boolean logout(String token) throws XWikiException;

    // Spaces Retrieval

    /**
     * Get all SpaceSummaries the user can view.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @return all the SpaceSummaries that the current user can see.
     * @throws XWikiException in case of error
     */
    Object[] getSpaces(String token) throws XWikiException;

    /**
     * Get one Space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey identifier for space
     * @return a single Space object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Map getSpace(String token, String spaceKey) throws XWikiException;

    // Spaces Management

    /**
     * Create a new space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceProperties Map containing all informations, we need to create a new space. We
     *            need the following keys: - key "name": the name of the space - key "key": the
     *            space key - key "description": the space description
     * @return created Space as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Map addSpace(String token, Map spaceProperties) throws XWikiException;

    /**
     * Remove a space completely by removing all of it's child documents.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey the space to be deleted
     * @return whether the operation suceeded or not
     * @throws XWikiException in case of error
     */
    boolean removeSpace(String token, String spaceKey) throws XWikiException;
    
    // Page Retrieval

    /**
     * Get all the PageSummaries in the space.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey to look for pages in
     * @return a vector of PageSummaries as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] getPages(String token, String spaceKey) throws XWikiException;

    /**
     * Get one Page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to look for
     * @return a Page object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Map getPage(String token, String pageId) throws XWikiException;

    /**
     * Returns all the PageHistorySummaries.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to look for
     * @return a vector of PageHistories as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] getPageHistory(String token, String pageId) throws XWikiException;

    // Page Dependencies

    /**
     * Get all the comments for a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId page identifier to get comments from
     * @return a vector of Comment objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] getComments(String token, String pageId) throws XWikiException;

    // Page Dependencies

    /**
     * Get all the Attachments for a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId id of page from where we want all Attachments
     * @return a vector of Attachment objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] getAttachments(String token, String pageId) throws XWikiException;

    // Page Management

    /**
     * Add or update a page.
     * 
     * For adding, the Page given as an argument should have - space - title - content
     * 
     * For updating, the Page given should have - id - space - title - content - version
     * 
     * The parentId field is always optional. All other fields will be ignored.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param page a xml-rpc Page
     * @return a Page object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Map storePage(String token, Map page) throws XWikiException;

    /**
     * Returns the HTML rendered content for a page.
     * 
     * If 'content' is provided, then that is rendered as if it were the body of the page (useful
     * for a 'preview page' function). If it's not provided, then the existing content of the page
     * is used instead (ie useful for 'view page' function).
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param spaceKey in which space is our page
     * @param pageId id of page to get rendered HTML
     * @param content if this is set, it will replace the original content for rendering
     * @return string representing rendered content of page as HTML
     * @throws XWikiException in case of error
     */
    String renderContent(String token, String spaceKey, String pageId, String content)
        throws XWikiException;

    /**
     * Remove a page.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param pageId id of page to delete
     * @throws XWikiException in case of error
     */
    void deletePage(String token, String pageId) throws XWikiException;

    // Search

    /**
     * Get a list of SearchResults which match a given search query.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param query search query
     * @param maxResults number of maximal results
     * @return a Vector of SearchResults as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] search(String token, String query, int maxResults) throws XWikiException;

    // User Management

    /**
     * Get a single User.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username the name of the user we want the User Object
     * @return a User object as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Map getUser(String token, String username) throws XWikiException;

    /**
     * Add a new user with the given password.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param user object of new user
     * @param password of the new user
     * @throws XWikiException in case of error
     */
    void addUser(String token, Map user, String password) throws XWikiException;

    /**
     * Add a new group.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param group name of group to add
     * @throws XWikiException in case of error
     */
    void addGroup(String token, String group) throws XWikiException;

    /**
     * Get a user's current groups.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username for which we want to recive all groups
     * @return a Vector of Group objects as xml-rpc representation
     * @throws XWikiException in case of error
     */
    Object[] getUserGroups(String token, String username) throws XWikiException;

    /**
     * Add a user to a particular group.
     * 
     * @param token the authentication token retrieved when calling the login method
     * @param username name of user to add to a group
     * @param groupname name of group to add user
     * @throws XWikiException in case of error
     */
    void addUserToGroup(String token, String username, String groupname) throws XWikiException;
}
