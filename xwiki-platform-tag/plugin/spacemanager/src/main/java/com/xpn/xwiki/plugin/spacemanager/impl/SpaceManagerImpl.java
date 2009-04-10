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
package com.xpn.xwiki.plugin.spacemanager.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.mailsender.Mail;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import com.xpn.xwiki.plugin.rightsmanager.RightsManagerPluginApi;
import com.xpn.xwiki.plugin.spacemanager.api.Space;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManager;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagerException;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagerExtension;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceManagers;
import com.xpn.xwiki.plugin.spacemanager.api.SpaceUserProfile;
import com.xpn.xwiki.plugin.spacemanager.plugin.SpaceManagerPluginApi;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.user.api.XWikiGroupService;

/**
 * Space manager plug-in implementation class. Manages {@link Space} spaces
 * 
 * @version $Id: $
 */
public class SpaceManagerImpl extends XWikiDefaultPlugin implements SpaceManager
{

    public final static String SPACEMANAGER_EXTENSION_CFG_PROP = "xwiki.spacemanager.extension";

    public final static String SPACEMANAGER_PROTECTED_SUBSPACES_PROP =
        "xwiki.spacemanager.protectedsubspaces";

    public final static String SPACEMANAGER_DEFAULT_PROTECTED_SUBSPACES = "";

    public final static String SPACEMANAGER_DEFAULT_EXTENSION =
        "org.xwiki.plugin.spacemanager.impl.SpaceManagerExtensionImpl";

    public final static String SPACEMANAGER_DEFAULT_MAIL_NOTIFICATION = "1";
    
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(SpaceManagerImpl.class);

    /**
     * The extension that defines specific functions for this space manager
     */
    protected SpaceManagerExtension spaceManagerExtension;

    protected boolean mailNotification;

    /**
     * Space manager constructor
     * 
     * @param name
     * @param className
     * @param context
     */
    public SpaceManagerImpl(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        String mailNotificationCfg =
            context.getWiki().Param("xwiki.spacemanager.mailnotification",
                SpaceManagerImpl.SPACEMANAGER_DEFAULT_MAIL_NOTIFICATION).trim();
        mailNotification = "1".equals(mailNotificationCfg);
    }

    /**
     * {@inheritDoc}
     */
    public void flushCache()
    {
        super.flushCache();
    }

