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

    public Document getDocument(String fullName) throws XWikiGWTException;
    public Document getDocument(String fullName, boolean withObject, boolean withRenderedContent) throws XWikiGWTException;
    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers) throws XWikiGWTException;

    public Document getDocument(String fullName, boolean withObject, boolean viewDisplayers, boolean editDisplayers, boolean withRenderedContent) throws XWikiGWTException;

    public String getUniquePageName(String space) throws XWikiGWTException;
    public String getUniquePageName(String space, String pageName) throws XWikiGWTException;
    public Document getUniqueDocument(String space, String pageName) throws XWikiGWTException;
    public Document getUniqueDocument(String space) throws XWikiGWTException;

    public User getUser(String fullName) throws XWikiGWTException;
    public User getUser() throws XWikiGWTException;

    public User[] getUserList(int nb, int start) throws XWikiGWTException;

    public boolean updateProperty(String docname, String className, String propertyname, String value) throws XWikiGWTException;
    public boolean updateProperty(String docname, String className, String propertyname, int value) throws XWikiGWTException;
    public boolean updateProperty(String docname, String className, String propertyname, List value) throws XWikiGWTException;


    public List searchDocuments(String sql, int nb, int start) throws XWikiGWTException;
    
    public List getDocuments(String sql, int nb, int start) throws XWikiGWTException;
    public List getDocuments(String sql, int nb, int start, boolean fullName) throws XWikiGWTException;
    public List getDocuments(String sql, int nb, int start, boolean fullName, boolean viewDisplayers, boolean editDisplayers) throws XWikiGWTException;

    public List getObjects(String sql, String className, int nb, int start) throws XWikiGWTException;
    public XObject getFirstObject(String sql, String className) throws XWikiGWTException;

    public XObject addObject(String fullName, String className) throws XWikiGWTException;
    public List addObject(String fullName, List classesName) throws XWikiGWTException;
    public boolean addObject(String docname, XObject xobject) throws XWikiGWTException;

    public Boolean lockDocument(String fullName, boolean force) throws XWikiGWTException;
    public void unlockDocument(String fullName) throws XWikiGWTException;
    public Boolean isLastDocumentVersion(String fullName, String version) throws XWikiGWTException;

    public String login(String username, String password, boolean rememberme) throws XWikiGWTException;    
    public String getLoginURL() throws XWikiGWTException;

    public Boolean saveDocumentContent(String fullName, String content) throws XWikiGWTException;
    public Boolean saveObject(XObject object) throws XWikiGWTException;
    public Boolean saveObjects(List objects) throws XWikiGWTException;

    public Boolean deleteObject(XObject object) throws XWikiGWTException;
    public Boolean deleteObject(String docName, String className, int number) throws XWikiGWTException;

    public boolean addComment(String docname, String message) throws XWikiGWTException;

    public List customQuery(String queryPage) throws XWikiGWTException;
    public List customQuery(String queryPage, int nb, int start) throws XWikiGWTException;
    public String getDocumentContent(String fullName) throws XWikiGWTException;
    public String getDocumentContent(String fullName, boolean rendered) throws XWikiGWTException;
    public String getDocumentContent(String fullName, boolean rendered, Map params) throws XWikiGWTException;

    public void logJSError(Map infos) throws XWikiGWTException;

    public Dictionary getTranslation(String translationPage, String local) throws XWikiGWTException;


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
