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
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class XWikiResourceProvider extends XWikiBaseProvider implements ResourceProvider {


    public boolean init(Properties properties) {
        return super.init(properties);
    }

    public String getRealm() {
        return "default";
    }

    // Any existing document can have access rights
    public boolean handles(String name) {
        if (name.equals("default"))
            return true;

        if (super.handles(name))
            return true;
        try {
            name = getName(name);
            List list = getxWiki().searchDocuments("where CONCAT(XWD_WEB,'.',XWD_NAME) ='" + name + "'", context);
            boolean result = (list.size()>0);
            if (result)
                getHandledNames().add(name);
            return result;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
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
        // This should not be implemented..
        // There are too many resources
        return null;
    }

    public String getUserAccessLevels(String userId, String resourceKey) throws NotFoundException {
        // This seems to imply hierachical levels..
        return null;
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
        return hasAccessLevel(groupId, resourceKey, accessLevel, false);
    }

    public void updateGroupAccessLevels(String groupId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateUserAccessLevel(String userId, String resourceKey, String accessLevel) throws NotFoundException, ImmutableException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean checkRight(String name, XWikiDocInterface doc, String accessLevel,
                              boolean user, boolean allow, boolean global) throws NotFoundException {
        String className = global ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights";
        String fieldName = user ? "users" : "groups";
        boolean found = false;

        Vector vobj = doc.getObjects(className);
        if (vobj!=null)
        {
            for (int i=0;i<vobj.size();i++) {
                BaseObject bobj = (BaseObject) vobj.get(i);
                if (bobj==null)
                    continue;
                String users = bobj.getStringValue(fieldName);
                String levels = bobj.getStringValue("levels");
                boolean allowdeny = (bobj.getIntValue("allow")==1);

                if (allowdeny == allow) {
                    String[] levelsarray = StringUtils.split(bobj.getStringValue("levels")," ,|");
                    if (ArrayUtils.contains(levelsarray, accessLevel)) {
                        found = true;
                        String[] userarray = StringUtils.split(users," ,|");
                        if (ArrayUtils.contains(userarray, name))
                           return true;
                    }
                }
            }
        }

        // Didn't found right at this level.. Let's go to group level
        List grouplist = null;
        try {
          grouplist = getxWiki().getAccessmanager().listGroupsForUser(name);
        } catch (NotFoundException e) {
        } catch (Exception e) {
            // This should not happen
            e.printStackTrace();
        }
          

        if (grouplist!=null) {
            for (int i=0;i<grouplist.size();i++) {
                String group = (String)grouplist.get(i);
                try {
                    boolean result = checkRight(group, doc, accessLevel, false, allow, global);
                    if (result)
                        return true;
                    else
                        found = true;
                } catch (Exception e) {
                    // This should not happen
                    e.printStackTrace();
                }
            }
        }

        if (found)
           return false;
        else
            throw new NotFoundException();
    }

    public boolean userHasAccessLevel(String userId, String resourceKey, String accessLevel) throws NotFoundException {
        return hasAccessLevel(userId, resourceKey, accessLevel, true);
    }

    public boolean hasAccessLevel(String name, String resourceKey, String accessLevel, boolean user) throws NotFoundException {
        boolean deny = false;
        boolean allow = false;
        boolean allow_found = false;
        boolean deny_found = false;
        try {
            // Make sure we remove the database name and set the context
            name = getName(name);

            XWikiDocInterface xwikidoc = getxWiki().getDocument("XWiki.XWikiPreferences", context);

            // Verify XWiki programming right
            if (accessLevel.equals("programming")) {
              // Programming right can only been given if user is from main wiki
                try {
                    allow = checkRight(name, xwikidoc , "programming", true, true, true);
                    if (allow)
                        return true;
                    else
                        return false;
                } catch (NotFoundException e) {}
                return false;
              }


            // Verify XWiki super user
            try {
                allow = checkRight(name, xwikidoc , "admin", true, true, true);
                if (allow) return true;
            } catch (NotFoundException e) {}

            // Verify Web super user
            String web = Util.getWeb(resourceKey);
            XWikiDocInterface webdoc = getxWiki().getDocument(web, "WebPreferences", context);
            try {
                allow = checkRight(name, webdoc , "admin", true, true, true);
                if (allow) return true;
            } catch (NotFoundException e) {}

            // First check if this document is denied to the specific user
            resourceKey = getName(resourceKey);
            XWikiDocInterface doc = getxWiki().getDocument(resourceKey, context);
            try {
                deny = checkRight(name, doc, accessLevel, true, false, false);
                deny_found = true;
                if (deny) return false;
            } catch (NotFoundException e) {}

            try {
                allow = checkRight(name, doc , accessLevel, true, true, false);
                allow_found = true;
                if (allow) return true;
            } catch (NotFoundException e) {}


            // Check if this document is denied/allowed
            // through the web WebPreferences Global Rights
            try {
                deny =  checkRight(name, webdoc, accessLevel, true, false, true);
                deny_found = true;
                if (deny) return false;
            } catch (NotFoundException e) {}
            try {
                allow = checkRight(name, webdoc , accessLevel, true, true, true);
                allow_found = true;
                if (allow) return true;
            } catch (NotFoundException e) {}

            // Check if this document is denied/allowed
            // through the XWiki.XWikiPreferences Global Rights
            try {
                deny = checkRight(name, xwikidoc , accessLevel, true, false, true);
                deny_found = true;
                if (deny) return false;
            } catch (NotFoundException e) {}
            try {
                allow = checkRight(name, xwikidoc , accessLevel, true, true, true);
                allow_found = true;
                if (allow) return true;
            } catch (NotFoundException e) {}

            // If neither doc, web or topic had any allowed ACL
            // and that all users that were not denied
            // should be allowed.
            if (!allow_found)
                return true;
            else
                return false;

        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }
}