    /**
     * @param context Xwiki context
     * @return Returns the Space Class as defined by the extension
     * @throws XWikiException
     */
    protected BaseClass getSpaceClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument(getSpaceClassName(), context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setFullName(getSpaceClassName());
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(getSpaceClassName());

        needsUpdate |= bclass.addTextField(SpaceImpl.SPACE_DISPLAYTITLE, "Display Name", 64);
        needsUpdate |= bclass.addTextAreaField(SpaceImpl.SPACE_DESCRIPTION, "Description", 45, 4);
        needsUpdate |= bclass.addTextField(SpaceImpl.SPACE_TYPE, "Group or plain space", 32);
        needsUpdate |= bclass.addTextField(SpaceImpl.SPACE_URLSHORTCUT, "URL Shortcut", 40);
        needsUpdate |=
            bclass.addStaticListField(SpaceImpl.SPACE_POLICY, "Membership Policy", 1, false,
                "open=Open membership|closed=Closed membership", "radio");
        needsUpdate |=
            bclass
                .addStaticListField(
                    SpaceImpl.SPACE_LANGUAGE,
                    "Language",
                    "en=English|zh=Chinese|nl=Dutch|fr=French|de=German|it=Italian|jp=Japanese|kr=Korean|po=Portuguese|ru=Russian|sp=Spanish");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWikiSpaceClass");
            doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceTypeName()
    {
        return getSpaceManagerExtension().getSpaceTypeName();
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceClassName()
    {
        return getSpaceManagerExtension().getSpaceClassName();
    }

    /**
     * Checks if this space manager has custom mapping
     * 
     * @return
     */
    public boolean hasCustomMapping()
    {
        return getSpaceManagerExtension().hasCustomMapping();
    }

    /**
     * {@inheritDoc}
     */
    public void init(XWikiContext context)
    {
        try {
            getSpaceManagerExtension(context);
            getSpaceManagerExtension().init(this, context);
            SpaceManagers.addSpaceManager(this);
            getSpaceClass(context);
            SpaceUserProfileImpl.getSpaceUserProfileClass(context);
            // Dirty migration of spaces potential corrupted XWiki.XWikiGlobalRights objects.
            migrateSpaceCorruptedRightsData(context);
        } catch (Exception e) {
            LOG.error("Error initializing plugin for main database", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void virtualInit(XWikiContext context)
    {
        try {
            getSpaceClass(context);
            getSpaceManagerExtension().virtualInit(this, context);
            // Dirty migration of spaces potential corrupted XWiki.XWikiGlobalRights objects.
            migrateSpaceCorruptedRightsData(context);
        } catch (Exception e) {
            LOG.error("Error initializing plugin for database [" + context.getDatabase() + "]", e);
        }
    }

    /**
     * Migrate wrong data created by spacemanager plugin versions < 1.0.2 or 1.1.1 or 1.2
     * The plugin used to create users and groups properties of XWiki.XWikiGlobalRights objects as
     * StringProperty instead of LargeStringProperty, thus making it impossible to save
     * a Space WebPreferences document from the object editor (see http://jira.xwiki.org/jira/browse/XPSM-12)
     * 
     * @since 1.0.2
     * @since 1.1.1
     * @since 1.2
     * @param context
     * @throws SpaceManagerException
     */
    private void migrateSpaceCorruptedRightsData(XWikiContext context) throws SpaceManagerException
    {
        try {
            context.getWiki().getHibernateStore().executeWrite(context, true, new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException
                {
                    Query q = session.createQuery("select s from BaseObject o, StringProperty s where o.className like 'XWiki.XWiki%Rights' and o.id=s.id and (s.name='users' or s.name='groups')");
                    List lst = q.list();
                    if (lst.size()==0)
                        return null;
                    LOG.warn("[Migrating corrupted rights properties (see http://jira.xwiki.org/jira/browse/XPSM-12)");
                    List lst2 = new ArrayList(lst.size());
                    for (Iterator it=lst.iterator(); it.hasNext(); ) {
                        StringProperty sp = (StringProperty) it.next();
                        LargeStringProperty lsp = new LargeStringProperty();
                        lsp.setId(sp.getId());
                        lsp.setName(sp.getName());
                        lsp.setValue(sp.getValue());
                        lst2.add(lsp);
                    }
                    for (Iterator it=lst.iterator(); it.hasNext(); )
                        session.delete(it.next());
                    for (Iterator it=lst2.iterator(); it.hasNext(); )
                        session.save(it.next());
                    return null;
                }
            });
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new SpaceManagerPluginApi((SpaceManager) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
    public SpaceManagerExtension getSpaceManagerExtension(XWikiContext context)
        throws SpaceManagerException
    {
        if (spaceManagerExtension == null) {
            String extensionName =
                context.getWiki().Param(SPACEMANAGER_EXTENSION_CFG_PROP,
                    SPACEMANAGER_DEFAULT_EXTENSION);

            try {
                if (extensionName != null) {
                    spaceManagerExtension =
                        (SpaceManagerExtension) Class.forName(extensionName).newInstance();
                }
            } catch (Throwable e) {
                try {
                    spaceManagerExtension =
                        (SpaceManagerExtension) Class.forName(SPACEMANAGER_DEFAULT_EXTENSION)
                            .newInstance();
                } catch (Throwable e2) {
                }
            }
        }

        if (spaceManagerExtension == null) {
            spaceManagerExtension = new SpaceManagerExtensionImpl();
        }

        return spaceManagerExtension;
    }

    public SpaceManagerExtension getSpaceManagerExtension()
    {
        return spaceManagerExtension;
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return "spacemanager";
    }

    private Object notImplemented() throws SpaceManagerException
    {
        throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
            SpaceManagerException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "not implemented");
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceWikiName(String spaceTitle, boolean unique, XWikiContext context)
    {
        return getSpaceManagerExtension().getSpaceWikiName(spaceTitle, unique, context);
    }

    /**
     * @param spaceName The name of the space
     * @return the name of the space document for a specific space
     */
    protected String getSpaceDocumentName(String spaceName)
    {
        return spaceName + ".WebPreferences";
    }

    /**
     * {@inheritDoc}
     */
    public String[] getProtectedSubSpaces(XWikiContext context)
    {
        String protectedSubSpaces =
            context.getWiki().Param(SPACEMANAGER_PROTECTED_SUBSPACES_PROP,
                SPACEMANAGER_DEFAULT_PROTECTED_SUBSPACES);
        if ((protectedSubSpaces != null) && (!protectedSubSpaces.equals(""))) {
            return protectedSubSpaces.split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * Gives a group certain rights over a space
     * 
     * @param spaceName Name of the space
     * @param groupName Name of the group that will have the value
     * @param level Access level
     * @param allow True if the right is allow, deny if not
     */
    protected boolean addRightToGroup(String spaceName, String groupName, String level,
        boolean allow, boolean global, XWikiContext context) throws XWikiException
    {
        final String rightsClass = global ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights";
        final String prefDocName = spaceName + ".WebPreferences";
        final String groupsField = "groups";
        final String levelsField = "levels";
        final String allowField = "allow";

        XWikiDocument prefDoc;
        prefDoc = context.getWiki().getDocument(prefDocName, context);

        // checks to see if the right is not already given
        boolean exists = false;
        boolean isUpdated = false;
        int indx = -1;
        boolean foundlevel = false;
        int allowInt;
        if (allow)
            allowInt = 1;
        else
            allowInt = 0;
        List<BaseObject> objs = prefDoc.getObjects(rightsClass);
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                BaseObject bobj = objs.get(i);
                if (bobj == null)
                    continue;
                String groups = bobj.getLargeStringValue(groupsField);
                String levels = bobj.getStringValue(levelsField);
                int allowDeny = bobj.getIntValue(allowField);
                boolean allowdeny = (bobj.getIntValue(allowField) == 1);
                List<String> groupsList = ListClass.getListFromString(groups, " ,|", false);
                List<String> levelsList = ListClass.getListFromString(levels, " ,|", false);
                if (groupsList.contains(groupName)) {
                    exists = true;
                    if (!foundlevel)
                        indx = i;
                    if (levelsList.contains(level)) {
                        foundlevel = true;
                        if (allowInt == allowDeny) {
                            isUpdated = true;
                            break;
                        }
                    }
                }
            }
        }

        // sets the rights. the aproach is to break rules/levels in as many
        // XWikiRigts elements so
        // we don't have to handle lots of situation when we change rights
        if (!exists) {
            BaseObject bobj = new BaseObject();
            bobj.setClassName(rightsClass);
            bobj.setName(prefDoc.getFullName());
            bobj.setLargeStringValue(groupsField, groupName);
            bobj.setStringValue(levelsField, level);
            bobj.setIntValue(allowField, allowInt);
            prefDoc.addObject(rightsClass, bobj);
            context.getWiki().saveDocument(prefDoc, context);
            return true;
        } else {
            if (isUpdated) {
                return true;
            } else {
                BaseObject bobj = (BaseObject) objs.get(indx);
                String groups = bobj.getLargeStringValue(groupsField);
                String levels = bobj.getStringValue(levelsField);

                List groupsList = ListClass.getListFromString(groups, " ,|", false);
                List levelsList = ListClass.getListFromString(levels, " ,|", false);

                if (levelsList.size() == 1 && groupsList.size() == 1
                    && levelsList.get(0) == level) {
                    // if there is only this group and this level in the rule
                    // update this rule
                } else {
                    // if there are more groups/levels, extract this one(s)
                    bobj = new BaseObject();
                    bobj.setName(prefDoc.getFullName());
                    bobj.setClassName(rightsClass);
                    bobj.setStringValue(levelsField, level);
                    bobj.setIntValue(allowField, allowInt);
                    bobj.setLargeStringValue(groupsField, groupName);
                }

                prefDoc.addObject(rightsClass, bobj);
                context.getWiki().saveDocument(prefDoc, context);
                return true;
            }
        }
    }

    /**
     * Gives a group certain rights over a space
     * 
     * @param spaceName Name of the space
     * @param groupName Name of the group that will have the value
     * @param level Access level
     * @param allow True if the right is allow, deny if not
     */
    protected boolean removeRightFromGroup(String spaceName, String groupName, String level,
        boolean allow, boolean global, XWikiContext context) throws XWikiException
    {
        final String rightsClass = global ? "XWiki.XWikiGlobalRights" : "XWiki.XWikiRights";
        final String prefDocName = spaceName + ".WebPreferences";
        final String groupsField = "groups";
        final String levelsField = "levels";
        final String allowField = "allow";

        XWikiDocument prefDoc;
        prefDoc = context.getWiki().getDocument(prefDocName, context);

        boolean foundlevel = false;
        int allowInt;
        if (allow)
            allowInt = 1;
        else
            allowInt = 0;
        List<BaseObject> objs = prefDoc.getObjects(rightsClass);
        if (objs != null) {
            for (int i = 0; i < objs.size(); i++) {
                BaseObject bobj = (BaseObject) objs.get(i);
                if (bobj == null)
                    continue;
                String groups = bobj.getLargeStringValue(groupsField);
                String levels = bobj.getStringValue(levelsField);
                int allowDeny = bobj.getIntValue(allowField);
                boolean allowdeny = (bobj.getIntValue(allowField) == 1);
                List levelsList = ListClass.getListFromString(levels, " ,|", false);
                List groupsList = ListClass.getListFromString(groups, " ,|", false);
                if (groupsList.contains(groupName)) {
                    if (!foundlevel)
                        if (levelsList.contains(level)) {
                            foundlevel = true;
                            if (allowInt == allowDeny) {
                                prefDoc.removeObject(bobj);
                                context.getWiki().saveDocument(prefDoc, context);
                                return true;
                            }
                        }
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public void setSpaceRights(Space newspace, XWikiContext context) throws SpaceManagerException
    {
        // Set admin edit rights on group prefs
        try {
            addRightToGroup(newspace.getSpaceName(), getAdminGroupName(newspace.getSpaceName()),
                "edit", true, false, context);
            // Set admin admin rights on group prefs
            addRightToGroup(newspace.getSpaceName(), getAdminGroupName(newspace.getSpaceName()),
                "admin", true, true, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }

        String[] subSpaces = getProtectedSubSpaces(context);
        for (int i = 0; i < subSpaces.length; i++) {
            setSubSpaceRights(newspace, subSpaces[i], context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateSpaceRights(Space space, String oldPolicy, String newPolicy,
        XWikiContext context) throws SpaceManagerException
    {
        try {
            if (oldPolicy.equals(newPolicy))
                return;

            String[] subSpaces = getProtectedSubSpaces(context);
            for (int i = 0; i < subSpaces.length; i++) {
                if (newPolicy.equals("closed")) {
                    addRightToGroup(subSpaces[i] + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "view", true, true, context);
                    addRightToGroup(subSpaces[i] + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "comment", true, true, context);
                } else if (newPolicy.equals("open")) {
                    removeRightFromGroup(subSpaces[i] + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "view", true, true, context);
                    removeRightFromGroup(subSpaces[i] + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "comment", true, true, context);
                }
            }
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSubSpaceRights(Space space, String subSpace, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            if ((subSpace != null) && (!subSpace.equals(""))) {
                // Set admin edit rights on Messages group prefs
                addRightToGroup(subSpace + "_" + space.getSpaceName(), getMemberGroupName(space
                    .getSpaceName()), "edit", true, true, context);
                // Set admin admin rights on Messages group prefs
                addRightToGroup(subSpace + "_" + space.getSpaceName(), getAdminGroupName(space
                    .getSpaceName()), "admin", true, true, context);
                // Set admin admin rights on Messages group prefs
                addRightToGroup(subSpace + "_" + space.getSpaceName(), getAdminGroupName(space
                    .getSpaceName()), "edit", true, false, context);

                if ("closed".equals(space.getPolicy())) {
                    addRightToGroup(subSpace + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "view", true, true, context);
                    addRightToGroup(subSpace + "_" + space.getSpaceName(),
                        getMemberGroupName(space.getSpaceName()), "comment", true, true, context);
                }
            }
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Space createSpace(String spaceTitle, XWikiContext context)
        throws SpaceManagerException
    {
        // Init out space object by creating the space
        // this will throw an exception when the space exists

        Space newspace = newSpace(null, spaceTitle, true, context);

        // execute precreate actions
        try {
            getSpaceManagerExtension().preCreateSpace(newspace.getSpaceName(), context);
        } catch (SpaceManagerException e) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_CREATION_ABORTED_BY_EXTENSION,
                "Space creation aborted by extension",
                e);
        }

        // Make sure we set the type
        newspace.setType(getSpaceTypeName());
        try {
            newspace.saveWithProgrammingRights();
            // we need to add the creator as a member and as an admin
            addAdmin(newspace.getSpaceName(), context.getUser(), context);
            addMember(newspace.getSpaceName(), context.getUser(), context);
            setSpaceRights(newspace, context);
            // execute post space creation
            getSpaceManagerExtension().postCreateSpace(newspace.getSpaceName(), context);

        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }

        sendMail(SpaceAction.CREATE, newspace, context); // this should be in
        // the extension's
        // postcreate
        return newspace;
    }

    /**
     * {@inheritDoc}
     */
    public Space createSpaceFromTemplate(String spaceTitle, String templateSpaceName,
        XWikiContext context) throws SpaceManagerException
    {
        // Init out space object by creating the space
        // this will throw an exception when the space exists
        Space newspace = newSpace(null, spaceTitle, false, context);

        // execute precreate actions
        try {
            getSpaceManagerExtension().preCreateSpace(newspace.getSpaceName(), context);
        } catch (SpaceManagerException e) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_CREATION_ABORTED_BY_EXTENSION,
                "Space creation aborted by extension",
                e);
        }

        // Make sure this space does not already exist
        if (!newspace.isNew())
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_ALREADY_EXISTS,
                "Space already exists");

        // Copy over template data over our current data
        try {
            context.getWiki().copyWikiWeb(templateSpaceName, context.getDatabase(),
                context.getDatabase(), null, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }

        // Make sure we set the type
        newspace.setType(getSpaceTypeName());
        newspace.setDisplayTitle(spaceTitle);
        newspace.setCreator(context.getUser());
        newspace.setCreationDate(new Date());

        try {
            newspace.saveWithProgrammingRights();
            // we need to add the creator as a member and as an admin
            addAdmin(newspace.getSpaceName(), context.getUser(), context);
            addMember(newspace.getSpaceName(), context.getUser(), context);
            setSpaceRights(newspace, context);
            // execute post space creation
            getSpaceManagerExtension().postCreateSpace(newspace.getSpaceName(), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
        sendMail(SpaceAction.CREATE, newspace, context);

        return newspace;
    }

    /**
     * {@inheritDoc}
     */
    public Space createSpaceFromApplication(String spaceTitle, String applicationName,
        XWikiContext context) throws SpaceManagerException
    {
        notImplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Space createSpaceFromRequest(String templateSpaceName, XWikiContext context)
        throws SpaceManagerException
    {
        // Init out space object by creating the space
        // this will throw an exception when the space exists
        String spaceTitle =
            context.getRequest().get(
                spaceManagerExtension.getSpaceClassName() + "_0_displayTitle");
        if (spaceTitle == null) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_TITLE_MISSING,
                "Space title is missing");
        }
        Space newspace = newSpace(null, spaceTitle, true, context);

        // execute precreate actions
        try {
            getSpaceManagerExtension().preCreateSpace(newspace.getSpaceName(), context);
        } catch (SpaceManagerException e) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_CREATION_ABORTED_BY_EXTENSION,
                "Space creation aborted by extension",
                e);
        }

        newspace.updateSpaceFromRequest();
        if (!newspace.validateSpaceData())
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_DATA_INVALID,
                "Space data is not valid");

        // Copy over template data over our current data
        if (templateSpaceName != null) {
            try {
                List<String> list =
                    context.getWiki().getStore().searchDocumentsNames(
                        "where doc.space='" + templateSpaceName + "'", context);
                for (String docname : list) {
                    XWikiDocument doc = context.getWiki().getDocument(docname, context);
                    context.getWiki().copyDocument(doc.getFullName(),
                        newspace.getSpaceName() + "." + doc.getName(), null, null, null, true,
                        false, true, context);
                }
            } catch (XWikiException e) {
                throw new SpaceManagerException(e);
            }
        }

        // Make sure we set the type
        newspace.setType(getSpaceTypeName());
        // we need to do it twice because data could have been overwritten by
        // copyWikiWeb
        newspace.updateSpaceFromRequest();
        newspace.setCreator(context.getUser());
        newspace.setCreationDate(new Date());

        try {
            newspace.saveWithProgrammingRights();
            // we need to add the creator as a member and as an admin
            addAdmin(newspace.getSpaceName(), context.getUser(), context);
            addMember(newspace.getSpaceName(), context.getUser(), context);
            setSpaceRights(newspace, context);
            // execute precreate actions
            getSpaceManagerExtension().postCreateSpace(newspace.getSpaceName(), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }

        sendMail(SpaceAction.CREATE, newspace, context);
        return newspace;
    }

    protected Space newSpace(String spaceName, String spaceTitle, boolean create,
        XWikiContext context) throws SpaceManagerException
    {
        return new SpaceImpl(spaceName, spaceTitle, create, this, context);
    }

    /**
     * {@inheritDoc}
     */
    public Space createSpaceFromRequest(XWikiContext context) throws SpaceManagerException
    {
        return createSpaceFromRequest(null, context);
    }

    /**
     * {@inheritDoc}
     */
    public void deleteSpace(String spaceName, boolean deleteData, XWikiContext context)
        throws SpaceManagerException
    {
        Space space = getSpace(spaceName, context);

        if (deleteData) {
            if (getSpaceManagerExtension().preDeleteSpace(space.getSpaceName(), true, context)) {
                // search for all documents in the space
                String hql = "where doc.space = '" + space.getSpaceName() + "'";
                try {
                    List<XWikiDocument> spaceDocs =
                        context.getWiki().getStore().searchDocuments(hql, context);
                    for (XWikiDocument toBeDeleted : spaceDocs) {
                        context.getWiki().deleteDocument(toBeDeleted, context);
                    }
                    getSpaceManagerExtension().postDeleteSpace(space.getSpaceName(), true,
                        context);
                } catch (XWikiException e) {
                    throw new SpaceManagerException(e);
                }
            }
        } else {
            // execute pre delete actions
            if (getSpaceManagerExtension().preDeleteSpace(space.getSpaceName(), deleteData,
                context)) {
                if (!space.isNew()) {
                    space.setType("deleted");
                    try {
                        space.saveWithProgrammingRights();
                        // execute post delete actions
                        getSpaceManagerExtension().postDeleteSpace(space.getSpaceName(),
                            deleteData, context);
                    } catch (XWikiException e) {
                        throw new SpaceManagerException(e);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteSpace(String spaceName, XWikiContext context) throws SpaceManagerException
    {
        deleteSpace(spaceName, false, context);
    }

    /**
     * {@inheritDoc}
     */
    public void undeleteSpace(String spaceName, XWikiContext context)
        throws SpaceManagerException
    {
        Space space = getSpace(spaceName, context);
        if (space.isDeleted()) {
            space.setType(getSpaceTypeName());
            try {
                space.saveWithProgrammingRights();
            } catch (XWikiException e) {
                throw new SpaceManagerException(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Space getSpace(String spaceName, XWikiContext context) throws SpaceManagerException
    {
        // Init the space object but do not create anything if it does not exist
        return newSpace(spaceName, spaceName, false, context);
    }

    /**
     * {@inheritDoc}
     */
    public List<Space> getSpaces(int nb, int start, XWikiContext context)
        throws SpaceManagerException
    {
        List<String> spaceNames = getSpaceNames(nb, start, context);
        return getSpaceObjects(spaceNames, context);
    }

    public List<Space> getSpaces(int nb, int start, String ordersql, XWikiContext context)
        throws SpaceManagerException
    {
        List<String> spaceNames = getSpaceNames(nb, start, ordersql, context);
        return getSpaceObjects(spaceNames, context);
    }

    /**
     * Returns a list of nb space names starting at start
     * 
     * @param context The XWiki Context
     * @return list of Space objects
     * @throws SpaceManagerException
     */
    protected List<Space> getSpaceObjects(List<String> spaceNames, XWikiContext context)
        throws SpaceManagerException
    {
        if (spaceNames == null)
            return null;
        List<Space> spaceList = new ArrayList<Space>();
        for (String spaceName : spaceNames) {
            Space space = getSpace(spaceName, context);
            spaceList.add(space);
        }
        return spaceList;
    }

    public List<String> getSpaceNames(int nb, int start, XWikiContext context)
        throws SpaceManagerException
    {
        return getSpaceNames(nb, start, "", context);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getSpaceNames(int nb, int start, String ordersql, XWikiContext context)
        throws SpaceManagerException
    {
        String type = getSpaceTypeName();
        String className = getSpaceClassName();
        String sql;
        if (hasCustomMapping())
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, "
                    + className + " as space where doc.fullName = obj.name and obj.className='"
                    + className + "' and obj.id = space.id and space.type='" + type + "'"
                    + ordersql;
        else
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, StringProperty typeprop where doc.fullName=obj.name and obj.className = '"
                    + className
                    + "' and obj.id=typeprop.id.id and typeprop.id.name='type' and typeprop.value='"
                    + type + "'" + ordersql;

        List<String> spaceList = Collections.emptyList();
        try {
            spaceList = context.getWiki().getStore().search(sql, nb, start, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
        return spaceList;
    }

    /**
     * Performs a search for spaces
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param ordersql The sql fragment describing the order in wich the spaces should be returned
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @param context XWiki context
     * @return A list with space objects matching the search
     * @throws SpaceManagerException
     */
    public List<Space> searchSpaces(String fromsql, String wheresql, String ordersql, int nb,
        int start, XWikiContext context) throws SpaceManagerException
    {
        List<String> spaceNames =
            searchSpaceNames(fromsql, wheresql, ordersql, nb, start, context);
        return getSpaceObjects(spaceNames, context);
    }

    /**
     * Performs a search for spaces. This variant returns the spaces ordered ascending by creation
     * date
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @param context XWiki context
     * @return A list with space objects matching the search
     * @throws SpaceManagerException
     */
    public List<Space> searchSpaces(String fromsql, String wheresql, int nb, int start,
        XWikiContext context) throws SpaceManagerException
    {
        return searchSpaces(fromsql, wheresql, "", nb, start, context);
    }

    /**
     * Performs a search for space names
     * 
     * @param fromsql The sql fragment describing the source of the search
     * @param wheresql The sql fragment describing the where clause of the search
     * @param ordersql The sql fragment describing the order in wich the spaces should be returned
     * @param nb The number of spaces to return (limit)
     * @param start Number of spaces to skip
     * @param context XWiki context
     * @return A list of strings representing the names of the spaces matching the search
     * @throws SpaceManagerException
     */
    public List<String> searchSpaceNames(String fromsql, String wheresql, String ordersql,
        int nb, int start, XWikiContext context) throws SpaceManagerException
    {
        String type = getSpaceTypeName();
        String className = getSpaceClassName();
        String sql;
        if (hasCustomMapping())
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, "
                    + className + " as space" + fromsql
                    + " where doc.fullName = obj.name and obj.className='" + className
                    + "' and obj.id = space.id and space.type='" + type + "'" + wheresql
                    + ordersql;
        else
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, StringProperty as typeprop"
                    + fromsql
                    + " where doc.fullName=obj.name and obj.className = '"
                    + className
                    + "' and obj.id=typeprop.id.id and typeprop.id.name='type' and typeprop.value='"
                    + type + "'" + wheresql + ordersql;

        List<String> spaceList = Collections.emptyList();
        try {
            spaceList = context.getWiki().getStore().search(sql, nb, start, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
        return spaceList;
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
    public List<String> searchSpaceNames(String fromsql, String wheresql, int nb, int start,
        XWikiContext context) throws SpaceManagerException
    {
        return searchSpaceNames(fromsql, wheresql, "", nb, start, context);
    }

    /**
     * {@inheritDoc}
     */
    public List<Space> getSpaces(String userName, String role, XWikiContext context)
        throws SpaceManagerException
    {
        List<String> spaceNames = getSpaceNames(userName, role, context);
        return getSpaceObjects(spaceNames, context);
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getSpaceNames(String userName, String role, XWikiContext context)
        throws SpaceManagerException
    {
        String sql;
        if (role == null)
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, StringProperty as memberprop where doc.name='MemberGroup' and doc.fullName=obj.name and obj.className = 'XWiki.XWikiGroups'"
                    + " and obj.id=memberprop.id.id and memberprop.id.name='member' and memberprop.value='"
                    + userName + "'";
        else {
            String roleGroupName = getRoleGroupName("", role).substring(1);
            sql =
                "select distinct doc.space from XWikiDocument as doc, BaseObject as obj, StringProperty as memberprop where doc.name='"
                    + roleGroupName
                    + "' and doc.fullName=obj.name and obj.className = 'XWiki.XWikiGroups'"
                    + " and obj.id=memberprop.id.id and memberprop.id.name='member' and memberprop.value='"
                    + userName + "'";

        }
        List<String> spaceList = Collections.emptyList();
        try {
            spaceList = context.getWiki().getStore().search(sql, 0, 0, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
        return spaceList;
    }

    /**
     * {@inheritDoc}
     */
    public boolean updateSpaceFromRequest(Space space, XWikiContext context)
        throws SpaceManagerException
    {
        space.updateSpaceFromRequest();
        if (space.validateSpaceData())
            return true;
        else
            return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean validateSpaceData(Space space, XWikiContext context)
        throws SpaceManagerException
    {
        return space.validateSpaceData();
    }

    /**
     * {@inheritDoc}
     */
    public void saveSpace(Space space, XWikiContext context) throws SpaceManagerException
    {
        try {
            space.saveWithProgrammingRights();
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addAdmin(String spaceName, String username, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            addUserToGroup(username, getAdminGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addAdmins(String spaceName, List<String> usernames, XWikiContext context)
        throws SpaceManagerException
    {
        for (int i = 0; i < usernames.size(); i++) {
            addAdmin(spaceName, (String) usernames.get(i), context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getAdmins(String spaceName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return getGroupService(context).getAllMembersNamesForGroup(
                getAdminGroupName(spaceName), 0, 0, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#removeAdmin(String, String, XWikiContext)
     */
    public void removeAdmin(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            removeUserFromGroup(userName, getAdminGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAdmin(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return isMemberOfGroup(userName, getAdminGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addUserToRole(String spaceName, String username, String role, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            addUserToGroup(username, getRoleGroupName(spaceName, role), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addUsersToRole(String spaceName, List<String> userNames, String role,
        XWikiContext context) throws SpaceManagerException
    {
        for (String userName : userNames) {
            addUserToRole(spaceName, userName, role, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getUsersForRole(String spaceName, String role, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return sortUserNames(getGroupService(context).getAllMembersNamesForGroup(
                getRoleGroupName(spaceName, role), 0, 0, context), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMember(String spaceName, String username, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return isMemberOfGroup(username, getMemberGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addUserToRoles(String spaceName, String username, List<String> roles,
        XWikiContext context) throws SpaceManagerException
    {
        for (int i = 0; i < roles.size(); i++) {
            addUserToRole(spaceName, username, (String) roles.get(i), context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addUsersToRoles(String spaceName, List<String> usernames, List<String> roles,
        XWikiContext context) throws SpaceManagerException
    {
        for (int i = 0; i < usernames.size(); i++) {
            addUserToRoles(spaceName, (String) usernames.get(i), roles, context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#removeUserFromRoles(String, String, List, XWikiContext)
     */
    public void removeUserFromRoles(String spaceName, String userName, List<String> roles,
        XWikiContext context) throws SpaceManagerException
    {
        for (int i = 0; i < roles.size(); i++) {
            removeUserFromRole(spaceName, userName, (String) roles.get(i), context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void removeUserFromRole(String spaceName, String userName, String role,
        XWikiContext context) throws SpaceManagerException
    {
        try {
            removeUserFromGroup(userName, getRoleGroupName(spaceName, role), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addMember(String spaceName, String username, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            addUserToGroup(username, getMemberGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#removeMember(String, String, XWikiContext)
     */
    public void removeMember(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            // remove admin role
            if (isAdmin(spaceName, userName, context)) {
                removeAdmin(spaceName, userName, context);
            }
            // remove all the other roles
            Iterator it = getRoles(spaceName, context).iterator();
            while (it.hasNext()) {
                String role = (String) it.next();
                removeUserFromRole(spaceName, userName, role, context);
            }
            // delete space user profile
            deleteSpaceUserProfile(spaceName, userName, context);
            // remove member
            removeUserFromGroup(userName, getMemberGroupName(spaceName), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    protected boolean isMemberOfGroup(String username, String groupname, XWikiContext context)
        throws XWikiException
    {
        ;
        Collection coll =
            ((RightsManagerPluginApi) context.getWiki().getPluginApi("rightsmanager", context))
                .getAllGroupsNamesForMember(username);
        Iterator it = coll.iterator();
        while (it.hasNext()) {
            if (groupname.equals((String) it.next()))
                return true;
        }
        return false;
    }

    /**
     * High speed user adding without resaving the whole groups doc
     * 
     * @param username
     * @param groupName
     * @param context
     * @throws XWikiException
     */
    protected void addUserToGroup(String username, String groupName, XWikiContext context)
        throws XWikiException
    {
        // don't add if he is already a member
        if (isMemberOfGroup(username, groupName, context))
            return;

        XWiki xwiki = context.getWiki();
        BaseClass groupClass = xwiki.getGroupClass(context);
        XWikiDocument groupDoc = xwiki.getDocument(groupName, context);

        BaseObject memberObject = (BaseObject) groupClass.newObject(context);
        memberObject.setClassName(groupClass.getName());
        memberObject.setName(groupDoc.getFullName());
        memberObject.setStringValue("member", username);
        groupDoc.addObject(groupClass.getName(), memberObject);
        String content = groupDoc.getContent();
        if ((content == null) || (content.equals("")))
            groupDoc.setContent("#includeForm(\"XWiki.XWikiGroupSheet\")");
        xwiki.saveDocument(groupDoc, context.getMessageTool()
            .get("core.comment.addedUserToGroup"), context);
    }

    private void removeUserFromGroup(String userName, String groupName, XWikiContext context)
        throws XWikiException
    {
        // don't remove if he's not a member
        if (!isMemberOfGroup(userName, groupName, context)) {
            return;
        }

        XWiki xwiki = context.getWiki();
        BaseClass groupClass = xwiki.getGroupClass(context);
        XWikiDocument groupDoc = xwiki.getDocument(groupName, context);
        BaseObject memberObject = groupDoc.getObject(groupClass.getName(), "member", userName);
        if (memberObject == null) {
            return;
        }
        groupDoc.removeObject(memberObject);
        xwiki.saveDocument(groupDoc, context.getMessageTool().get(
            "core.comment.removedUserFromGroup"), context);
    }

    /**
     * {@inheritDoc}
     */
    public void addMembers(String spaceName, List usernames, XWikiContext context)
        throws SpaceManagerException
    {
        for (int i = 0; i < usernames.size(); i++) {
            addMember(spaceName, (String) usernames.get(i), context);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#getMembers(String, XWikiContext)
     */
    public Collection<String> getMembers(String spaceName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return sortUserNames(getGroupService(context).getAllMembersNamesForGroup(
                getMemberGroupName(spaceName), 0, 0, context), context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    private List<String> sortUserNames(Collection<String> collectionOfUsers,
        final XWikiContext context)
    {
        List<String> users = new ArrayList<String>(collectionOfUsers);
        Collections.sort(users, new Comparator<String>()
        {
            public int compare(String a, String b)
            {
                try {
                    XWikiDocument aDoc = context.getWiki().getDocument(a, context);
                    XWikiDocument bDoc = context.getWiki().getDocument(b, context);
                    String aFirstName =
                        aDoc.getObject("XWiki.XWikiUsers").getStringValue("first_name");
                    String bFirstName =
                        bDoc.getObject("XWiki.XWikiUsers").getStringValue("first_name");
                    int cmp = aFirstName.compareToIgnoreCase(bFirstName);
                    if (cmp == 0) {
                        String aLastName =
                            aDoc.getObject("XWiki.XWikiUsers").getStringValue("last_name");
                        String bLastName =
                            bDoc.getObject("XWiki.XWikiUsers").getStringValue("last_name");
                        return aLastName.compareTo(bLastName);
                    } else {
                        return cmp;
                    }
                } catch (Exception e) {
                    return ((String) a).compareTo((String) b);
                }
            }
        });
        return users;
    }

    public String getMemberGroupName(String spaceName)
    {
        return getSpaceManagerExtension().getMemberGroupName(spaceName);
    }

    public String getAdminGroupName(String spaceName)
    {
        return getSpaceManagerExtension().getAdminGroupName(spaceName);
    }

    public String getRoleGroupName(String spaceName, String role)
    {
        return getSpaceManagerExtension().getRoleGroupName(spaceName, role);
    }

    protected XWikiGroupService getGroupService(XWikiContext context) throws XWikiException
    {
        return context.getWiki().getGroupService(context);
    }

    public SpaceUserProfile getSpaceUserProfile(String spaceName, String username,
        XWikiContext context) throws SpaceManagerException
    {
        return newUserSpaceProfile(username, spaceName, context);
    }

    private void deleteSpaceUserProfile(String spaceName, String userName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            String docName = getSpaceUserProfilePageName(userName, spaceName);
            XWikiDocument doc = context.getWiki().getDocument(docName, context);
            if (!doc.isNew())
                context.getWiki().deleteDocument(doc, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    public String getSpaceUserProfilePageName(String userName, String spaceName)
    {
        return getSpaceManagerExtension().getSpaceUserProfilePageName(userName, spaceName);
    }

    protected SpaceUserProfile newUserSpaceProfile(String user, String space, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            return new SpaceUserProfileImpl(user, space, this, context);
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getLastModifiedDocuments(String spaceName, XWikiContext context,
        boolean recursive, int nb, int start) throws SpaceManagerException
    {
        notImplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Collection<String> getRoles(String spaceName, XWikiContext context)
        throws SpaceManagerException
    {
        // The roles notion is not natively implemented in the spacemanager plugin yet,
        // so we return a empty list. Plugins that extend the spacemanager plugin can overload this
        // method to implement roles, either by hardcoding or retrieving from DB the roles list.
        // @TODO Implement methods to set a list of roles for a space.
        return Collections.emptyList();
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#getRoles(String, String, XWikiContext)
     */
    public Collection<String> getRoles(String spaceName, String memberName, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            Collection<String> memberRoles =
                context.getWiki().getGroupService(context).getAllGroupsNamesForMember(memberName,
                    0, 0, context);
            Collection<String> spaceRoles = getRoles(spaceName, context);
            memberRoles.retainAll(spaceRoles);
            return memberRoles;
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List getLastModifiedDocuments(String spaceName, XWikiContext context)
        throws SpaceManagerException
    {
        notImplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public List searchDocuments(String spaceName, String hql, XWikiContext context)
        throws SpaceManagerException
    {
        notImplemented();
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public int countSpaces(XWikiContext context) throws SpaceManagerException
    {
        String type = getSpaceTypeName();
        String className = getSpaceClassName();
        String sql;
        if (hasCustomMapping())
            sql =
                "select count(*) from XWikiDocument as doc, BaseObject as obj, " + className
                    + " as space" + " where doc.fullName = obj.name and obj.className='"
                    + className + "' and obj.id = space.id and space.type='" + type + "'";
        else
            sql =
                "select count(*) from XWikiDocument as doc, BaseObject as obj, StringProperty as typeprop"
                    + " where doc.fullName=obj.name and obj.className = '"
                    + className
                    + "' and obj.id=typeprop.id.id and typeprop.id.name='type' and typeprop.value='"
                    + type + "'";

        try {
            List result = context.getWiki().search(sql, context);
            Integer res = (Integer) result.get(0);
            return res.intValue();
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SpaceManager#joinSpace(String, XWikiContext)
     */
    public boolean joinSpace(String spaceName, XWikiContext context) throws SpaceManagerException
    {
        try {
            SpaceUserProfile userProfile =
                newUserSpaceProfile(context.getUser(), spaceName, context);
            userProfile.updateProfileFromRequest();
            userProfile.saveWithProgrammingRights();
            addMember(spaceName, context.getUser(), context);
            sendMail(SpaceAction.JOIN, getSpace(spaceName, context), context);
            return true;
        } catch (XWikiException e) {
            throw new SpaceManagerException(e);
        }
    }

    /**
     * Helper function to send email after a space action. This mail template full name is composed
     * of the space name and the action with the following convention : spaceName + "." +
     * "MailTemplate" + action + "Space", as in MySpace.MailTemplateJoinSpace
     * 
     * @see getTemplateMailPageName
     * @param action the action which triggered the mail sending. See
     *            {@link SpaceManager.SpaceAction} for possible actions.
     * @param space The space on which the action has triggered the mail sending
     * @throws SpaceManagerException
     */
    private void sendMail(String action, Space space, XWikiContext context)
        throws SpaceManagerException
    {
        if (!mailNotification) {
            return;
        }

        VelocityContext vContext = new VelocityContext();
        vContext.put("space", space);
        String fromUser = context.getWiki().getXWikiPreference("space_email", context);
        if (fromUser == null || fromUser.trim().length() == 0) {
            fromUser = context.getWiki().getXWikiPreference("admin_email", context);
        }
        String[] toUsers = new String[0];
        if (SpaceAction.CREATE.equals(action)) {
            // notify space administrators upon space creation
            Collection<String> admins = getAdmins(space.getSpaceName(), context);
            toUsers = (String[]) admins.toArray(new String[admins.size()]);
        } else if (SpaceAction.JOIN.equals(action)) {
            // send join group confirmation e-mail
            boolean optOutEmail = false;
            try {
                optOutEmail = context.getWiki().getUserPreferenceAsInt("opt_out", context) != 0;
            } catch(NumberFormatException e) {
                // if nothing is specified then we don't opt out
            }
            if (optOutEmail) {
                return;
            } else {
                toUsers = new String[] {context.getUser()};
            }
        }

        if (fromUser == null) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_SENDER_EMAIL_INVALID,
                "Sender email is invalid");
        }

        boolean toUsersValid = toUsers.length > 0;
        for (int i = 0; i < toUsers.length && toUsersValid; i++) {
            if (!isEmailAddress(toUsers[i])) {
                toUsers[i] = getEmailAddress(toUsers[i], context);
            }
            if (toUsers[i] == null) {
                toUsersValid = false;
            }
        }

        if (!toUsersValid) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_TARGET_EMAIL_INVALID,
                "Target email is invalid");
        }
        String strToUsers = join(toUsers, ",");

        MailSenderPlugin mailSender = getMailSenderPlugin(context);

        try {
            String templateDocFullName =
                getTemplateMailPageName(space.getSpaceName(), action, context);
            XWikiDocument mailDoc = context.getWiki().getDocument(templateDocFullName, context);
            XWikiDocument translatedMailDoc = mailDoc.getTranslatedDocument(context);
            mailSender
                .prepareVelocityContext(fromUser, strToUsers, null, null, vContext, context);
            vContext.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), context));
            vContext.put("context", new com.xpn.xwiki.api.Context(context));
            String mailSubject =
                XWikiVelocityRenderer.evaluate(translatedMailDoc.getTitle(), templateDocFullName,
                    vContext, context);
            String mailContent =
                XWikiVelocityRenderer.evaluate(translatedMailDoc.getContent(),
                    templateDocFullName, vContext, context);

            Mail mail =
                new Mail(fromUser, strToUsers, null, null, mailSubject, mailContent, null);
            mailSender.sendMail(mail, context);
        } catch (Exception e) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_SENDING_EMAIL_FAILED,
                "Sending notification email failed",
                e);
        }
    }

    private MailSenderPlugin getMailSenderPlugin(XWikiContext context)
        throws SpaceManagerException
    {
        MailSenderPlugin mailSender =
            (MailSenderPlugin) context.getWiki().getPlugin("mailsender", context);

        if (mailSender == null)
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_MANAGER_REQUIRES_MAILSENDER_PLUGIN,
                "SpaceManager requires the mail sender plugin");

        return mailSender;
    }

    // code duplicated from InvitationManagerImpl !!!
    private static final String join(String[] array, String separator)
    {
        StringBuffer result = new StringBuffer("");
        if (array.length > 0) {
            result.append(array[0]);
        }
        for (int i = 1; i < array.length; i++) {
            result.append("," + array[i]);
        }
        return result.toString();
    }

    // code duplicated from InvitationManagerImpl !!!
    private boolean isEmailAddress(String str)
    {
        return str.contains("@");
    }

    // code duplicated from InvitationManagerImpl !!!
    private String getEmailAddress(String user, XWikiContext context)
        throws SpaceManagerException
    {
        try {
            String wikiuser = (user.startsWith("XWiki.")) ? user : "XWiki." + user;

            if (wikiuser == null)
                return null;

            XWikiDocument userDoc = null;
            userDoc = context.getWiki().getDocument(wikiuser, context);

            if (userDoc.isNew())
                return null;

            String email = "";
            try {
                email = userDoc.getObject("XWiki.XWikiUsers").getStringValue("email");
            } catch (Exception e) {
                return null;
            }
            if ((email == null) || (email.equals("")))
                return null;

            return email;
        } catch (Exception e) {
            throw new SpaceManagerException(SpaceManagerException.MODULE_PLUGIN_SPACEMANAGER,
                SpaceManagerException.ERROR_SPACE_CANNOT_FIND_EMAIL_ADDRESS,
                "Cannot find email address of user " + user,
                e);
        }
    }

    /**
     * Convention based private helper to retrieve a mail template full name from a space name and
     * an action name. TODO I think we should have the possibility to select for each action a
     * template that is not located in the space itself, but in another web of the wiki. This would
     * be to avoid copying this template each time we create a space if its content is not supposed
     * to be modified, And thus reduce the impact of space creation on the db size.
     */
    private String getTemplateMailPageName(String spaceName, String action, XWikiContext context)
    {
        String docName = spaceName + "." + "MailTemplate" + action + "Space";
        try {
            if (context.getWiki().getDocument(docName, context).isNew()) {
                docName = null;
            }
        } catch (XWikiException e) {
            docName = null;
        }
        if (docName == null) {
            docName = getDefaultResourceSpace(context) + "." + "MailTemplate" + action + "Space";
        }

        return docName;
    }

    private String getDefaultResourceSpace(XWikiContext context)
    {
        return context.getWiki().Param("xwiki.spacemanager.resourcespace",
            SpaceManager.DEFAULT_RESOURCE_SPACE);
    }

    public boolean isMailNotification()
    {
        return mailNotification;
    }

    public void setMailNotification(boolean mailNotification)
    {
        this.mailNotification = mailNotification;
    }
}
