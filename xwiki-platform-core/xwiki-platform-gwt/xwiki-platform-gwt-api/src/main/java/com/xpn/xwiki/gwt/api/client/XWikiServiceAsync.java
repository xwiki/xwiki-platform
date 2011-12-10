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
package com.xpn.xwiki.gwt.api.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.xpn.xwiki.gwt.api.client.app.XWikiAsyncCallback;

import java.util.List;
import java.util.Map;               

public interface XWikiServiceAsync {

    void getDocument(String fullName, AsyncCallback async);

    void getDocument(String fullName, boolean withObject, boolean withRenderedContent, AsyncCallback async);

    void getDocument(String fullName, boolean full, boolean viewDisplayers, boolean editDisplayers, AsyncCallback async);

    void getDocument(String fullName, boolean withObject, boolean viewDisplayers, boolean editDisplayers, boolean withRenderedContent, AsyncCallback async);

    void deleteDocument(String docName, AsyncCallback async);

    void deleteDocuments(String sql, AsyncCallback async);

    void getUniquePageName(String space, AsyncCallback async);

    void getUniquePageName(String space, String pageName, AsyncCallback async);

    void getUniqueDocument(String space, String pageName, AsyncCallback async);

    void getUniqueDocument(String space, AsyncCallback async);

    void getUser(String fullName, AsyncCallback async);

    void getUser(AsyncCallback async);

    void getUserList(int nb, int start, AsyncCallback async);

    void updateProperty(String docname, String className, String propertyname, String value, AsyncCallback async);

    void updateProperty(String docname, String className, String propertyname, int value, AsyncCallback async);

    void updateProperty(String docname, String className, String propertyname, List value, AsyncCallback async);

    void searchDocuments(String sql, int nb, int start, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, boolean fullName, AsyncCallback async);

    void getDocuments(String sql, int nb, int start, boolean fullName, boolean viewDisplayers, boolean editDisplayers, AsyncCallback async);

    void getObjects(String sql, String className, int nb, int start, AsyncCallback async);

    void getFirstObject(String sql, String className, AsyncCallback async);

    void addObject(String fullName, String className, AsyncCallback async);

    void addObject(String fullName, List classesName, AsyncCallback async);

    void addObject(String docname, XObject xobject, AsyncCallback async);

    void lockDocument(String fullName, boolean force, AsyncCallback async);

    void unlockDocument(String fullName, AsyncCallback async);

    void isLastDocumentVersion(String fullName, String version, AsyncCallback async);

    void login(String username, String password, boolean rememberme, AsyncCallback async);

    void getLoginURL(AsyncCallback async);

    void saveDocumentContent(String fullName, String content, AsyncCallback async);

    void saveObject(XObject object, AsyncCallback async);

    void saveObjects(List objects, AsyncCallback async);

    void deleteObject(XObject object, AsyncCallback async);

    void deleteObject(String docName, String className, int number, AsyncCallback async);

    void addComment(String docname, String message, AsyncCallback async);

    void customQuery(String queryPage, AsyncCallback async);

    void customQuery(String queryPage, int nb, int start, AsyncCallback async);

    void customQuery(String queryPage, Map params, AsyncCallback async);

    void customQuery(String queryPage, Map params, int nb, int start, AsyncCallback async);

    void getDocumentContent(String fullName, AsyncCallback async);

    void getDocumentContent(String fullName, boolean rendered, AsyncCallback async);

    void getDocumentContent(String fullName, boolean rendered, Map params, AsyncCallback async);

    void logJSError(Map infos, AsyncCallback async);

    void getTranslation(String translationPage, String local, AsyncCallback async);

    void getDocumentVersions(String pageName, int nb, int start, AsyncCallback async);
    
    void hasAccessLevel(String level, String docName, AsyncCallback async);
    
    void hasAccessLevel(String level, String username, String docName, AsyncCallback async);
}
