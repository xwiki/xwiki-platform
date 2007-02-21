/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 *
 * @author jeremi
 * @author ldubost
 *
 */
package com.xpn.xwiki.gwt.api.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.xpn.xwiki.web.*;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.gwt.api.client.*;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCResponse;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCContext;
import com.xpn.xwiki.xmlrpc.MockXWikiServletContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

import javax.servlet.ServletContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Date;


public class XWikiServiceImpl extends RemoteServiceServlet implements XWikiService {

    protected XWikiContext getXWikiContext() throws XWikiException {
        XWikiRequest  request = new XWikiServletRequest(getThreadLocalRequest()); // use fake request
        XWikiResponse response = new XWikiXMLRPCResponse(getThreadLocalResponse()); // use fake response
        XWikiEngineContext engine;

        ServletContext sContext = null;
        try {
            sContext = getServletContext();
        } catch (Exception ignore) { }
        if (sContext != null) {
            engine = new XWikiServletContext(sContext);
        } else {
            engine = new XWikiServletContext(new MockXWikiServletContext());
        }

        XWikiContext context = Utils.prepareContext("", request, response, engine);
        context.setMode(XWikiContext.MODE_GWT);
        context.setDatabase("xwiki");

        XWiki xwiki = XWiki.getXWiki(context);
        XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
        context.setURLFactory(urlf);
        XWikiVelocityRenderer.prepareContext(context);

        String username = "XWiki.XWikiGuest";
        XWikiUser user = context.getWiki().checkAuth(context);
        if (user != null)
            username = user.getUser();
        context.setUser(username);        

        if (context.getDoc() == null)
            context.setDoc(new XWikiDocument("Fake", "Document"));
        return context;
    }


    public Document getDocument(String fullName) {
        return getDocument(fullName, false, false, false, false);
    }


// TODO check conflicts problems
    public Document getDocument(String fullName, boolean full, boolean withRenderedContent) {
        return getDocument(fullName, full, false, false, withRenderedContent);
    }

