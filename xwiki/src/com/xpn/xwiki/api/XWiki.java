/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 26 févr. 2004
 * Time: 17:50:47
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.objects.meta.MetaClass;

import java.util.Date;
import java.util.List;

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

     public String getRequestURL() {
         return com.xpn.xwiki.XWiki.getRequestURL(context.getRequest());
     }

     public String getRealPath(String path) {
          return xwiki.getRealPath(path);
     }

     public Document getDocument(String fullname) throws XWikiException {
         XWikiDocInterface doc = xwiki.getDocument(fullname, context);
         if (xwiki.checkAccess("view", doc, context)==false) {
                    // Finally we return null, otherwise showing search result is a real pain
                    return null;
                    /* throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "Access to this document is denied");
                    */
                }

         Document newdoc = new Document(doc, context);
         return newdoc;
     }

     public Document getDocument(String web, String fullname) throws XWikiException {
         XWikiDocInterface doc = xwiki.getDocument(web, fullname, context);
         if (xwiki.checkAccess("view", doc, context)==false) {
                     // Finally we return null, otherwise showing search result is a real pain
                    return null;
                    /* throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "Access to this document is denied");
                    */
                }

         Document newdoc = new Document(doc, context);
         return newdoc;
     }

     public String getBase() {
         return xwiki.getBase(context);
     }

     public String getFormEncoded(String content) {
        return xwiki.getFormEncoded(content);
     }

     public String getXMLEncoded(String content) {
        return xwiki.getXMLEncoded(content);
     }

     public String getTextArea(String content) {
        return xwiki.getTextArea(content);
     }


    public List getClassList() throws XWikiException {
        return xwiki.getClassList(context);
    }

    public MetaClass getMetaclass() {
        return xwiki.getMetaclass();
    }

    public List search(String wheresql) throws XWikiException {
        return xwiki.search(wheresql, context);
    }

    public List search(String wheresql, int nb, int start) throws XWikiException {
        return xwiki.search(wheresql, nb, start, context);
    }

    public List searchDocuments(String wheresql) throws XWikiException {
        return xwiki.searchDocuments(wheresql, context);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        return xwiki.searchDocuments(wheresql, nb, start, context);
    }

    public String parseContent(String content) {
        return xwiki.parseContent(content, context);
    }

    public String parseTemplate(String template) {
            return xwiki.parseTemplate(template, context);
        }

    public String getSkinFile(String filename) {
        return xwiki.getSkinFile(filename, context);
    }

    public String getSkin() {
        return xwiki.getSkin(context);
    }

    public String getWebCopyright() {
        return xwiki.getWebCopyright(context);
    }

    public String getXWikiPreference(String prefname) {
        return xwiki.getXWikiPreference(prefname, context);
    }

    public String getWebPreference(String prefname) {
        return xwiki.getWebPreference(prefname, context);
    }

    public String getUserPreference(String prefname) {
        return xwiki.getUserPreference(prefname, context);
    }

    public void flushCache() {
        xwiki.flushCache();
    }

    public int createUser() throws XWikiException {
        return createUser(false);
    }

    public int createUser(boolean withValidation) throws XWikiException {
        if (checkProgrammingRights())
         return xwiki.createUser(withValidation, context);
        else
         return -2;
        // TODO: We might need to send a notification email here.
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

    public String includeTopic(String topic) {
        return xwiki.includeTopic(topic, context);
    }

    public String includeForm(String topic) {
        return xwiki.includeForm(topic, context);
    }


    public boolean hasAccessLevel(String level) {
       try {
           return xwiki.getAccessManager(context).userHasAccessLevel(context.getUser(), context.getDoc().getFullName(), level);
       } catch (Exception e) {
           return false;
       }
    }

    public String renderText(String text, XWikiDocInterface doc) {
        return xwiki.getRenderingEngine().renderText(text, doc, context);
    }

    // Usefull date functions
    public Date getCurrentDate() {
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

    public String getBaseUrl(String fullname) {
        return xwiki.getBaseUrl(fullname, context);
    }

    public String getActionUrl(String fullname, String action) {
        return xwiki.getActionUrl(fullname, action, context);
    }
}
