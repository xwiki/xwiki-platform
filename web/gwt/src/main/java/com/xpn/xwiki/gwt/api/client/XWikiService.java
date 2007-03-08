package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.core.client.GWT;
import java.util.List;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 nov. 2006
 * Time: 19:40:30
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiService extends RemoteService {

    public Document getDocument(String fullName);
    public Document getDocument(String fullName, boolean full);
    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers);
    public User getUser(String fullName);
    public User[] getUserList(int nb, int start);
    public List searchDocuments(String sql, int nb, int start);
    public List getDocuments(String sql, int nb, int start);
    public List getDocuments(String sql, int nb, int start, boolean fullName);
    public List getDocuments(String sql, int nb, int start, boolean fullName, boolean viewDisplayers, boolean editDisplayers);
    public boolean updateProperty(String doc, String className, String propertyname, String value);
    public boolean updateProperty(String doc, String className, String propertyname, int value);
    public boolean updateProperty(String docname, String className, String propertyname, List value);
    public boolean addComment(String docname, String message);
    public boolean addObject(String docname, XObject xobject);
    public List customQuery(String queryPage);
    public List customQuery(String queryPage, int nb, int start);
    public String getDocumentContent(String fullName);
    public String getDocumentContent(String fullName, boolean rendered);
    public String getDocumentContent(String fullName, boolean rendered, Map params);


    /**
     * Utility/Convinience class.
     * Use XWikiService.App.getInstance() to access static instance of XWikiAsync
     */
    public static class App {
        private static XWikiServiceAsync ourInstance = null;

        public static synchronized XWikiServiceAsync getInstance() {
            if (ourInstance == null) {
                ourInstance = (XWikiServiceAsync) GWT.create(XWikiService.class);
                ((ServiceDefTarget) ourInstance).setServiceEntryPoint(GWT.getModuleBaseURL() + "/XWikiService");
            }
            return ourInstance;
        }
    }
}
