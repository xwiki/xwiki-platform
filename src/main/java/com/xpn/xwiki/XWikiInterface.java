/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 */
package com.xpn.xwiki;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.hibernate.HibernateException;
import org.securityfilter.filter.URLPatternMatcher;

import com.xpn.xwiki.api.User;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;

public interface XWikiInterface {
    void updateDatabase(String appname, XWikiContext context) throws HibernateException, XWikiException;

    List getVirtualWikiList();

    void initXWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate) throws XWikiException;

    String getVersion();

    URL getResource(String s) throws MalformedURLException;

    InputStream getResourceAsStream(String s) throws MalformedURLException;

    String getResourceContent(String name) throws IOException;

    boolean resourceExists(String name);

    XWikiConfig getConfig();

    String getRealPath(String path);

    String Param(String key);

    String ParamAsRealPath(String key);

    String ParamAsRealPath(String key, XWikiContext context);

    String ParamAsRealPathVerified(String param);

    String Param(String key, String default_value);

    long ParamAsLong(String key);

    long ParamAsLong(String key, long default_value);

    XWikiStoreInterface getStore();

    void saveDocument(XWikiDocument doc, XWikiContext context) throws XWikiException;

    void saveDocument(XWikiDocument doc, XWikiDocument olddoc, XWikiContext context) throws XWikiException;

    XWikiDocument getDocument(XWikiDocument doc, String revision, XWikiContext context) throws XWikiException;

    XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException;

    XWikiDocument getDocument(String web, String fullname, XWikiContext context) throws XWikiException;

    XWikiDocument getDocumentFromPath(String path, XWikiContext context) throws XWikiException;

    XWikiRenderingEngine getRenderingEngine();

    void setRenderingEngine(XWikiRenderingEngine renderingEngine);

    MetaClass getMetaclass();

    void setMetaclass(MetaClass metaclass);

    List getClassList(XWikiContext context) throws XWikiException;

    List search(String wheresql, XWikiContext context) throws XWikiException;

    List search(String wheresql, int nb, int start, XWikiContext context) throws XWikiException;

    boolean isTest();

    void setTest(boolean test);

    String parseContent(String content, XWikiContext context);

    String parseTemplate(String template, XWikiContext context);

    String getSkinFile(String filename, XWikiContext context);

    String getSkin(XWikiContext context);

    String getWebCopyright(XWikiContext context);

    String getXWikiPreference(String prefname, XWikiContext context);

    String getXWikiPreference(String prefname, String default_value, XWikiContext context);

    String getWebPreference(String prefname, XWikiContext context);

    String getWebPreference(String prefname, String default_value, XWikiContext context);

    String getUserPreference(String prefname, XWikiContext context);

    String getUserPreferenceFromCookie(String prefname, XWikiContext context);

    String getUserPreference(String prefname, boolean useCookie, XWikiContext context);

    String getLanguagePreference(XWikiContext context);

    long getXWikiPreferenceAsLong(String prefname, XWikiContext context);

    long getWebPreferenceAsLong(String prefname, XWikiContext context);

    long getXWikiPreferenceAsLong(String prefname, long default_value, XWikiContext context);

    long getWebPreferenceAsLong(String prefname, long default_value, XWikiContext context);

    int getXWikiPreferenceAsInt(String prefname, XWikiContext context);

    int getWebPreferenceAsInt(String prefname, XWikiContext context);

    int getXWikiPreferenceAsInt(String prefname, int default_value, XWikiContext context);

    int getWebPreferenceAsInt(String prefname, int default_value, XWikiContext context);

    void flushCache();

    XWikiPluginManager getPluginManager();

    void setPluginManager(XWikiPluginManager pluginManager);

    void setConfig(XWikiConfig config);

    void setStore(XWikiStoreInterface store);

    void setVersion(String version);

    XWikiNotificationManager getNotificationManager();

    void setNotificationManager(XWikiNotificationManager notificationManager);

    void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context);

    BaseClass getUserClass(XWikiContext context) throws XWikiException;

    BaseClass getGroupClass(XWikiContext context) throws XWikiException;

    BaseClass getRightsClass(String pagename,XWikiContext context) throws XWikiException;

    BaseClass getRightsClass(XWikiContext context) throws XWikiException;

    BaseClass getGlobalRightsClass(XWikiContext context) throws XWikiException;

    int createUser(XWikiContext context) throws XWikiException;

    int validateUser(boolean withConfirmEmail, XWikiContext context) throws XWikiException;

    int createUser(boolean withValidation, String userRights, XWikiContext context) throws XWikiException;

    void sendValidationEmail(String xwikiname, String password, String email, String validkey, String contentfield, XWikiContext context) throws XWikiException;

    void sendMessage(String sender, String[] recipient, String message, XWikiContext context) throws XWikiException;

    void sendMessage(String sender, String recipient, String message, XWikiContext context) throws XWikiException;

    String generateValidationKey(int size);

    int createUser(String xwikiname, Map map, String parent, String content, String userRights, XWikiContext context) throws XWikiException;

    User getUser(XWikiContext context);

    void prepareResources(XWikiContext context);

    XWikiUser checkAuth(XWikiContext context) throws XWikiException;

    boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
            throws XWikiException;

    String include(String topic, XWikiContext context, boolean isForm) throws XWikiException;

    void deleteDocument(XWikiDocument doc, XWikiContext context) throws XWikiException;

    String getDatabase();

    void setDatabase(String database);

    void gc();

    long freeMemory();

    long totalMemory();

    long maxMemory();

    String[] split(String str, String sep);

    String printStrackTrace(Throwable e);

    boolean copyDocument(String docname, String sourceWiki, String targetWiki, String language, XWikiContext context) throws XWikiException;

    int copyWikiWeb(String web, String sourceWiki, String targetWiki, String language, XWikiContext context) throws XWikiException;

    int copyWiki(String sourceWiki, String targetWiki, String language, XWikiContext context) throws XWikiException;

    int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
                      String baseWikiName, String description, String language, boolean failOnExist, XWikiContext context) throws XWikiException;

    String getEncoding();

    URL getServerURL(String database, XWikiContext context) throws MalformedURLException;

    String getURL(String fullname, String action, XWikiContext context) throws XWikiException;

    String getURL(String fullname, String action, String querystring, XWikiContext context) throws XWikiException;

    // Usefull date functions
    Date getCurrentDate();

    int getTimeDelta(long time);

    Date getDate(long time);

    boolean isMultiLingual(XWikiContext context);

    boolean isVirtual();

    boolean isExo();

    int checkActive(XWikiContext context) throws XWikiException;

    boolean prepareDocuments(XWikiRequest request, XWikiContext context, VelocityContext vcontext) throws XWikiException, IOException;

    XWikiEngineContext getEngineContext();

    void setEngineContext(XWikiEngineContext engine_context);

    URLPatternMatcher getUrlPatternMatcher();

    void setUrlPatternMatcher(URLPatternMatcher urlPatternMatcher);

    void setAuthService(XWikiAuthService authService);

    void setRightService(XWikiRightService rightService);

    XWikiGroupService getGroupService();

    void setGroupService(XWikiGroupService groupService);

    XWikiAuthService getAuthService();

    XWikiRightService getRightService();

    Object getService(String className) throws XWikiException;

    String getUserName(String user, XWikiContext context);

    String getUserName(String user, String format, XWikiContext context);

}
