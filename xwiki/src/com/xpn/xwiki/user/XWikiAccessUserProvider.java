/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 12 avr. 2004
 * Time: 10:24:35
 */

package com.xpn.xwiki.user;

import com.opensymphony.module.access.provider.UserProvider;
import com.opensymphony.module.access.provider.osuser.PropertySetMethods;
import com.opensymphony.module.access.entities.Group_I;
import com.opensymphony.module.access.entities.User_I;
import com.opensymphony.module.access.*;
import com.opensymphony.module.user.*;
import com.opensymphony.module.user.ImmutableException;
import com.opensymphony.module.propertyset.PropertySet;

import javax.ejb.CreateException;
import java.util.*;

public class XWikiAccessUserProvider extends XWikiBaseProvider implements UserProvider {

    public UserManager getUserManager() {
        return getXWiki().getUserManager(context);
    }

    public String getRealm() {
        return "default";
    }

    public boolean handles(String name) {
            return true;
    }

    public Group_I getGroup(String groupId) throws NotFoundException {
        try {
            return new OsGroup(getUserManager().getGroup(groupId));
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     *
     * Since OSUSer does not support groups inside of groups always return false
     *
     * @param childGroupId
     * @param parentGroupId
     * @return false
     * @throws NotFoundException
     */
    public boolean isGroupInGroup(String childGroupId, String parentGroupId) throws NotFoundException {
        return false;
    }

    public ArrayList getGroups() throws NotFoundException {
        UserManager um = getUserManager();
        Collection groups = um.getGroups();
        ArrayList ret = new ArrayList();

        for (Iterator itr = groups.iterator(); itr.hasNext();) {
            com.opensymphony.module.user.Group group = (com.opensymphony.module.user.Group) itr.next();
            ret.add(new OsGroup(group));
        }

        return ret;
    }

    public User_I getUser(String userId) throws NotFoundException {
        try {
            return new OsUser(getUserManager().getUser(userId));
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Return true if the user is in the group
     *
     * @param userId
     * @param groupId
     * @return true if the user is in the group
     * @throws NotFoundException
     */
    public boolean isUserInGroup(String userId, String groupId) throws NotFoundException {
        try {
            UserManager um = getUserManager();
            User user = um.getUser(userId);

            return user.inGroup(groupId);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    public ArrayList getUsers() throws NotFoundException {
        UserManager um = getUserManager();
        List users = um.getUsers();
        ArrayList ret = new ArrayList();

        for (int i = 0; i < users.size(); i++) {
            com.opensymphony.module.user.User user = (com.opensymphony.module.user.User) users.get(i);
            ret.add(new OsUser(user));
        }

        return ret;
    }

    public ArrayList getUsersForGroup(String groupId) throws NotFoundException {
        Group_I group = getGroup(groupId);

        if (group != null) {
            return group.getUsers();
        } else {
            return new ArrayList();
        }
    }

    /**
     * Add chil;d group to the parent group
     *
     * @param childGroupId
     * @param parentGroupId
     * @throws NotFoundException parent group not found
     * @throws com.opensymphony.module.access.ImmutableException Datastore is not writable
     */
    public void addGroupToGroup(String childGroupId, String parentGroupId) throws NotFoundException, com.opensymphony.module.access.ImmutableException {
        throw new com.opensymphony.module.access.ImmutableException("OSUser does not support adding Groups to Groups");
    }

    /**
     * Add the passed user Id to the passed group
     *
     * @param userId
     * @param groupId
     * @throws NotFoundException
     */
    public void addUserToGroup(String userId, String groupId) throws NotFoundException {
        try {
            UserManager um = getUserManager();
            User user = um.getUser(userId);
            user.addToGroup(um.getGroup(groupId));
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    /**
     * Create a new Group
     * @param name
     * @param optionParams
     * @throws com.opensymphony.module.access.DuplicateKeyException
     */
    public void createGroup(String name, PropertySet optionParams) throws DuplicateKeyException, CreateException {
        try {
            UserManager um = getUserManager();
            Group group = um.createGroup(name);

            OsGroup og = new OsGroup(group);
            og.setProperties(optionParams);
        } catch (DuplicateEntityException e) {
            throw new DuplicateKeyException(e.getMessage());
        } catch (com.opensymphony.module.user.ImmutableException e) {
            throw new CreateException(e.getMessage());
        }
    }

    /**
     * Create a new user
     *
     * @param userId
     * @param password
     * @param optionalParams
     * @throws DuplicateKeyException
     */
    public void createUser(String userId, String password, PropertySet optionalParams) throws DuplicateKeyException, CreateException {
        try {
            UserManager um = getUserManager();
            User user = um.createUser(userId);
            user.setPassword(password);

            OsUser ou = new OsUser(user);
            ou.setProperties(optionalParams);
        } catch (DuplicateEntityException e) {
            throw new DuplicateKeyException(e.getMessage());
        } catch (com.opensymphony.module.user.ImmutableException e) {
            throw new CreateException(e.getMessage());
        }
    }



     /**
     * Called by UserManager before any other method.
     * Allows for UserProvider specific initialization.
     *
     * @param properties Extra properties passed across by UserManager.
     */
    public boolean init(Properties properties) {
        // m_realm = properties.getProperty("realm", "default");
        // nothing needs to be done
        return true;
    }

    /**
     * Return a list of groups that the user belongs to
     *
     * @param userId
     * @return a list of groups that the user belongs to
     * @throws NotFoundException
     */
    public List listGroupsForUser(String userId) throws NotFoundException {
        List list = new ArrayList();

        try {
            // user returns a list of group names not gorup objects
            UserManager um = getUserManager();
            list = um.getUser(userId).getGroups();
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }

        if (list.size() > 0) {
            return list;
        } else {
            return null;
        }
    }

    /**
     * Return a list of the groupId of all the groups that are members of the group
     *
     * @param group
     * @return a list of the groupId of all the groups that are members of the group
     * @throws NotFoundException
     */
    public List listGroupsInGroup(String group) throws NotFoundException {
        return null;
    }

    /**
     * Return a list of groupId's for all the groups the passed group belongs to
     *
     * @param group
     * @return a list of groupId's for all the groups the passed group belongs to
     * @throws NotFoundException
     */
    public List listParentGroupsOfGroup(String group) throws NotFoundException {
        return null;
    }

    /**
     * Return a list of user id's of all the members pf the group
     *
     * @param groupId
     * @return a list of user id's of all the members pf the group
     * @throws NotFoundException
     */
    public List listUsersInGroup(String groupId) throws NotFoundException {
        List list = new ArrayList();

        try {
            UserManager um = getUserManager();
            Collection users = um.getGroup(groupId).getUsers();

            for (Iterator itr = users.iterator(); itr.hasNext();) {
                User l_user = (User) itr.next();
                list.add(l_user.getName());
            }
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }

        if (list.size() > 0) {
            return list;
        } else {
            return null;
        }
    }

    /**
     * Load Entity.
     *
     * @return Whether entity was successfully loaded.
     */
    public boolean load(String name, com.opensymphony.module.access.Entity.Accessor accessor) {
        return false;
    }

    /**
     * Remove a group
     *
     * @param name
     * @throws NotFoundException
     * @throws com.opensymphony.module.access.ImmutableException
     */
    public void removeGroup(String name) throws NotFoundException, com.opensymphony.module.access.ImmutableException {
        try {
            UserManager um = getUserManager();
            Group group = um.getGroup(name);
            group.remove();
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (com.opensymphony.module.user.ImmutableException e) {
            throw new com.opensymphony.module.access.ImmutableException(e.getMessage());
        }
    }

    /**
     *
     * @param childGroupId
     * @param parentGroupId
     * @throws NotFoundException
     * @throws com.opensymphony.module.access.ImmutableException
     */
    public void removeGroupFromGroup(String childGroupId, String parentGroupId) throws NotFoundException, com.opensymphony.module.access.ImmutableException {
        throw new com.opensymphony.module.access.ImmutableException("OSUser does not support adding Groups to Groups");
    }

    public void removeUser(String userName) throws NotFoundException, com.opensymphony.module.access.ImmutableException {
        try {
            User user = getUserManager().getUser(userName);
            user.remove();
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (com.opensymphony.module.user.ImmutableException e) {
            throw new com.opensymphony.module.access.ImmutableException(e.getMessage());
        }
    }

    /**
     * Remove the user from the group
     *
     * @param userId
     * @param groupId
     * @throws NotFoundException
     * @throws com.opensymphony.module.access.ImmutableException Datastore is not writable
     */
    public void removeUserFromGroup(String userId, String groupId) throws NotFoundException, com.opensymphony.module.access.ImmutableException {
        try {
            UserManager um = getUserManager();
            Group group = um.getGroup(groupId);
            User user = um.getUser(userId);
            group.removeUser(user);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    public class OsGroup implements Group_I {
        //~ Instance fields ////////////////////////////////////////////////////////

        Group m_group;

        //~ Constructors ///////////////////////////////////////////////////////////

        protected OsGroup(Group group) {
            m_group = group;
        }

        //~ Methods ////////////////////////////////////////////////////////////////

        public void setGroupId(String group) {
        }

        public String getGroupId() {
            return m_group.getName();
        }

        public void setProperties(PropertySet props) {
            PropertySet ps = m_group.getPropertySet();
            Iterator itr = props.getKeys().iterator();

            PropertySetMethods pm = new PropertySetMethods();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                pm.setPropertyValue(ps, key, props.getType(key), pm.getPropertyValue(props, key));
            }
        }

        public PropertySet getProperties() {
            return m_group.getPropertySet();
        }

        public ArrayList getUsers() {
            return new ArrayList(m_group.getUsers());
        }
    }

    public class OsUser implements User_I {
    //~ Instance fields ////////////////////////////////////////////////////////

    User m_user;

    //~ Constructors ///////////////////////////////////////////////////////////

    protected OsUser(User user) {
        m_user = user;
    }

    //~ Methods ////////////////////////////////////////////////////////////////

    public String getFullName() {
        return m_user.getFullName();
    }

    public void setPassword(String password) throws ImmutableException {
        m_user.setPassword(password);
    }

    public String getPassword() {
        return "";
    }

    public void setProperties(PropertySet props) {
        PropertySet ps = m_user.getPropertySet();
        Iterator itr = props.getKeys().iterator();

        PropertySetMethods pm = new PropertySetMethods();

        while (itr.hasNext()) {
            String key = (String) itr.next();
            pm.setPropertyValue(ps, key, props.getType(key), pm.getPropertyValue(props, key));
        }
    }

    public PropertySet getProperties() {
        return m_user.getPropertySet();
    }

    public void setUserId(String userId) throws ImmutableException {
        throw new ImmutableException("Can not change userid in osuser provider");
    }

    public String getUserId() {
        return m_user.getName();
    }
  }
}

