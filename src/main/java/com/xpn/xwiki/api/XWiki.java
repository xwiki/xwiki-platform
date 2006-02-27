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
 * @author ludovic
 * @author wr0ngway
 * @author erwan
 * @author jeremi
 * @author sdumitriu
 * @author thomas
 */


package com.xpn.xwiki.api;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import org.apache.commons.jrcs.diff.Chunk;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.web.Utils;

public class XWiki extends Api {
    private com.xpn.xwiki.XWiki xwiki;

    public XWiki(com.xpn.xwiki.XWiki xwiki, XWikiContext context) {
       super(context);
       this.xwiki = xwiki;
    }

    public com.xpn.xwiki.XWiki getXWiki() {
        if (checkProgrammingRights())
            return xwiki;
        else
            return null;
    }

     public String getVersion() {
          return xwiki.getVersion();
     }

     public String getRequestURL() throws XWikiException {
         return context.getURLFactory().getRequestURL(context).toString();
     }

     /**
      * Loads an Document from the database. Rights are checked before sending back the document.
      * @param fullname Fullname of the XWiki document to be loaded
      * @return a Document object or null if it is not accessible
      * @throws XWikiException
      */
     public Document getDocument(String fullname) throws XWikiException {
         XWikiDocument doc = xwiki.getDocument(fullname, context);
         if (xwiki.getRightService().hasAccessLevel("view", context.getUser(), doc.getFullName(), context)==false) {
                    return null;
                }

         Document newdoc = new Document(doc, context);
         return newdoc;
     }

    /**
     * Returns wether a document exists or not
     * @param fullname Fullname of the XWiki document to be loaded
     * @return true if the document exists, false if not
     * @throws XWikiException
     */
    public boolean exists(String fullname) throws XWikiException {
        return xwiki.exists(fullname, context);
    }

    /**
     * Verify the rights the current user has on a document
     * @param docname fullname of the document
     * @param right right to check ("view", "edit", "admin", "delete")
     * @return true if it exists
     */
    public boolean checkAccess(String docname, String right) {
        try {
            XWikiDocument doc =new XWikiDocument();
            doc.setFullName(docname, context);
            return context.getWiki().checkAccess(right, doc, context);
        } catch (XWikiException e) {
            return false;
        }
    }


    /**
     * Loads an Document from the database. Rights are checked before sending back the document.
     *
     * @param web  Space to use in case no space is defined in the fullname
     * @param fullname Fullname or relative name of the document to load
     * @return a Document object or null if it is not accessible
     * @throws XWikiException
     */
     public Document getDocument(String web, String fullname) throws XWikiException {
         XWikiDocument doc = xwiki.getDocument(web, fullname, context);
         if (xwiki.getRightService().hasAccessLevel("view", context.getUser(), doc.getFullName(), context)==false) {
                    return null;
                }

         Document newdoc = new Document(doc, context);
         return newdoc;
     }

