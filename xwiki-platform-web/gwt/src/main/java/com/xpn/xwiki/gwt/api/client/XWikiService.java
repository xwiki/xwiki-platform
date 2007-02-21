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


/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 19 nov. 2006
 * Time: 19:40:30
 * To change this template use File | Settings | File Templates.
 */
public interface XWikiService extends RemoteService {

    public Document getDocument(String fullName);
    public Document getDocument(String fullName, boolean withObject, boolean withRenderedContent);
    public Document getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers);

    public Document getDocument(String fullName, boolean withObject, boolean viewDisplayers, boolean editDisplayers, boolean withRenderedContent);

    public String getUniquePageName(String space);
    public String getUniquePageName(String space, String pageName);
    public Document getUniqueDocument(String space, String pageName);
    public Document getUniqueDocument(String space);

    public User getUser(String fullName);
    public User[] getUserList(int nb, int start);

    public boolean updateProperty(String docname, String className, String propertyname, String value);
    public boolean updateProperty(String docname, String className, String propertyname, int value);
    public boolean updateProperty(String docname, String className, String propertyname, List value);


    public List searchDocuments(String sql, int nb, int start);
    
    public List getDocuments(String sql, int nb, int start);
    public List getDocuments(String sql, int nb, int start, boolean fullName);
    public List getDocuments(String sql, int nb, int start, boolean fullName, boolean viewDisplayers, boolean editDisplayers);

    public List getObjects(String sql, String className, int nb, int start);
    public XObject getFirstObject(String sql, String className);

    public XObject addObject(String fullName, String className);
    public List addObject(String fullName, List classesName);
    public boolean addObject(String docname, XObject xobject);

    public Boolean lockDocument(String fullName, boolean force);
    public void unlockDocument(String fullName);
    public Boolean isLastDocumentVersion(String fullName, String version);

    public String getLoginURL();

    public Boolean saveDocumentContent(String fullName, String content);
    public Boolean saveObject(XObject object);
    public Boolean saveObjects(List objects);

    public Boolean deleteObject(XObject object);
    public Boolean deleteObject(String docName, String className, int number);

    public boolean addComment(String docname, String message);

    public List customQuery(String queryPage);
    public List customQuery(String queryPage, int nb, int start);

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
