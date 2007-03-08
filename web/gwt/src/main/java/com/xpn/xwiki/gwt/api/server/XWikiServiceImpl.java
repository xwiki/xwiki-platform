package com.xpn.xwiki.gwt.api.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.xpn.xwiki.web.*;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCResponse;
import com.xpn.xwiki.xmlrpc.XWikiXMLRPCContext;
import com.xpn.xwiki.xmlrpc.MockXWikiServletContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.gwt.api.client.XWikiService;
import com.xpn.xwiki.gwt.api.client.Document;
import com.xpn.xwiki.gwt.api.client.User;
import com.xpn.xwiki.gwt.api.client.XObject;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 nov. 2006
 * Time: 19:40:30
 * To change this template use File | Settings | File Templates.
 */
public class XWikiServiceImpl extends RemoteServiceServlet implements XWikiService {

    protected XWikiContext getXWikiContext() throws XWikiException {
        XWikiRequest  request = new XWikiServletRequest(getThreadLocalRequest()); // use fake request
        XWikiResponse response = new XWikiXMLRPCResponse(getThreadLocalResponse()); // use fake response
        XWikiEngineContext engine;

        ServletContext sContext = null;
        try {
            sContext = getServletConfig().getServletContext();
        } catch (Exception ignore) { }
        if (sContext != null) {
            engine = new XWikiXMLRPCContext(sContext);
        } else {
            engine = new XWikiXMLRPCContext(new MockXWikiServletContext());
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
        return context;
    }


    public Document getDocument(String fullName) {
        return getDocument(fullName, false, false, false);
    }

    public Document getDocument(String fullName, boolean full) {
        return getDocument(fullName, full, false, false);
    }

    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);

                return newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, context);
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
        return getDocuments(sql, nb, start, false, false, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full) {
        return getDocuments(sql, nb, start, full, false, false);
    }

    public List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers) {
        try {
            XWikiContext context = getXWikiContext();
            return getDocuments(sql, nb, start, full, viewDisplayers, editDisplayers, context);
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

    public List getDocuments(String sql, int nb, int start, boolean full, boolean viewDisplayers, boolean editDisplayers, XWikiContext context) {
          List newlist = new ArrayList();
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(sql, nb, start, context);
            if ((list==null)&&(list.size()==0))
                return newlist;
            for (int i=0;i<list.size();i++) {
                if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), (String) list.get(i), context)==true) {
                    XWikiDocument doc = context.getWiki().getDocument((String) list.get(i), context);
                    Document apidoc = newDocument(new Document(), doc, full, viewDisplayers, editDisplayers, context);
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
                List list = context.getWiki().getStore().search(queryDoc.getRenderedContent(context), nb, start, context);
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
        return newDocument(doc, xdoc, withObjects, false, false, context);
    }

    private Document newDocument(Document doc, XWikiDocument xdoc, boolean withObjects, boolean withViewDisplayers,
                                 boolean withEditDisplayers, XWikiContext context) {
        doc.setId(xdoc.getId());
        doc.setTitle(xdoc.getTitle());
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
        return doc;
    }

    private XObject newObject(XObject xObject, BaseObject baseObject, boolean withViewDisplayers,
                              boolean withEditDisplayers,XWikiContext context) {
        xObject.setName(baseObject.getName());
        xObject.setNumber(baseObject.getNumber());
        xObject.setClassName(baseObject.getClassName());
        Object[] propnames = baseObject.getPropertyNames();
        for (int i=0;i<propnames.length;i++) {
            String propname = (String) propnames[i];
            // TODO: this needs to be a param
            if (!propname.equals("fullcontent")) {
            try {
                BaseProperty prop = (BaseProperty) baseObject.get(propname);
                if (prop!=null) {
                    Object value = prop.getValue();
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
                    xObject.setViewProperty(propname, baseObject.displayView(propname, "", context));
            } catch (Exception e) {}
            try {
                if (withEditDisplayers)
                    xObject.setEditProperty(propname, baseObject.displayEdit(propname, "", context));
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

    public String getDocumentContent(String fullName) {
        return getDocumentContent(fullName, false, null);
    }

    protected BaseObject newBaseObject(BaseObject baseObject, XObject xObject, XWikiContext context) {
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

    public String getDocumentContent(String fullName, boolean rendered) {
        return getDocumentContent(fullName, rendered, null);
    }

    public String getDocumentContent(String fullName, boolean rendered, Map params) {
        try {
            XWikiContext context = getXWikiContext();
            if (context.getWiki().getRightService().hasAccessLevel("view", context.getUser(), fullName, context)==true) {
                XWikiDocument doc = context.getWiki().getDocument(fullName, context);
                context.setDoc(doc);
                if (rendered==false)
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
            e.printStackTrace();  
            return null;
        }
    }


}
