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
package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiServletURLFactory extends XWikiDefaultURLFactory
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiServletURLFactory.class);

    private EntityReferenceResolver<String> relativeEntityReferenceResolver;

    private EntityResourceActionLister actionLister;

    protected URL originalURL;

    protected String defaultURL;

    /**
     * This is the URL which was requested by the user possibly with the host modified if x-forwarded-host header is set
     * or if xwiki.home parameter is set and we are viewing the main page.
     */
    protected Map<String, URL> defaultURLs;

    protected String contextPath;

    public XWikiServletURLFactory()
    {
    }

    // Used by tests
    public XWikiServletURLFactory(URL defaultURL, String contextPath, String actionPath)
    {
        this.contextPath = contextPath;
        this.originalURL = defaultURL;
    }

    /**
     * Creates a new URL factory that uses the server URL and context path specified by the given XWiki context. This
     * constructor should be used only in tests. Make sure {@link XWikiContext#setURL(URL)} is called before this
     * constructor.
     *
     * @param context
     */
    public XWikiServletURLFactory(XWikiContext context)
    {
        init(context);
    }

    @Override
    public void init(XWikiContext context)
    {
        this.defaultURLs = null;

        this.contextPath = context.getWiki().getWebAppPath(context);

        URL homepageConfigration = getXWikiHomeParameter(context);

        // Set the configured home URL for the main wiki
        setDefaultURL(null, homepageConfigration);

        // Remember initial request base URL for path for last resort
        if (homepageConfigration != null && context.isMainWiki()) {
            // If the main wiki base URL is forced in the configuration use it
            this.originalURL = homepageConfigration;
        } else {
            // Remember the request base URL for last resort
            this.originalURL = HttpServletUtils.getSourceBaseURL(context.getRequest());

            // If protocol is forced in the configuration witch to it
            String protocolConfiguration = context.getWiki().Param("xwiki.url.protocol");
            if (StringUtils.isNoneEmpty(protocolConfiguration)) {
                try {
                    this.defaultURL =
                        new URL(protocolConfiguration, this.originalURL.getHost(), this.originalURL.getPort(), "")
                            .toString();
                } catch (MalformedURLException e) {
                    LOGGER.warn("The configured protocol [{}] produce an invalid URL: {}", protocolConfiguration,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }

        // Only take into account initial request if it's meant to be
        XWikiRequest request = context.getRequest();
        if (!(request.getHttpServletRequest() instanceof XWikiServletRequestStub)
            || !((XWikiServletRequestStub) request.getHttpServletRequest()).isDaemon()) {
            this.defaultURL = this.originalURL.toString();
            setDefaultURL(context.getOriginalWikiId(), this.originalURL);
        }
    }

    /**
     * @param wikiId the wiki identifier to associate with this URL
     * @param baseURL the input URL to take into account, null to disable it
     * @since 10.3
     */
    public void setDefaultURL(String wikiId, URL baseURL)
    {
        if (this.defaultURLs == null) {
            this.defaultURLs = new HashMap<>();
        }

        this.defaultURLs.put(wikiId, baseURL);
    }

    protected URL getDefaultURL(String wikiId, XWikiContext xcontext)
    {
        if (this.defaultURLs == null) {
            return this.originalURL;
        }

        URL url = this.defaultURLs.get(wikiId);

        if (url != null) {
            return url;
        }

        // Main wiki can be associated to null
        if (xcontext.isMainWiki(wikiId)) {
            return this.defaultURLs.get(null);
        }

        return null;
    }

    /**
     * Returns the part of the URL identifying the web application. In a normal install, that is <tt>xwiki/</tt>.
     *
     * @return The configured context path.
     */
    public String getContextPath()
    {
        return this.contextPath;
    }

    /**
     * @return a URL made from the xwiki.cfg parameter xwiki.home or null if undefined or unparsable.
     */
    private static URL getXWikiHomeParameter(final XWikiContext context)
    {
        final String surl = getXWikiHomeParameterAsString(context);
        if (StringUtils.isNotEmpty(surl)) {
            try {
                return normalizeURL(surl, context);
            } catch (MalformedURLException e) {
                LOGGER.warn("Could not create URL from xwiki.cfg xwiki.home parameter: {}. Ignoring parameter.", surl);
            }
        }

        return null;
    }

    /**
     * @return the xwiki.home parameter or null if undefined.
     */
    private static String getXWikiHomeParameterAsString(final XWikiContext context)
    {
        return context.getWiki().Param("xwiki.home", null);
    }

    @Override
    public URL getServerURL(XWikiContext context) throws MalformedURLException
    {
        return getServerURL(context.getWikiId(), context);
    }

    /**
     * Get the url of the server EG: http://www.xwiki.org/ This function sometimes will return a URL with a trailing /
     * and other times not. This is because the xwiki.home param is recommended to have a trailing / but this.serverURL
     * never does.
     *
     * @param wikiId the identifier of the wiki, if null it is assumed to be the same as the wiki which we are currently
     *            displaying.
     * @param context the XWikiContext used to determine the current wiki and the value if the xwiki.home parameter if
     *            needed as well as access the xwiki server document if in virtual mode.
     * @return a URL containing the protocol, host, and port (if applicable) of the server to use for the given
     *         database.
     */
    public URL getServerURL(String wikiId, XWikiContext context) throws MalformedURLException
    {
        if (wikiId == null) {
            wikiId = context.getWikiId();
        }

        URL inputURL = getDefaultURL(wikiId, context);
        if (inputURL != null) {
            // This is the case if we are getting a URL for a page which is in
            // the same wiki as the page which is now being displayed.
            return inputURL;
        }

        URL url = context.getWiki().getServerURL(wikiId, context);
        if (url != null) {
            return url;
        }

        // Fallback on initial request base URL
        return this.originalURL;
    }

    @Override
    public URL createURL(String spaces, String name, String action, boolean redirect, XWikiContext context)
    {
        return createURL(spaces, name, action, context);
    }

    @Override
    public URL createURL(String spaces, String name, String action, String querystring, String anchor, String xwikidb,
        XWikiContext context)
    {
        // Action and Query String transformers
        if (("view".equals(action)) && (context.getLinksAction() != null)) {
            action = context.getLinksAction();
        }
        if (context.getLinksQueryString() != null) {
            if (querystring == null) {
                querystring = context.getLinksQueryString();
            } else {
                querystring = querystring + "&" + context.getLinksQueryString();
            }
        }

        StringBuilder path = new StringBuilder(this.contextPath);
        addServletPath(path, xwikidb, context);

        // Parse the spaces list into Space References
        EntityReference spaceReference = getRelativeEntityReferenceResolver().resolve(spaces, EntityType.SPACE);

        // For how to encode the various parts of the URL, see http://stackoverflow.com/a/29948396/153102
        addAction(path, spaceReference, action, context);
        addSpaces(path, spaceReference);
        addName(path, name, action, context);

        if (!StringUtils.isEmpty(querystring)) {
            path.append("?");
            path.append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
        }

        if (!StringUtils.isEmpty(anchor)) {
            path.append("#");
            path.append(encodeWithinQuery(anchor));
        }

        URL result;
        try {
            result = normalizeURL(new URL(getServerURL(xwikidb, context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            result = null;
        }

        return result;
    }

    private void addServletPath(StringBuilder path, String xwikidb, XWikiContext context)
    {
        if (xwikidb == null) {
            xwikidb = context.getWikiId();
        }

        path.append(context.getWiki().getServletPath(xwikidb, context));
    }

    private void addAction(StringBuilder path, EntityReference spaceReference, String action, XWikiContext context)
    {
        boolean showViewAction = context.getWiki().showViewAction(context);

        // - Always output the action if it's not "view" or if showViewAction is true
        // - Output "view/<first space name>" when the first space name is an action name and the action is View
        // (and showViewAction = false)
        if ((!"view".equals(action) || showViewAction)
            || (spaceReference != null && "view".equals(action) && getActionLister().listActions()
                .contains(spaceReference.extractFirstReference(EntityType.SPACE).getName()))) {
            path.append(action).append("/");
        }
    }

    /**
     * Add the spaces to the path.
     */
    private void addSpaces(StringBuilder path, EntityReference spaceReference)
    {
        for (EntityReference reference : spaceReference.getReversedReferenceChain()) {
            appendSpacePathSegment(path, reference);
        }
    }

    private void appendSpacePathSegment(StringBuilder path, EntityReference spaceReference)
    {
        path.append(encodeWithinPath(spaceReference.getName())).append('/');
    }

    /**
     * Add the page name to the path.
     */
    private void addName(StringBuilder path, String name, String action, XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        if ((xwiki.useDefaultAction(context))
            || (!name.equals(xwiki.getDefaultPage(context)) || (!"view".equals(action)))) {
            path.append(encodeWithinPath(name));
        }
    }

    protected void addFileName(StringBuilder path, String fileName, XWikiContext context)
    {
        addFileName(path, fileName, true, context);
    }

    protected void addFileName(StringBuilder path, String fileName, boolean encode, XWikiContext context)
    {
        path.append("/");
        if (encode) {
            // Encode the given file name as a single path segment.
            path.append(encodeWithinPath(fileName).replace("+", "%20"));
        } else {
            try {
                // The given file name is actually a file path and so we need to encode each path segment separately.
                path.append(new URI(null, null, fileName, null));
            } catch (URISyntaxException e) {
                LOGGER.debug("Failed to encode the file path [{}]. Root cause: [{}]", fileName,
                    ExceptionUtils.getRootCauseMessage(e));
                // Use the raw file path as a fall-back.
                path.append(fileName);
            }
        }
    }

    /**
     * Encode a URL path following the URL specification so that space is encoded as {@code %20} in the path (and not as
     * {@code +} wnich is not correct). Note that for all other characters we encode them even though some don't need to
     * be encoded. For example we encode the single quote even though it's not necessary (see
     * <a href="http://tinyurl.com/j6bjgaq">this explanation</a>). The reason is that otherwise it becomes dangerous to
     * use a returned URL in the HREF attribute in HTML. Imagine the following {@code <a href='$url'...} and
     * {@code #set ($url = $doc.getURL(...))}. Now let's assume that {@code $url}'s value is
     * {@code http://localhost:8080/xwiki/bin/view/A'/B}. This would generate a HTML of
     * {@code <a href='http://localhost:8080/xwiki/bin/view/A'/B'} which would generated a wrong link to
     * {@code http://localhost:8080/xwiki/bin/view/A}... Thus if we were only encoding the characters that require
     * encoding, we would need HMTL writers to encode the received URL and right now we don't do that anywhere in our
     * code. Thus in order to not introduce any problem and keep it safe we just handle the {@code +} character
     * specially and encode the rest.
     *
     * @param name the path to encode
     * @return the URL-encoded path segment
     */
    private String encodeWithinPath(String name)
    {
        // Note: Ideally the following would have been the correct way of writing this method but it causes the issues
        // mentioned in the javadoc of this method
        // String encodedName;
        // try {
        // encodedName = URIUtil.encodeWithinPath(name, "UTF-8");
        // } catch (URIException e) {
        // throw new RuntimeException("Missing charset [UTF-8]", e);
        // }
        // return encodedName;

        String encodedName;
        try {
            encodedName = URLEncoder.encode(name, "UTF-8");
        } catch (Exception e) {
            // Should not happen (UTF-8 is always available)
            throw new RuntimeException("Missing charset [UTF-8]", e);
        }

        // The previous call will convert " " into "+" (and "+" into "%2B") so we need to convert "+" into "%20"
        encodedName = encodedName.replaceAll("\\+", "%20");

        return encodedName;
    }

    /**
     * Same rationale as {@link #encodeWithinPath(String, XWikiContext)}. Note that we also encode spaces as {@code %20}
     * even though we could also have encoded them as {@code +}. We do this for consistency (it allows to have the same
     * implementation for both URL paths and query string).
     *
     * @param name the query string part to encode
     * @return the URL-encoded query string part
     */
    private String encodeWithinQuery(String name)
    {
        // Note: Ideally the following would have been the correct way of writing this method but it causes the issues
        // mentioned in the javadoc of this method
        // String encodedName;
        // try {
        // encodedName = URIUtil.encodeWithinQuery(name, "UTF-8");
        // } catch (URIException e) {
        // throw new RuntimeException("Missing charset [UTF-8]", e);
        // }
        // return encodedName;

        return encodeWithinPath(name);
    }

    @Override
    public URL createExternalURL(String spaces, String name, String action, String querystring, String anchor,
        String xwikidb, XWikiContext context)
    {
        return this.createURL(spaces, name, action, querystring, anchor, xwikidb, context);
    }

    @Override
    public URL createSkinURL(String filename, String skin, XWikiContext context)
    {
        StringBuilder path = new StringBuilder(this.contextPath);
        path.append("skins/");
        path.append(skin);
        addFileName(path, filename, false, context);
        try {
            return normalizeURL(new URL(getServerURL(context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createSkinURL(String filename, String spaces, String name, String xwikidb, XWikiContext context)
    {
        StringBuilder path = new StringBuilder(this.contextPath);
        addServletPath(path, xwikidb, context);

        // Parse the spaces list into Space References
        EntityReference spaceReference = getRelativeEntityReferenceResolver().resolve(spaces, EntityType.SPACE);

        addAction(path, null, "skin", context);
        addSpaces(path, spaceReference);
        addName(path, name, "skin", context);
        addFileName(path, filename, false, context);
        try {
            return normalizeURL(new URL(getServerURL(xwikidb, context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createResourceURL(String filename, boolean forceSkinAction, XWikiContext context)
    {
        StringBuilder path = new StringBuilder(this.contextPath);
        if (forceSkinAction) {
            addServletPath(path, context.getWikiId(), context);
            addAction(path, null, "skin", context);
        }
        path.append("resources");
        addFileName(path, filename, false, context);
        try {
            return normalizeURL(new URL(getServerURL(context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    public URL createTemplateURL(String fileName, XWikiContext context)
    {
        StringBuilder path = new StringBuilder(this.contextPath);
        path.append("templates");
        addFileName(path, fileName, false, context);
        try {
            return normalizeURL(new URL(getServerURL(context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    @Override
    public URL createAttachmentURL(String filename, String spaces, String name, String action, String querystring,
        String xwikidb, XWikiContext context)
    {
        if ((context != null) && "viewrev".equals(context.getAction()) && context.get("rev") != null
            && isContextDoc(xwikidb, spaces, name, context)) {
            try {
                String docRevision = context.get("rev").toString();
                XWikiAttachment attachment =
                    findAttachmentForDocRevision(context.getDoc(), docRevision, filename, context);
                if (attachment == null) {
                    action = "viewattachrev";
                } else {
                    long arbId = findDeletedAttachmentForDocRevision(context.getDoc(), docRevision, filename, context);
                    return createAttachmentRevisionURL(filename, spaces, name, attachment.getVersion(), arbId,
                        querystring, xwikidb, context);
                }
            } catch (XWikiException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exception while trying to get attachment version !", e);
                }
            }
        }

        StringBuilder path = new StringBuilder(this.contextPath);
        addServletPath(path, xwikidb, context);

        // Parse the spaces list into Space References
        EntityReference spaceReference = getRelativeEntityReferenceResolver().resolve(spaces, EntityType.SPACE);

        addAction(path, spaceReference, action, context);
        addSpaces(path, spaceReference);
        addName(path, name, action, context);
        addFileName(path, filename, context);

        if (!StringUtils.isEmpty(querystring)) {
            path.append("?");
            path.append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
        }

        try {
            return normalizeURL(new URL(getServerURL(xwikidb, context), path.toString()), context);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if a document is the original context document. This is needed when generating attachment revision URLs,
     * since only attachments of the context document should also be versioned.
     *
     * @param wiki the wiki name of the document to check
     * @param spaces the space names of the document to check
     * @param name the document name of the document to check
     * @param context the current request context
     * @return {@code true} if the provided document is the same as the current context document, {@code false}
     *         otherwise
     */
    protected boolean isContextDoc(String wiki, String spaces, String name, XWikiContext context)
    {
        if (context == null || context.getDoc() == null) {
            return false;
        }

        // Use the local serializer so that we don't serialize the wiki part since all we want to do is compare the
        // passed spaces represented as a String with the current doc's spaces.
        EntityReferenceSerializer<String> serializer =
            Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        DocumentReference currentDocumentReference = context.getDoc().getDocumentReference();
        return serializer.serialize(currentDocumentReference.getLastSpaceReference()).equals(spaces)
            && currentDocumentReference.getName().equals(name)
            && (wiki == null || currentDocumentReference.getWikiReference().getName().equals(wiki));
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision,
        String querystring, String xwikidb, XWikiContext context)
    {
        return createAttachmentRevisionURL(filename, spaces, name, revision, -1, querystring, xwikidb, context);
    }

    public URL createAttachmentRevisionURL(String filename, String spaces, String name, String revision, long recycleId,
        String querystring, String xwikidb, XWikiContext context)
    {
        String action = "downloadrev";
        StringBuilder path = new StringBuilder(this.contextPath);
        addServletPath(path, xwikidb, context);

        // Parse the spaces list into Space References
        EntityReference spaceReference = getRelativeEntityReferenceResolver().resolve(spaces, EntityType.SPACE);

        addAction(path, spaceReference, action, context);
        addSpaces(path, spaceReference);
        addName(path, name, action, context);
        addFileName(path, filename, context);

        String qstring = "rev=" + revision;
        if (recycleId >= 0) {
            qstring += "&rid=" + recycleId;
        }
        if (!StringUtils.isEmpty(querystring)) {
            qstring += "&" + querystring;
        }
        path.append("?");
        path.append(StringUtils.removeEnd(StringUtils.removeEnd(qstring, "&"), "&amp;"));

        try {
            return normalizeURL(new URL(getServerURL(xwikidb, context), path.toString()), context);
        } catch (MalformedURLException e) {
            // This should not happen
            return null;
        }
    }

    /**
     * Converts a URL to a relative URL if it's a XWiki URL (keeping only the path + query string + anchor) and leave
     * the URL unchanged if it's an external URL.
     * <p>
     * An URL is considered to be external if its server component doesn't match the server of the current request URL.
     * This means that URLs are made relative with respect to the current request URL rather than the current wiki set
     * on the XWiki context. Let's take an example:
     *
     * <pre>
     * {@code
     * request URL: http://playground.xwiki.org/xwiki/bin/view/Sandbox/TestURL
     * current wiki: code (code.xwiki.org)
     * URL (1): http://code.xwiki.org/xwiki/bin/view/Main/WebHome
     * URL (2): http://playground.xwiki.org/xwiki/bin/view/Spage/Page
     * 
     * The result will be:
     * (1) http://code.xwiki.org/xwiki/bin/view/Main/WebHome
     * (2) /xwiki/bin/view/Spage/Page
     * }
     * </pre>
     *
     * @param url the URL to convert
     * @return the converted URL as a string
     * @see com.xpn.xwiki.web.XWikiDefaultURLFactory#getURL(java.net.URL, com.xpn.xwiki.XWikiContext)
     */
    @Override
    public String getURL(URL url, XWikiContext context)
    {
        String relativeURL = "";

        try {
            if (url != null) {
                String surl = url.toString();

                if (this.defaultURL == null || !surl.startsWith(this.defaultURL)) {
                    // External URL: leave it as is.
                    relativeURL = surl;
                } else {
                    // Internal XWiki URL: convert to relative.
                    StringBuilder relativeURLBuilder = new StringBuilder(url.getPath());
                    String querystring = url.getQuery();
                    if (!StringUtils.isEmpty(querystring)) {
                        relativeURLBuilder.append("?")
                            .append(StringUtils.removeEnd(StringUtils.removeEnd(querystring, "&"), "&amp;"));
                    }

                    String anchor = url.getRef();
                    if (!StringUtils.isEmpty(anchor)) {
                        relativeURLBuilder.append("#").append(anchor);
                    }

                    relativeURL = relativeURLBuilder.toString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to create URL", e);
        }

        return StringUtils.defaultIfEmpty(relativeURL, "/");
    }

    @Override
    public URL getRequestURL(XWikiContext context)
    {
        final URL url = super.getRequestURL(context);

        try {
            final URL servurl = getServerURL(context);
            // if use apache mod_proxy we needed to know external host address
            return normalizeURL(new URL(servurl.getProtocol(), servurl.getHost(), servurl.getPort(), url.getFile()),
                context);
        } catch (MalformedURLException e) {
            // This should not happen
            LOGGER.error("Failed to create request URL", e);
        }

        return url;
    }

    public XWikiAttachment findAttachmentForDocRevision(XWikiDocument doc, String docRevision, String filename,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachment attachment = null;
        XWikiDocument rdoc = context.getWiki().getDocument(doc, docRevision, context);
        if (filename != null) {
            attachment = rdoc.getAttachment(filename);
        }

        return attachment;
    }

    public long findDeletedAttachmentForDocRevision(XWikiDocument doc, String docRevision, String filename,
        XWikiContext context) throws XWikiException
    {
        XWikiAttachment attachment = null;
        XWikiDocument rdoc = context.getWiki().getDocument(doc, docRevision, context);
        if (context.getWiki().hasAttachmentRecycleBin(context) && filename != null) {
            attachment = rdoc.getAttachment(filename);
            if (attachment != null) {
                List<DeletedAttachment> deleted = context.getWiki().getAttachmentRecycleBinStore()
                    .getAllDeletedAttachments(attachment, context, true);
                Collections.reverse(deleted);
                for (DeletedAttachment entry : deleted) {
                    if (entry.getDate().after(rdoc.getDate())) {
                        return entry.getId();
                    }
                }
            }
        }

        return -1;
    }

    /**
     * Encodes the passed URL and offers the possibility for Servlet Filter to perform URL rewriting (this is used for
     * example by Tuckey's URLRewriteFilter for rewriting outbound URLs, see
     * http://platform.xwiki.org/xwiki/bin/view/Main/ShortURLs).
     * <p>
     * However Servlet Container will also add a ";jsessionid=xxx" content to the URL while encoding the URL and we
     * strip it since we don't want to have that in our URLs as it can cause issues with:
     * <ul>
     * <li>security</li>
     * <li>SEO</li>
     * <li>clients not expecting jsessionid in URL, for example RSS feed readers which will think that articles are
     * different as they'll get different URLs everytime they call the XWiki server</li>
     * </ul>
     * See why jsessionid are considered harmful
     * <a href="https://randomcoder.org/articles/jsessionid-considered-harmful">here</a> and
     * <a href="http://java.dzone.com/articles/java-jsessionid-harmful">here</a>
     *
     * @param url the URL to encode and normalize
     * @param context the XWiki Context used to get access to the Response for encoding the URL
     * @return the normalized URL
     * @throws MalformedURLException if the passed URL is invalid
     */
    protected static URL normalizeURL(URL url, XWikiContext context) throws MalformedURLException
    {
        return normalizeURL(url.toExternalForm(), context);
    }

    /**
     * Encodes the passed URL and offers the possibility for Servlet Filter to perform URL rewriting (this is used for
     * example by Tuckey's URLRewriteFilter for rewriting outbound URLs, see
     * http://platform.xwiki.org/xwiki/bin/view/Main/ShortURLs).
     * <p>
     * However Servlet Container will also add a ";jsessionid=xxx" content to the URL while encoding the URL and we
     * strip it since we don't want to have that in our URLs as it can cause issues with:
     * <ul>
     * <li>security</li>
     * <li>SEO</li>
     * <li>clients not expecting jsessionid in URL, for example RSS feed readers which will think that articles are
     * different as they'll get different URLs everytime they call the XWiki server</li>
     * </ul>
     * See why jsessionid are considered harmful
     * <a href="https://randomcoder.org/articles/jsessionid-considered-harmful">here</a> and
     * <a href="http://java.dzone.com/articles/java-jsessionid-harmful">here</a>
     *
     * @param url the URL to encode and normalize
     * @param context the XWiki Context used to get access to the Response for encoding the URL
     * @return the normalized URL
     * @throws MalformedURLException if the passed URL is invalid
     */
    protected static URL normalizeURL(String url, XWikiContext context) throws MalformedURLException
    {
        // For robust session tracking, all URLs emitted by a servlet should be encoded. Otherwise, URL rewriting
        // cannot be used with browsers which do not support cookies.
        String encodedURLAsString = context.getResponse().encodeURL(url);

        // Remove a potential jsessionid in the URL
        encodedURLAsString = encodedURLAsString.replaceAll(";jsessionid=.*?(?=\\?|$)", "");

        return new URL(encodedURLAsString);
    }

    private EntityReferenceResolver<String> getRelativeEntityReferenceResolver()
    {
        if (this.relativeEntityReferenceResolver == null) {
            this.relativeEntityReferenceResolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");
        }
        return this.relativeEntityReferenceResolver;
    }

    private EntityResourceActionLister getActionLister()
    {
        if (this.actionLister == null) {
            this.actionLister = Utils.getComponent(EntityResourceActionLister.class);
        }
        return this.actionLister;
    }
}
