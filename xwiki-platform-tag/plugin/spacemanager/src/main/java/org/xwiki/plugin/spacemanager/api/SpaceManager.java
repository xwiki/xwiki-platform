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
package org.xwiki.plugin.spacemanager.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

import java.util.List;
import java.util.Collection;

/**
 * The interface exposed by the spacemanager plugin
 * 
 * @version $Id: $
 */
public interface SpaceManager extends XWikiPluginInterface
{

    public static interface SpaceAction
    {
        String CREATE = "Create";

        String JOIN = "Join";
    }

    public static final String SPACE_DEFAULT_TYPE = "space";

    public static final String SPACE_CLASS_NAME = "XWiki.SpaceClass";

    String DEFAULT_RESOURCE_SPACE = "SpaceManagerResources";

    /**
     * Translate a space name to a space Wiki name
     * 
     * @param spaceTitle
     * @param unique
     * @param context
     */
    public String getSpaceWikiName(String spaceTitle, boolean unique, XWikiContext context);

    /**
     * Loads the SpaceManagerExtension specified in the config file
     * 
     * @throws SpaceManagerException
     */
    public SpaceManagerExtension getSpaceManagerExtension(XWikiContext context)
        throws SpaceManagerException;

    /**
     * Gets the name use to define spaces
     */
    public String getSpaceTypeName();

    /**
     * Gets the class name used to store class data
     */
    public String getSpaceClassName();

    /**
     * Create a space from scratch It will create an empty space or will copy the default space
     * template if there is one
     * 
     * @param spaceName the name of the space to create
     * @param context the xwiki context at creation time
     * @return On success returns the newly created space and null on failure
     * @throws SpaceManagerException
     */
    public Space createSpace(String spaceName, XWikiContext context) throws SpaceManagerException;