    /**
     * Load a specific revision of a document
     * @param doc Document for which to load a specific revision
     * @param rev Revision number
     * @return  Specific revision of a document
     * @throws XWikiException
     */
    public Document getDocument(Document doc, String rev) throws XWikiException {
        if ((doc==null)||(doc.getDoc()==null))
            return null;

        if (xwiki.getRightService().hasAccessLevel("view", context.getUser(), doc.getFullName(), context)==false) {
                    // Finally we return null, otherwise showing search result is a real pain
                   return null;
               }

        try {
            XWikiDocument revdoc = xwiki.getDocument(doc.getDoc(), rev, context);
            Document newdoc = new Document(revdoc, context);
            return newdoc;
        } catch (Exception e) {
            // Can't read versioned document
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Transform a text in a form compatible text
     * @param content text to transform
     * @return  encoded result
     */
     public String getFormEncoded(String content) {
        return com.xpn.xwiki.XWiki.getFormEncoded(content);
     }

    /**
     * Transform a text in a URL compatible text
     * @param content text to transform
     * @return  encoded result
     */

    public String getURLEncoded(String content) {
       return com.xpn.xwiki.XWiki.getURLEncoded(content);
    }

    /**
     * Transform a text in a XML compatible text
     * @param content text to transform
     * @return  encoded result
     */
     public String getXMLEncoded(String content) {
        return com.xpn.xwiki.XWiki.getXMLEncoded(content);
     }

    /**
     * Output content in the edit content textarea
     * @param content content to output
     * @return the textarea text content
     */
     public String getTextArea(String content) {
        return com.xpn.xwiki.XWiki.getTextArea(content, context);
     }

    /**
     * Output content in the edit content htmlarea
     * @param content content to output
     * @return the htmlarea text content
     */
    public String getHTMLArea(String content) {
        return xwiki.getHTMLArea(content, context);
    }

    /**
     * Get the list of available classes in the wiki
     * @return list of classes names
     * @throws XWikiException
     */
    public List getClassList() throws XWikiException {
        return xwiki.getClassList(context);
    }

    /**
     * Get the global MetaClass object
     * @return MetaClass object
     */
    public MetaClass getMetaclass() {
        return xwiki.getMetaclass();
    }

    public List search(String wheresql) throws XWikiException {
        if (checkProgrammingRights())
         return xwiki.search(wheresql, context);
        else
         return null;
    }

    public List search(String wheresql, int nb, int start) throws XWikiException {
        if (checkProgrammingRights())
         return xwiki.search(wheresql, nb, start, context);
        else
         return null;
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return xwiki.getStore().searchDocumentsNames(wheresql, context);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        return xwiki.getStore().searchDocumentsNames(wheresql, nb, start, context);
    }

    public List searchDocuments(String wheresql, int nb, int start, String selectColumns) throws XWikiException {
        if (checkProgrammingRights())
         return xwiki.getStore().searchDocumentsNames(wheresql, nb, start, selectColumns, context);
        else
         return null;
    }

    public List searchDocuments(String wheresql, boolean distinctbylanguage) throws XWikiException {
        return wrapDocs(xwiki.getStore().searchDocuments(wheresql, context));
    }

    public List searchDocuments(String wheresql, boolean distinctbylanguage, int nb, int start) throws XWikiException {
        return wrapDocs(xwiki.getStore().searchDocuments(wheresql, nb, start, context));
    }

    private List wrapDocs(List docs)
    {
        List result = new ArrayList();
        if (docs != null)
        {
            for (Iterator iter = result.iterator(); iter.hasNext();)
            {
                XWikiDocument doc = (XWikiDocument) iter.next();
                Document wrappedDoc = new Document(doc, context);
                result.add(wrappedDoc);
            }
        }
        return result;
    }

    public String parseContent(String content) {
        return xwiki.parseContent(content, context);
    }

    public String parseTemplate(String template) {
            return xwiki.parseTemplate(template, context);
        }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity
     * templates; works by creating a RequestDispatcher, buffering the output,
     * then returning it as a string.
     *
     * @author LBlaze
     */
    public String invokeServletAndReturnAsString(String url) {
        return xwiki.invokeServletAndReturnAsString(url, context);
    }

    public String getSkinFile(String filename) {
        return xwiki.getSkinFile(filename, context);
    }

    public String getSkin() {
        return xwiki.getSkin(context);
    }

    public String getBaseSkin() {
        return xwiki.getBaseSkin(context);
    }

    public String getWebCopyright() {
        return xwiki.getWebCopyright(context);
    }

    public String getXWikiPreference(String prefname) {
        return xwiki.getXWikiPreference(prefname, context);
    }

    public String getXWikiPreference(String prefname, String default_value) {
        return xwiki.getXWikiPreference(prefname, default_value, context);
    }

    public String getWebPreference(String prefname) {
        return xwiki.getWebPreference(prefname, context);
    }

    public String getWebPreference(String prefname, String default_value) {
        return xwiki.getWebPreference(prefname, default_value, context);
    }

    public long getXWikiPreferenceAsLong(String prefname, long default_value) {
        return xwiki.getXWikiPreferenceAsLong(prefname, default_value, context);
    }

    public long getXWikiPreferenceAsLong(String prefname) {
        return xwiki.getXWikiPreferenceAsLong(prefname, context);
    }

    public long getWebPreferenceAsLong(String prefname, long default_value) {
        return xwiki.getWebPreferenceAsLong(prefname, default_value, context);
    }

    public long getWebPreferenceAsLong(String prefname) {
        return xwiki.getWebPreferenceAsLong(prefname, context);
    }

    public int getXWikiPreferenceAsInt(String prefname, int default_value) {
        return xwiki.getXWikiPreferenceAsInt(prefname, default_value, context);
    }

    public int getXWikiPreferenceAsInt(String prefname) {
        return xwiki.getXWikiPreferenceAsInt(prefname, context);
    }

    public int getWebPreferenceAsInt(String prefname, int default_value) {
        return xwiki.getWebPreferenceAsInt(prefname, default_value, context);
    }

    public int getWebPreferenceAsInt(String prefname) {
        return xwiki.getWebPreferenceAsInt(prefname, context);
    }

    public String getUserPreference(String prefname) {
        return xwiki.getUserPreference(prefname, context);
    }

    public String getUserPreferenceFromCookie(String prefname) {
        return xwiki.getUserPreferenceFromCookie(prefname, context);
    }

    public String getLanguagePreference() {
        return xwiki.getLanguagePreference(context);
    }

    public boolean isVirtual() {
        return xwiki.isVirtual();
    }

    public boolean isMultiLingual() {
        return xwiki.isMultiLingual(context);
    }

    public void flushCache() {
        if (hasProgrammingRights())
         xwiki.flushCache();
    }

    public void resetRenderingEngine() {
        if (hasProgrammingRights())
            try {
                xwiki.resetRenderingEngine(context);
            } catch (XWikiException e) {
            }
    }

    public int createUser() throws XWikiException {
        return createUser(false, "edit");
    }

    public int createUser(boolean withValidation) throws XWikiException {
        return createUser(withValidation, "edit");
    }

    public int createUser(boolean withValidation, String userRights) throws XWikiException {
        boolean registerRight;
        try {
            if (checkProgrammingRights()) {
                registerRight = true;
            } else {
                registerRight = xwiki.getRightService().hasAccessLevel("register", context.getUser(),
                        "XWiki.XWikiPreferences", context);
            }

            if (registerRight)
                return xwiki.createUser(withValidation, userRights, context);
            else
                return -1;

        } catch (Exception e) {
            e.printStackTrace();
            return -2;
        }

    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
                             String baseWikiName, boolean failOnExist) throws XWikiException {
                return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, "", null, failOnExist);
    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
                             String baseWikiName, String description, boolean failOnExist) throws XWikiException {
                return createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, null, failOnExist);
    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin,
                             String baseWikiName, String description, String language, boolean failOnExist) throws XWikiException {
        if (checkProgrammingRights())
            return xwiki.createNewWiki(wikiName, wikiUrl, wikiAdmin, baseWikiName, description, language, failOnExist, context);
        else return -1;
    }

