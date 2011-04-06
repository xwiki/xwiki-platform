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
 *
 */
package com.xpn.xwiki.plugin.userdirectory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.usertools.XWikiUserManagementTools;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.web.XWikiRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


public class UserDirectoryPlugin  extends XWikiDefaultPlugin implements XWikiPluginInterface {
    private static Log mLogger =
            LogFactory.getLog(UserDirectoryPlugin.class);

    public static final String DEFAULT_PLUGIN_SPACE = "Directory";
    public static final String DEFAULT_GROUP_TEMPLATE= "XWiki.DirectoryGroupTemplate";
    public static final String DEFAULT_DEACTIVATION_MESSAGE_PAGE = "XWiki.DeactivationMessage";
    public static final int ERROR_USERDIRECTORYPLUGIN_UNKNOWN = 0;
    public static final int ERROR_USERDIRECTORYPLUGIN_ALREADYEXIST = 1;
    public static final int ERROR_USERDIRECTORYPLUGIN_GRPDOESNTEXIST = 2;
    private static final int ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL = 3;
    private static final int ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG = 4;

    public UserDirectoryPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public String getName() {
        return "userdirectory";
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new UserDirectoryPluginAPI((UserDirectoryPlugin) plugin, context);
    }


    /**
     * Add a group from the request
     * @param context
     * @return
     */
    public Group addGroup(XWikiContext context) throws XWikiException {
        XWikiRequest req = context.getRequest();
        String name = req.get("XWiki.DirectoryGroupClass_0_name");
        name = context.getWiki().clearName(name, context);
        XWikiDocument tmpDoc = context.getWiki().getDocument(DEFAULT_PLUGIN_SPACE, name, context);
        if (!tmpDoc.isNew())
            throw new PluginException(UserDirectoryPlugin.class, ERROR_USERDIRECTORYPLUGIN_ALREADYEXIST, "This document already exist, try another name");
        Document doc = tmpDoc.newDocument(context);
        doc.addObjectFromRequest("XWiki.DirectoryGroupClass");
        doc.setContent(getTemplate(context));
        doc.saveWithProgrammingRights();
        return new Group(doc, context);
    }

    public String getTemplate(XWikiContext context) throws XWikiException {
        return context.getWiki().getDocument(DEFAULT_GROUP_TEMPLATE, context).getContent();
    }

    public void updateGroup(XWikiContext context) throws XWikiException {
        XWikiRequest req = context.getRequest();
        String pageName = req.get("pageName");
        XWikiDocument tmpDoc = context.getWiki().getDocument(pageName, context);
        Document doc = tmpDoc.newDocument(context);
        doc.updateObjectFromRequest("XWiki.DirectoryGroupClass");
        doc.save();
    }

    public boolean groupExist(String name, XWikiContext context) throws XWikiException {
        return getGroup(name, context).isNew();
    }

     public Group getGroup(String space, String name, XWikiContext context) throws XWikiException {
        return Group.getGroup(space, name, context);
    }

    public Group getGroup(String name, XWikiContext context) throws XWikiException {
        return getGroup(DEFAULT_PLUGIN_SPACE, name, context);
    }

    public List getAllGroupsPageName(XWikiContext context) throws XWikiException {
        return Group.getAllGroupsPageName(context);
    }

    public List getAllGroupsPageName(String orderBy, XWikiContext context) throws XWikiException {
        return Group.getAllGroupsPageName(orderBy, context);
    }

    public List getAllGroups(XWikiContext context) throws XWikiException {
        return getAllGroups(null, context);
    }

    public List getAllGroups(String orderBy, XWikiContext context) throws XWikiException {
        List allGroupsPageName;
        if (orderBy == null)
            allGroupsPageName = getAllGroupsPageName(context);
        else
             allGroupsPageName = getAllGroupsPageName(orderBy, context);
        List groups = new ArrayList();
        if (allGroupsPageName == null)
            return groups;
        Iterator it = allGroupsPageName.iterator();
        while (it.hasNext())
            groups.add(getGroup((String) it.next(), context));
        return groups;
    }

