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
 *
 */
package com.xpn.xwiki.xmlrpc;

import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;
import java.io.StringReader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Comment;
import org.codehaus.swizzle.confluence.ServerInfo;
import org.codehaus.swizzle.confluence.Space;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.xmlrpc.XWikiXmlRpcApi;
import org.xwiki.xmlrpc.model.XWikiExtendedId;
import org.xwiki.xmlrpc.model.XWikiObject;
import org.xwiki.xmlrpc.model.XWikiPage;
import org.xwiki.xmlrpc.model.XWikiPageHistorySummary;
import org.xwiki.xmlrpc.model.XWikiPageSummary;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.converter.Converter;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.web.Utils;

/**
 * The class containing the implementation of the XML-RPC API. Methods tagged with the ConfluenceAPI are compatible with
 * Confluence.
 * 
 * @version $Id$
 */
public class XWikiXmlRpcApiImpl implements XWikiXmlRpcApi
{
    private static final Log LOG = LogFactory.getLog(XWikiXmlRpcApiImpl.class);

    private XWikiContext xwikiContext;

    private com.xpn.xwiki.XWiki xwiki;

    private com.xpn.xwiki.api.XWiki xwikiApi;

    public XWikiXmlRpcApiImpl()
    {
        this.xwikiContext = getContext();
        this.xwiki = this.xwikiContext.getWiki();
        this.xwikiApi = new com.xpn.xwiki.api.XWiki(this.xwiki, this.xwikiContext);
    }

    protected XWikiContext getContext()
    {
        Execution execution = Utils.getComponent(Execution.class);
        ExecutionContext executionContext = execution.getContext();
        return (XWikiContext) executionContext.getProperty("xwikicontext");
    }

    /**
     * Login.
     * 
     * @return A token to be used in subsequent calls as an identification.
     * @throws Exception If authentication fails.
     */
    public String login(String userName, String password) throws Exception
    {
        String token;

        Principal principal = this.xwiki.getAuthService().authenticate(userName, password, this.xwikiContext);
        if (principal != null) {
            // Generate "unique" token using a random number
            token = this.xwiki.generateValidationKey(128);
            String ip = this.xwikiContext.getRequest().getRemoteAddr();

            XWikiUtils.getTokens(this.xwikiContext).put(token, new XWikiXmlRpcUser(principal.getName(), ip));

            return token;
        } else {
            throw new Exception(String.format("[Authentication failed for user '%s']", userName));
        }
    }

    /**
     * Logout.
     * 
     * @param token The authentication token.
     * @return True is logout was successful.
     * @throws Exception An invalid token is provided.
     */
    public Boolean logout(String token) throws Exception
    {
        XWikiUtils.checkToken(token, this.xwikiContext);

        return XWikiUtils.getTokens(this.xwikiContext).remove(token) != null;
    }

    /**
     * Get server information.
     * 
     * @param token The authentication token.
     * @return The server information
     * @throws Exception An invalid token is provided.
     */
    public Map getServerInfo(String token) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getServerInfo()", user.getName()));

        String version = this.xwikiApi.getVersion();
        Integer majorVersion = null;
        Integer minorVersion = null;
        Map serverInfoMap = new HashMap();
        serverInfoMap.put("DefaultSyntax", xwikiApi.getDefaultDocumentSyntax());
        serverInfoMap.put("ConfiguredSyntaxes", xwikiApi.getConfiguredSyntaxes());

        ServerInfo serverInfo = new ServerInfo(serverInfoMap);
        if (version != null) {
            serverInfo.setBuildId(version);
            if (version.indexOf('.') != -1) {
                String[] components = version.split("\\.");
                majorVersion = new Integer(components[0]);
                serverInfo.setMajorVersion(majorVersion);
                if (components[1].indexOf('-') != -1) {
                    // Removing possible suffixes (-SNAPSHOT for example)
                    minorVersion = new Integer(components[1].substring(0, components[1].indexOf('-')));
                } else {
                    minorVersion = new Integer(components[1]);
                }
                serverInfo.setMinorVersion(minorVersion);
            }
        } else {
            serverInfo.setMajorVersion(0);
            serverInfo.setMinorVersion(0);
        }

        serverInfo.setPatchLevel(0);
        serverInfo.setBaseUrl(this.xwikiApi.getURL(""));

