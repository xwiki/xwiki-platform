/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.suigeneris.jrcs.rcs.Version;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ConfluenceRpcHandler extends BaseRpcHandler implements ConfluenceRpcInterface
{
    private static final Log log =
        LogFactory.getLog(ConfluenceRpcHandler.class);

    public class RemoteUser
    {

        public RemoteUser(String username, String ip)
        {
            this.ip = ip;
            this.username = username;
        }

        public String ip;

        public String username;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#login(String, String)
     */
    public String login(String username, String password) throws XWikiException
    {
        String token;
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Guest users are always allowed, others need to be checked
        if (username.equals("guest")
            || xwiki.getAuthService().authenticate(username, password, context) != null) {
            // Token should be unique for each session. Use a random number that doesn't guarantee
            // uniqueness but that should be unique enough to be good enough.
            token = xwiki.generateValidationKey(128);
            String ip = context.getRequest().getRemoteAddr();
            getTokens(context).put(token, new RemoteUser("XWiki." + username, ip));
            log.info("Logged in '" + username + "'");
        } else {
            // TODO: Throw an exception instead of returning null
            log.info("Failed authentication for '" + username + "'");
            token = null;
        }

        return token;
    }

    private Map getTokens(XWikiContext context)
    {
        Map tokens = (Map) context.getEngineContext().getAttribute("xmlrpc_tokens");
        if (tokens == null) {
            tokens = new HashMap();
            context.getEngineContext().setAttribute("xmlrpc_tokens", tokens);
        }
        return tokens;
    }

    private void checkToken(String token, XWikiContext context) throws XWikiException
    {
        RemoteUser user = null;
        String ip = context.getRequest().getRemoteAddr();
        if (token != null)
            user = (RemoteUser) getTokens(context).get(token);
        if ((user == null) || (!user.ip.equals(ip))) {
            Object[] args = {token, ip};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID,
                "Access Denied: authentification token {0} for ip {1} is invalid",
                null,
                args);
        }
        context.setUser(user.username);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#logout(String)
     */
    public boolean logout(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();

        // Verify authentication token
        checkToken(token, context);

        getTokens(context).remove(token);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getServerInfo(String)
     */
    public Map getServerInfo(String token) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getSpaces(String)
     */
    public Object[] getSpaces(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        List webs = xwiki.search("select distinct doc.web from XWikiDocument doc", context);
        ArrayList spaces = new ArrayList(webs.size());
        for (int i = 0; i < webs.size(); i++) {
            String web = (String) webs.get(i);
            SpaceSummary spacesum =
                new SpaceSummary(web, web, "http://127.0.0.1:9080/xwiki/bin/view/" + web
                    + "/WebHome");
            spaces.add(spacesum.getParameters());
        }
        return spaces.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getSpace(String, String)
     */
    public Map getSpace(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument doc = new XWikiDocument(spaceKey, "WebHome");
        return (new Space(spaceKey, spaceKey, doc.getURL("view", context), spaceKey, "WebHome"))
            .getParameters();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getPages(String, String)
     */
    public Object[] getPages(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        List docs =
            xwiki.getStore().searchDocumentsNames(
                "where doc.web='" + Utils.SQLFilter(spaceKey) + "'", context);
        ArrayList pages = new ArrayList(docs.size());
        for (int i = 0; i < docs.size(); i++) {
            String docname = (String) docs.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            PageSummary pagesum = new PageSummary(doc, context);
            pages.add(pagesum.getParameters());
        }
        return pages.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getPage(String, String)
     */
    public Map getPage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument doc = xwiki.getDocument(pageId, context);
        Page page = new Page(doc, context);
        return page.getParameters();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getPageHistory(String, String)
     */
    public Object[] getPageHistory(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument doc = xwiki.getDocument(pageId, context);
        Version[] versions = doc.getRevisions(context);
        ArrayList result = new ArrayList(versions.length);
        for (int i = 0; i < versions.length; i++) {
            String version = versions[i].toString();
            XWikiDocument revdoc = xwiki.getDocument(doc, version, context);
            result.add((new PageHistorySummary(revdoc)).getParameters());
        }
        return result.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#search(String, String, int)
     */
    public Object[] search(String token, String query, int maxResults) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();

        // Verify authentication token
        checkToken(token, context);

        List doclist =
            xwiki.getStore().searchDocumentsNames(
                "where doc.content like '%" + Utils.SQLFilter(query) + "%' or doc.name like '%"
                    + Utils.SQLFilter(query) + "%'", context);
        if (doclist == null)
            return new Object[0];

        List result = new ArrayList(doclist.size());
        for (int i = 0; i < doclist.size(); i++) {
            String docname = (String) doclist.get(i);
            XWikiDocument document = xwiki.getDocument(docname, context);
            SearchResult sresult = new SearchResult(document);
            result.add(sresult.getParameters());
        }
        return result.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#renderContent(String, String, String, String)
     */
    public String renderContent(String token, String spaceKey, String pageId, String content)
        throws XWikiException
    {
        XWikiContext context = null;
        String result = "";
        try {
            context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            context.setAction("view");

            // Verify authentication token
            checkToken(token, context);

            XWikiDocument document = xwiki.getDocument(pageId, context);
            context.setDoc(document);
            xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
                .get("vcontext"));
            result = xwiki.getRenderingEngine().renderText(content, document, context);
        } catch (Throwable e) {
            e.printStackTrace();
            result = handleException(e, context);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getAttachments(String, String)
     */
    public Object[] getAttachments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = null;
        context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
            .get("vcontext"));

        List attachlist = document.getAttachmentList();
        ArrayList result = new ArrayList(attachlist.size());
        for (int i = 0; i < attachlist.size(); i++) {
            Attachment attach =
                new Attachment(document, (XWikiAttachment) attachlist.get(i), context);
            result.add(attach.getParameters());
        }
        return result.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getComments(String, String)
     */
    public Object[] getComments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = null;
        context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
            .get("vcontext"));

        List commentlist = document.getObjects("XWiki.XWikiComments");
        if (commentlist != null) {
            ArrayList result = new ArrayList(commentlist.size());
            for (int i = 0; i < commentlist.size(); i++) {
                Comment comment = new Comment(document, (BaseObject) commentlist.get(i), context);
                result.add(comment);
            }
            return result.toArray();
        }
        return new Object[0];
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#storePage(String, java.util.Map)
     */
    public Map storePage(String token, Map pageht) throws XWikiException
    {
        try {
            Page page = new Page(new HashMap(pageht));

            XWikiContext context = null;
            context = getXWikiContext();
            XWiki xwiki = context.getWiki();
            context.setAction("save");

            // Verify authentication token
            checkToken(token, context);

            XWikiDocument document = xwiki.getDocument(page.getId(), context);
            context.setDoc(document);
            xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
                .get("vcontext"));

            if (page.getParentId() != null)
                document.setParent(page.getParentId());

            document.setAuthor(context.getUser());
            document.setContent(page.getContent());
            context.getWiki().saveDocument(document, page.getComment(), context);
            return (new Page(document, context)).getParameters();
        } catch (XWikiException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#deletePage(String, String)
     */
    public void deletePage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = null;
        context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("delete");

        // Verify authentication token
        checkToken(token, context);

        XWikiDocument document = xwiki.getDocument(pageId, context);
        context.setDoc(document);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
            .get("vcontext"));
        context.getWiki().deleteDocument(document, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getUser(String, String)
     */
    public Map getUser(String token, String username) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addUser(String, java.util.Map, String)
     */
    public void addUser(String token, Map user, String password) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addGroup(String, String)
     */
    public void addGroup(String token, String group) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getUserGroups(String, String)
     */
    public Object[] getUserGroups(String token, String username) throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addUserToGroup(String, String, String)
     */
    public void addUserToGroup(String token, String username, String groupname)
        throws XWikiException
    {
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    protected String handleException(Throwable e, XWikiContext context)
    {

        if (!(e instanceof XWikiException)) {
            e =
                new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Uncaught exception",
                    e);
        }

        VelocityContext vcontext = ((VelocityContext) context.get("vcontext"));
        if (vcontext == null) {
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

    private String parseTemplate(String template, XWikiContext context)
    {
        context.setMode(XWikiContext.MODE_XMLRPC);
        String content = context.getWiki().parseTemplate(template + ".vm", context);
        return content;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addSpace(String, java.util.Map)
     */
    public Map addSpace(String token, Map spaceProperties) throws XWikiException
    {
        Space space = new Space(new HashMap(spaceProperties));

        XWikiContext context = getXWikiContext();
        context.setAction("save");

        // Verify authentication token
        checkToken(token, context);

        // Create a new document and store it
        XWikiDocument document = new XWikiDocument(space.getKey(), "WebHome");
        document.setAuthor(context.getUser());

        context.getWiki().saveDocument(document, context);

        // Set space settings
        space.setUrl(document.getURL("view", context));
        space.setHomepage(new Long(document.getId()).toString());

        return space.getParameters();
    }
}
