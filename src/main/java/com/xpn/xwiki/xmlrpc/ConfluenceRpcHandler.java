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
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.suigeneris.jrcs.rcs.Version;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * Ids (eg. page ids) should be treated as opaque handlers. If you ever find yourself parsing them
 * then you are doing something wrong.
 * 
 * @author hritcu
 */
public class ConfluenceRpcHandler extends BaseRpcHandler implements ConfluenceRpcInterface
{
    // TODO ensure that the ConfluenceRpcInterface uses the same order as here

    // TODO Q: if we use swizzle then is ConfluenceRpcInterface still needed ?

    // TODO either use this log or remove it

    private static final Log log = LogFactory.getLog(ConfluenceRpcHandler.class);

    private class RemoteUser
    {

        public RemoteUser(String username, String ip)
        {
            this.ip = ip;
            this.username = username;
        }

        public String ip;

        public String username;
    }

    private class DocObjectPair
    {
        public DocObjectPair(XWikiDocument doc, BaseObject obj)
        {
            this.doc = doc;
            this.obj = obj;
        }

        public XWikiDocument doc;

        public BaseObject obj;
    }

    // //////////////////////////
    // Authentication Methods //
    // //////////////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#login(String, String)
     */
    public String login(String username, String password) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        String token;

        if (xwiki.getAuthService().authenticate(username, password, context) != null) {
            // Generate "unique" token using a random number
            token = xwiki.generateValidationKey(128);
            String ip = context.getRequest().getRemoteAddr();
            getTokens(context).put(token, new RemoteUser("XWiki." + username, ip));
            log.info("Logged in '" + username + "'");
            return token;
        } else {
            throw exception("Failed authentication for user '" + username + "'.");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#logout(String)
     */
    public boolean logout(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        checkToken(token, context);

        return getTokens(context).remove(token) != null;
    }

    // ///////////
    // General //
    // ///////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getServerInfo(String)
     */
    public Map getServerInfo(String token) throws XWikiException
    {
        // TODO this should return as if this was Confluence
        // the version should be the largest one we implement fully
        // ServerInfo info = new ServerInfo();
        // info.setBaseUrl(baseUrl);
        //        
        // info.setMajorVersion(1);
        // info.setMinorVersion(0);
        // info.setPatchLevel(0);
        //                
        // return info.getParameters();

        throw exception("Not implemented.", XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED);
    }

    // ///////////////////
    // Space Retrieval //
    // ///////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getSpaces(String)
     */
    public Object[] getSpaces(String token) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        List spaces = xwiki.getSpaces(context);
        ArrayList result = new ArrayList(spaces.size());
        for (int i = 0; i < spaces.size(); i++) {
            String key = (String) spaces.get(i);
            String fullName = key + ".WebHome";
            XWikiDocument doc = xwiki.getDocument(fullName, context);
            String name = doc.getTitle();
            String url = doc.getURL("view", context);
            SpaceSummary spacesum = new SpaceSummary(key, name, url);
            result.add(spacesum.toMap());
        }
        return result.toArray();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getSpace(String, String)
     */
    public Map getSpace(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        String fullName = spaceKey + "." + "WebHome";
        if (xwiki.exists(fullName, context)) {
            XWikiDocument doc = xwiki.getDocument(fullName, context);
            String url = doc.getURL("view", context);
            String name = doc.getTitle();
            Space space = new Space(spaceKey, name, url, "", fullName);
            return space.toMap();
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }

    }

    // ////////////////////
    // Space Management //
    // ////////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addSpace(String, java.util.Map)
     */
    public Map addSpace(String token, Map spaceProperties) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("save");
        checkToken(token, context);

        Space space = new Space(new HashMap(spaceProperties));
        String spaceKey = space.getKey();
        String fullName = spaceKey + "." + "WebHome";
        if (!xwiki.exists(fullName, context)) {
            // Create a new WebHome document and store it
            XWikiDocument doc = new XWikiDocument(spaceKey, "WebHome");
            doc.setAuthor(context.getUser());
            if (space.getName() != null) {
                doc.setTitle(space.getName());
            }
            xwiki.saveDocument(doc, context);
            String url = doc.getURL("view", context);
            String name = doc.getTitle();
            Space resultSpace = new Space(spaceKey, name, url, "", fullName);
            return resultSpace.toMap();
        } else {
            throw exception("The space '" + spaceKey + "' already exists.");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.xmlrpc.ConfluenceRpcInterface#removeSpace(java.lang.String,
     *      java.lang.String)
     */
    public boolean removeSpace(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("delete");
        checkToken(token, context);

        if (xwiki.exists(spaceKey + "." + "WebHome", context)) {
            // Delete each page individually
            List docNames = xwiki.getSpaceDocsName(spaceKey, context);
            for (int i = 0; i < docNames.size(); i++) {
                removePage(token, spaceKey + "." + docNames.get(i));
            }
            return true;
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }
    }

    // //////////////////
    // Page Retrieval //
    // //////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getPages(String, String)
     */
    public Object[] getPages(String token, String spaceKey) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        String fullName = spaceKey + "." + "WebHome";
        if (xwiki.exists(fullName, context)) {
            List docNames = xwiki.getSpaceDocsName(spaceKey, context);
            ArrayList pages = new ArrayList(docNames.size());
            for (int i = 0; i < docNames.size(); i++) {
                String docFullName = spaceKey + "." + docNames.get(i);
                XWikiDocument doc = xwiki.getDocument(docFullName, context);
                PageSummary pagesum = new PageSummary(doc, context);
                pages.add(pagesum.toMap());
            }
            return pages.toArray();
        } else {
            throw exception("The space '" + spaceKey + "' does not exist.");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getPage(String, String)
     */
    public Map getPage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();

        checkToken(token, context);

        XWikiDocument doc = getPageDoc(pageId, context);
        Page page = new Page(doc, context);
        return page.toMap();
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
        checkToken(token, context);

        XWikiDocument doc = getPageDoc(pageId, context);

        // We only consider the old(!) versions of the document in the page history
        Version[] versions = doc.getRevisions(context);
        ArrayList result = new ArrayList();
        for (int i = 0; i < versions.length && !versions[i].toString().equals(doc.getVersion()); i++) {
            String version = versions[i].toString();
            XWikiDocument revdoc = xwiki.getDocument(doc, version, context);
            result.add((new PageHistorySummary(revdoc)).toMap());
        }
        return result.toArray();
    }

    // /////////////////////
    // Page Dependencies //
    // /////////////////////

    // TODO add missing methods + test attachments
    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getAttachments(String, String)
     */
    public Object[] getAttachments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = getPageDoc(pageId, context);

        List attachlist = doc.getAttachmentList();
        ArrayList result = new ArrayList(attachlist.size());
        for (int i = 0; i < attachlist.size(); i++) {
            XWikiAttachment xAttach = (XWikiAttachment) attachlist.get(i);
            Attachment attach = new Attachment(doc, xAttach, context);
            result.add(attach.toMap());
        }
        return result.toArray();
    }

