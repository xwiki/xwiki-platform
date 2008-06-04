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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.apache.xmlrpc.XmlRpcException;
import org.codehaus.swizzle.confluence.Attachment;
import org.codehaus.swizzle.confluence.Comment;
import org.codehaus.swizzle.confluence.ServerInfo;
import org.codehaus.swizzle.confluence.Space;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.xmlrpc.model.XWikiExtendedId;
import org.xwiki.xmlrpc.model.XWikiObject;
import org.xwiki.xmlrpc.model.XWikiPage;
import org.xwiki.xmlrpc.model.XWikiPageSummary;
import org.xwiki.velocity.VelocityManager;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * The class containing the implementation of the XML-RPC API. Methods tagged with the ConfluenceAPI
 * are compatible with Confluence.
 */
public class XWikiXmlRpcHandler
{
    private XWikiXmlRpcHttpRequestConfig xwikiXmlRpcHttpRequestConfig;

    private static final Log LOG = LogFactory.getLog(XWikiXmlRpcHandler.class);

    /**
     * Initialize the XML-RPC handler with respect to the current HTTP request.
     * 
     * @param servlet The servlet requesting the XML-RPC call handling.
     * @param httpRequest The current HTTP request.
     */
    public void init(XWikiXmlRpcHttpRequestConfig requestConfig)
    {
        this.xwikiXmlRpcHttpRequestConfig = requestConfig;
    }

    /**
     * Login.
     * 
     * @return A token to be used in subsequent calls as an identification.
     * @throws XmlRpcException If authentication fails.
     * @category ConfluenceAPI
     */
    public String login(String userName, String password) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();

        com.xpn.xwiki.XWiki xwiki = xwikiXmlRpcContext.getBaseXWiki();
        String token;