    public int validateUser(boolean withConfirmEmail) throws XWikiException {
        return xwiki.validateUser(withConfirmEmail, context);
    }

    public void sendMessage(String sender, String recipient, String message) throws XWikiException {
        if (checkProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, context);
    }

    public void sendMessage(String sender, String[] recipient, String message) throws XWikiException {
        if (checkProgrammingRights())
            xwiki.sendMessage(sender, recipient, message, context);
    }


    public boolean copyDocument(String docname, String targetdocname) throws XWikiException {
        if (checkProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, null, null, null, false, context);
        else
            return false;
    }

    public boolean copyDocument(String docname, String targetdocname, String wikilanguage) throws XWikiException {
        if (checkProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, null, null, wikilanguage, false, context);
        else
            return false;
    }

    public boolean copyDocument(String docname, String sourceWiki, String targetWiki, String wikilanguage) throws XWikiException {
        if (checkProgrammingRights())
            return xwiki.copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage, true, context);
        else
            return false;
    }

    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki, String wikilanguage, boolean reset) throws XWikiException {
        if (checkProgrammingRights())
            return xwiki.copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage, reset, context);
        else
            return false;
    }

    public String includeTopic(String topic) throws XWikiException {
        return includeTopic(topic, true);
    }

    public String includeForm(String topic) throws XWikiException {
        return includeForm(topic, true);
    }

    public String includeTopic(String topic, boolean pre) throws XWikiException {
        if (pre)
            return "{pre}" + xwiki.include(topic, context, false) + "{/pre}";
        else
            return xwiki.include(topic, context, false);
    }

    public String includeForm(String topic, boolean pre) throws XWikiException {
        if (pre)
            return "{pre}" + xwiki.include(topic, context, true) + "{/pre}";
        else
            return xwiki.include(topic, context, true);
    }

    public boolean hasAccessLevel(String level) {
       try {
           return xwiki.getRightService().hasAccessLevel(level, context.getUser(), context.getDoc().getFullName(), context);
       } catch (Exception e) {
           return false;
       }
    }

    public boolean hasAccessLevel(String level, String user, String docname) {
       try {
           return xwiki.getRightService().hasAccessLevel(level, user, docname, context);
       } catch (Exception e) {
           return false;
       }
    }

    public String renderText(String text, Document doc) {
        return xwiki.getRenderingEngine().renderText(text, doc.getDoc(), context);
    }

    public String renderChunk(Chunk chunk, Document doc) {
        return renderChunk(chunk, false, doc);
    }

    public String renderChunk(Chunk chunk, boolean source, Document doc) {
        StringBuffer buf = new StringBuffer();
        chunk.toString(buf, "", "\n");
        if (source==true)
            return buf.toString();

        try {
            return xwiki.getRenderingEngine().renderText(buf.toString(), doc.getDoc(), context);
        } catch (Exception e) {
            return buf.toString();
        }
    }

    public List getSpaces() throws XWikiException {
        return xwiki.getSpaces(context);
    }

    public List getSpaceDocsName(String SpaceName) throws XWikiException {
        return xwiki.getSpaceDocsName(SpaceName, context);
    }

    // Usefull date functions
    public Date getCurrentDate() {
        return xwiki.getCurrentDate();
    }

    public Date getDate() {
        return xwiki.getCurrentDate();
    }

    public int getTimeDelta(long time) {
        return xwiki.getTimeDelta(time);
    }

    public Date getDate(long time) {
        return xwiki.getDate(time);
    }

    public String[] split(String str, String sep) {
        return xwiki.split(str, sep);
    }

    public String printStrackTrace(Throwable e) {
        return xwiki.printStrackTrace(e);
    }

    public String getEncoding() {
        return xwiki.getEncoding();
    }

    public Object getNull() {
        return null;
    }

    public String getNl() {
        return "\n";
    }

    public String getAttachmentURL(String fullname, String filename) throws XWikiException {
        return xwiki.getAttachmentURL(fullname, filename, context);
    }

    public String getURL(String fullname) throws XWikiException {
        return xwiki.getURL(fullname, "view", context);
    }

    public String getURL(String fullname, String action) throws XWikiException {
        return xwiki.getURL(fullname, action, context);
    }

    public String getURL(String fullname, String action, String querystring) throws XWikiException {
        return xwiki.getURL(fullname, action, querystring, context);
    }

    public java.lang.Object getService(String className) throws XWikiException {
        if (hasProgrammingRights())
         return xwiki.getService(className);
        else
         return null;
    }

    public java.lang.Object getPortalService(String className) throws XWikiException {
        if (hasProgrammingRights())
         return xwiki.getPortalService(className);
        else
         return null;
    }

    public List getArrayList() {
        return new ArrayList();
    }

    public Map getHashMap() {
        return new HashMap();
    }

    public List sort(List list) {
            Collections.sort(list);
            return list;
    }

    public void outputImage(BufferedImage image) throws IOException {
        JPEGImageEncoder encoder;
        OutputStream ostream = context.getResponse().getOutputStream();
        encoder = JPEGCodec.createJPEGEncoder(ostream);
        encoder.encode(image);
        ostream.flush();
    }

    public DocumentStats getCurrentMonthXWikiStats(String action) {
       return context.getWiki().getStatsService(context).getDocMonthStats("", "view", new Date(), context);
    }

    public String getRefererText(String referer) {
        try {
         return xwiki.getRefererText(referer, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String getShortRefererText(String referer, int length) {
        try {
         return xwiki.getRefererText(referer, context).substring(0, length);
        } catch (Exception e) {
            return xwiki.getRefererText(referer, context);
        }
    }

    public String getFullNameSQL() {
        return xwiki.getFullNameSQL();
    }

    public String getUserName(String user) {
        return xwiki.getUserName(user, null, context);
    }

    public String getUserName(String user, String format) {
        return xwiki.getUserName(user, format, context);
    }

    public String getLocalUserName(String user) {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), null, context);
        } catch (Exception e) {
            return xwiki.getUserName(user, null, context);
        }
    }

    public String getLocalUserName(String user, String format) {
        try {
            return xwiki.getUserName(user.substring(user.indexOf(":") + 1), format, context);
        } catch (Exception e) {
            return xwiki.getUserName(user, format, context);
        }
    }

    public String formatDate(Date date) {
        return xwiki.formatDate(date, null, context);
    }


    public String formatDate(Date date, String format) {
        return xwiki.formatDate(date, format, context);
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified.
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api get(String name) {
        return xwiki.getPluginApi(name, context);
    }

    /**
     * Returns a plugin from the plugin API. Plugin Rights can be verified.
     * @param name Name of the plugin to retrieve (either short of full class name)
     * @return a plugin object
     */
    public Api getPlugin(String name) {
        return xwiki.getPluginApi(name, context);
    }

    /**
     * Returns the recently visited pages for a specific action
     * @param action ("view" or "edit")
     * @param size how many recent actions to retrieve
     * @return a ArrayList of document names
     */
    public java.util.Collection getRecentActions(String action, int size) {
        XWikiStatsService stats = context.getWiki().getStatsService(context);
        if (stats==null)
            return new ArrayList();
        else
            return stats.getRecentActions(action, size, context);
    }

    /**
     * Returns the Advertisement system from the preferences
     * @return "google" or "none"
     */
    public String getAdType() {
        return xwiki.getAdType(context);
    }

    /**
     * Returns the Advertisement client ID from the preferences
     * @return an Ad affiliate ID
     */
    public String getAdClientId() {
        return xwiki.getAdClientId(context);
    }

    /**
     * Retrieves a int from a String
     * @param str String to convert to int
     * @return  the int or zero in case of exception
     */
    public int parseInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a int from a String
     * @param str String to convert to int
     * @return  the int or zero in case of exception
     */
    public Integer parseInteger(String str) {
        return new Integer(parseInt(str));
    }

    /**
     * Retrieves a long from a String
     * @param str String to convert to long
     * @return  the long or zero in case of exception
     */
    public long parseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a float from a String
     * @param str String to convert to float
     * @return  the float or zero in case of exception
     */
    public float parseFloat(String str) {
        try {
            return Float.parseFloat(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Retrieves a double from a String
     * @param str String to convert to double
     * @return  the double or zero in case of exception
     */
    public double parseDouble(String str) {
        try {
            return Double.parseDouble(str);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl, String username, String password) throws IOException {
        try {
            return xwiki.getURLContent(surl, username, password);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public String getURLContent(String surl) throws IOException {
        try {
            return xwiki.getURLContent(surl);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL protected using Basic Authentication as Bytes
     * @param surl url to retrieve
     * @param username username for the basic authentication
     * @param password password for the basic authentication
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl, String username, String password) throws IOException {
        try {
            return xwiki.getURLContentAsBytes(surl, username, password);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the content of an HTTP/HTTPS URL as Bytes
     * @param surl url to retrieve
     * @return Content of the specified URL
     * @throws IOException
     */
    public byte[] getURLContentAsBytes(String surl) throws IOException {
        try {
            return xwiki.getURLContentAsBytes(surl);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Filters text to be include in = or like clause in SQL
     * @param text text to filter
     * @return filtered text
     */
    public String sqlfilter(String text) {
        return Utils.SQLFilter(text);
    }

    /**
     * Returns the list of Macros documents in the specified content
     * @param defaultweb Default Web to use for relative path names
     * @param content Content to parse
     * @return ArrayList of document names
     */
    public List getIncludedMacros(String defaultweb, String content) {
        return xwiki.getIncludedMacros(defaultweb, content, context);
    }


    /**
     * returns true if xwiki.readonly is set in the configuration file
     *
     * @return the value of xwiki.isReadOnly()
     *
     * @see #com.xpn.xwiki.XWiki
     *
     */
    public boolean isReadOnly () {
        return xwiki.isReadOnly();
    }

    public void setReadOnly (boolean ro) {
        if (hasAdminRights()) {
            xwiki.setReadOnly(ro);
        }
    }

    public void refreshLinks() throws XWikiException{
        if (hasAdminRights()){
            xwiki.refreshLinks(context);
        }
    }

    public boolean hasBacklinks() throws XWikiException {
            return xwiki.hasBacklinks(context);
    }

    public void renamePage (XWikiDocument doc, String newFullName) throws XWikiException {
       if (xwiki.getRightService().hasAccessLevel("edit", context.getUser(), doc.getFullName(), context)){
            xwiki.renamePage(doc, context, newFullName) ;
        }
    }

    public String getEditorPreference() {
        return xwiki.getEditorPreference(context);
    }
}

