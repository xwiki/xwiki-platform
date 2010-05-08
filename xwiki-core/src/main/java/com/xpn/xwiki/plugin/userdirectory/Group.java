package com.xpn.xwiki.plugin.userdirectory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.PluginException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public class Group
{
    private Document doc;

    private Object objDirectoryGroup;

    private boolean isNew = false;

    public static final int ERROR_USERDIRECTORYPLUGIN_GROUP_UNKNOWN = 0;

    public static final int ERROR_USERDIRECTORYPLUGIN_GROUP_DOESNT_EXIST = 1;

    public Group(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        this(doc.newDocument(context), context);
    }

    public Group(Document doc, XWikiContext context) throws XWikiException
    {
        reload(doc, context);
    }

    public void reload(XWikiContext context) throws XWikiException
    {
        reload(null, context);
    }

    public void reload(Document doc, XWikiContext context) throws XWikiException
    {
        if (doc == null)
            doc = context.getWiki().getDocument(this.doc.getFullName(), context).newDocument(context);
        this.doc = doc;

        BaseClass dirGrpClass = getXWikiDirectoryGroupClass(context);
        Object obj;

        if ((obj = doc.getObject(dirGrpClass.getName())) == null) {
            doc.createNewObject(dirGrpClass.getName());
            obj = doc.getObject(dirGrpClass.getName());
            this.isNew = true;
        }

        this.objDirectoryGroup = obj;
    }

    public boolean isNew()
    {
        return isNew;
    }

    public void setName(String name)
    {
        objDirectoryGroup.set("name", name);
    }

    public String getName()
    {
        return (String) objDirectoryGroup.get("name");
    }

    public String getPageName()
    {
        return doc.getFullName();
    }

    public void set(String key, java.lang.Object value, XWikiContext context)
    {
        objDirectoryGroup.set(key, value);
    }

    public java.lang.Object get(String key, XWikiContext context)
    {
        return objDirectoryGroup.get(key);
    }

    public List getUsersPageName(XWikiContext context) throws XWikiException
    {
        List usersPageName = new ArrayList();
        List objs = doc.getObjects(getXWikiGroupsClass(context).getName());
        Iterator it = objs.iterator();
        while (it.hasNext()) {
            usersPageName.add(((Object) it.next()).get("member"));
        }

        return usersPageName;
    }

    /**
     * Add a parent to this group
     * 
     * @param name the name of the page of the parent group
     * @param context
     * @return false if the group is already parent of this one
     * @throws XWikiException if the parent group doesn't exist
     */
    public boolean addParentName(String name, UserDirectoryPlugin userDirPlugin, XWikiContext context)
        throws XWikiException
    {
        if (isParentPage(name, context))
            return false;
        Group parentGroup = getGroup(name, context);
        if (parentGroup.isNew())
            throw new PluginException(UserDirectoryPlugin.class, ERROR_USERDIRECTORYPLUGIN_GROUP_DOESNT_EXIST,
                "This group doesn't exist");
        addParentName(name, context);
        return true;
    }

    public boolean isParentPage(String name, XWikiContext context) throws XWikiException
    {
        List parentsPage = getParentPages(context);
        Iterator it = parentsPage.iterator();
        while (it.hasNext()) {
            String pageName = (String) it.next();
            if (pageName.equals(name))
                return true;
        }
        return false;
    }

    public boolean removeParent(String name, UserDirectoryPlugin userDirPlugin, XWikiContext context)
        throws XWikiException
    {
        if (!isParentPage(name, context))
            return false;
        removeParent(name, context);
        return true;
    }

    private void removeParent(String name, XWikiContext context) throws XWikiException
    {
        String className = getXWikiGroupRelationClass(context).getName();
        Vector objs = doc.getObjects(className);
        Iterator it = objs.iterator();

        while (it.hasNext()) {
            Object obj = (Object) it.next();
            if (obj.get("parentpage").equals(name)) {
                doc.removeObject(obj);
                return;
            }
        }
    }

    private void addParentName(String name, XWikiContext context) throws XWikiException
    {
        String className = getXWikiGroupRelationClass(context).getName();
        int nb = doc.createNewObject(className);

        Object obj = doc.getObject(className, nb);
        obj.set("parentpage", name);
    }

    /**
     * @param context
     * @return the list of parent group's page
     * @throws XWikiException
     */
    public List getParentPages(XWikiContext context) throws XWikiException
    {
        ArrayList parents = new ArrayList();

        Vector objs = doc.getObjects(getXWikiGroupRelationClass(context).getName());
        if (objs == null)
            return parents;

        Iterator it = objs.iterator();
        while (it.hasNext()) {
            Object obj = (Object) it.next();
            String parentsPage = (String) obj.get("parentpage");
            if (parentsPage != null && parentsPage.length() > 0)
                parents.add(parentsPage);
        }
        return parents;
    }

    public List getParents(XWikiContext context) throws XWikiException
    {
        List parentsPage = getParentPages(context);
        List res = new ArrayList();
        Iterator it = parentsPage.iterator();
        while (it.hasNext()) {
            String str = (String) it.next();
            res.add(getGroup(str, context));
        }
        return res;
    }

    /**
     * @param name
     * @param context
     * @return
     * @throws XWikiException
     */
    public boolean addUser(String name, XWikiContext context) throws XWikiException
    {
        if (isMember(name, context))
            return false;
        String className = getXWikiGroupsClass(context).getName();
        int nb = doc.createNewObject(className);
        Object obj = doc.getObject(className, nb);
        obj.set("member", name);
        return true;
    }

    public boolean isMember(String docName, XWikiContext context) throws XWikiException
    {
        List users = getMembers(context);
        return users.contains(docName);
    }

    public List getUnactivatedMembers(XWikiContext context) throws XWikiException
    {
        List unactivatedMembers = new ArrayList();
        List members = getMembers(context);
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String userPage = (String) it.next();
            Document doc = context.getWiki().getDocument(userPage, context).newDocument(context);
            doc.use("XWiki.XWikiUsers");
            Integer active = (Integer) doc.getValue("active");
            if (active == null || active.intValue() == 0) {
                unactivatedMembers.add(userPage);
            }
        }
        return unactivatedMembers;
    }

    public List getActivatedMembers(XWikiContext context) throws XWikiException
    {
        List unactivatedMembers = new ArrayList();
        List members = getMembers(context);
        Iterator it = members.iterator();
        while (it.hasNext()) {
            String userPage = (String) it.next();
            Document doc = context.getWiki().getDocument(userPage, context).newDocument(context);
            doc.use("XWiki.XWikiUsers");
            String active = (String) doc.getValue("active");
            if (active.equals("0")) {
                unactivatedMembers.add(userPage);
            }
        }
        return unactivatedMembers;
    }

    public List getMembers(XWikiContext context) throws XWikiException
    {
        List objs = doc.getObjects(Group.getXWikiGroupsClass(context).getName());
        Iterator it = objs.iterator();
        List members = new ArrayList();
        while (it.hasNext()) {
            members.add(((Object) it.next()).get("member"));
        }
        return members;
    }

    public boolean removeUser(String docName, XWikiContext context) throws XWikiException
    {
        if (!isMember(docName, context))
            return false;
        String className = getXWikiGroupsClass(context).getName();
        Vector objs = doc.getObjects(className);

        Object obj;
        Iterator it = objs.iterator();

        while (it.hasNext()) {
            obj = (Object) it.next();
            if (obj.get("member").equals(docName)) {
                doc.removeObject(obj);
                return true;
            }
        }
        return false;
    }

    public void save(XWikiContext context) throws XWikiException
    {
        doc.saveWithProgrammingRights();
    }

    protected static BaseClass getXWikiGroupsClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument("XWiki.XWikiGroups", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName("XWikiGroups");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.XWikiGroups");
        needsUpdate |= bclass.addTextField("member", "Member", 30);
        needsUpdate |= bclass.addTextField("role", "Role", 30);
        needsUpdate |= bclass.addTextAreaField("description", "Description", 40, 5);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWikiGroups");
            doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    protected static BaseClass getXWikiDirectoryGroupClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument("XWiki.DirectoryGroupClass", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName("DirectoryGroupClass");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.DirectoryGroupClass");
        needsUpdate |= bclass.addTextField("name", "Name", 30);
        needsUpdate |= bclass.addTextAreaField("description", "Description", 40, 5);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 DirectoryGroupClass");
            doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    protected static BaseClass getXWikiGroupRelationClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument("XWiki.GroupRelationClass", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName("GroupRelationClass");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.GroupRelationClass");
        needsUpdate |= bclass.addTextField("name", "Name", 30);
        needsUpdate |= bclass.addTextField("parentpage", "Parent", 30);
        needsUpdate |= bclass.addTextAreaField("description", "Description", 40, 5);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWikiGroup");
            doc.setSyntaxId(XWikiDocument.XWIKI10_SYNTAXID);
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    public static List getAllGroupsPageName(XWikiContext context) throws XWikiException
    {
        String className = getXWikiDirectoryGroupClass(context).getName();
        String hql = ", BaseObject as obj where obj.name=doc.fullName" + " and obj.className='" + className + "'";
        return context.getWiki().getStore().searchDocumentsNames(hql, context);
    }

    public static boolean isValidGroup(String grpName, XWikiContext context) throws XWikiException
    {
        Document doc = context.getWiki().getDocument(grpName, context).newDocument(context);
        if (doc.isNew())
            return false;
        return (doc.getObjects(getXWikiDirectoryGroupClass(context).getName()).size() > 0);
    }

    public static Group getGroup(String space, String name, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = context.getWiki().getDocument(space, name, context);
        return (new Group(doc, context));
    }

    public Group getGroup(String name, XWikiContext context) throws XWikiException
    {
        if (name.indexOf('.') >= 0) {
            String[] grp = name.split("\\.");
            return getGroup(grp[0], grp[1], context);
        } else
            return getGroup(UserDirectoryPlugin.DEFAULT_PLUGIN_SPACE, name, context);
    }

    public static List getAllGroupsPageName(String orderBy, XWikiContext context) throws XWikiException
    {
        String className = getXWikiDirectoryGroupClass(context).getName();
        String hql =
            ", BaseObject as obj, StringProperty as prop where obj.name=doc.fullName" + " and obj.className='"
                + className + "' and obj.id=prop.id.id and prop.name='" + orderBy + "' order by prop.value";
        return context.getWiki().getStore().searchDocumentsNames(hql, context);
    }
}