    public List getMembers(String grpPage, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpPage, context);
        return grp.getMembers(context);
    }

    public List getUnactivatedMembers(String grpPage, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpPage, context);
        return grp.getUnactivatedMembers(context);
    }

    public boolean removeGroup(String name, XWikiContext context){
        return false;
    }

    public boolean addParentGroup(String childGroupName, String parentGroupName, XWikiContext context) throws XWikiException {
        Group grp = getGroup(childGroupName, context);
        boolean res = grp.addParentName(parentGroupName, this, context);
        if (res){
            grp.save(context);
            return true;
        }
        return false;
    }

    public boolean removeParentGroup(String childGroupName, String parentGroupName, XWikiContext context) throws XWikiException {
        Group grp = getGroup(childGroupName, context);
        boolean res =  grp.removeParent(parentGroupName, this, context);
        if (res){
            grp.save(context);
            return true;
        }
        return false;
    }

    public List getParentGroups(String grpName, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpName, context);
        return grp.getParents(context);
    }

    public String inviteToGroup(String name, String firstName, String email, String group, XWikiContext context) throws XWikiException {
        String pageName = context.getWiki().convertUsername(email, context);
        XWikiUserManagementTools userTools = (XWikiUserManagementTools) context.getWiki().getPlugin("usermanagementtools", context);
        XWikiDocument doc = context.getWiki().getDocument(userTools.getUserSpace(context) + "." + pageName, context);
        if (doc.isNew()) {
            String userDocName = userTools.inviteUser(name, email, context);
            Document userDoc = context.getWiki().getDocument(userDocName, context).newDocument(context);
            userDoc.use("XWiki.XWikiUsers");
            userDoc.set("first_name", firstName);
            userDoc.saveWithProgrammingRights();
        }
        addUserToGroup(doc.getFullName(), group, context);
        return doc.getFullName();
    }

    public void addUserToGroup(String userPage, String group, XWikiContext context) throws XWikiException {
        Group grp = getGroup(group, context);
        if (grp == null)
            throw new PluginException(UserDirectoryPlugin.class, ERROR_USERDIRECTORYPLUGIN_GRPDOESNTEXIST, "This group doesn't exist");
        if (grp.addUser(userPage, context))
            grp.save(context);
    }

    public String getUserName(String userPage, XWikiContext context) throws XWikiException {
        XWikiUserManagementTools userTools = (XWikiUserManagementTools) context.getWiki().getPlugin("usermanagementtools", context);
        return userTools.getUserName(userPage, context);
    }

    public String getUserEmail(String userPage, XWikiContext context) throws XWikiException {
        XWikiUserManagementTools userTools = (XWikiUserManagementTools) context.getWiki().getPlugin("usermanagementtools", context);
        return userTools.getEmail(userPage, context);
    }

    public List getUsersDocumentName(String grpPage, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpPage, context);
        return grp.getUsersPageName(context);
    }

    public List getUsersDocument(String grpPage, XWikiContext context) throws XWikiException {
        List users = getUsersDocumentName(grpPage, context);
        List usersDoc = new ArrayList();
        Iterator it = users.iterator();
        while(it.hasNext()){
            usersDoc.add(context.getWiki().getDocument((String) it.next(), context).newDocument(context));
        }
        return usersDoc;
    }

    public boolean removeMemberships(String userPage, String grpPage, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpPage, context);
        grp.removeUser(userPage, context);
        grp.save(context);
        return true;
    }

    public void sendDeactivationEMail(String userPage, String grpPage, XWikiContext context) throws XWikiException {
        Group grp = getGroup(grpPage, context);
        String userName = getUserName(userPage, context);
        String email = getUserEmail(userPage, context);
        String message = prepareDeactivationMessage(userName, email, grp.getName(), context);
        String sender;
        try {
            sender = context.getWiki().getXWikiPreference("admin_email", context);
        } catch (Exception e) {
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG,
                    "Exception while reading the validation email config", e, null);

        }
        context.getWiki().sendMessage(sender, email, message, context);
    }

    private String prepareDeactivationMessage(String name,String email, String grp, XWikiContext context) throws XWikiException {
        XWikiDocument doc = context.getWiki().getDocument(getDeactivationMessageDocument(context), context);

        String content = doc.getContent();

        try {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put("name", name);
            vcontext.put("group", grp);
            vcontext.put("email", email);
            content = context.getWiki().parseContent(content, context);
        } catch (Exception e) {
            throw new PluginException(getName(), ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL,
                    "Exception while preparing the validation email", e, null);

        }
        return content;
    }

    protected String getDeactivationMessageDocument(XWikiContext context)
    {
        return DEFAULT_DEACTIVATION_MESSAGE_PAGE;
    }

    public List getUserMemberships(String userPage, XWikiContext context) throws XWikiException {
        XWikiGroupService groupService = context.getWiki().getGroupService(context);
        Collection groups = groupService.listGroupsForUser(userPage, context);
        List userGrps = new ArrayList();
        Iterator it = groups.iterator();
        while(it.hasNext()){
            String grpName = (String) it.next();
            if (Group.isValidGroup(grpName, context))
                userGrps.add(grpName);
        }
        return userGrps;
    }

    public boolean deactivateAccount(String userPage, XWikiContext context) throws XWikiException {
        Document doc = context.getWiki().getDocument(userPage, context).newDocument(context);
        doc.use("XWiki.XWikiUsers");
        doc.set("active", "0");
        String validkey = context.getWiki().generateValidationKey(16);
        doc.set("validkey", validkey);
        doc.saveWithProgrammingRights();
        return true;
    }

    public void resendInvitation(String userPage, XWikiContext context) throws XWikiException {
        XWikiUserManagementTools userTools = (XWikiUserManagementTools) context.getWiki().getPlugin("usermanagementtools", context);

        userTools.resendInvitation(userTools.getEmail(userPage, context), context);
    }



}
