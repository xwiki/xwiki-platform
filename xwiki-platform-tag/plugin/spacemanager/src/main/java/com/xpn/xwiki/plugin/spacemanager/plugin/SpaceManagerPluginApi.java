/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.spacemanager.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.spacemanager.api.Space;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManager;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagerException;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceUserProfile;

/**
 * Api for creating and retrieving Spaces
 * The plugin will contain SpaceManager api calls to the underlying space manager plugin.
 * Security will be handled by this plugin.
 * 
 * @version $Id: $
 */
public class SpaceManagerPluginApi extends PluginApi
{
    public static String getVersion()
    {
        return "dd";
    }

    public SpaceManagerPluginApi(SpaceManager plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    protected SpaceManager getSpaceManager()
    {
        return (SpaceManager) getProtectedPlugin();
    }

    /**
     * @return the Space associated with the context web
     */
    public Space getCurrentSpace() throws SpaceManagerException
    {
        return getSpace(context.getDoc().getSpace());
    }

    public Space getSpace(String spaceName) throws SpaceManagerException
    {
        Space space = getSpaceManager().getSpace(spaceName, context);
        return space;
    }

    /**
     * Create a space from scratch It will create an empty space or will copy the default space
     * template if there is one
     * 
     * @param spaceName
     * @return On success returns the newly created space and null on failure
     * @throws SpaceManagerException
     */
    public Space createSpace(String spaceName) throws SpaceManagerException
    {
        Space space;
        try {
            if (!hasProgrammingRights())
                return null;
            space = getSpaceManager().createSpace(spaceName, context);
        } catch (SpaceManagerException e) {
            if (e.getCode() == SpaceManagerException.ERROR_SPACE_DATA_INVALID) {
                return null;
            } else {
                throw e;
            }
        }
        return space;
    }

    /**
     * Create a space based on a template space
     * 
     * @param spaceName
     * @param templateSpaceName
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromTemplate(String spaceName, String templateSpaceName)
        throws SpaceManagerException
    {
        Space space;
        try {
            if (!hasProgrammingRights())
                return null;
            space =
                getSpaceManager().createSpaceFromTemplate(spaceName, templateSpaceName, context);
        } catch (SpaceManagerException e) {
            if (e.getCode() == SpaceManagerException.ERROR_SPACE_DATA_INVALID) {
                return null;
            } else {
                throw e;
            }
        }
        return space;
    }

    /**
     * Create a space and install an application in the space An application is handled by the
     * ApplicationManager plugin and can include other sub-applications
     * 
     * @param spaceName
     * @param applicationName
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromApplication(String spaceName, String applicationName)
        throws SpaceManagerException
    {
        Space space;
        try {
            if (!hasProgrammingRights())
                return null;
            space =
                getSpaceManager().createSpaceFromApplication(spaceName, applicationName, context);
        } catch (SpaceManagerException e) {
            if (e.getCode() == SpaceManagerException.ERROR_SPACE_DATA_INVALID) {
                return null;
            } else {
                throw e;
            }
        }
        return space;
    }

    /**
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromRequest() throws SpaceManagerException
    {
        Space space;
        try {
            if (!hasProgrammingRights())
                return null;
            space = getSpaceManager().createSpaceFromRequest(context);
        } catch (SpaceManagerException e) {
            if (e.getCode() == SpaceManagerException.ERROR_SPACE_DATA_INVALID) {
                return null;
            } else {
                throw e;
            }
        }
        return space;
    }

    /**
     * @return On success returns the newly created space and null on failure
     */
    public Space createSpaceFromRequest(String templateSpace) throws SpaceManagerException
    {
        Space space;
        try {
            if (!hasProgrammingRights())
                return null;
            space = getSpaceManager().createSpaceFromRequest(templateSpace, context);
        } catch (SpaceManagerException e) {
            if (e.getCode() == SpaceManagerException.ERROR_SPACE_DATA_INVALID) {
                return null;
            } else {
                throw e;
            }
        }
        return space;
    }

    /**
     * Delete a space, including or not the space data
     * 
     * @param spaceName
     * @param deleteData
     */

    public boolean deleteSpace(String spaceName, boolean deleteData) throws SpaceManagerException
    {
        if (!hasProgrammingRights())
            return false;
        getSpaceManager().deleteSpace(spaceName, deleteData, context);
        return true;
    }

    /**
     * @param spaceName
     */
    public boolean undeleteSpace(String spaceName) throws SpaceManagerException
    {
        if (!hasProgrammingRights())
            return false;
        getSpaceManager().undeleteSpace(spaceName, context);
        return true;

    }

    /**
     * Get the list of space objects
     * 
     * @param nb
     * @param start
     * @return list of space objects
     */
    public List getSpaces(int nb, int start) throws SpaceManagerException
    {
        List spacesList = getSpaceManager().getSpaces(nb, start, context);
        return spacesList;
    }

    /**
     * Get the list of space objects
     * 
     * @param nb
     * @param start
     * @param ordersql
     * @return list of space objects
     */
    public List getSpaces(int nb, int start, String ordersql) throws SpaceManagerException
    {
        List spacesList = getSpaceManager().getSpaces(nb, start, ordersql, context);
        return spacesList;
    }

    /**
     * Get the list of space objects
     * 
     * @param start
     * @param nb
     * @return list of space names
     */
    public List getSpaceNames(int nb, int start) throws SpaceManagerException
    {
        return getSpaceManager().getSpaceNames(nb, start, context);
    }

    /**
     * Get the list of space objects
     * 
     * @param start
     * @param nb
     * @param ordersql
     * @return list of space names
     */
    public List getSpaceNames(int nb, int start, String ordersql) throws SpaceManagerException
    {
        return getSpaceManager().getSpaceNames(nb, start, ordersql, context);
    }

    /**
     * Performs a search for spaces. This variant returns the spaces ordered ascending by creation
     * date
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @return A list with space objects matching the search
     * @throws SpaceManagerException
     */
    public List searchSpaces(String fromsql, String wheresql, int nb, int start)
        throws SpaceManagerException
    {
        List spacesList = getSpaceManager().searchSpaces(fromsql, wheresql, nb, start, context);
        return spacesList;

    }

    /**
     * Performs a search for spaces
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param ordersql The sql fragment describing the order in wich the spaces should be returned
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @return A list with space objects matching the search
     * @throws SpaceManagerException
     */
    public List searchSpaces(String fromsql, String wheresql, String ordersql, int nb, int start)
        throws SpaceManagerException
    {
        List spacesList =
            getSpaceManager().searchSpaces(fromsql, wheresql, ordersql, nb, start, context);
        return spacesList;
    }

    /**
     * Performs a search for space names. This variant returns the spaces ordered ascending by
     * creation date
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @param context XWiki context
     * @return A list of strings representing the names of the spaces matching the search
     * @throws SpaceManagerException
     */
    public List searchSpaceNames(String fromsql, String wheresql, int nb, int start)
        throws SpaceManagerException
    {
        return getSpaceManager().searchSpaceNames(fromsql, wheresql, nb, start, context);
    }

    /**
     * Performs a search for space names
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param ordersql The sql fragment describing the order in wich the spaces should be returned
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @return A list of strings representing the names of the spaces matching the search
     * @throws SpaceManagerException
     */
    public List searchSpaceNames(String fromsql, String wheresql, String ordersql, int nb,
        int start) throws SpaceManagerException
    {
        return getSpaceManager()
            .searchSpaceNames(fromsql, wheresql, ordersql, nb, start, context);
    }

    /**
     * Get the list of spaces for a user in a specific role If role is null it will get all spaces
     * in which the user is member return space name
     * 
     * @param userName
     * @param role
     * @return list of space objects
     */
    public List getSpaces(String userName, String role) throws SpaceManagerException
    {
        List spacesList = getSpaceManager().getSpaces(userName, role, context);
        return spacesList;
    }

    /**
     * Get the list of spaces for a user in a specific role If role is null it will get all spaces
     * in which the user is member
     * 
     * @param userName
     * @param role
     * @return list of space names
     */
    public List getSpaceNames(String userName, String role) throws SpaceManagerException
    {
        return getSpaceManager().getSpaceNames(userName, role, context);
    }

    /**
     * Updates a space object from the HTTP request data
     * 
     * @param space
     */
    public boolean updateSpaceFromRequest(Space space) throws SpaceManagerException
    {
        if (!hasProgrammingRights())
            return false;

        return getSpaceManager().updateSpaceFromRequest(space, context);
    }

    /**
     * Validate that the space data is valid. Wrong data are stored in the context
     * 
     * @param space
     * @return
     */
    public boolean validateSpaceData(Space space) throws SpaceManagerException
    {
        return getSpaceManager().validateSpaceData(space, context);
    }

    /**
     * Save the space data to the storage system
     * 
     * @param space
     */
    // public void saveSpace(Space space) throws SpaceManagerException;
    /**
     * Get the list of last modified documents in the space
     * 
     * @param space The space in which the search is performed
     * @param recursive Determines if the search is performed in the child spaces too
     * @param nb Number of documents to be retrieved
     * @return start Pagination option saying at what document index to start the search
     */
    public List getLastModifiedDocuments(String spaceName, boolean recursive, int nb, int start)
        throws SpaceManagerException
    {
        return getSpaceManager().getLastModifiedDocuments(spaceName, context, recursive, nb,
            start);
    }

    /**
     * Return the list of members of the space
     * 
     * @param spaceName
     * @throws SpaceManagerException
     */
    public Collection getMembers(String spaceName) throws SpaceManagerException
    {
        return getSpaceManager().getMembers(spaceName, context);
    }

    /**
     * Join the space
     * 
     * @param spaceName
     */
    public boolean joinSpace(String spaceName) throws SpaceManagerException
    {
        Space space = getSpace(spaceName);
        if ("open".equals(space.getPolicy())) {
            return getSpaceManager().joinSpace(spaceName, context);
        } else {
            return false;
        }
    }

    /**
     * Add a wiki user as member in the space
     * 
     * @param spaceName
     * @param wikiname
     */
    public void addMember(String spaceName, String wikiname) throws SpaceManagerException
    {
        if (hasProgrammingRights())
            getSpaceManager().addMember(spaceName, wikiname, context);
    }

    /**
     * Removes a wiki user from the list of space members
     * 
     * @param spaceName
     * @param wikiName
     * @throws SpaceManagerException
     */
    public void removeMember(String spaceName, String wikiName) throws SpaceManagerException
    {
        if (hasProgrammingRights()) {
            getSpaceManager().removeMember(spaceName, wikiName, context);
        }
    }

    /**
     * Add a wiki user as admin in the space
     * 
     * @param spaceName
     * @param wikiname
     */
    public void addAdmin(String spaceName, String wikiname) throws SpaceManagerException
    {
        if (hasProgrammingRights())
            getSpaceManager().addAdmin(spaceName, wikiname, context);
    }

    /**
     * Removes a wiki user from the list of space admins
     * 
     * @param spaceName
     * @param wikiName
     * @throws SpaceManagerException
     */
    public void removeAdmin(String spaceName, String wikiName) throws SpaceManagerException
    {
        if (hasProgrammingRights()) {
            getSpaceManager().removeAdmin(spaceName, wikiName, context);
        }
    }


    /**
     * @return the list of all members of the space that are admins
     */
    public Collection getAdmins(String spaceName) throws SpaceManagerException
    {
        return getSpaceManager().getAdmins(spaceName, context);
    }


    /**
     * Get the list of users for a role
     * 
     * @param space
     * @param role
     * @return
     */
    public Collection getUsersForRole(String spaceName, String role)
    {
        context.remove("SpaceManagerException");
        try {
            return getSpaceManager().getUsersForRole(spaceName, role, context);
        } catch (SpaceManagerException e) {
            context.put("SpaceManagerException", e);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * @return the list of roles available for the given space
     */
    public Collection getRoles(String spaceName)
    {
        context.remove("SpaceManagerException");
        try {
            return getSpaceManager().getRoles(spaceName, context);
        } catch (SpaceManagerException e) {
            context.put("SpaceManagerException", e);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * @return the list of roles the specified user has as a member of the specified space
     */
    public Collection getRoles(String spaceName, String memberName)
    {
        context.remove("SpaceManagerException");
        try {
            return getSpaceManager().getRoles(spaceName, memberName, context);
        } catch (SpaceManagerException e) {
            context.put("SpaceManagerException", e);
            return Collections.EMPTY_LIST;
        }
    }

    /**
     * Gets a user profile object
     * 
     * @param spaceName
     * @param user
     * @return
     */
    public SpaceUserProfile getSpaceUserProfile(String spaceName, String user)
        throws SpaceManagerException
    {
        return getSpaceManager().getSpaceUserProfile(spaceName, user, context);
    }

    /**
     * Count number of spaces
     * 
     * @return
     */
    public int countSpaces() throws SpaceManagerException
    {
        return getSpaceManager().countSpaces(context);
    }

    public boolean isMember(String spaceName, String username) throws SpaceManagerException
    {
        return getSpaceManager().isMember(spaceName, username, context);
    }

    public boolean isAdmin(String spaceName, String userName) throws SpaceManagerException
    {
        return getSpaceManager().isAdmin(spaceName, userName, context);
    }

    public void updateSpaceRights(Space space, String oldPolicy, String newPolicy)
        throws SpaceManagerException
    {
        if (hasProgrammingRights()) {
            getSpaceManager().updateSpaceRights(space, oldPolicy, newPolicy, context);
        }
    }

    public void setSpaceRights(Space space) throws SpaceManagerException
    {
        if (hasProgrammingRights()) {
            getSpaceManager().setSpaceRights(space, context);
        }
    }

    public void setSubSpaceRights(Space space, String subSpace) throws SpaceManagerException
    {
        if (hasProgrammingRights()) {
            getSpaceManager().setSubSpaceRights(space, subSpace, context);
        }
    }

}
