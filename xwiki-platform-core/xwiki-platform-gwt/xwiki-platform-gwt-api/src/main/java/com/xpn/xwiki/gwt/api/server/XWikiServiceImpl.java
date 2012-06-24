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
package com.xpn.xwiki.gwt.api.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.gwt.api.client.Attachment;
import com.xpn.xwiki.gwt.api.client.Dictionary;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.User;
import com.xpn.xwiki.gwt.api.client.VersionInfo;
import com.xpn.xwiki.gwt.api.client.XObject;
import com.xpn.xwiki.gwt.api.client.XWikiGWTException;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

public class XWikiServiceImpl extends RemoteServiceServlet implements XWikiService
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    /**
     * We override the default processCall method in order to provide XWiki initialization before we handle the request.
     * This allows us to initialize the XWiki Context and the new Container Objects (which are using ThreadLocal
     * variables).
     * 
     * @see RemoteServiceServlet#processCall(String)
     */
    @Override
    public String processCall(String payload) throws SerializationException
    {
        String result;

        try {
            initXWiki();
            result = super.processCall(payload);
        } catch (Exception e) {
            throw new SerializationException("Failed to initialize XWiki GWT subsystem", e);
        } finally {
            // Perform cleanup here
            cleanupContainerComponent();
        }

        return result;
    }

    /**
     * Initialize XWiki Context and XWiki Container Objects.
     */
    private void initXWiki() throws Exception
    {
        XWikiEngineContext engine = new XWikiServletContext(getServletContext());
        XWikiRequest request = new XWikiServletRequest(getThreadLocalRequest());
        XWikiResponse response = new XWikiServletResponse(getThreadLocalResponse());

        XWikiContext context = Utils.prepareContext("", request, response, engine);
        context.setMode(XWikiContext.MODE_GWT);
        context.setDatabase("xwiki");

        initializeContainerComponent(context);

        XWiki xwiki = XWiki.getXWiki(context);
        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);

        xwiki.prepareResources(context);

        String username = "XWiki.XWikiGuest";
        if (context.getMode() == XWikiContext.MODE_GWT_DEBUG) {
            username = "XWiki.superadmin";
        }
        XWikiUser user = context.getWiki().checkAuth(context);
        if (user != null) {
            username = user.getUser();
        }
        context.setUser(username);

        if (context.getDoc() == null) {
            context.setDoc(new XWikiDocument("Fake", "Document"));
        }

        context.put("ajax", new Boolean(true));
    }

    private void initializeContainerComponent(XWikiContext context) throws ServletException
    {
        // Initialize the Container fields (request, response, session).
        // Note that this is a bridge between the old core and the component architecture.
        // In the new component architecture we use ThreadLocal to transport the request,
        // response and session to components which require them.
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.class);
        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new ServletException("Failed to initialize request/response or session", e);
        }
    }

    private void cleanupContainerComponent()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);

        // We must ensure we clean the ThreadLocal variables located in the Container and Execution
        // components as otherwise we will have a potential memory leak.
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

    /**
     * Helper method to retrieve the {@link XWikiContext} from the {@link Execution} context.
     * 
     * @return this execution's {@link XWikiContext}, set upon initialization
     */
    protected XWikiContext getXWikiContext()
    {
        return (XWikiContext) Utils.getComponent(Execution.class).getContext().getProperty("xwikicontext");
    }

    protected XWikiGWTException getXWikiGWTException(Exception e)
    {
        // let's make sure we are informed
        if (LOGGER.isErrorEnabled()) {
            LOGGER.error("Unhandled exception on the server", e);
        }

        if (e instanceof XWikiGWTException) {
            return (XWikiGWTException) e;
        }

        XWikiException exp;
        if (e instanceof XWikiException)
            exp = (XWikiException) e;
        else
            exp =
                new XWikiException(XWikiException.MODULE_XWIKI_GWT_API, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Unknown GWT Exception", e);

        return new XWikiGWTException(exp.getMessage(), exp.getFullMessage(), exp.getCode(), exp.getModule());
    }

    public Document getDocument(String fullName) throws XWikiGWTException
    {
        return getDocument(fullName, false, false, false, false);
    }

    public Document getDocument(String fullName, boolean full, boolean withRenderedContent) throws XWikiGWTException
    {
        return getDocument(fullName, full, false, false, withRenderedContent);
    }

    public String getUniquePageName(String space) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getUniquePageName(space, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public String getUniquePageName(String space, String pageName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getUniquePageName(space, pageName, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Document getUniqueDocument(String space, String pageName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            String fullName = context.getWiki().getUniquePageName(space, pageName, context);
            return getDocument(fullName);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Document getUniqueDocument(String space) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            String fullName = context.getWiki().getUniquePageName(space, context);
            return getDocument(space + "." + fullName);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers)
        throws XWikiGWTException
    {
        return getDocument(fullName, full, viewDisplayers, editDisplayers, false);
    }

    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers,
        boolean withRenderedContent) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);

                return newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, withRenderedContent,
                    context);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean deleteDocument(String docName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("delete", context.getUser(), docName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(docName, context);
                context.getWiki().deleteDocument(doc, context);
                return Boolean.valueOf(true);
            } else {
                return Boolean.valueOf(false);
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public int deleteDocuments(String sql) throws XWikiGWTException
    {
        int nb = 0;
        List newlist = new ArrayList();
        try {
            XWikiContext context = getXWikiContext();
            List list = context.getWiki().getStore().searchDocumentsNames(sql, context);
            if ((list == null) && (list.size() == 0))
                return nb;
            for (int i = 0; i < list.size(); i++) {
                if (context.getWiki().getRightService()
                    .hasAccessLevel("delete", context.getUser(), (String) list.get(i), context) == true) {
                    XWikiDocument doc = context.getWiki().getDocument((String) list.get(i), context);
                    context.getWiki().deleteDocument(doc, context);
                    nb++;
                }
            }
            return nb;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public User getUser() throws XWikiGWTException
    {
        try {
            return getUser(getXWikiContext().getUser());
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public User getUser(String fullName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                return newUser(new User(), doc, context);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public User[] getUserList(int nb, int start) throws XWikiGWTException
    {
        User[] users = new User[nb];
        try {
            XWikiContext context = getXWikiContext();
            List list =
                searchDocuments(",BaseObject as obj where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'",
                    nb, start, context);
            if (list == null)
                return new User[0];

            for (int i = 0; i < list.size(); i++) {
                String username = (String) list.get(i);
                XWikiDocument userdoc = context.getWiki().getDocument(username, context);
                users[i] = newUser(new User(), userdoc, context);
            }

            return users;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public List searchDocuments(String sql, int nb, int start) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return searchDocuments(sql, nb, start, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public List getDocuments(String sql, int nb, int start) throws XWikiGWTException
    {
        return getDocuments(sql, nb, start, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full) throws XWikiGWTException
    {
        return getDocuments(sql, nb, start, full, false, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers)
        throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return getDocuments(sql, nb, start, full, viewDisplayers, editDisplayers, false, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, String value)
        throws XWikiGWTException
    {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseObject bobject = doc.getObject(className);
                if (bobject == null) {
                    bobject = new BaseObject();
                    doc.addObject(className, bobject);
                }
                bobject.setName(doc.getFullName());
                bobject.setClassName(className);
                bobject.set(propertyname, value, context);
                doc.setContentDirty(true);
                doc.setAuthor(context.getUser());
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.updateProperty"),
                    context);
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, int value)
        throws XWikiGWTException
    {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                doc.setIntValue(className, propertyname, value);
                doc.setAuthor(context.getUser());
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.updateProperty"),
                    context);
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, List value)
        throws XWikiGWTException
    {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseClass bclass = context.getWiki().getClass(className, context);
                ListClass lclass = (ListClass) ((bclass == null) ? null : bclass.get(propertyname));
                BaseProperty prop = lclass.fromValue(value);
                doc.setProperty(className, propertyname, prop);
                doc.setAuthor(context.getUser());
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.updateProperty"),
                    context);
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    private List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers,
        boolean editDisplayers, XWikiContext context) throws XWikiGWTException
    {
        return getDocuments(sql, nb, start, full, viewDisplayers, editDisplayers, false, context);
    }

    private List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers,
        boolean editDisplayers, boolean withRenderedContent, XWikiContext context) throws XWikiGWTException
    {
        List newlist = new ArrayList();
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(sql, nb, start, context);
            if ((list == null) && (list.size() == 0))
                return newlist;
            for (int i = 0; i < list.size(); i++) {
                if (context.getWiki().getRightService()
                    .hasAccessLevel("view", context.getUser(), (String) list.get(i), context) == true) {
                    XWikiDocument doc = context.getWiki().getDocument((String) list.get(i), context);
                    Document apidoc =
                        newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, withRenderedContent,
                            context);
                    newlist.add(apidoc);
                }
            }

            return newlist;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    private List searchDocuments(String sql, int nb, int start, XWikiContext context) throws XWikiGWTException
    {
        List newlist = new ArrayList();
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(sql, nb, start, context);
            if ((list == null) && (list.size() == 0))
                return newlist;

            for (int i = 0; i < list.size(); i++) {
                if (context.getWiki().getRightService()
                    .hasAccessLevel("view", context.getUser(), (String) list.get(i), context) == true)
                    newlist.add(list.get(i));
            }

            return newlist;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public List getObjects(String sql, String className, int nb, int start) throws XWikiGWTException
    {
        List docs = getDocuments(sql, nb, start, true);
        List objects = new ArrayList();
        Iterator it = docs.iterator();
        while (it.hasNext()) {
            Document doc = (Document) it.next();
            List docObjs = doc.getObjects(className);
            if (docObjs != null)
                objects.addAll(docObjs);
        }
        return objects;
    }

    public XObject getFirstObject(String sql, String className) throws XWikiGWTException
    {
        List objs = getObjects(sql, className, 1, 0);
        if (objs != null && objs.size() > 0)
            return (XObject) objs.get(0);
        return null;
    }

    public XObject addObject(XWikiDocument doc, String className) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            int index = doc.createNewObject(className, context);
            return newObject(new XObject(), doc.getObject(className, index), false, false, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public XObject addObject(String fullName, String className) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), fullName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                XObject obj = addObject(doc, className);

                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.updateProperty"),
                    context);

                return obj;
            }
            return null;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public List addObject(String fullName, List classesName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument doc = context.getWiki().getDocument(fullName, context);
            Iterator it = classesName.iterator();
            List objs = new ArrayList();
            while (it.hasNext()) {
                objs.add(addObject(doc, (String) it.next()));
            }

            context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.addObject"), context);
            return objs;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public boolean addObject(String docname, XObject xobject) throws XWikiGWTException
    {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseObject newObject = doc.newObject(xobject.getClassName(), context);

                mergeObject(xobject, newObject, context);
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.addObject"), context);
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    /**
     * save only the content of a document TODO manage translations
     * 
     * @param fullName
     * @param content
     * @return
     */
    public Boolean saveDocumentContent(String fullName, String content) throws XWikiGWTException
    {
        return saveDocumentContent(fullName, content, null);
    }

    /**
     * save only the content of a document TODO manage translations
     * 
     * @param fullName
     * @param content
     * @return
     */
    public Boolean saveDocumentContent(String fullName, String content, String comment) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), fullName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                doc.setContent(content);
                doc.setAuthor(context.getUser());
                if (doc.isNew())
                    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc,
                    (comment == null) ? context.getMessageTool().get("core.comment.updateContent") : comment, context);
                return Boolean.valueOf(true);
            } else {
                return Boolean.valueOf(false);
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean saveObject(XObject object) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService()
                .hasAccessLevel("edit", context.getUser(), object.getName(), context)) {
                XWikiDocument doc = context.getWiki().getDocument(object.getName(), context);
                BaseObject bObject =
                    newBaseObject(doc.getObject(object.getClassName(), object.getNumber()), object, context);
                doc.setObject(object.getClassName(), object.getNumber(), bObject);
                doc.setAuthor(context.getUser());
                if (doc.isNew())
                    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.updateObject"), context);
                return Boolean.valueOf(true);
            } else {
                return Boolean.valueOf(false);
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean deleteObject(XObject object) throws XWikiGWTException
    {
        return deleteObject(object.getName(), object.getClassName(), object.getNumber());
    }

    public Boolean deleteObject(String docName, String className, int number) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(docName, context);

                BaseObject bObj = doc.getObject(className, number);

                if (!doc.removeObject(bObj))
                    return Boolean.valueOf(false);

                doc.setAuthor(context.getUser());
                if (doc.isNew())
                    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.deleteObject"), context);
                return Boolean.valueOf(true);
            } else {
                return Boolean.valueOf(false);
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean saveObjects(List objects) throws XWikiGWTException
    {
        Iterator it = objects.iterator();
        boolean error = false;
        while (it.hasNext()) {
            error |= !saveObject((XObject) it.next()).booleanValue();
        }
        return Boolean.valueOf(!error);
    }

    /**
     * return true if can be locked return null in case of an error return false in all the other cases
     * 
     * @param fullName
     * @param force
     * @return
     */
    public Boolean lockDocument(String fullName, boolean force) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument doc = context.getWiki().getDocument(fullName, context);
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), fullName, context)) {

                /* Setup a lock */
                XWikiLock lock = doc.getLock(context);
                if ((lock == null) || (lock.getUserName().equals(context.getUser())) || force) {
                    if (lock != null)
                        doc.removeLock(context);
                    doc.setLock(context.getUser(), context);
                    return Boolean.valueOf(true);
                } else
                    return Boolean.valueOf(false);
            } else
                return Boolean.valueOf(false);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public void unlockDocument(String fullName) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument doc = context.getWiki().getDocument(fullName, context);
            XWikiLock lock = doc.getLock(context);
            if (lock != null)
                doc.removeLock(context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean isLastDocumentVersion(String fullName, String version) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return Boolean.valueOf(context.getWiki().getDocument(fullName, context).getVersion().equals(version));
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public String getLoginURL() throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getDocument("XWiki.XWikiLogin", context).getExternalURL("login", context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public String login(String username, String password, boolean rememberme) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiUser user =
                context.getWiki().getAuthService().checkAuth(username, password, rememberme ? "yes" : "no", context);
            if (user == null)
                return "XWiki.XWikiGuest";
            else
                return user.getUser();
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public boolean addComment(String docname, String message) throws XWikiGWTException
    {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("comment", context.getUser(), docname, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseObject newObject = doc.newObject("XWiki.XWikiComments", context);
                newObject.set("author", context.getUser(), context);
                newObject.set("date", new Date(), context);
                newObject.set("comment", message, context);
                doc.setContentDirty(false); // consider comments not being content
                context.getWiki().saveDocument(doc, context.getMessageTool().get("core.comment.addComment"), context);
                return true;
            } else
                return false;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public List customQuery(String queryPage) throws XWikiGWTException
    {
        return customQuery(queryPage, null, 0, 0);
    }

    public List customQuery(String queryPage, Map params) throws XWikiGWTException
    {
        return customQuery(queryPage, params, 0, 0);
    }

    public List customQuery(String queryPage, int nb, int start) throws XWikiGWTException
    {
        return customQuery(queryPage, null, nb, start);
    }

    public List customQuery(String queryPage, Map params, int nb, int start) throws XWikiGWTException
    {
        List newlist = new ArrayList();
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument queryDoc = context.getWiki().getDocument(queryPage, context);
            if (context.getWiki().getRightService().hasProgrammingRights(queryDoc, context)) {
                if (params != null) {
                    XWikiRequestWrapper srw = new XWikiRequestWrapper(context.getRequest());
                    Iterator it = params.keySet().iterator();
                    while (it.hasNext()) {
                        String key = (String) it.next();
                        Object value = params.get(key);
                        if (key instanceof String) {
                            // we clean params so that they cannot close a string
                            params.put(key, ((String) value).replaceAll("'", ""));
                        } else {
                            params.remove(key);
                        }
                    }
                    srw.setParameterMap(params);
                    context.setRequest(srw);

                }
                List list =
                    context.getWiki().getStore().search(queryDoc.getRenderedContent(context), nb, start, context);
                for (int i = 0; i < list.size(); i++) {
                    Object[] item = (Object[]) list.get(i);
                    List itemlist = new ArrayList();
                    for (int j = 0; j < item.length; j++) {
                        itemlist.add(item[j]);
                    }
                    newlist.add(itemlist);
                }
                return newlist;
            }
            return null;
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    protected User newUser(User user, XWikiDocument xdoc, XWikiContext context) throws XWikiGWTException
    {
        newDocument(user, xdoc, context);
        user.setFirstName(xdoc.getStringValue("XWiki.XWikiUsers", "first_name"));
        user.setLastName(xdoc.getStringValue("XWiki.XWikiUsers", "last_name"));
        user.setEmail(xdoc.getStringValue("XWiki.XWikiUsers", "email"));

        XWiki xwiki = context.getWiki();
        user.setAdmin(xwiki.getRightService().hasAdminRights(context));
        return user;
    }

    protected Document newDocument(Document doc, XWikiDocument xdoc, XWikiContext context) throws XWikiGWTException
    {
        return newDocument(doc, xdoc, false, context);
    }

    protected Document newDocument(Document doc, XWikiDocument xdoc, boolean withObjects, XWikiContext context)
        throws XWikiGWTException
    {
        return newDocument(doc, xdoc, withObjects, false, false, false, context);
    }

    public boolean hasAccessLevel(String level, String fullName, XWikiContext context) throws XWikiGWTException
    {
        try {
            return getXWikiContext().getWiki().getRightService()
                .hasAccessLevel(level, context.getUser(), fullName, context);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    protected void assertEditRight(XWikiDocument doc, XWikiContext context) throws XWikiGWTException, XWikiException
    {
        if (context.getMode() == XWikiContext.MODE_GWT_DEBUG)
            return;
        if (!hasAccessLevel("edit", doc.getFullName(), context))
            raiseRightException(context);
    }

    protected void assertViewRight(String fullName, XWikiContext context) throws XWikiGWTException, XWikiException
    {
        if (context.getMode() == XWikiContext.MODE_GWT_DEBUG)
            return;
        if (!hasAccessLevel("view", fullName, context))
            raiseRightException(context);
    }

    protected void raiseRightException(XWikiContext context) throws XWikiException
    {
        if (context.getUser().equals("XWiki.XWikiGuest")) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID, "User needs to be logged-in");
        } else
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "User needs appropriate rights");
    }

    protected void assertViewRight(XWikiDocument doc, XWikiContext context) throws XWikiGWTException, XWikiException
    {
        assertViewRight(doc.getFullName(), context);
    }

    protected Document newDocument(Document doc, XWikiDocument xdoc, boolean withObjects, boolean withViewDisplayers,
        boolean withEditDisplayers, boolean withRenderedContent, XWikiContext context) throws XWikiGWTException
    {
        doc.setId(xdoc.getId());
        doc.setTitle(xdoc.getTitle());
        doc.setFullName(xdoc.getFullName());
        doc.setParent(xdoc.getParent());
        doc.setSpace(xdoc.getSpace());
        doc.setName(xdoc.getName());
        doc.setContent(xdoc.getContent());
        doc.setMeta(xdoc.getMeta());
        doc.setFormat(xdoc.getFormat());
        doc.setCreator(xdoc.getCreator());
        doc.setAuthor(xdoc.getAuthor());
        doc.setContentAuthor(xdoc.getContentAuthor());
        doc.setCustomClass(xdoc.getCustomClass());
        doc.setVersion(xdoc.getVersion());
        doc.setContentUpdateDate(xdoc.getContentUpdateDate().getTime());
        doc.setDate(xdoc.getDate().getTime());
        doc.setCreationDate(xdoc.getCreationDate().getTime());
        doc.setMostRecent(xdoc.isMostRecent());
        doc.setNew(xdoc.isNew());
        doc.setTemplate(xdoc.getTemplate());
        doc.setLanguage(xdoc.getLanguage());
        doc.setDefaultLanguage(xdoc.getDefaultLanguage());
        doc.setTranslation(xdoc.getTranslation());
        doc.setComment(xdoc.getComment());
        List comments = xdoc.getComments();
        doc.setCommentsNumber((comments == null) ? 0 : comments.size());
        doc.setUploadURL(xdoc.getExternalURL("upload", "ajax=1", context)); // "ajax=1"
        doc.setViewURL(xdoc.getExternalURL("view", context));
        try {
            doc.setSaveURL(context.getWiki().getExternalURL(xdoc.getFullName(), "save", "ajax=1", context)); // ,
                                                                                                             // "ajax=1"
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
        doc.setHasElement(xdoc.getElements());
        try {
            doc.setEditRight(context.getWiki().getRightService()
                .hasAccessLevel("edit", context.getUser(), xdoc.getFullName(), context));
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
        try {
            doc.setCommentRight(context.getWiki().getRightService()
                .hasAccessLevel("comment", context.getUser(), xdoc.getFullName(), context));
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
        if (withObjects) {
            Iterator<List<BaseObject>> it = xdoc.getXObjects().values().iterator();
            while (it.hasNext()) {
                List<BaseObject> list = it.next();

                for (int i = 0; i < list.size(); i++) {
                    BaseObject bobj = list.get(i);
                    if (bobj != null) {
                        XObject obj = newObject(new XObject(), bobj, withViewDisplayers, withEditDisplayers, context);
                        doc.addObject(bobj.getClassName(), obj);
                    }
                }
            }
        }
        if (xdoc.getAttachmentList() != null && xdoc.getAttachmentList().size() > 0) {
            Iterator it = xdoc.getAttachmentList().iterator();
            while (it.hasNext()) {
                XWikiAttachment xAtt = (XWikiAttachment) it.next();
                Attachment att = newAttachment(new Attachment(), xAtt, context);
                doc.addAttachments(att);
            }
        }
        if (withRenderedContent) {
            try {
                doc.setRenderedContent(xdoc.getRenderedContent(context));
            } catch (Exception e) {
                throw getXWikiGWTException(e);
            }
        }
        return doc;
    }

    protected Attachment newAttachment(Attachment att, XWikiAttachment xAtt, XWikiContext context)
    {
        att.setAttDate(xAtt.getDate().getTime());
        att.setAuthor(xAtt.getAuthor());
        att.setFilename(xAtt.getFilename());
        att.setId(xAtt.getId());
        att.setImage(xAtt.isImage(context));
        att.setMimeType(xAtt.getMimeType(context));
        att.setFilesize(xAtt.getFilesize());
        att.setDownloadUrl(context.getWiki().getExternalAttachmentURL(xAtt.getDoc().getFullName(), xAtt.getFilename(),
            context));

        return att;
    }

    protected XObject newObject(XObject xObject, BaseObject baseObject, boolean withViewDisplayers,
        boolean withEditDisplayers, XWikiContext context)
    {
        xObject.setName(baseObject.getName());
        xObject.setNumber(baseObject.getNumber());
        xObject.setClassName(baseObject.getClassName());
        String prefix = baseObject.getXClass(context).getName() + "_" + baseObject.getNumber() + "_";

        Object[] propnames = baseObject.getXClass(context).getFieldList().toArray();
        for (int i = 0; i < propnames.length; i++) {
            String propname = ((PropertyInterface) propnames[i]).getName();
            // TODO: this needs to be a param
            if (!propname.equals("fullcontent")) {
                try {
                    BaseProperty prop = (BaseProperty) baseObject.get(propname);
                    if (prop != null) {
                        Object value = prop.getValue();
                        if (value instanceof Date)
                            xObject.set(propname, new Date(((Date) prop.getValue()).getTime()));
                        else if (value instanceof List) {
                            List newlist = new ArrayList();
                            for (int j = 0; j < ((List) value).size(); j++) {
                                newlist.add(((List) value).get(j));
                            }
                            xObject.set(propname, newlist);
                        } else
                            xObject.set(propname, prop.getValue());
                    }
                } catch (Exception e) {
                }
                try {
                    if (withViewDisplayers)
                        xObject.setViewProperty(propname, baseObject.displayView(propname, prefix, context));
                } catch (Exception e) {
                }
                try {
                    if (withEditDisplayers) {
                        xObject.setEditProperty(propname, baseObject.displayEdit(propname, prefix, context));
                        xObject.setEditPropertyFieldName(propname, prefix + propname);
                    }
                } catch (Exception e) {
                }
            }
        }
        return xObject;
    }

    protected void mergeObject(XObject xobject, BaseObject baseObject, XWikiContext context)
    {
        BaseClass bclass = baseObject.getXClass(context);
        Object[] propnames = bclass.getPropertyNames();
        for (int i = 0; i < propnames.length; i++) {
            String propname = (String) propnames[i];
            Object propdata = xobject.getProperty(propname);
            baseObject.set(propname, propdata, context);
        }
    }

    public String getDocumentContent(String fullName) throws XWikiGWTException
    {
        return getDocumentContent(fullName, false, null);
    }

    protected BaseObject newBaseObject(BaseObject baseObject, XObject xObject, XWikiContext context)
        throws XWikiException
    {
        if (baseObject == null) {
            baseObject = (BaseObject) context.getWiki().getClass(xObject.getClassName(), context).newObject(context);
            baseObject.setName(xObject.getName());
            baseObject.setNumber(xObject.getNumber());
        }
        Object[] propnames = xObject.getPropertyNames().toArray();
        for (int i = 0; i < propnames.length; i++) {
            String propname = (String) propnames[i];
            try {
                // TODO will not work for a date
                baseObject.set(propname, xObject.get(propname), context);
            } catch (Exception e) {
            }
        }
        return baseObject;
    }

    public String getDocumentContent(String fullName, boolean rendered) throws XWikiGWTException
    {
        return getDocumentContent(fullName, rendered, null);
    }

    public String getDocumentContent(String fullName, boolean rendered, Map params) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                context.setDoc(doc);
                if (!rendered)
                    return doc.getContent();
                else {
                    XWikiRequestWrapper srw = new XWikiRequestWrapper(context.getRequest());
                    srw.setParameterMap(params);
                    context.setRequest(srw);
                    return doc.getRenderedContent(context);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    // get version history of a document
    public List getDocumentVersions(String fullName, int nb, int start) throws XWikiGWTException
    {
        try {
            List versionsList = new ArrayList();
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                String[] versions =
                    (nb == 0) ? doc.getRecentRevisions(0, context) : doc.getRecentRevisions(nb + start, context);
                int nbVersions = (nb == 0) ? (versions.length - start) : nb;
                for (int i = 0; i < nbVersions; i++) {
                    int j = i + start;
                    if (j < versions.length) {
                        String version = versions[j];
                        XWikiDocument vdoc = null;
                        try {
                            vdoc = context.getWiki().getDocument(doc, version, context);
                        } catch (Exception e) {
                        }

                        if (vdoc != null) {
                            String authorLink = context.getWiki().getURL(vdoc.getAuthor(), "view", context);
                            String author = context.getWiki().getLocalUserName(vdoc.getAuthor(), null, false, context);
                            versionsList.add(new VersionInfo(version, vdoc.getDate().getTime(), author, authorLink,
                                vdoc.getComment()));

                        } else {
                            versionsList.add(new VersionInfo(version, 0, "?", "?", "?"));
                        }
                    } else {
                        break;
                    }
                }
                return versionsList;
            } else {
                return versionsList;
            }
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public void logJSError(Map infos)
    {
        LOGGER.warn("[GWT-JS] useragent:" + infos.get("useragent") + "\n" + "module:" + infos.get("module") + "\n");
        // + "stacktrace" + infos.get("stacktrace"));
    }

    public Dictionary getTranslation(String translationPage, String locale) throws XWikiGWTException
    {
        try {
            XWikiContext context = getXWikiContext();
            XWikiMessageTool msg = context.getMessageTool();
            String defaultLanguage = context.getWiki().getDefaultLanguage(context);

            // Get the translated version of the translation page document with the default language one
            List docBundles = msg.getDocumentBundles(translationPage, defaultLanguage);
            Properties properties = new Properties();

            // loop backwards to have the default language updated first and then overwritten with the current language
            for (int i = 0; i < docBundles.size(); i++) {
                Properties encproperties =
                    (msg == null) ? null : msg.getDocumentBundleProperties((XWikiDocument) docBundles.get(docBundles
                        .size() - i - 1));
                if (encproperties != null) {
                    properties.putAll(encproperties);
                }
            }

            if (properties == null)
                return new Dictionary();
            else
                return new Dictionary(properties);
        } catch (Exception e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean hasAccessLevel(String level, String docName) throws XWikiGWTException
    {
        XWikiContext context = getXWikiContext();
        try {
            return Boolean.valueOf(context.getWiki().getRightService()
                .hasAccessLevel(level, context.getUser(), docName, context));
        } catch (XWikiException e) {
            throw getXWikiGWTException(e);
        }
    }

    public Boolean hasAccessLevel(String level, String username, String docName) throws XWikiGWTException
    {
        try {
            return Boolean.valueOf(getXWikiContext().getWiki().getRightService()
                .hasAccessLevel(level, username, docName, getXWikiContext()));
        } catch (XWikiException e) {
            throw getXWikiGWTException(e);
        }
    }
}