    public String getUniquePageName(String space){
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getUniquePageName(space, context);
        } catch (XWikiException e) {
            return null;
        }
    }

    public String getUniquePageName(String space, String pageName){
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getUniquePageName(space, pageName, context);
        } catch (XWikiException e) {
            return null;
        }
    }

    public Document getUniqueDocument(String space, String pageName) {
        try {
            XWikiContext context = getXWikiContext();
            String fullName = context.getWiki().getUniquePageName(space, pageName, context);
            return getDocument(fullName);
        } catch (XWikiException e) {
            return null;
        }
    }

    public Document getUniqueDocument(String space) {
        try {
            XWikiContext context = getXWikiContext();
            String fullName = context.getWiki().getUniquePageName(space, context);
            return getDocument(space + "." + fullName);
        } catch (XWikiException e) {
            return null;
        }
    }

    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers) {
		return getDocument(fullName, full, viewDisplayers, editDisplayers, false);
	}

    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers, boolean withRenderedContent) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context) == true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);

                return newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, withRenderedContent, context);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    public User getUser(String fullName) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                return newUser(new User(), doc, context);
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }

    public User[] getUserList(int nb, int start) {
        User[] users = new User[nb];
        try {
            XWikiContext context = getXWikiContext();
            List list = searchDocuments(",BaseObject as obj where doc.fullName=obj.name and obj.className='XWiki.XWikiUsers'", nb, start, context);
            if (list==null)
                return new User[0];

            for (int i=0;i<list.size();i++) {
                String username = (String) list.get(i);
                XWikiDocument userdoc = context.getWiki().getDocument(username, context);
                users[i] = newUser(new User(), userdoc, context);
            }

            return users;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return new User[0];
        }
    }

    public List searchDocuments(String sql, int nb, int start) {
        try {
            XWikiContext context = getXWikiContext();
            return searchDocuments(sql, nb, start, context);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List getDocuments(String sql, int nb, int start) {
        return getDocuments(sql, nb, start, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full) {
        return getDocuments(sql, nb, start, full, false, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers) {
        try {
            XWikiContext context = getXWikiContext();
            return getDocuments(sql, nb, start, full, viewDisplayers, editDisplayers, false, context);
        } catch (Exception e) {
           e.printStackTrace();
            return null;
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, String value) {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                doc.setStringValue(className, propertyname, value);
                context.getWiki().saveDocument(doc, context);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, int value) {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                doc.setIntValue(className, propertyname, value);
                context.getWiki().saveDocument(doc, context);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateProperty(String docname, String className, String propertyname, List value) {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                doc.setListValue(className, propertyname, value);
                context.getWiki().saveDocument(doc, context);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers, XWikiContext context) {
		return getDocuments(sql, nb, start, full, viewDisplayers, editDisplayers, false, context);	
	}

    private List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers, boolean withRenderedContent, XWikiContext context) {
          List newlist = new ArrayList();
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(sql, nb, start, context);
            if ((list==null)&&(list.size()==0))
                return newlist;
            for (int i=0;i<list.size();i++) {
                if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), (String) list.get(i), context)==true) {
                    XWikiDocument doc = context.getWiki().getDocument((String) list.get(i), context);
                    Document apidoc = newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, withRenderedContent, context);
                    newlist.add(apidoc);
                }
            }

            return newlist;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private List searchDocuments(String sql, int nb, int start, XWikiContext context) {
        List newlist = new ArrayList();
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(sql, nb, start, context);
            if ((list==null)&&(list.size()==0))
                return newlist;

            for (int i=0;i<list.size();i++) {
                if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), (String) list.get(i), context)==true)
                 newlist.add(list.get(i));
            }

            return newlist;
        } catch (Exception e) {
            e.printStackTrace();  
            return null;
        }
    }

    public List getObjects(String sql, String className, int nb, int start){
        List docs = getDocuments(sql, nb, start, true);
        List objects = new ArrayList();
        Iterator it = docs.iterator();
        while(it.hasNext()){
            Document doc = (Document) it.next();
            List docObjs = doc.getObjects(className);
            if (docObjs != null)
                objects.addAll(docObjs);
        }
        return objects;
    }

    public XObject getFirstObject(String sql, String className){
        List objs = getObjects(sql, className, 1, 0);
        if (objs != null && objs.size() > 0)
            return (XObject) objs.get(0);
        return null;
    }

    public XObject addObject(XWikiDocument doc, String className){
        try {
            XWikiContext context = getXWikiContext();
            int index = doc.createNewObject(className, context);
            return newObject(new XObject(), doc.getObject(className, index), false, false, context);
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return null;

    }

    public XObject addObject(String fullName, String className) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), fullName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                XWikiDocument oldDoc = (XWikiDocument) doc.clone();

                XObject obj = addObject(doc, className);

                context.getWiki().saveDocument(doc, oldDoc, context);

                return obj;
            }
            return null;
        } catch (XWikiException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public List addObject(String fullName, List classesName){
        try{
            XWikiContext context = getXWikiContext();
            XWikiDocument doc = context.getWiki().getDocument(fullName, context);
            XWikiDocument oldDoc = (XWikiDocument) doc.clone();
            Iterator it = classesName.iterator();
            List objs = new ArrayList();
            while(it.hasNext()){
                objs.add(addObject(doc, (String) it.next()));
            }
            context.getWiki().saveDocument(doc, oldDoc, context);
            return objs;
        } catch (XWikiException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }


    public boolean addObject(String docname, XObject xobject) {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docname, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseObject newObject = doc.newObject(xobject.getClassName(), context);

                mergeObject(xobject, newObject, context);
                context.getWiki().saveDocument(doc, context);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * save only the content of a document
     * TODO manage translations
     * @param fullName
     * @param content
     * @return
     */
    public Boolean saveDocumentContent(String fullName, String content) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), fullName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                XWikiDocument oldDoc = (XWikiDocument) doc.clone();
                doc.setContent(content);
                doc.setAuthor(context.getUser());
                if (doc.isNew())
				    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc, oldDoc, context);
                return Boolean.valueOf(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(false);
    }


    public Boolean saveObject(XObject object) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), object.getName(), context)) {
                XWikiDocument doc = context.getWiki().getDocument(object.getName(), context);
                XWikiDocument oldDoc = (XWikiDocument) doc.clone();
                BaseObject bObject = newBaseObject(doc.getObject(object.getClassName(), object.getNumber()),object, context);
                doc.setObject(object.getClassName(), object.getNumber(), bObject);
                doc.setAuthor(context.getUser());
                if (doc.isNew())
				    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc, oldDoc, context);
                return Boolean.valueOf(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(false);
    }

    public Boolean deleteObject(XObject object) {
        return deleteObject(object.getName(), object.getClassName(), object.getNumber());   
    }

    public Boolean deleteObject(String docName, String className, int number) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), docName, context)) {
                XWikiDocument doc = context.getWiki().getDocument(docName, context);
                XWikiDocument oldDoc = (XWikiDocument) doc.clone();

                BaseObject bObj = doc.getObject(className, number);

                if (!doc.removeObject(bObj))
                    return Boolean.valueOf(false);

                doc.setAuthor(context.getUser());
                if (doc.isNew())
				    doc.setCreator(context.getUser());
                context.getWiki().saveDocument(doc, oldDoc, context);
                return Boolean.valueOf(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Boolean.valueOf(false);
    }


    public Boolean saveObjects(List objects) {
        Iterator it = objects.iterator();
        boolean error = false;
        while(it.hasNext()){
            error |= !saveObject((XObject) it.next()).booleanValue();
        }
        return Boolean.valueOf(!error);
    }

    /**
     * return true if can be locked
     * return null in case of an error
     * return false in all the other cases

     * @param fullName
     * @param force
     * @return
     */
    public Boolean lockDocument(String fullName, boolean force) {
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
            }
            else
                return Boolean.valueOf(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        return null;
    }

    public void unlockDocument(String fullName) {
        try {
            XWikiContext context = getXWikiContext();

            XWikiDocument doc = context.getWiki().getDocument(fullName, context);
            XWikiLock lock = doc.getLock(context);
            if (lock != null)
                doc.removeLock(context);
        } catch (XWikiException e) {
            e.printStackTrace();
        }
    }

    public Boolean isLastDocumentVersion(String fullName, String version) {
        try {
            XWikiContext context = getXWikiContext();
            return Boolean.valueOf(context.getWiki().getDocument(fullName, context).getVersion().equals(version));
        } catch (XWikiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getLoginURL() {
        try {
            XWikiContext context = getXWikiContext();
            return context.getWiki().getDocument("XWiki.XWikiLogin", context).getExternalURL("login", context);
        } catch (XWikiException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addComment(String docname, String message) {
        XWikiContext context = null;
        try {
            context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("comment", context.getUser(), docname, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(docname, context);
                BaseObject newObject = doc.newObject("XWiki.XWikiComments", context);
                newObject.set("author", context.getUser(), context);
                newObject.set("date", new Date(), context);
                newObject.set("comment", message, context);
                context.getWiki().saveDocument(doc, context);
                return true;
            } else
                return false;
        } catch (XWikiException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List customQuery(String queryPage) {
        return customQuery(queryPage, 0, 0);
    }

    public List customQuery(String queryPage, int nb, int start) {
        List newlist = new ArrayList();
        try {
            XWikiContext context = getXWikiContext();
            XWikiDocument queryDoc = context.getWiki().getDocument(queryPage, context);
            if (context.getWiki().getRightService().hasProgrammingRights(queryDoc, context)) {
                List list = context.getWiki().getStore().search(queryDoc.getContent(), nb, start, context);
                for (int i=0;i<list.size();i++) {
                    Object[] item = (Object[]) list.get(i);
                    List itemlist = new ArrayList();
                    for (int j=0;j<item.length;j++) {
                        itemlist.add(item[j]);
                    }
                    newlist.add(itemlist);
                }
                return newlist;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private User newUser(User user, XWikiDocument xdoc, XWikiContext context) {
        newDocument(user, xdoc, context);
        user.setFirstName(xdoc.getStringValue("XWiki.XWikiUsers", "first_name"));
        user.setLastName(xdoc.getStringValue("XWiki.XWikiUsers", "last_name"));
        user.setEmail(xdoc.getStringValue("XWiki.XWikiUsers", "email"));
        return user;
    }

    private Document newDocument(Document doc, XWikiDocument xdoc, XWikiContext context) {
        return newDocument(doc, xdoc, false, context);
    }

    private Document newDocument(Document doc, XWikiDocument xdoc, boolean withObjects, XWikiContext context) {
        return newDocument(doc, xdoc, withObjects, false, false, false, context);
    }

    private Document newDocument(Document doc, XWikiDocument xdoc, boolean withObjects, boolean withViewDisplayers,
                                 boolean withEditDisplayers, boolean withRenderedContent, XWikiContext context) {
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
        doc.setUploadURL(xdoc.getExternalURL("upload", "ajax=1", context));
        try {
            doc.setSaveURL(context.getWiki().getExternalURL(xdoc.getFullName(), "save", "ajax=1", context));
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        doc.setHasElement(xdoc.getElements());
        try {
            doc.setEditRight(context.getWiki().getRightService().hasAccessLevel("edit", context.getUser(), xdoc.getFullName(), context));
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        if (withObjects) {
            Iterator it = xdoc.getxWikiObjects().values().iterator();
            while (it.hasNext()) {
                List list = (List) it.next();

                for(int i=0;i< list.size();i++) {
                    BaseObject bobj = (BaseObject) list.get(i);
                    if (bobj!=null) {
                        XObject obj = newObject(new XObject(),bobj, withViewDisplayers, withEditDisplayers, context);
                        doc.addObject(bobj.getClassName(), obj);
                    }
                }
            }
        }
        if(xdoc.getAttachmentList() != null && xdoc.getAttachmentList().size() > 0){
            Iterator it = xdoc.getAttachmentList().iterator();
            while (it.hasNext()) {
                XWikiAttachment xAtt = (XWikiAttachment) it.next();
                Attachment att = newAttachment(new Attachment(), xAtt, context);
                doc.addAttachments(att);
            }
        }
        if (withRenderedContent){
            try {
                doc.setRenderedContent(xdoc.getRenderedContent(context));
            } catch (XWikiException e) {
                e.printStackTrace();
            }
        }
        return doc;
    }

    private Attachment newAttachment(Attachment att, XWikiAttachment xAtt, XWikiContext context){
        att.setAttDate(xAtt.getDate().getTime());
        att.setAuthor(xAtt.getAuthor());
        att.setFilename(xAtt.getFilename());
        att.setId(xAtt.getId());
        att.setImage(xAtt.isImage(context));
        att.setMimeType(xAtt.getMimeType(context));
        att.setFilesize(xAtt.getFilesize());
        att.setDownloadUrl(context.getWiki().getExternalAttachmentURL(xAtt.getDoc().getFullName(), xAtt.getFilename(), context));

        return att;
    }

    private XObject newObject(XObject xObject, BaseObject baseObject, boolean withViewDisplayers,
                              boolean withEditDisplayers,XWikiContext context) {
        xObject.setName(baseObject.getName());
        xObject.setNumber(baseObject.getNumber());
        xObject.setClassName(baseObject.getClassName());
        String prefix = baseObject.getxWikiClass(context).getName() + "_" + baseObject.getNumber() + "_";

        Object[] propnames = baseObject.getxWikiClass(context).getFieldList().toArray();
        for (int i=0;i<propnames.length;i++) {
            String propname = ((PropertyInterface)propnames[i]).getName();
            // TODO: this needs to be a param
            if (!propname.equals("fullcontent")) {
            try {
                BaseProperty prop = (BaseProperty) baseObject.get(propname);
                if (prop!=null) {
                    Object value = prop.getValue();
                    //TODO We should better put it in a standart format to be sure to be able to modify it
                    if (value instanceof Date)
                     xObject.set(propname, baseObject.displayView(propname, "", context));                    
                    else if (value instanceof List) {
                        List newlist = new ArrayList();
                        for (int j=0;j<((List)value).size();j++) {
                            newlist.add(((List)value).get(j));                            
                        }
                        xObject.set(propname, newlist);
                    }
                    else
                     xObject.set(propname, prop.getValue());
                }
            } catch (Exception e) {}
            try {
                if (withViewDisplayers)
                    xObject.setViewProperty(propname, baseObject.displayView(propname, prefix, context));
            } catch (Exception e) {}
            try {
                if (withEditDisplayers) {
                    xObject.setEditProperty(propname, baseObject.displayEdit(propname, prefix, context));
                    xObject.setEditPropertyFieldName(propname, prefix + propname);
                }
            } catch (Exception e) {}
            }
        }
        return xObject;
    }

    private void mergeObject(XObject xobject, BaseObject baseObject, XWikiContext context) {
        BaseClass bclass = baseObject.getxWikiClass(context);
        Object[] propnames = bclass.getPropertyNames();
        for (int i=0;i<propnames.length;i++) {
            String propname = (String) propnames[i];
            Object propdata = xobject.getProperty(propname);
            baseObject.set(propname, propdata, context);
        }
    }

    private BaseObject newBaseObject(BaseObject baseObject, XObject xObject, XWikiContext context) {
        Object[] propnames = xObject.getPropertyNames().toArray();
        for (int i = 0; i < propnames.length; i++) {
            String propname = (String) propnames[i];
            try {
                //TODO will not work for a date
                baseObject.set(propname, xObject.get(propname), context);
            } catch (Exception e) {
            }
        }
        return baseObject;
    }

}