        return serverInfo.toMap();
    }

    /**
     * Get the list of spaces.
     * 
     * @param token The authentication token.
     * @return A list of Maps that represent SpaceSummary objects.
     * @throws Exception An invalid token is provided.
     */
    public List getSpaces(String token) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getSpaces()", user.getName()));

        List result = new ArrayList();
        List<String> spaceKeys = this.xwikiApi.getSpaces();

        for (String spaceKey : spaceKeys) {
            String spaceWebHomeId = String.format("%s.WebHome", spaceKey);

            if (!this.xwikiApi.exists(spaceWebHomeId)) {
                result.add(DomainObjectFactory.createSpaceSummary(spaceKey).toRawMap());
            } else {
                Document spaceWebHome = this.xwikiApi.getDocument(spaceWebHomeId);

                /*
                 * If doc is null, then we don't have the rights to access the document, and therefore to the space.
                 */
                if (spaceWebHome != null) {
                    result.add(DomainObjectFactory.createSpaceSummary(spaceWebHome).toRawMap());
                }
            }
        }

        return result;
    }

    /**
     * Get information about a given space.
     * 
     * @param token The authentication token.
     * @param spaceKey The space name.
     * @return A map representing a Space object.
     * @throws Exception An invalid token is provided or the user doesn't have enough rights to access the space.
     */
    public Map getSpace(String token, String spaceKey) throws Exception
    {

        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getSpace()", user.getName()));

        if (!this.xwikiApi.getSpaces().contains(spaceKey)) {
            throw new Exception(String.format("[Space '%s' does not exist]", spaceKey));
        }

        String spaceWebHomeId = String.format("%s.WebHome", spaceKey);
        if (!this.xwikiApi.exists(spaceWebHomeId)) {
            return DomainObjectFactory.createSpace(spaceKey).toRawMap();
        } else {
            Document spaceWebHome = this.xwikiApi.getDocument(spaceWebHomeId);

            /*
             * If doc is null, then we don't have the rights to access the document
             */
            if (spaceWebHome != null) {
                return DomainObjectFactory.createSpace(spaceWebHome).toRawMap();
            } else {
                throw new RuntimeException(String.format("[Space '%s' cannot be accessed]", spaceKey));
            }
        }
    }

    /**
     * Add a new space. It basically creates a SpaceKey.WebHome page with no content and the space title as its title.
     * 
     * @param token The authentication token.
     * @param spaceMap The map representing a Space object.
     * @return The newly created space as a Space object.
     * @throws Exception An invalid token is provided or the space cannot be created or it already exists and the user
     *             has not the rights to modify it
     */
    public Map addSpace(String token, Map spaceMap) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called addSpace()", user.getName()));

        Space space = new Space(spaceMap);

        if (this.xwikiApi.getSpaces().contains(space.getKey())) {
            throw new Exception(String.format("[Space '%s' already exists]", space.getKey()));
        }

        String spaceWebHomeId = String.format("%s.WebHome", space.getKey());
        Document spaceWebHome = this.xwikiApi.getDocument(spaceWebHomeId);
        if (spaceWebHome != null) {
            spaceWebHome.setContent("");
            spaceWebHome.setTitle(space.getName());
            spaceWebHome.save();

            return DomainObjectFactory.createSpace(spaceWebHome).toRawMap();
        } else {
            throw new Exception(String.format(
                "[Space cannot be created or it already exists and user '%s' has not the right to modify it]",
                user.getName()));
        }
    }

    /**
     * Removes a space by deleting every page in it.
     * 
     * @param token The authentication token.
     * @param spaceKey The space name.
     * @return True if the space has been successfully deleted.
     * @throws Exception An invalid token is provided or there was a problem while deleting the space.
     */
    public Boolean removeSpace(String token, String spaceKey) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called removeSpace()", user.getName()));

        if (!this.xwikiApi.getSpaces().contains(spaceKey)) {
            throw new Exception(String.format("[Space '%s' does not exist.]", spaceKey));
        }

        boolean spaceDeleted = true;
        List<String> pageNames = this.xwikiApi.getSpaceDocsName(spaceKey);
        for (String pageName : pageNames) {
            String pageFullName = String.format("%s.%s", spaceKey, pageName);
            if (this.xwikiApi.exists(pageFullName)) {
                Document doc = this.xwikiApi.getDocument(pageFullName);
                if (doc != null) {
                    try {
                        if (!doc.getLocked()) {
                            doc.delete();
                        } else {
                            /*
                             * We cannot delete a locked page, so the space is not fully deleted.
                             */
                            spaceDeleted = false;
                        }
                    } catch (XWikiException e) {
                        /*
                         * An exception here means that we haven't succeeded in deleting a page, so there might be some
                         * pages that still belong to the space, so the space is not fully deleted
                         */
                        System.out.format("%s\n", e.getMessage());
                        spaceDeleted = false;
                    }
                } else {
                    spaceDeleted = false;
                }
            }
        }

        return spaceDeleted;
    }

    /**
     * @param token The authentication token.
     * @param spaceKey The space name.
     * @return A list containing PageSummary objects.
     * @throws Exception An invalid token is provided.
     */
    public List getPages(String token, String spaceKey) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getPages()", user.getName()));

        List result = new ArrayList();
        List<String> pageNames = this.xwikiApi.getSpaceDocsName(spaceKey);
        for (String pageName : pageNames) {
            String pageFullName = String.format("%s.%s", spaceKey, pageName);

            if (!this.xwikiApi.exists(pageFullName)) {
                LOG.warn(String.format("[Page '%s' appears to be in space '%s' but no information is available.]",
                    pageName, spaceKey));
            } else {
                Document doc = this.xwikiApi.getDocument(pageFullName);

                /* We only add pages we have the right to access */
                if (doc != null) {
                    XWikiPageSummary pageSummary = DomainObjectFactory.createXWikiPageSummary(doc);
                    result.add(pageSummary.toRawMap());
                }
            }
        }

        return result;
    }

    /**
     * Retrieves a page. The returned page title is set the current title if the current title != "", otherwise it is
     * set to the page name (i.e., the name part in the page Space.Name id)
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page[?language=l&version=v&minorVersion=mv]' format.
     * @return A map representing a Page object containing information about the requested page.
     * @throws Exception An invalid token is provided or the user has not the right to access the page or the page does
     *             not exist.
     */
    public Map getPage(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getPage()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        return DomainObjectFactory.createXWikiPage(doc, false).toRawMap();
    }

    /**
     * Store or rename a page, or create it if it doesn't exist.
     * <p>
     * The way confluence clients rename pages is the following:
     * </p>
     * <code>
     * page = getPage(pageId);
     * page.setSpace("New space");
     * page.setTitle("New title");
     * storePage(page);
     * </code>
     * <p>
     * In XWiki in order to rename a page we need to change its ID, and no client written for confluence will do this.
     * Currently the authoritative source for the page location is the ID (basically storePage ignores the space field)
     * and changing the title will only affect the page title. However if we agree to assume that the when using XMLRPC
     * the semantics of the page title is that of the page name in an XWiki ID, we will be able to be confluence
     * compatible.
     * </p>
     * </p> There are three possible cases: Let P=(id, space, title) the definition of a page. Let CP be the current
     * page and NP the page to be stored (i.e. the page passed to storePage): </p>
     * <p>
     * 1) CP=("Space.Name", "Space", "Title") NP=("Space.Name", "NewSpace", "Title") Here it is clear that the user
     * wants to "rename" the page by moving it to another space. So we rename the page to ("NewSpace.Name", "NewSpace",
     * "Title")
     * </p>
     * <p>
     * 2) CP=("Space.Name", "Space", "Title") NP=("Space.Name", "NewSpace", "NewTitle"); Here it is also clear that we
     * want to move the page to NewSpace but we have a problem about how to name the new page: NewSpace.Name or
     * NewSpace.NewTitle? According to the assumption stated before, we rename the page and use NewTitle as the page
     * name. The renamed page will have the NewSpace.NewTitle id. We also set the renamed page title to NewTitle.
     * </p>
     * <p>
     * 3) CP=("Space.Name", "Space", "Title") NP=("Space.Name", "Space", "NewTitle"); Here we have an ambiguity. Does
     * the user want to to rename the page or set its title? According to the assumption stated before we assume that
     * the user wants to rename the page so we will rename the page to Space.NewTitle and set its title to NewTitle.
     * </p>
     * 
     * @param token The authentication token.
     * @param pageMap A map representing the Page object to be stored.
     * @return A map representing a Page object with the updated information.
     * @throws Exception An invalid token is provided or some data is missing or the page is locked or the user has no
     *             right to modify the page.
     */
    public Map storePage(String token, Map pageMap) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called storePage()", user.getName()));

        XWikiPage page = new XWikiPage(pageMap);

        /*
         * Confluence semantics compatibility. In order to say to create a page, Confluence sets the id to null.
         */
        if (page.getId() == null) {
            if (page.getTitle() == null) {
                throw new Exception(String.format("[Neither page title, nor page id is specified!]"));
            }

            if (page.getSpace() == null) {
                throw new Exception(String.format("[Neither page's space, nor page id is specified!]"));
            }

            page.setId(String.format("%s.%s", page.getSpace(), page.getTitle()));
        }

        /* If the language field is null set it to the default language "" */
        if (page.getLanguage() == null) {
            page.setLanguage("");
        }

        /* If the syntax field is null then set it to the default wiki syntax */
        if (page.getSyntaxId() == null) {
            page.setSyntaxId(xwikiApi.getDefaultDocumentSyntax());
        }

        /* Build the extended id from the page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(page.getId());

        /* If the space is null then set it to the space encoded in the page id. */
        if (page.getSpace() == null) {
            String[] components = extendedId.getBasePageId().split("\\.");
            page.setSpace(components[0]);
        }

        /*
         * Even though the extended id could contain parameters such as language and version, here we ignore them and
         * just consider the base page id. The language to be stored will be taken from the result of
         * page.getLanguage(). Version is not meaningful because we don't allow to replace previous versions of the
         * page.
         */
        boolean pageAlreadyExists = this.xwikiApi.exists(extendedId.getBasePageId());
        Document doc = this.xwikiApi.getDocument(extendedId.getBasePageId());

        if (doc != null) {
            if (doc.getLocked()) {
                throw new Exception(String.format("[Unable to store document. Document locked by '%s']",
                    doc.getLockingUser()));
            }

            /*
             * Confluence semantics compatibility. Here we assume that the authoritative source for the page ID is the
             * combination of the fields Space + Title as specified in the page structure passed to the function. If the
             * title or the space (or both) are different from the current values then we must handle the request as a
             * page rename.
             */
            boolean rename = false;

            /*
             * Check if the page already exists. If it doesn't this is surely not a rename request! Check also that the
             * title is not null. This might happen if a client creates an XWikiPage object with an existing page id and
             * initializes only the content field (which is perfectly legal for non-existing pages but here causes a
             * NPE).
             */
            if (pageAlreadyExists && page.getTitle() != null) {
                /* The page already exists... check if we have received a rename request. */
                if (!page.getTitle().equals(doc.getName())) {
                    /*
                     * Here the title is different from the document's name. This check is necessary because getPage
                     * sets the title to the page name when the actual title is "". So when this is the case, even
                     * though the document title is different from the page title, the caller might not have changed the
                     * title field for renaming the page.
                     */
                    if (!page.getTitle().equals(doc.getTitle())) {
                        /*
                         * Here the page already exists, titles are different and the passed title is different from the
                         * page name. So the request is surely a rename request.
                         */

                        if (!page.getTitle().equals("")) {
                            /*
                             * Check a degenerate case where the user tries to rename the page to a page with an empty
                             * name. If we are here, the new title (i.e., the new page name is valid).
                             */
                            rename = true;
                        }
                    }
                }

                /* Check also on the space side */
                if (!doc.getSpace().equals(page.getSpace())) {
                    /*
                     * Here the document space and the space field of the page passed as parameter are different. So
                     * this is surely a rename request.
                     */
                    if (!page.getSpace().equals("")) {
                        /*
                         * Check a degenerate case where the user tries to rename the page to a space with an empty
                         * name. If we are here, the new space name is valid.
                         */
                        rename = true;
                    }
                }
            }

            /*
             * In order to rename a page we must rename the default language one. This simplifies the logic for handling
             * Confluence compatibility. In fact every page translation has its own title and we consider the default
             * translation title as the page name when renaming.
             */
            if (rename && page.getLanguage().equals("")) {
                /* This is a rename request */

                /*
                 * Given the conditions before, here we have that newSpace != "" AND newName != "", AND either newSpace
                 * != documentSpace OR newName != documentName.
                 */
                String newSpace = page.getSpace();
                String newName = page.getTitle();

                String newPageId = String.format("%s.%s", newSpace, newName);
                this.xwikiApi.renamePage(doc, newPageId);
                doc = this.xwikiApi.getDocument(newPageId);
            } else {
                /*
                 * Normal document storage.
                 */
                if (page.getLanguage() != null && !page.getLanguage().equals("")
                    && !page.getLanguage().equals(doc.getDefaultLanguage())) {
                    /*
                     * Try to get the document in the translation specified in the page parameter...
                     */
                    doc = doc.getTranslatedDocument(page.getLanguage());

                    if (!doc.getLanguage().equals(page.getLanguage())) {
                        /*
                         * If we are here, then the document returned by getTranslatedDocument is the same of the
                         * default translation, i.e., the page in the current translation does not exist. So we have to
                         * create it. Here we have to use the low-level XWiki API because it is not possible to set the
                         * language of a Document.
                         */
                        XWikiDocument xwikiDocument = new XWikiDocument(page.getSpace(), extendedId.getBasePageId());
                        xwikiDocument.setLanguage(page.getLanguage());
                        doc = new Document(xwikiDocument, this.xwikiContext);
                    }
                }

                if (!StringUtils.isEmpty(page.getSyntaxId())) {
                    doc.setSyntaxId(page.getSyntaxId());
                }

                doc.setContent(page.getContent());
                doc.setTitle(page.getTitle());
                doc.setParent(page.getParentId()); /* Allow reparenting */
                doc.save();
            }

            return DomainObjectFactory.createXWikiPage(doc, false).toRawMap();
        } else {
            throw new Exception(String.format("[Cannot access document for page '%s']", extendedId.getBasePageId()));
        }
    }

    /**
     * Remove a page.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public Boolean removePage(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called removePage()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        if (doc != null) {
            if (doc.getLocked()) {
                throw new Exception(String.format("[Unable to remove page. Document '%s' locked by '%s']",
                    doc.getName(), doc.getLockingUser()));
            }

            doc.delete();
        } else {
            throw new Exception(String.format("[Page '%s' cannot be accessed]", pageId));
        }

        return true;
    }

    /**
     * Get revision history of a page.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page[?language=lang]' format.
     * @return A list of maps representing PageHistorySummary objects.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public List getPageHistory(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getPageHistory()", user.getName()));

        List result = new ArrayList();

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        Version[] versions = doc.getRevisions();
        for (Version version : versions) {
            Document docRevision = this.xwikiApi.getDocument(doc, version.toString());

            /*
             * The returned document has the right content but the wrong content update date, that is always equals to
             * the current date. Don't know why.
             */
            result.add(DomainObjectFactory.createXWikiPageHistorySummary(docRevision).toRawMap());
        }

        return result;
    }

    /**
     * Render a page or content in HTML.
     * 
     * @param token The authentication token.
     * @param space Ignored
     * @param pageId The page id in the form of Space.Page
     * @param content The content to be rendered. If content == "" then the page content is rendered.
     * @return The rendered content.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public String renderContent(String token, String space, String pageId, String content) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called renderContent()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        this.xwikiContext.setAction("view");

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        XWikiDocument baseDocument = this.xwiki.getDocument(extendedId.getBasePageId(), this.xwikiContext);
        this.xwikiContext.setDoc(baseDocument);

        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();

        this.xwiki.prepareDocuments(this.xwikiContext.getRequest(), this.xwikiContext, vcontext);
        if (content.length() == 0) {
            /*
             * If content is not provided, then the existing content of the page is used
             */
            content = doc.getContent();
        } else {
            baseDocument.setAuthor(this.xwikiContext.getUser());
            baseDocument.setContentAuthor(this.xwikiContext.getUser());
        }

        return baseDocument.getRenderedContent(content, baseDocument.getSyntaxId(), xwikiContext);
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing Comment objects.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public List getComments(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getComments()", user.getName()));

        List result = new ArrayList();

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        Vector<com.xpn.xwiki.api.Object> comments = doc.getComments();
        if (comments != null) {
            for (com.xpn.xwiki.api.Object commentObject : comments) {
                result.add(DomainObjectFactory.createComment(doc, commentObject).toRawMap());
            }
        }

        return result;
    }

    /**
     * Get a specific comment. This method is here just for completeness because getComments already returns the list
     * containing all the objects that might be retrieved using this method.
     * 
     * @param token The authentication token.
     * @param commentId The comment id.
     * @return A map representing a Comment object.
     * @throws Exception An invalid token is provided or if the comment cannot be retrieved. access it.
     */
    public Map getComment(String token, String commentId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getComment()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(commentId);
        int commentNumericalId = Integer.parseInt(extendedId.getParameter(XWikiExtendedId.COMMENT_ID_PARAMETER));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, commentId, true);

        com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", commentNumericalId);

        return DomainObjectFactory.createComment(doc, commentObject).toRawMap();
    }

    /**
     * Add a comment.
     * 
     * @param token The authentication token.
     * @param commentMap A map representing a Comment object.
     * @return A map representing a Comment object with updated information.
     * @throws Exception An invalid token is provided or if the user has not the right to access it.
     */
    public Map addComment(String token, Map commentMap) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called addComment()", user.getName()));

        Comment comment = new Comment(commentMap);

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(comment.getPageId());
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);

        int id = doc.createNewObject("XWiki.XWikiComments");
        com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", id);
        commentObject.set("author", user.getName());
        commentObject.set("date", new Date());
        commentObject.set("comment", comment.getContent());

        doc.save();

        return DomainObjectFactory.createComment(doc, commentObject).toRawMap();
    }

    /**
     * Remove a comment.
     * 
     * @param token The authentication token.
     * @return True if the comment has been successfully removed.
     * @throws Exception An invalid token is provided or the user has not the right to access it.
     */
    public Boolean removeComment(String token, String commentId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called removeComment()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(commentId);
        int commentNumericalId = Integer.parseInt(extendedId.getParameter(XWikiExtendedId.COMMENT_ID_PARAMETER));

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);
        if (doc.getLocked()) {
            throw new Exception(String.format("[Unable to remove attachment. Document '%s' locked by '%s']",
                doc.getName(), doc.getLockingUser()));
        }

        com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", commentNumericalId);
        doc.removeObject(commentObject);
        doc.save();

        return true;
    }

    /**
     * Get attachments.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing Attachment objects.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public List getAttachments(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getAttachments()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        List result = new ArrayList();

        List<com.xpn.xwiki.api.Attachment> attachments = doc.getAttachmentList();
        for (com.xpn.xwiki.api.Attachment xwikiAttachment : attachments) {
            result.add(DomainObjectFactory.createAttachment(xwikiAttachment).toRawMap());
        }

        return result;
    }

    /**
     * Add an attachment.
     * 
     * @param token The authentication token.
     * @param contentId Ignored
     * @param attachmentMap The Attachment object used to identify the page id, and attachment metadata.
     * @param attachmentData The actual attachment data.
     * @return An Attachment object describing the newly added attachment.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public Map addAttachment(String token, Integer contentId, Map attachmentMap, byte[] attachmentData)
        throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called addAttachment()", user.getName()));

        Attachment attachment = new Attachment(attachmentMap);

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(attachment.getPageId());
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);

        if (doc.getLocked()) {
            throw new Exception(String.format("Unable to add attachment. Document locked by %s", doc.getLockingUser()));
        }

        com.xpn.xwiki.api.Attachment xwikiAttachment = doc.addAttachment(attachment.getFileName(), attachmentData);
        doc.save();

        return DomainObjectFactory.createAttachment(xwikiAttachment).toRawMap();
    }

    /**
     * Get the attachment data.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param versionNumber (Ignored)
     * @return An array of bytes with the actual attachment content.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it or the attachment with the given fileName does not exist on the given page.
     */
    public byte[] getAttachmentData(String token, String pageId, String fileName, String versionNumber)
        throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getAttachmentData()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(fileName);
        if (xwikiAttachment != null) {
            return xwikiAttachment.getContent();
        } else {
            throw new Exception(String.format("[Attachment '%s' does not exist on page '%s']", fileName, pageId));
        }
    }

    /**
     * Remove attachment.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return True if the attachment has been removed.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it or the attachment with the given fileName does not exist on the given page.
     */
    public Boolean removeAttachment(String token, String pageId, String fileName) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called removeAttachment()", user.getName()));

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);

        if (doc.getLocked()) {
            throw new Exception(String.format("Unable to remove attachment. Document '%s' locked by '%s'",
                doc.getName(), doc.getLockingUser()));
        }

        com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(fileName);
        if (xwikiAttachment != null) {
            /*
             * Here we must use low-level XWiki API because there is no way for removing an attachment through the XWiki
             * User's API. Basically we use the com.xpn.xwiki.XWiki object to re-do all the steps that we have already
             * done so far by using the API. It is safe because if we are here, we know that everything exists and we
             * have the proper rights to do things. So nothing should fail.
             */
            XWikiDocument baseXWikiDocument = this.xwiki.getDocument(extendedId.getBasePageId(), this.xwikiContext);
            XWikiAttachment baseXWikiAttachment = baseXWikiDocument.getAttachment(fileName);
            baseXWikiDocument.deleteAttachment(baseXWikiAttachment, this.xwikiContext);
            
            this.xwiki.saveDocument(baseXWikiDocument, this.xwikiContext);
        } else {
            throw new Exception(String.format("Attachment '%s' does not exist on page '%s'", fileName,
                extendedId.getBasePageId()));
        }

        return true;
    }

    /**
     * Get classes.
     * 
     * @param token The authentication token.
     * @return A list of maps representing XWikiClass objects.
     * @throws Exception An invalid token is provided.
     */
    public List getClasses(String token) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getClasses()", user.getName()));

        List result = new ArrayList();

        List<String> classNames = this.xwikiApi.getClassList();
        for (String className : classNames) {
            result.add(DomainObjectFactory.createXWikiClassSummary(className).toRawMap());
        }

        return result;
    }

    /**
     * Get a specific class.
     * 
     * @param token The authentication token.
     * @param className The class name.
     * @return A map representing a XWikiClass object.
     * @throws Exception An invalid token is provided or if the given class does not exist.
     */
    public Map getClass(String token, String className) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getClass()", user.getName()));

        if (!this.xwikiApi.exists(className)) {
            throw new Exception(String.format("[Class '%s' does not exist]", className));
        }

        com.xpn.xwiki.api.Class userClass = this.xwikiApi.getClass(className);

        return DomainObjectFactory.createXWikiClass(userClass).toRawMap();
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing XWikiObject objects.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public List getObjects(String token, String pageId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getObjects()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        List result = new ArrayList();

        Map<String, Vector<com.xpn.xwiki.api.Object>> classToObjectsMap = doc.getxWikiObjects();

        for (String className : classToObjectsMap.keySet()) {
            Vector<com.xpn.xwiki.api.Object> objects = classToObjectsMap.get(className);
            for (com.xpn.xwiki.api.Object object : objects) {
                result.add(DomainObjectFactory.createXWikiObjectSummary(doc, object).toRawMap());
            }
        }

        return result;
    }

    /**
     * The getObject function will return an XWikiObject where only non-null properties are included in the mapping
     * 'field' -> 'value' In order to know all the available fields and their respective types and attributes, clients
     * should refer to the object's class.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param className The class of the object.
     * @param id The id (number) of the object.
     * @return The XWikiObject containing the information about all the properties contained in the selected object.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it or no object with the given id exist in the page.
     */
    public Map getObject(String token, String pageId, String className, Integer id) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getObject()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        com.xpn.xwiki.api.Object object = doc.getObject(className, id);

        if (object != null) {
            return DomainObjectFactory.createXWikiObject(this.xwiki, this.xwikiContext, doc, object).toRawMap();
        } else {
            throw new Exception(String.format("[Unable to find object %s[%d] on page '%s']", className, id, pageId));
        }
    }

    /**
     * Update the object or create a new one if it doesn't exist.
     * 
     * @param token The authentication token.
     * @param objectMap A map representing the XWikiObject to be updated/created.
     * @return A map representing the XWikiObject with the updated information.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it.
     */
    public Map storeObject(String token, Map objectMap) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called storeObject()", user.getName()));

        XWikiObject object = new XWikiObject(objectMap);

        /* The passed object must specify a class name otherwise we cannot set properties on the xwiki object */
        if (object.getClassName() == null) {
            throw new Exception(String.format("[Object doesn't specify a class]"));
        }

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(object.getPageId());
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);

        if (doc.getLocked()) {
            throw new Exception(String.format("Unable to store object. Document locked by %s", doc.getLockingUser()));
        }

        com.xpn.xwiki.api.Object xwikiObject = null;

        /* First try to lookup the object by guid if specified, otherwise use classname[number] */
        if (object.getGuid() != null) {
            xwikiObject = XWikiUtils.getObjectByGuid(doc, object.getGuid());
        } else if (xwikiObject == null) {
            xwikiObject = doc.getObject(object.getClassName(), object.getId());
        }

        /* If the object does not exist create it */
        if (xwikiObject == null) {
            int id = doc.createNewObject(object.getClassName());
            /* Get the newly created object for update */
            xwikiObject = doc.getObject(object.getClassName(), id);

            /* We must initialize all the fields to an empty value in order to correctly create the object */
            com.xpn.xwiki.api.Class xwikiClass = this.xwikiApi.getClass(object.getClassName());
            for (Object propertyNameObject : xwikiClass.getPropertyNames()) {
                String propertyName = (String) propertyNameObject;

                xwikiObject.set(propertyName, "");
            }
        }

        /*
         * Allow clients to specify/override a guid. This is necessary to allow replication of objects between xwikis.
         * If it's null then a guid will be generated when the page is saved.
         */
        if (object.getGuid() != null) {
            xwikiObject.setGuid(object.getGuid());
        }

        /*
         * We iterate on the XWikiObject-passed-as-a-parameter's properties instead of the one retrieved through the API
         * because, a newly created object has no properties, and they should be added via set. Apparently setting
         * properties that do not belong to the object's class is harmless.
         */
        for (String propertyName : object.getProperties()) {
            /*
             * Object values are always sent as strings (or arrays/maps of strings)... let the actual object perform the
             * conversion
             */
            xwikiObject.set(propertyName, object.getProperty(propertyName));
        }

        doc.save();

        return DomainObjectFactory.createXWikiObject(this.xwiki, this.xwikiContext, doc, xwikiObject).toRawMap();
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param id The object's id.
     * @return True if the object has been successfully deleted.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it or no object with the given id exist in the page.
     */
    public Boolean removeObject(String token, String pageId, String className, Integer id) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called removeObject()", user.getName()));

        /* Ignore the language or version parameters passed with the page id, and use the base page id */
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.getBasePageId(), true);

        if (doc.getLocked()) {
            throw new Exception(String.format("[Unable to remove attachment. Document '%s' locked by '%s']",
                doc.getName(), doc.getLockingUser()));
        }

        com.xpn.xwiki.api.Object object = doc.getObject(className, id);
        if (object != null) {
            doc.removeObject(object);
            doc.save();
        } else {
            throw new Exception(String.format("[Object %s[%d] on page '%s' does not exist]", className, id,
                extendedId.getBasePageId()));
        }

        return true;
    }

    /**
     * Search pages.
     * 
     * @param token The authentication token.
     * @param query The string to be looked for. If it is "__ALL_PAGES__" the search will return all the page ids
     *            available in the Wiki.
     * @return A list of maps representing Search Results.
     * @throws Exception An invalid token is provided.
     */
    public List search(String token, String query, int maxResults) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called search()", user.getName()));

        List result = new ArrayList();
        if (query.equals("__ALL_PAGES__")) {
            List<String> spaceKeys = this.xwikiApi.getSpaces();
            for (String spaceKey : spaceKeys) {
                List<String> pageNames = this.xwikiApi.getSpaceDocsName(spaceKey);
                for (String pageName : pageNames) {
                    result.add(DomainObjectFactory.createSearchResult(String.format("%s.%s", spaceKey, pageName)).toMap());
                }
            }
        } else {
            List<String> searchResults =
                    this.xwiki.getStore().searchDocumentsNames(
                        "where doc.content like '%" + com.xpn.xwiki.web.Utils.SQLFilter(query)
                            + "%' or doc.name like '%" + com.xpn.xwiki.web.Utils.SQLFilter(query) + "%'",
                        this.xwikiContext);
            int i = 0;
            for (String pageId : searchResults) {
                if (maxResults > 0 && i < maxResults) {
                    result.add(DomainObjectFactory.createSearchResult(pageId).toMap());
                } else {
                    break;
                }
                i++;
            }
        }

        return result;
    }

    /**
     * Returns a list of XWikiPageHistorySummary containing all the pages that have been modified since a given date in
     * all their versions.
     * 
     * @param token
     * @param date The starting date
     * @param numberOfResults The number of results to be returned
     * @param start The start offset in the result set
     * @param fromLatest True if the result set will list recent changed pages before.
     * @return A list of maps representing XWikiPageHistorySummary
     * @throws Exception An invalid token is provided.
     */
    public List getModifiedPagesHistory(String token, Date date, int numberOfResults, int start, boolean fromLatest)
        throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getModifiedPagesHistory()", user.getName()));

        List result = new ArrayList();

        String order = fromLatest ? "desc" : "asc";
        String query =
                String.format(
                    "select doc.fullName, rcs.id, rcs.date, rcs.author from XWikiRCSNodeInfo as rcs, XWikiDocument as doc where rcs.id.docId=doc.id and rcs.date > :date order by rcs.date %s, rcs.id.version1 %s, rcs.id.version2 %s",
                    order, order, order);

        QueryManager queryManager = Utils.getComponent(QueryManager.class);
        List<Object> queryResult =
                queryManager.createQuery(query, Query.XWQL).bindValue("date", date).setLimit(numberOfResults).setOffset(
                    start).execute();

        for (Object o : queryResult) {
            Object[] fields = (Object[]) o;
            String pageId = (String) fields[0];
            XWikiRCSNodeId nodeId = (XWikiRCSNodeId) fields[1];
            Timestamp timestamp = (Timestamp) fields[2];
            String author = (String) fields[3];

            XWikiPageHistorySummary pageHistory = new XWikiPageHistorySummary();
            pageHistory.setId(pageId);
            pageHistory.setVersion(nodeId.getVersion().at(0));
            pageHistory.setMinorVersion(nodeId.getVersion().at(1));
            pageHistory.setModified(new Date(timestamp.getTime()));
            pageHistory.setModifier(author);

            result.add(pageHistory.toRawMap());
        }

        return result;
    }

    /**
     * This is a version of storePage that fails to store the page if the current version of the page doesn't match the
     * one of the page passed as parameter (i.e., the page has been modified since the last getPage)
     * 
     * @param token
     * @param pageMap A map representing the Page object to be stored.
     * @param checkVersion True if the current version of the page and the one of the page passed as parameter must
     *            match.
     * @return A map representing an XWikiPage with updated information, or an XWikiPage whose fields are all empty if
     *         there is a version mismatch.
     * @throws Exception An invalid token is provided.
     */
    public Map storePage(String token, Map pageMap, boolean checkVersion) throws Exception
    {
        if (checkVersion) {
            XWikiPage page = new XWikiPage(pageMap);

            /*
             * Build a new extended id by removing version information in order to retrieve the latest version, in case
             * in a given language if specified
             */
            XWikiExtendedId extendedId = new XWikiExtendedId(page.getId());
            extendedId.setParameter(XWikiExtendedId.LANGUAGE_PARAMETER, page.getLanguage());
            extendedId.setParameter(XWikiExtendedId.VERSION_PARAMETER, null);
            extendedId.setParameter(XWikiExtendedId.MINOR_VERSION_PARAMETER, null);

            /* If the page doesn't exist then use directly the standard storePage */
            if (!this.xwikiApi.exists(extendedId.getBasePageId())) {
                return storePage(token, pageMap);
            }

            Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.toString(), true);

            if (doc.getRCSVersion().at(0) == page.getVersion() && doc.getRCSVersion().at(1) == page.getMinorVersion()) {
                return storePage(token, pageMap);
            }

            return DomainObjectFactory.createEmptyXWikiPage().toRawMap();
        } else {
            return storePage(token, pageMap);
        }

    }

    /**
     * This is a version of storeObject that fails to store the object if the current version of the page associated to
     * the object doesn't match the one of the object passed as parameter (i.e., the object's page has been modified
     * since the last getObject)
     * 
     * @param token The authentication token.
     * @param objectMap A map representing the XWikiObject to be updated/created.
     * @param checkVersion True if the current version of the object's page and the one of the one passed as parameter
     *            must match.
     * @return A map representing the XWikiObject with the updated information, or an XWikiObject whose fields are all
     *         empty if there is a version mismatch.
     * @throws Exception An invalid token is provided.
     */
    public Map storeObject(String token, Map objectMap, boolean checkVersion) throws Exception
    {
        if (checkVersion) {
            XWikiObject object = new XWikiObject(objectMap);

            /*
             * Build a new extended id by removing version information in order to retrieve the latest version.For
             * objects we don't care about the language.
             */
            XWikiExtendedId extendedId = new XWikiExtendedId(object.getPageId());
            extendedId.setParameter(XWikiExtendedId.LANGUAGE_PARAMETER, null);
            extendedId.setParameter(XWikiExtendedId.VERSION_PARAMETER, null);
            extendedId.setParameter(XWikiExtendedId.MINOR_VERSION_PARAMETER, null);

            Document doc = XWikiUtils.getDocument(this.xwikiApi, extendedId.toString(), true);

            if (doc.getRCSVersion().at(0) == object.getPageVersion()
                && doc.getRCSVersion().at(1) == object.getPageMinorVersion()) {
                return storeObject(token, objectMap);
            }

            return DomainObjectFactory.createEmptyXWikiObject().toRawMap();
        } else {
            return storeObject(token, objectMap);
        }
    }

    /**
     * The getObject function will return an XWikiObject where only non-null properties are included in the mapping
     * 'field' -> 'value' In order to know all the available fields and their respective types and attributes, clients
     * should refer to the object's class.
     * 
     * @param token The authentication token.
     * @param pageId The pageId.
     * @param guid The object's guid.
     * @return The XWikiObject containing the information about all the properties contained in the selected object.
     * @throws Exception An invalid token is provided or if the page does not exist or the user has not the right to
     *             access it or no object with the given id exist in the page.
     */
    public Map getObject(String token, String pageId, String guid) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getObject()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        com.xpn.xwiki.api.Object object = XWikiUtils.getObjectByGuid(doc, guid);
        if (object == null) {
            throw new Exception(String.format("[Unable to find object with guid '%s' on page '%s']", guid, pageId));
        }

        return DomainObjectFactory.createXWikiObject(this.xwiki, this.xwikiContext, doc, object).toRawMap();
    }

    /**
     * Converts a wiki source from a syntax to another syntax.
     * 
     * @param token The authentication token.
     * @param source The content to be converted.
     * @param initialSyntaxId The initial syntax of the source.
     * @param targetSyntaxId The final syntax of the returned content.
     * @return The converted source.
     * @throws Exception An invalid token is provided, the syntaxId is not supported, the source is invalid or the
     *             conversion fails.
     */
    public String convert(String token, String source, String initialSyntaxId, String targetSyntaxId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        try {
            SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);
            Syntax initialSyntax = syntaxFactory.createSyntaxFromIdString(initialSyntaxId);
            Syntax targetSyntax = syntaxFactory.createSyntaxFromIdString(targetSyntaxId);
            WikiPrinter printer = new DefaultWikiPrinter();
            Converter converter = Utils.getComponent(Converter.class);
            converter.convert(new StringReader(source), initialSyntax, targetSyntax, printer);

            return printer.toString();
        } catch (Throwable t) {
            throw new RuntimeException("Exception while performing syntax conversion.", t);
        }
    }

    /**
     * Gets all syntaxes supported by the rendering parsers as an input for a syntax conversion.
     * 
     * @param token The authentication token.
     * @return A list containing all syntaxes supported by rendering parsers.
     * @throws Exception An invalid token is provided or the syntax lookup fails.
     */
    public List<String> getInputSyntaxes(String token) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        List<String> syntaxes = new ArrayList<String>();
        List<Parser> parsers;
        ComponentManager componentManager = Utils.getComponentManager();
        try {
            parsers = componentManager.lookupList(Parser.class);
            for (Parser parser : parsers) {
                syntaxes.add(parser.getSyntax().toIdString());
            }
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to lookup the list of available parser syntaxes", e);
        }
        return syntaxes;
    }

    /**
     * Gets all syntaxes supported by the rendering as an output for a syntax conversion.
     * 
     * @param token The authentication token.
     * @return A list containing all syntaxes supported by renderers.
     * @throws Exception An invalid token is provided or the syntax lookup fails.
     */
    public List<String> getOutputSyntaxes(String token) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        List<String> syntaxes = new ArrayList<String>();
        List<PrintRendererFactory> renderers;
        ComponentManager componentManager = Utils.getComponentManager();
        try {
            // TODO: use BlockRenderer
            renderers = componentManager.lookupList(PrintRendererFactory.class);
            for (PrintRendererFactory renderer : renderers) {
                syntaxes.add(renderer.getSyntax().toIdString());
            }
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to lookup the list of available renderer syntaxes", e);
        }
        return syntaxes;
    }

    /**
     * Renders a text in the context of a wiki page.
     *
     * @param token The authentication token.
     * @param pageId The id of the page.
     * @param content The context to be rendered.
     * @param sourceSyntaxId The syntax of the content.
     * @param targetSyntaxId The target syntax of the rendered content
     * @return The rendered content.
     * @throws Exception If a invalid token is provided, an unsupported syntax id is given or the rendering fails.
     */
    public String renderPageContent(String token, String pageId, String content, String sourceSyntaxId,
        String targetSyntaxId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called renderPageContent()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);

        return doc.getRenderedContent(content, sourceSyntaxId, targetSyntaxId);
    }

    /**
     * Gets the rendered content of an existing document.
     *
     * @param token The authentication token.
     * @param pageId The id of the page.
     * @param syntaxId The target syntax of the rendered content
     * @return The rendered content
     * @throws Exception If a invalid token is provided, an unsupported syntax id is given or the rendering fails.
     */
    public String getRenderedContent(String token, String pageId, String syntaxId) throws Exception
    {
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, this.xwikiContext);
        LOG.debug(String.format("User %s has called getRenderedContent()", user.getName()));

        Document doc = XWikiUtils.getDocument(this.xwikiApi, pageId, true);
        SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);
        Syntax syntax = syntaxFactory.createSyntaxFromIdString(syntaxId);

        return doc.getRenderedContent(syntax);
    }
}