    // TODO test history + comments
    // TODO generalize this to arbitrary objects (there the class name is no longer fixed)
    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getComments(String, String)
     */
    public Object[] getComments(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = getPageDoc(pageId, context);

        String className = xwiki.getCommentsClass(context).getName();
        List commentlist = doc.getObjects(className);
        if (commentlist != null) {
            ArrayList result = new ArrayList(commentlist.size());
            for (int i = 0; i < commentlist.size(); i++) {
                BaseObject obj = (BaseObject) commentlist.get(i);
                if (obj != null) {
                    // Note: checking for null values here is crucial
                    // because comments are just set to null when deleted
                    Comment comment = new Comment(doc, obj, context);
                    result.add(comment.toMap());
                }
            }
            return result.toArray();
        } else {
            return new Object[0];
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getComment(String, String)
     */
    public Map getComment(String token, String commentId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        context.setAction("view");
        checkToken(token, context);

        DocObjectPair pair = getDocObjectPair(commentId, context);
        return (new Comment(pair.doc, pair.obj, context)).toMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addComment(String, java.util.Map)
     */
    public Map addComment(String token, Map commentParams) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("commentadd");

        checkToken(token, context);

        Comment comment = new Comment(commentParams);
        XWikiDocument doc = getPageDoc(comment.getPageId(), context);

        // TODO Q: does this really have to be so complex ? (taken from CommentAddAction)
        Map map = new HashMap();
        map.put("author", context.getUser());
        map.put("date", ""); // dummy value needed
        map.put("comment", comment.getContent());
        BaseClass baseclass = xwiki.getCommentsClass(context);
        String className = baseclass.getName();
        int nb = doc.createNewObject(className, context);
        BaseObject oldobject = doc.getObject(className, nb);
        BaseObject newobject = (BaseObject) baseclass.fromMap(map, oldobject);

        newobject.setNumber(oldobject.getNumber());
        newobject.setName(doc.getFullName());
        doc.setObject(className, nb, newobject);
        String msg = context.getMessageTool().get("core.comment.addComment");
        xwiki.saveDocument(doc, msg, context);

        return (new Comment(doc, newobject, context)).toMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#removeComment(String, String)
     */
    public boolean removeComment(String token, String commentId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");
        checkToken(token, context);

        DocObjectPair pair = getDocObjectPair(commentId, context);
        pair.doc.removeObject(pair.obj);
        String msg = context.getMessageTool().get("core.comment.deleteObject");
        xwiki.saveDocument(pair.doc, msg, context);

        return true;
    }

    // ///////////////////
    // Page Management //
    // ///////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#storePage(String, java.util.Map)
     */
    public Map storePage(String token, Map pageMap) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("save");
        checkToken(token, context);

        Page page = new Page(new HashMap(pageMap));
        XWikiDocument doc = null;
        if (page.getId() != null) {
            // page id is set -> save page
            doc = xwiki.getDocument(page.getId(), context);
            // TODO Q: What should happen when storing an old version of a page ?
        } else {
            // page id not set -> create new page or overwrite existing one
            String space = page.getSpace();
            String title = page.getTitle();
            if (space == null || title == null) {
                throw exception("Space and title are required when calling storePage with no id");
            }

            // if page already exists then overwrite it -- could also raise exception
            doc = xwiki.getDocument(space + "." + title, context);

            if (doc == null) {
                // otherwise create a new page
                doc = new XWikiDocument(space, title);
            }
        }
        if (page.getParentId() != null) {
            doc.setParent(page.getParentId());
        }
        doc.setAuthor(context.getUser());
        doc.setContent(page.getContent());
        context.getWiki().saveDocument(doc, page.getComment(), context);
        return (new Page(doc, context)).toMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#renderContent(String, String, String, String)
     */
    public String renderContent(String token, String spaceKey, String pageId, String content)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("view");
        checkToken(token, context);

        XWikiDocument doc = getPageDoc(pageId, context);
        context.setDoc(doc);
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        xwiki.prepareDocuments(context.getRequest(), context, vcontext);
        if (content.length() == 0) {
            // If content is not provided, then the existing content of the page is used instead
            content = doc.getContent();
        }
        String result = xwiki.getRenderingEngine().renderText(content, doc, context);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#removePage(String, String)
     */
    public boolean removePage(String token, String pageId) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        context.setAction("delete");
        checkToken(token, context);

        // TODO page has to exist
        // TODO What about deleting old revisions?
        // Should they cause the page to be deleted or an exception?
        XWikiDocument doc = xwiki.getDocument(pageId, context);
        context.setDoc(doc);
        xwiki.prepareDocuments(context.getRequest(), context, (VelocityContext) context
            .get("vcontext"));
        context.getWiki().deleteDocument(doc, context);
        // Note: false is never be returned from this function,
        // instead an exception is thrown in case of error.
        return true;
    }

    // //////////
    // Search //
    // //////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#search(String, String, int)
     */
    public Object[] search(String token, String query, int maxResults) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        checkToken(token, context);

        List doclist =
            xwiki.getStore().searchDocumentsNames(
                "where doc.content like '%" + Utils.SQLFilter(query) + "%' or doc.name like '%"
                    + Utils.SQLFilter(query) + "%'", context);

        // TODO: This is not enough and search is implemented in velocity (SUPER STUPID!!!)
        // http://localhost:8080/xwiki/bin/edit/XWiki/WebSearchCode
        // xwiki.search(sql, context)
        
        // TODO Can i use the Lucerne search JV just added ?

        if (doclist == null)
            return new Object[0];

        List result = new ArrayList(doclist.size());
        for (int i = 0; i < doclist.size(); i++) {
            String docName = (String) doclist.get(i);
            XWikiDocument doc = xwiki.getDocument(docName, context);
            SearchResult sresult = new SearchResult(doc, context);
            result.add(sresult.toMap());
        }
        return result.toArray();
    }

    // /////////////////////
    // Rights Management //
    // /////////////////////

    // ///////////////////
    // User Management //
    // ///////////////////

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#getUser(String, String)
     */
    public Map getUser(String token, String username) throws XWikiException
    {
        // TODO implement!
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addUser(String, java.util.Map, String)
     */
    public boolean addUser(String token, Map user, String password) throws XWikiException
    {
        // TODO implement!
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addGroup(String, String)
     */
    public boolean addGroup(String token, String group) throws XWikiException
    {
        // TODO implement!
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
        // TODO implement!
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ConfluenceRpcInterface#addUserToGroup(String, String, String)
     */
    public boolean addUserToGroup(String token, String username, String groupname)
        throws XWikiException
    {
        // TODO implement!
        throw new XWikiException(XWikiException.MODULE_XWIKI_XMLRPC,
            XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
            "Not implemented");
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

    /**
     * Verify the authentication token
     * 
     * @param token
     * @param context
     * @throws XWikiException
     */
    private void checkToken(String token, XWikiContext context) throws XWikiException
    {
        RemoteUser user = null;
        String ip = context.getRequest().getRemoteAddr();

        if (token != null) {
            if (!token.equals("")) {
                user = (RemoteUser) getTokens(context).get(token);
            } else {
                // anonymous access
                user = new RemoteUser("XWiki.XWikiGuest", ip);
            }
        }

        if ((user == null) || (!user.ip.equals(ip))) {
            throw exception("Access Denied: authentification token {" + token + "} for ip {" + ip
                + "} is invalid", XWikiException.ERROR_XWIKI_ACCESS_TOKEN_INVALID);
        }

        context.setUser(user.username);
    }

    private XWikiDocument getPageDoc(String pageId, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        if (!pageId.contains(":")) {
            // Current version of document
            if (xwiki.exists(pageId, context)) {
                return xwiki.getDocument(pageId, context);
            } else {
                throw exception("The page '" + pageId + "' does not exist.");
            }
        } else {
            int i = pageId.indexOf(":");
            String fullName = pageId.substring(0, i);
            String version = pageId.substring(i + 1);
            if (xwiki.exists(fullName, context)) {
                XWikiDocument currentDoc = xwiki.getDocument(fullName, context);
                return xwiki.getDocument(currentDoc, version, context);
            } else {
                throw exception("The page '" + fullName + "' does not exist.");
            }
        }
    }

    private DocObjectPair getDocObjectPair(String commentId, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        String pageId = commentId.substring(0, commentId.indexOf(";"));
        int nb = (new Integer(commentId.substring(commentId.indexOf(";") + 1))).intValue();
        XWikiDocument doc = getPageDoc(pageId, context);
        BaseObject obj = doc.getObject(xwiki.getCommentsClass(context).getName(), nb);
        return new DocObjectPair(doc, obj);
    }

    private XWikiException exception(String message)
    {
        return exception(message, 0);
    }

    private XWikiException exception(String message, int code)
    {
        log.info("Exception thrown to XML-RPC client: " + message);
        // return new Exception(message);
        XWikiException ex = new XWikiException();
        ex.setModule(XWikiException.MODULE_XWIKI_XMLRPC);
        ex.setCode(code);
        ex.setMessage(message);
        return ex;
    }
}
