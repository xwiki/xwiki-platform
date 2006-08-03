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
 * @author sdumitriu
 */

package com.xpn.xwiki.xmlrpc;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.suigeneris.jrcs.rcs.Version;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;

public class ConfluenceRpcHandler extends BaseRpcHandler {

    public class RemoteUser {

        public RemoteUser (String username, String ip) {
            this.ip = ip;
            this.username = username;
        }

        public String ip;
        public String username;
    }

    public ConfluenceRpcHandler(XWikiRequest request, XWikiResponse response, XWikiEngineContext econtext) {
        super(request, response, econtext);
    }

    public String login(String username, String password) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();
        if (username.equals("guest")) {
            String ip = context.getRequest().getRemoteAddr();
            String token = getValidationHash("guest", "guest", ip);
            getTokens(context).put(token, new RemoteUser("XWiki.XWikiGuest", ip));
            return token;
        }   else if (xwiki.getAuthService().authenticate(username, password, context)!=null) {
            String ip = context.getRequest().getRemoteAddr();
            String token = getValidationHash(username, password, ip);
            getTokens(context).put(token, new RemoteUser("XWiki." + username, ip));
            return token;
        } else
            return null;
    }

    private Hashtable getTokens(XWikiContext context) {
        Hashtable tokens = (Hashtable) context.getEngineContext().getAttribute("xmlrpc_tokens");
        if (tokens==null) {
            tokens = new Hashtable();
            context.getEngineContext().setAttribute("xmlrpc_tokens", tokens);
        }
        return tokens;
    }

    private String getValidationHash(String username, String password, String clientIP) {
        String validationKey = "xmlrpcapi";
        MessageDigest md5 = null;
        StringBuffer sbValueBeforeMD5 = new StringBuffer();

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error: " + e);
        }

        try {
            sbValueBeforeMD5.append(username.toString());
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(password.toString());
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(clientIP.toString());
            sbValueBeforeMD5.append(":");
            sbValueBeforeMD5.append(validationKey.toString());

            String valueBeforeMD5 = sbValueBeforeMD5.toString();
            md5.update(valueBeforeMD5.getBytes());

            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            String valueAfterMD5 = sb.toString();
            return valueAfterMD5;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void checkToken(String token, XWikiContext context) throws XWikiException {
        RemoteUser user = null;
        String ip = context.getRequest().getRemoteAddr();
        if (token != null)
             user = (RemoteUser)getTokens(context).get(token);
        if ((user==null)||(!user.ip.equals(ip))) {
            Object[] args = { token, ip };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID,
                                     "Access Denied: authentification token {0} for ip {1} is invalid", null, args);
        }
        context.setUser(user.username);
    }

    public boolean logout(String token) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        getTokens(context).remove(token);
        return true;
    }

    Hashtable getServerInfo(String token) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        return null;
    }

    public Vector getSpaces(String token) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        Vector spaces = new Vector();
        List webs = xwiki.search("select distinct doc.web from XWikiDocument doc", context);
        if (webs==null)
            return spaces;
        for (int i=0;i<webs.size();i++) {
            String web = (String)webs.get(i);
            SpaceSummary spacesum = new SpaceSummary(web, web, "http://127.0.0.1:9080/xwiki/bin/view/" + web + "/WebHome");
            spaces.add(spacesum.getHashtable());
        }
        return spaces;
    }

    public Hashtable getSpace(String token, String spaceKey) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument doc = new XWikiDocument(spaceKey, "WebHome");
        return (new Space(spaceKey, spaceKey, doc.getURL("view", context), spaceKey, "WebHome")).getHashtable();
    }

    public Vector getPages(String token, String spaceKey) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        Vector pages = new Vector();
        List docs = xwiki.getStore().searchDocumentsNames("where doc.web='" + Utils.SQLFilter(spaceKey) + "'", context);
        if (docs==null)
            return null;
        for (int i=0;i<docs.size();i++) {
            String docname = (String)docs.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            PageSummary pagesum = new PageSummary(doc, context);
            pages.add(pagesum.getHashtable());
        }
        return pages;
    }

    public Hashtable getPage(String token, String pageId) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument doc = xwiki.getDocument(pageId, context);
        Page page = new Page(doc, context);
        return page.getHashtable();
    }

    public Vector getPageHistory(String token, String pageId) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        Vector result = new Vector();
        XWikiDocument doc = xwiki.getDocument(pageId, context);
        Version[] versions = doc.getRevisions(context);
        for (int i=0;i<versions.length;i++) {
            String version = versions[i].toString();
            XWikiDocument revdoc = xwiki.getDocument(doc, version, context);
            result.add((new PageHistorySummary(revdoc)).getHashtable());
        }
        return result;
    }

    public Vector search(String token, String query, int maxResults) throws XWikiException {
        XWikiContext context = init();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        Vector result = new Vector();
        List doclist = xwiki.getStore().searchDocumentsNames("where doc.content like '%" + Utils.SQLFilter(query) +
                "%' or doc.name like '%" + Utils.SQLFilter(query) + "%'", context);
        if (doclist == null)
            return result;

        for (int i=0;i<doclist.size();i++) {
            String docname = (String)doclist.get(i);
            XWikiDocument document = xwiki.getDocument(docname, context);
            SearchResult sresult = new SearchResult(document);
            result.add(sresult.getHashtable());
        }
        return result;
    }


    public String renderContent(String token, String spaceKey, String pageId, String content) {
        XWikiContext context = null;
        String result = "";
        try {
            context = init();
            XWiki xwiki = context.getWiki();
            context.setAction("view");

            // Verify authentication token
            checkToken(token, context);

            XWikiDocument document = xwiki.getDocument(pageId, context);
            context.setDoc(document);
            xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext)context.get("vcontext"));
            result = xwiki.getRenderingEngine().renderText(content, document, context);
        } catch (Throwable e) {
            e.printStackTrace();
            result = handleException(e, context);
        }
        return result;
    }

    public Vector getAttachments(String token, String pageId) throws XWikiException {
        XWikiContext context = null;
        context = init();
        XWiki xwiki = context.getWiki();
        context.setAction("view");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext)context.get("vcontext"));

        Vector result = new Vector();
        List attachlist = document.getAttachmentList();
        if (attachlist!=null) {
            for (int i=0;i<attachlist.size();i++) {
                Attachment attach = new Attachment(document, (XWikiAttachment)attachlist.get(i), context);
                result.add(attach.getHashtable());
            }
        }
        return result;
    }

    public Vector getComments(String token, String pageId) throws XWikiException {
        XWikiContext context = null;
        context = init();
        XWiki xwiki = context.getWiki();
        context.setAction("view");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext)context.get("vcontext"));

        Vector result = new Vector();
        Vector commentlist = document.getObjects("XWiki.XWikiComments");
        if (commentlist!=null) {
            for (int i=0;i<commentlist.size();i++) {
                Comment comment = new Comment(document, (BaseObject)commentlist.get(i), context);
                result.add(comment);
            }
        }
        return result;
    }

    public Hashtable storePage(String token, Hashtable pageht) throws XWikiException {
        try {
        Page page = new Page(pageht);

        XWikiContext context = null;
        context = init();
        XWiki xwiki = context.getWiki();
        context.setAction("save");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(page.getId(), context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext)context.get("vcontext"));

        XWikiDocument newdoc = (XWikiDocument) document.clone();
        if (page.getParentId()!=null)
         newdoc.setParent(page.getParentId());

        newdoc.setAuthor(context.getUser());
        newdoc.setContent(page.getContent());
        context.getWiki().saveDocument(newdoc, document, context);
        return (new Page(newdoc, context)).getHashtable();
        } catch (XWikiException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void deletePage(String token, String pageId) throws XWikiException {
        XWikiContext context = null;
        context = init();
        XWiki xwiki = context.getWiki();
        context.setAction("delete");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext)context.get("vcontext"));
        context.getWiki().deleteDocument(document, context);
    }

    public Hashtable getUser(String token, String username) {
        return null;
    }

    public void addUser(String token, Hashtable user, String password) {
    }

    public void addGroup(String token, String group) {
    }

    public Vector getUserGroups(String token, String username) {
        return null;
    }

    public void addUserToGroup(String token, String username, String groupname) {
    }

    protected String handleException(Throwable e, XWikiContext context) {

        if (!(e instanceof XWikiException)) {
            e = new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception", e);
        }

        VelocityContext vcontext = ((VelocityContext)context.get("vcontext"));
        if (vcontext==null) {
            vcontext = new VelocityContext();
            context.put("vcontext", vcontext);
        }
        vcontext.put("exp", e);

        try {
            return parseTemplate("exception", context);
        } catch (Exception e2) {
            // I hope this never happens
            e.printStackTrace();
            e2.printStackTrace();
            return "Exception while serving request: " + e.getMessage();
        }
    }

    private String parseTemplate(String template, XWikiContext context) {
        context.setMode(XWikiContext.MODE_XMLRPC);
        String content = context.getWiki().parseTemplate(template + ".vm", context);
        return content;
    }

}
