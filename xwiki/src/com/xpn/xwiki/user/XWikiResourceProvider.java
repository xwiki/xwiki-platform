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

 * Created by
 * User: Ludovic Dubost
 * Date: 27 janv. 2004
 * Time: 00:47:27
 */
package com.xpn.xwiki.user;

import com.opensymphony.module.access.DuplicateKeyException;
import com.opensymphony.module.access.ImmutableException;
import com.opensymphony.module.access.NotFoundException;
import com.opensymphony.module.access.entities.Acl_I;
import com.opensymphony.module.access.entities.Resource_I;
import com.opensymphony.module.access.provider.ResourceProvider;
import com.opensymphony.module.propertyset.PropertySet;

import java.util.ArrayList;
import java.util.Properties;

public class XWikiResourceProvider extends XWikiBaseProvider implements ResourceProvider {


    public boolean init(Properties properties) {
        return super.init(properties);
    }

    public String getRealm() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean handles(String name) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArrayList getAcls() throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Acl_I getAclsByAclId(Long acl) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArrayList getAclsByGroupId(String groupId) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArrayList getAclsByResourceKey(String resourceKey) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArrayList getAclsByUserId(String userId) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getGroupAccessLevels(String groupId, String resourceKey) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getGroupAccessLevels(String groupId, String resourceKey, boolean checkParents) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PropertySet getPropertySet(String resourceKey) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Resource_I getResourceByKey(String resourceKey) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ArrayList getResources() throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUserAccessLevels(String userId, String resourceKey) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getUserAccessLevels(String userId, String resourceKey, boolean checkGroups) throws NotFoundException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void createGroupAccessLevel(String groupId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void createResource(String resourceKey, String shortDesc, String description, String availAccess) throws DuplicateKeyException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void createUserAccessLevel(String userId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException, DuplicateKeyException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteGroupAccessLevel(String groupId, String resourceKey) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteResource(String resourceKey) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteResourceAndRights(String resourceKey) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteUserAccessLevel(String userId, String resourceKey) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean groupHasAccessLevel(String groupId, String resourceKey, String accessLevel) throws NotFoundException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateGroupAccessLevels(String groupId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateUserAccessLevel(String userId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean userHasAccessLevel(String userId, String resourceKey, String accessLevel) throws NotFoundException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