    /**
     * Create a space based on a template space
     * 
     * @param spaceName the name of the space to create
     * @param templateSpaceName the wikiname of the space to use as a template for the new space
     * @param context the xwiki context at creation time
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromTemplate(String spaceName, String templateSpaceName,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Create a space and install an application in the space An application is handled by the
     * ApplicationManager plugin and can include other sub-applications
     * 
     * @param spaceName the name of the space to create
     * @param applicationName the name of the application to install in the newly create space
     * @param context the xwiki context at creation time
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromApplication(String spaceName, String applicationName,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Create a space from HTTP request parameters.
     * 
     * @param context the xwiki context at creation time
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromRequest(XWikiContext context) throws SpaceManagerException;

    /**
     * Create a space from HTTP request parameters and a space template
     * 
     * @param templateSpaceName template space name to copy
     * @param context the xwiki context when creating the space
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromRequest(String templateSpaceName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Delete a space, including or not the space data
     * 
     * @param spaceName the name of the space to delete
     * @param deleteData if true, the documents held by the space will be deleted, otherwise they
     *            will be preserved in the wiki
     * @param context
     */
    public void deleteSpace(String spaceName, boolean deleteData, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Undelete a space that has been delete with the
     * 
     * @{link {@link #deleteSpace(String, boolean, XWikiContext)} method
     * @param spaceName
     * @param context
     */
    public void undeleteSpace(String spaceName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Lists spaces in the wiki
     * 
     * @param nb the maximum number of spaces to retrieve
     * @param start the offset to start retrieving spaces at
     * @return list of space objects
     */
    public List getSpaces(int nb, int start, XWikiContext context) throws SpaceManagerException;

    /**
     * Get a list of space names
     * 
     * @param nb the maximum number of space names to retrieve
     * @param start the offset to start retrieving the names at
     * @return list of space names
     */
    public List getSpaceNames(int nb, int start, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Search for spaces using an HQL query returning Space objects
     * 
     * @see #searchSpaceNames(String, String, int, int, XWikiContext) for parameters details.
     * @return list of space objects
     */
    public List searchSpaces(String fromsql, String wherehql, int nb, int start,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Search for spaces using an HQL query returning Space Names
     * 
     * @param fromsql the from clause of the hql query. Should start with a comma if not empty
     *            (since appended to the actual search spaces from clause)
     * @param wheresql the where clause of the hql query. Should start with " and" if not empty
     *            (since appended to the actual search spaces where clause).
     * @param nb the maximum number of spaces to retrieve
     * @param start the offset to start retrieving the spaces at.
     * @param context
     * @return list of space names matching the generated query
     */
    public List searchSpaceNames(String fromsql, String wheresql, int nb, int start,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Get the list of spaces for a user in a specific role If role is null it will get all spaces
     * in which the user is member return space name
     * 
     * @param userName the wikiname of the user to retrieve the spaces for
     * @param role the role to retrieve the spaces for the user. If null, retrieves every space
     *            where the user is in the member group
     * @return list of space objects
     */
    public List getSpaces(String userName, String role, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Get the list of spaces names for a user in a specific role
     * 
     * @see #getSpaces(String, String, XWikiContext) for parameters
     * @return list of space names
     */
    public List getSpaceNames(String userName, String role, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Get the space object corresponding to the space named "space"
     * 
     * @param spaceName the wiki name of the space to get
     */
    public Space getSpace(String spaceName, XWikiContext context) throws SpaceManagerException;

    /**
     * Updates a space object from the HTTP request data
     * 
     * @param space
     * @param context
     */
    public boolean updateSpaceFromRequest(Space space, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Validate that the space data is valid. Wrong data are stored in the context
     * 
     * @param space
     * @param context
     * @return
     */
    public boolean validateSpaceData(Space space, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Save the space data to the storage system
     * 
     * @param space the space to save
     */
    public void saveSpace(Space space, XWikiContext context) throws XWikiException;

    /**
     * Get the list of last modified documents in the space
     * 
     * @param spaceName The space in which the search is performed
     * @param context The XWikiContext of the request
     * @param recursive Determines if the search is performed in the child spaces too
     * @param nb Number of documents to be retrieved
     * @return start Pagination option saying at what document index to start the search
     */
    public List getLastModifiedDocuments(String spaceName, XWikiContext context,
        boolean recursive, int nb, int start) throws SpaceManagerException;

    /**
     * Search for documents in the space
     * 
     * @param spaceName
     * @param hql
     * @param context
     * @return
     */
    public List searchDocuments(String spaceName, String hql, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add a list of members to a space
     * 
     * @param spaceName the wiki name of the space to add the members to
     * @param usernames a list of wiki names of the users to add as members of the space
     * @throws SpaceManagerException
     */
    public void addMembers(String spaceName, List usernames, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add a member to a space
     * 
     * @param spaceName the wiki name of the space to add the member to
     * @param username the wiki name of the user to add as a member
     * @throws SpaceManagerException
     */
    public void addMember(String spaceName, String username, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Removes the member with the given name from the space with the specified name.
     * 
     * @param spaceName the wiki name of the space
     * @param userName the wiki name of the user
     * @throws SpaceManagerException
     */
    void removeMember(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Retrieve all members of a space
     * 
     * @param spaceName the wiki name of the space to retrieve the members for
     * @throws SpaceManagerException
     */
    public Collection getMembers(String spaceName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add a wiki user as admin in the space
     * 
     * @param spaceName the wiki name of the space
     * @param username the wiki name of the user to add as admin
     * @param context
     */
    public void addAdmin(String spaceName, String username, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add a list of admins in the space
     * 
     * @param spaceName the wiki name of the space
     * @param usernames a list of wiki names of users to add as admins
     */
    public void addAdmins(String spaceName, List usernames, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Removes the user with the given name from the list of admins of the specified space. The user
     * remains a member of the space.
     * 
     * @param spaceName the wiki name of the space
     * @param userName the wiki name of the admin to remove
     * @throws SpaceManagerException
     */
    void removeAdmin(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * @return the list of all members of the space that are admins
     */
    public Collection getAdmins(String spaceName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add user to the specific roles in the space
     * 
     * @param spaceName
     * @param username
     * @param roles
     * @param context
     */
    public void addUserToRoles(String spaceName, String username, List roles, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Add a list of users to a specific role in the space
     * 
     * @param spaceName
     * @param usernames
     * @param roles
     * @param context
     */
    public void addUsersToRoles(String spaceName, List usernames, List roles, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Removes the user with the given name from the list of users who have the given roles.
     * 
     * @param spaceName
     * @param userName
     * @param roles
     * @param context
     * @throws SpaceManagerException
     */
    void removeUserFromRoles(String spaceName, String userName, List roles, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Get the list of users for a role
     * 
     * @param spaceName
     * @param role
     * @param context
     * @return
     */
    public Collection getUsersForRole(String spaceName, String role, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Checks if a user is a member in a space
     * 
     * @param spaceName
     * @param user
     * @param context
     * @return
     * @throws SpaceManagerException
     */
    public boolean isMember(String spaceName, String user, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Checks if the user with the given name is an admin of the specified space
     * 
     * @param spaceName
     * @param userName
     * @param context
     * @return
     * @throws SpaceManagerException
     */
    boolean isAdmin(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * @param spaceName
     * @param context
     * @return
     */
    public Collection getRoles(String spaceName, XWikiContext context)
        throws SpaceManagerException;

    /**
     * @param spaceName
     * @param user
     * @param context
     * @return
     */
    public SpaceUserProfile getSpaceUserProfile(String spaceName, String user,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Get the space user profile page name
     * 
     * @param userName
     * @param spaceName
     * @return
     */
    public String getSpaceUserProfilePageName(String userName, String spaceName);

    /**
     * Count spaces
     * 
     * @param context
     * @return int
     */
    public int countSpaces(XWikiContext context) throws SpaceManagerException;

    /**
     * Allows the current user to join the space
     * 
     * @param spaceName
     * @param context
     * @return
     */
    public boolean joinSpace(String spaceName, XWikiContext context) throws SpaceManagerException;

    /**
     * Set the rights in the space
     * 
     * @param space
     * @param context
     * @throws SpaceManagerException
     */
    public void setSpaceRights(Space space, XWikiContext context) throws SpaceManagerException;

    /**
     * Set the rights in the space
     * 
     * @param space
     * @param oldPolicy previous policy
     * @param newPolicy new policy
     * @param context
     * @throws SpaceManagerException
     */
    public void updateSpaceRights(Space space, String oldPolicy, String newPolicy,
        XWikiContext context) throws SpaceManagerException;

    /**
     * Set the rights in the sub-space
     * 
     * @param space
     * @param subSpace
     * @param context
     * @throws SpaceManagerException
     */
    public void setSubSpaceRights(Space space, String subSpace, XWikiContext context)
        throws SpaceManagerException;

    /**
     * Get the list of sub-spaces to protect
     * 
     * @param context
     * @return
     * @throws SpaceManagerException
     */
    public String[] getProtectedSubSpaces(XWikiContext context) throws SpaceManagerException;

}