        if (xwikiXmlRpcContext.getBaseXWiki().getAuthService().authenticate(userName, password,
            xwikiXmlRpcContext.getXWikiContext()) != null) {
            // Generate "unique" token using a random number
            token = xwiki.generateValidationKey(128);
            String ip = xwikiXmlRpcContext.getXWikiContext().getRequest().getRemoteAddr();

            XWikiUtils.getTokens(xwikiXmlRpcContext.getXWikiContext()).put(token,
                new XWikiXmlRpcUser(String.format("XWiki.%s", userName), ip));

            return token;
        } else {
            throw new XmlRpcException(String.format("[Authentication failed for user %s.]",
                userName));
        }
    }

    /**
     * Logout.
     * 
     * @param token The authentication token.
     * @return True is logout was successful.
     * @throws XmlRpcException An invalid token is provided.
     * @category ConfluenceAPI
     */
    public Boolean logout(String token) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());

        return XWikiUtils.getTokens(xwikiXmlRpcContext.getXWikiContext()).remove(token) != null;
    }

    /**
     * Get server information.
     * 
     * @param token The authentication token.
     * @return The server information
     * @throws XmlRpcException An invalid token is provided.
     * @category ConfluenceAPI
     */
    public Map getServerInfo(String token) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getServerInfo()", user.getName()));

        String version = xwiki.getVersion();
        Integer majorVersion = null;
        Integer minorVersion = null;
        ServerInfo serverInfo = new ServerInfo();

        if (version != null) {
            serverInfo.setBuildId(version);
            if (version.indexOf('.') != -1) {
                String[] components = version.split("\\.");
                majorVersion = new Integer(components[0]);
                serverInfo.setMajorVersion(majorVersion);
                if (components[1].indexOf('-') != -1) {
                    // Removing possible suffixes (-SNAPSHOT for example)
                    minorVersion =
                        new Integer(components[1].substring(0, components[1].indexOf('-')));
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
        serverInfo.setBaseUrl(xwiki.getURL(""));

        return serverInfo.toMap();
    }

    /**
     * @param token The authentication token.
     * @return A list of Maps that represents SpaceSummary objects.
     * @throws XmlRpcException An invalid token is provided.
     * @category ConfluenceAPI
     */
    public List getSpaces(String token) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getSpaces()", user.getName()));

        List result = new ArrayList();
        List<String> spaceKeys = xwiki.getSpaces();

        for (String spaceKey : spaceKeys) {
            String spaceWebHomeId = String.format("%s.WebHome", spaceKey);

            if (!xwiki.exists(spaceWebHomeId)) {
                result.add(DomainObjectFactory.createSpaceSummary(spaceKey).toRawMap());
            } else {
                Document spaceWebHome = xwiki.getDocument(spaceWebHomeId);

                /*
                 * If doc is null, then we don't have the rights to access the document, and
                 * therefore to the space.
                 */
                if (spaceWebHome != null) {
                    result.add(DomainObjectFactory.createSpaceSummary(spaceWebHome).toRawMap());
                }
            }
        }

        return result;
    }

    /**
     * @param token The authentication token.
     * @return A map representing a Space object.
     * @throws XmlRpcException An invalid token is provided or the user doesn't have enough rights
     *             to access the space.
     * @category ConfluenceAPI
     */
    public Map getSpace(String token, String spaceKey) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getSpace()", user.getName()));

        if (!xwiki.getSpaces().contains(spaceKey)) {
            throw new XmlRpcException(String.format("[Space '%s' does not exist]", spaceKey));
        }

        String spaceWebHomeId = String.format("%s.WebHome", spaceKey);
        if (!xwiki.exists(spaceWebHomeId)) {
            return DomainObjectFactory.createSpace(spaceKey).toRawMap();
        } else {
            Document spaceWebHome = xwiki.getDocument(spaceWebHomeId);

            /*
             * If doc is null, then we don't have the rights to access the document
             */
            if (spaceWebHome != null) {
                return DomainObjectFactory.createSpace(spaceWebHome).toRawMap();
            } else {
                throw new XmlRpcException(String.format("[Space '%s' cannot be accessed]",
                    spaceKey));
            }
        }
    }

    /**
     * Add a new space. It basically creates a SpaceKey.WebHome page with no content and the space
     * title as its title.
     * 
     * @param token The authentication token.
     * @param spaceMap The map representing a Space object.
     * @return The newly created space as a Space object.
     * @throws XmlRpcException Space cannot be created or it already exists and the user has not the
     *             rights to modify it
     * @category ConfluenceAPI
     */
    public Map addSpace(String token, Map spaceMap) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called addSpace()", user.getName()));

        Space space = new Space(spaceMap);

        if (xwiki.getSpaces().contains(space.getKey())) {
            throw new XmlRpcException(String
                .format("[Space '%s' already exists]", space.getKey()));
        }

        String spaceWebHomeId = String.format("%s.WebHome", space.getKey());
        Document spaceWebHome = xwiki.getDocument(spaceWebHomeId);
        if (spaceWebHome != null) {
            spaceWebHome.setContent("");
            spaceWebHome.setTitle(space.getName());
            spaceWebHome.save();

            return DomainObjectFactory.createSpace(spaceWebHome).toRawMap();
        } else {
            throw new XmlRpcException(String
                .format(
                    "[Space cannot be created or it already exists and user '%s' has not the right to modify it]",
                    user.getName()));
        }
    }

    /**
     * Removes a space by deleting every page in it.
     * 
     * @param token The authentication token.
     * @return True if the space has been successfully deleted.
     * @category ConfluenceAPI
     */
    public Boolean removeSpace(String token, String spaceKey) throws XWikiException,
        XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called removeSpace()", user.getName()));

        if (!xwiki.getSpaces().contains(spaceKey)) {
            throw new XmlRpcException(String.format("[Space '%s' does not exist.]", spaceKey));
        }

        boolean spaceDeleted = true;
        List<String> pageNames = xwiki.getSpaceDocsName(spaceKey);
        for (String pageName : pageNames) {
            String pageFullName = String.format("%s.%s", spaceKey, pageName);
            if (xwiki.exists(pageFullName)) {
                Document doc = xwiki.getDocument(pageFullName);
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
                         * An exception here means that we haven't succeeded in deleting a page, so
                         * there might be some pages that still belong to the space, so the space is
                         * not fully deleted
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
     * @return A list containing PageSummary objects.
     * @category ConfluenceAPI
     */
    public List getPages(String token, String spaceKey) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getPages()", user.getName()));

        List result = new ArrayList();
        List<String> pageNames = xwiki.getSpaceDocsName(spaceKey);
        for (String pageName : pageNames) {
            String pageFullName = String.format("%s.%s", spaceKey, pageName);

            if (!xwiki.exists(pageFullName)) {
                LOG.warn(String.format(
                    "[Page '%s' appears to be in space '%s' but no information is available.]",
                    pageName));
            } else {
                Document doc = xwiki.getDocument(pageFullName);

                /* We only add pages we have the right to access */
                if (doc != null) {
                    XWikiPageSummary pageSummary =
                        DomainObjectFactory.createXWikiPageSummary(doc);
                    result.add(pageSummary.toRawMap());
                }
            }
        }

        return result;
    }

    /**
     * Retrieves a page.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param language The language id for the translation
     * @param version The desired version. If version == 0 then the latest version is returned.
     * @param minorVersion The desired minor version (ignored if version == 0).
     * @return A map representing a Page object containing information about the page at version
     *         'version.minorVersion'
     * @throws XmlRpcException If the user has not the right to access the page or the page does not
     *             exist at version 'version.minorVersion'.
     */
    public Map getPage(String token, String pageId) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getPage()", user.getName()));

        /* Extract all needed information from the extended xwiki id */
        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);
        String versionString = extendedId.getParameter("version");
        int version = versionString != null ? Integer.parseInt(versionString) : 0;
        String minorVersionString = extendedId.getParameter("minorVersion");
        int minorVersion = minorVersionString != null ? Integer.parseInt(minorVersionString) : 1;
        String language =
            extendedId.getParameter("language") != null ? extendedId.getParameter("language")
                : "";

        if (!xwiki.exists(extendedId.getBasePageId())) {
            throw new XmlRpcException(String.format("Unable to get page %s", extendedId
                .getBasePageId()));
        }

        Document doc = xwiki.getDocument(extendedId.getBasePageId());
        if (doc != null) {
            if (language.equals("") || !doc.getTranslationList().contains(language)) {
                language = doc.getDefaultLanguage();
            }

            /*
             * If version == 0 then don't get a specific version and keep the latest version
             * returned by the previous call of getDocument().
             */
            if (version != 0) {
                doc = xwiki.getDocument(doc, String.format("%d.%d", version, minorVersion));
            }

            if (doc != null) {
                doc = doc.getTranslatedDocument(language);

                if (doc != null) {
                    if (version != 0) {
                        /*
                         * If a specific page version has been requested, encode it it the page id
                         * using extended page id version
                         */
                        return DomainObjectFactory.createXWikiPage(doc, true).toRawMap();
                    } else {
                        return DomainObjectFactory.createXWikiPage(doc, false).toRawMap();
                    }
                } else {
                    throw new XmlRpcException(String.format(
                        "Page '%s' does not have an '%s' translation",
                        extendedId.getBasePageId(), language));
                }
            } else {
                throw new XmlRpcException(String.format(
                    "Page '%s' does not exist at version %d.%d", extendedId.getBasePageId(),
                    version, minorVersion));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' cannot be accessed", extendedId
                .getBasePageId()));
        }
    }

    /**
     * Store a page or create it if it doesn't exist.
     * 
     * @param token The authentication token.
     * @param pageMap A map representing the Page object to be stored.
     * @return A map representing a Page object with the updated information.
     * @category ConfluenceAPI
     */
    public Map storePage(String token, Map pageMap) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called storePage()", user.getName()));

        XWikiPage page = new XWikiPage(pageMap);

        /*
         * Confluence semantics compatibility. In order to say to create a page, Confluence sets the
         * id to null.
         */
        if (page.getId() == null) {
            if (page.getTitle() == null) {
                throw new XmlRpcException(String
                    .format("[Neither page title, nor page id is specified!]"));
            }
            page
                .setId(String.format("%s.%s", page.getSpace(), page.getTitle().replace(' ', '_')));
        }

        if (page.getLanguage() == null) {
            page.setLanguage("");
        }

        XWikiExtendedId extendedId = new XWikiExtendedId(page.getId());

        Document doc = xwiki.getDocument(extendedId.getBasePageId());

        if (doc != null) {
            if (doc.getLocked()) {
                throw new XmlRpcException(String.format(
                    "Unable to store document. Document locked by %s", doc.getLockingUser()));
            }

            if (!page.getLanguage().equals("")
                && !page.getLanguage().equals(doc.getDefaultLanguage())) {
                /*
                 * Try to get the document in the translation specified in the page parameter...
                 */
                doc = doc.getTranslatedDocument(page.getLanguage());

                if (!doc.getLanguage().equals(page.getLanguage())) {
                    /*
                     * If we are here, then the document returned by getTranslatedDocument is the
                     * same of the default translation, i.e., the page in the current translation
                     * does not exist. So we have to create it. Here we have to use the low-level
                     * XWiki API because it is not possible to set the language of a Document.
                     */
                    XWikiDocument xwikiDocument =
                        new XWikiDocument(page.getSpace(), extendedId.getBasePageId());
                    xwikiDocument.setLanguage(page.getLanguage());
                    doc = new Document(xwikiDocument, xwikiXmlRpcContext.getXWikiContext());
                }
            }

            doc.setContent(page.getContent());
            doc.setTitle(page.getTitle());
            doc.setParent(page.getParentId()); /* Allow reparenting */

            doc.save();

            return DomainObjectFactory.createXWikiPage(doc, false).toRawMap();
        } else {
            throw new XmlRpcException(String.format("Cannot get document for page '%s'",
                extendedId.getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public Boolean removePage(String token, String pageId) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called removePage()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to remove attachment. Document '%s' locked by '%s'", doc
                            .getName(), doc.getLockingUser()));
                }

                doc.delete();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return true;
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing PageHistorySummary objects.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public List getPageHistory(String token, String pageId) throws XWikiException,
        XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getPageHistory()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        List result = new ArrayList();
        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                Version[] versions = doc.getRevisions();
                for (Version version : versions) {
                    Document docRevision = xwiki.getDocument(doc, version.toString());

                    /*
                     * The returned document has the right content but the wrong content update
                     * date, that is always equals to the current date. Don't know why.
                     */
                    result.add(DomainObjectFactory.createXWikiPageHistorySummary(docRevision)
                        .toRawMap());
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return result;
    }

    /**
     * Render a page or content in HTML.
     * 
     * @param token The authentication token.
     * @param space Ignored
     * @param pageId The page id in the form of Space.Page
     * @param content The content to be rendered. If content == "" then the page content is
     *            rendered.
     * @return The rendered content.
     * @throws XmlRpcException XmlRpcException If the page does not exist or the user has not the
     *             right to access it.
     * @category ConfluenceAPI
     */
    public String renderContent(String token, String space, String pageId, String content)
        throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called renderContent()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        List result = new ArrayList();
        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                /*
                 * This is the old implementation. TODO: Check if it can be made more lightweight...
                 * xwiki.renderText() doesn't work fine as the following.
                 */
                XWikiContext context = xwikiXmlRpcContext.getXWikiContext();
                com.xpn.xwiki.XWiki baseXWiki = context.getWiki();
                context.setAction("view");

                XWikiDocument baseDocument =
                    baseXWiki.getDocument(extendedId.getBasePageId(), context);
                context.setDoc(baseDocument);
                
                VelocityManager velocityManager =
                    (VelocityManager) Utils.getComponent(VelocityManager.ROLE);
                VelocityContext vcontext = velocityManager.getVelocityContext();

                baseXWiki.prepareDocuments(context.getRequest(), context, vcontext);
                if (content.length() == 0) {
                    // If content is not provided, then the existing content of
                    // the page is used
                    content = doc.getContent();
                }
                return baseXWiki.getRenderingEngine().renderText(content, baseDocument, context);
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing Comment objects.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public List getComments(String token, String pageId) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getComments()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        List result = new ArrayList();

        /* Here we are interested only in the page id without any extended information */
        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                Vector<com.xpn.xwiki.api.Object> comments = doc.getComments();
                if (comments != null) {
                    for (com.xpn.xwiki.api.Object commentObject : comments) {
                        result.add(DomainObjectFactory.createComment(doc, commentObject)
                            .toRawMap());
                    }
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return result;
    }

    public Map getComment(String token, String commentId) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getComment()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(commentId);
        int commentNumericalId = Integer.parseInt(extendedId.getParameter("commentId"));

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to remove attachment. Document '%s' locked by '%s'", doc
                            .getName(), doc.getLockingUser()));
                }

                com.xpn.xwiki.api.Object commentObject =
                    doc.getObject("XWiki.XWikiComments", commentNumericalId);

                return DomainObjectFactory.createComment(doc, commentObject).toRawMap();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param commentMap A map representing a Comment object.
     * @return A map representing a Comment object with updated information.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public Map addComment(String token, Map commentMap) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called addComment()", user.getName()));

        Comment comment = new Comment((Map<String, Object>) commentMap);
        XWikiExtendedId extendedId = new XWikiExtendedId(comment.getPageId());

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                int id = doc.createNewObject("XWiki.XWikiComments");
                com.xpn.xwiki.api.Object commentObject = doc.getObject("XWiki.XWikiComments", id);
                commentObject.set("author", user.getName());
                commentObject.set("date", new Date());
                commentObject.set("comment", comment.getContent());

                doc.save();

                return DomainObjectFactory.createComment(doc, commentObject).toRawMap();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return True if the comment has been successfully removed.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public Boolean removeComment(String token, String commentId) throws XWikiException,
        XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called removeComment()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(commentId);
        int commentNumericalId = Integer.parseInt(extendedId.getParameter("commentId"));

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to remove attachment. Document '%s' locked by '%s'", doc
                            .getName(), doc.getLockingUser()));
                }

                com.xpn.xwiki.api.Object commentObject =
                    doc.getObject("XWiki.XWikiComments", commentNumericalId);
                doc.removeObject(commentObject);
                doc.save();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return true;
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing Attachment objects.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public List getAttachments(String token, String pageId) throws XWikiException,
        XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getAttachments()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        List result = new ArrayList();
        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                List<com.xpn.xwiki.api.Attachment> attachments = doc.getAttachmentList();
                for (com.xpn.xwiki.api.Attachment xwikiAttachment : attachments) {
                    result.add(DomainObjectFactory.createAttachment(xwikiAttachment).toRawMap());
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return result;
    }

    /**
     * @param token The authentication token.
     * @param contentId Ignored
     * @param attachment The Attachment object used to identify the page id, and attachment
     *            metadata.
     * @param attachmentData The actual attachment data.
     * @return An Attachment object describing the newly added attachment.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     * @category ConfluenceAPI
     */
    public Map addAttachment(String token, Integer contentId, Map attachmentMap,
        byte[] attachmentData) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called addAttachment()", user.getName()));

        Attachment attachment = new Attachment((Map) attachmentMap);
        XWikiExtendedId extendedId = new XWikiExtendedId(attachment.getPageId());

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());

            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to add attachment. Document locked by %s", doc.getLockingUser()));
                }

                /*
                 * Here we need to switch to the low-level API because the user's API support for
                 * attachment is not very well understandable.
                 */

                /*
                 * FIXME: CHECK THE FOLLOWING if it's ok!
                 */
                com.xpn.xwiki.XWiki baseXWiki = xwikiXmlRpcContext.getBaseXWiki();
                XWikiDocument xwikiDocument =
                    baseXWiki.getDocument(extendedId.getBasePageId(), xwikiXmlRpcContext
                        .getXWikiContext());

                XWikiAttachment xwikiBaseAttachment =
                    xwikiDocument.getAttachment(attachment.getFileName());
                if (xwikiBaseAttachment == null) {
                    xwikiBaseAttachment = new XWikiAttachment();
                    xwikiDocument.getAttachmentList().add(xwikiBaseAttachment);
                }
                xwikiBaseAttachment.setContent(attachmentData);
                xwikiBaseAttachment.setFilename(attachment.getFileName());
                xwikiBaseAttachment.setAuthor(user.getName());

                xwikiBaseAttachment.setDoc(xwikiDocument);
                xwikiDocument.saveAttachmentContent(xwikiBaseAttachment, xwikiXmlRpcContext
                    .getXWikiContext());

                xwikiXmlRpcContext.getBaseXWiki().saveDocument(xwikiDocument,
                    xwikiXmlRpcContext.getXWikiContext());

                com.xpn.xwiki.api.Attachment xwikiAttachment =
                    doc.getAttachment(attachment.getFileName());

                return DomainObjectFactory.createAttachment(xwikiAttachment).toRawMap();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param versionNumber (Ignored)
     * @return An array of bytes with the actual attachment content.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access it
     *             or the attachment with the given fileName does not exist on the given page.
     * @category ConfluenceAPI
     */
    public byte[] getAttachmentData(String token, String pageId, String fileName,
        String versionNumber) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getAttachmentData()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(fileName);
                if (xwikiAttachment != null) {
                    return xwikiAttachment.getContent();
                } else {
                    throw new XmlRpcException(String.format(
                        "Attachment '%s' does not exist on page '%s'", fileName, extendedId
                            .getBasePageId()));
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return True if the attachment has been removed.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access it
     *             or the attachment with the given fileName does not exist on the given page.
     * @category ConfluenceAPI
     */
    public Boolean removeAttachment(String token, String pageId, String fileName)
        throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called removeAttachment()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to remove attachment. Document '%s' locked by '%s'", doc
                            .getName(), doc.getLockingUser()));
                }

                com.xpn.xwiki.api.Attachment xwikiAttachment = doc.getAttachment(fileName);
                if (xwikiAttachment != null) {
                    /*
                     * Here we must use low-level XWiki API because there is no way for removing an
                     * attachment through the XWiki User's API. Basically we use the
                     * com.xpn.xwiki.XWiki object to re-do all the steps that we have already done
                     * so far by using the API. It is safe because if we are here, we know that
                     * everything exists and we have the proper rights to do things. So nothing
                     * should fail.
                     */
                    com.xpn.xwiki.XWiki baseXWiki = xwikiXmlRpcContext.getBaseXWiki();
                    XWikiContext xwikiContext = xwikiXmlRpcContext.getXWikiContext();
                    XWikiDocument baseXWikiDocument =
                        baseXWiki.getDocument(extendedId.getBasePageId(), xwikiContext);
                    XWikiAttachment baseXWikiAttachment =
                        baseXWikiDocument.getAttachment(fileName);
                    baseXWikiDocument.deleteAttachment(baseXWikiAttachment, xwikiContext);
                } else {
                    throw new XmlRpcException(String.format(
                        "Attachment '%s' does not exist on page '%s'", fileName, extendedId
                            .getBasePageId()));
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return true;
    }

    /**
     * @param token The authentication token.
     * @return A list of maps representing XWikiClass objects
     */
    public List getClasses(String token) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getClasses()", user.getName()));

        List result = new ArrayList();

        List<String> classNames = xwiki.getClassList();
        for (String className : classNames) {
            result.add(DomainObjectFactory.createXWikiClassSummary(className).toRawMap());
        }

        return result;
    }

    /**
     * @param token The authentication token.
     * @return A map representing a XWikiClass object.
     * @throws XmlRpcException If the given class does not exist.
     */
    public Map getClass(String token, String className) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getClass()", user.getName()));

        if (!xwiki.exists(className)) {
            throw new XmlRpcException(String.format("Class '%s' does not exist", className));
        }

        com.xpn.xwiki.api.Class userClass = xwiki.getClass(className);

        Map map = DomainObjectFactory.createXWikiClass(userClass).toRawMap();

        return map; // DomainObjectFactory.createXWikiClass(userClass).toRawMap();
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @return A list of maps representing XWikiObject objects.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     */
    public List getObjects(String token, String pageId) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getObjects()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());

            if (doc != null) {
                List result = new ArrayList();

                Map<String, Vector<com.xpn.xwiki.api.Object>> classToObjectsMap =
                    doc.getxWikiObjects();

                for (String className : classToObjectsMap.keySet()) {
                    Vector<com.xpn.xwiki.api.Object> objects = classToObjectsMap.get(className);
                    for (com.xpn.xwiki.api.Object object : objects) {
                        result.add(DomainObjectFactory.createXWikiObjectSummary(doc, object)
                            .toRawMap());
                    }
                }

                return result;
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Unable to get page %s", extendedId
                .getBasePageId()));
        }
    }

    /**
     * The getObject function will return an XWikiObject where only non-null properties are included
     * in the mapping 'field' -> 'value' In order to know all the available fields and their
     * respective types and attributes, clients should refer to the object's class.
     * 
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param className The class of the object.
     * @param id The id (number) of the object.
     * @return The XWikiObject containing the information about all the properties contained in the
     *         selected object.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access it
     *             or no object with the given id exist in the page.
     */
    public Map getObject(String token, String pageId, String className, Integer id)
        throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called getObject()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());

            if (doc != null) {
                XWikiObject xwikiObject = null;
                com.xpn.xwiki.api.Object object = doc.getObject(className, id);

                if (object != null) {
                    return DomainObjectFactory.createXWikiObject(doc, object).toRawMap();
                } else {
                    throw new XmlRpcException(String.format("Unable to find object id %d", id));
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Unable to get page %s", extendedId
                .getBasePageId()));
        }
    }

    /**
     * Update the object or create a new one if it doesn't exist.
     * 
     * @param token The authentication token.
     * @param objectMap A map representing the XWikiObject to be updated/created.
     * @return A map representing the XWikiObject with the updated information.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access
     *             it.
     */
    public Map storeObject(String token, Map objectMap) throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called storeObject()", user.getName()));

        XWikiObject object = new XWikiObject(objectMap);
        XWikiExtendedId extendedId = new XWikiExtendedId(object.getPageId());

        if (xwiki.exists(extendedId.getBasePageId())) {

            Document doc = xwiki.getDocument(extendedId.getBasePageId());

            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to store object. Document locked by %s", doc.getLockingUser()));
                }

                com.xpn.xwiki.api.Object xwikiObject =
                    doc.getObject(object.getClassName(), object.getId());

                /* If the object does not exist create it */
                if (xwikiObject == null) {
                    int id = doc.createNewObject(object.getClassName());
                    /* Get the newly created object for update */
                    xwikiObject = doc.getObject(object.getClassName(), id);
                }

                /*
                 * We iterate on the XWikiObject-passed-as-a-parameter's properties instead of the
                 * one retrieved through the API because, a newly created object has no properties,
                 * and they should be added via set. Apparently setting properties that do not
                 * belong to the object's class is harmless.
                 */
                for (String propertyName : object.getProperties()) {
                    /*
                     * Object values are always sent as strings (or arrays/maps of strings)... let
                     * the actual object perform the conversion
                     */

                    xwikiObject.set(propertyName, object.getProperty(propertyName));
                }

                doc.save();

                return DomainObjectFactory.createXWikiObject(doc, xwikiObject).toRawMap();
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Unable to get page %s", extendedId
                .getBasePageId()));
        }
    }

    /**
     * @param token The authentication token.
     * @param pageId The pageId in the 'Space.Page' format.
     * @param id The object's id.
     * @return True if the object has been successfully deleted.
     * @throws XmlRpcException If the page does not exist or the user has not the right to access it
     *             or no object with the given id exist in the page.
     */
    public Boolean removeObject(String token, String pageId, String className, Integer id)
        throws XWikiException, XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called removeObject()", user.getName()));

        XWikiExtendedId extendedId = new XWikiExtendedId(pageId);

        if (xwiki.exists(extendedId.getBasePageId())) {
            Document doc = xwiki.getDocument(extendedId.getBasePageId());
            if (doc != null) {
                if (doc.getLocked()) {
                    throw new XmlRpcException(String.format(
                        "Unable to remove attachment. Document '%s' locked by '%s'", doc
                            .getName(), doc.getLockingUser()));
                }

                com.xpn.xwiki.api.Object commentObject = doc.getObject(className, id);
                if (commentObject != null) {
                    doc.removeObject(commentObject);
                    doc.save();
                } else {
                    throw new XmlRpcException(String.format(
                        "Object %s[%d] on page '%s' does not exist", className, id, extendedId
                            .getBasePageId()));
                }
            } else {
                throw new XmlRpcException(String.format("Page '%s' cannot be accessed",
                    extendedId.getBasePageId()));
            }
        } else {
            throw new XmlRpcException(String.format("Page '%s' doesn't exist", extendedId
                .getBasePageId()));
        }

        return true;
    }

    /**
     * @param token The authentication token.
     * @param query The string to be looked for. If it is "__ALL_PAGES__" the search will return all
     *            the page ids available in the Wiki.
     * @return A list of SearchResults
     */
    public List search(String token, String query, int maxResults) throws XWikiException,
        XmlRpcException
    {
        XWikiXmlRpcContext xwikiXmlRpcContext = xwikiXmlRpcHttpRequestConfig.getXmlRpcContext();
        XWikiXmlRpcUser user = XWikiUtils.checkToken(token, xwikiXmlRpcContext.getXWikiContext());
        XWiki xwiki = xwikiXmlRpcContext.getXWiki();
        LOG.debug(String.format("User %s has called search()", user.getName()));

        com.xpn.xwiki.XWiki baseXWiki = xwikiXmlRpcContext.getBaseXWiki();

        List result = new ArrayList();
        if (query.equals("__ALL_PAGES__")) {
            List<String> spaceKeys = xwiki.getSpaces();
            for (String spaceKey : spaceKeys) {
                List<String> pageNames = xwiki.getSpaceDocsName(spaceKey);
                for (String pageName : pageNames) {
                    result.add(DomainObjectFactory.createSearchResult(
                        String.format("%s.%s", spaceKey, pageName)).toMap());
                }
            }
        } else {
            List<String> searchResults =
                baseXWiki.getStore().searchDocumentsNames(
                    "where doc.content like '%" + com.xpn.xwiki.web.Utils.SQLFilter(query)
                        + "%' or doc.name like '%" + com.xpn.xwiki.web.Utils.SQLFilter(query)
                        + "%'", xwikiXmlRpcContext.getXWikiContext());
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
}
