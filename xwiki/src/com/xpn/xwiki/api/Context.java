package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.web.XWikiAction;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by IntelliJ IDEA.
 * User: ludovic
 * Date: 15 mars 2004
 * Time: 00:41:24
 * To change this template use File | Settings | File Templates.
 */
public class Context extends Api {

    public Context(XWikiContext context) {
        super(context);
    }

    public HttpServletRequest getRequest() {
       return context.getRequest();
    }

    public HttpServletResponse getResponse() {
       return context.getResponse();
    }

    public String getDatabase() {
        return context.getDatabase();
    }

    public void setDatabase(String database) {
        if (checkProgrammingRights())
          context.setDatabase(database);
    }

    public String getBaseUrl() {
        return context.getBaseUrl();
    }

    public boolean isVirtual() {
        return context.isVirtual();
    }

    public HttpServlet getServlet() {
        if (checkProgrammingRights())
         return context.getServlet();
        else
         return null;
    }

    public XWikiAction getAction() {
        if (checkProgrammingRights())
         return context.getAction();
        else
         return null;
    }

    public com.xpn.xwiki.XWiki getXWiki() {
        if (checkProgrammingRights())
         return context.getWiki();
        else
         return null;
    }

    public XWikiDocInterface getDoc() {
        if (checkProgrammingRights())
         return context.getDoc();
        else
         return null;
    }

    public void setDoc(XWikiDocInterface doc) {
        if (checkProgrammingRights())
          context.setDoc(doc);
    }

    public XWikiContext getContext() {
        if (checkProgrammingRights())
         return context;
        else
         return null;
    }

    public Object get(String key) {
        if (checkProgrammingRights())
            return context.get(key);
        else
            return null;
    }

    public void put(String key, Object value) {
        if (checkProgrammingRights())
            context.put(key, value);
    }
}
