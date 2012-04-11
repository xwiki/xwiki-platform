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
package org.xwiki.url.internal.standard;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.InvalidURLException;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.XWikiURLType;
import org.xwiki.url.standard.HostResolver;
import org.xwiki.url.standard.StandardURLConfiguration;
import org.xwiki.url.standard.XWikiURLBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * There are 2 possibilities:
 * <ul>
 *   <li>Path-based multiwiki: {@code http://server/(ignorePrefix)/wiki/wikiname/type/action/space/page/attachment}</li>
 *   <li>Domain-based multiwiki: {@code http://server/(ignorePrefix)/type/action/space/page/attachment}</li>
 * </ul>
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("standard")
@Singleton
public class StandardXWikiURLFactory implements XWikiURLFactory<URL>
{
    /**
     * @see #createURL(java.net.URL, java.util.Map)
     */
    private static final String IGNORE_PREFIX_KEY = "ignorePrefix";

    @Inject
    private StandardURLConfiguration configuration;

    @Inject
    @Named("path")
    private HostResolver pathBasedHostResolver;

    @Inject
    @Named("domain")
    private HostResolver domainHostResolver;

    @Inject
    private ComponentManager componentManager;
    
    private class URLParsingState
    {
        public List<String> urlSegments;
        public XWikiURLType urlType; 
    }

    /**
     * {@inheritDoc}
     *
     * <p/>
     * Supported parameters:
     * <ul>
     *   <li>"ignorePrefix": the starting part of the URL Path (i.e. after the Authority part) to ignore. This is
     *       useful for example for passing the Web Application Context (for a web app) which should be ignored.
     *       Example: "/xwiki".</li> 
     * </ul>
     *
     * @see org.xwiki.url.XWikiURLFactory#createURL(Object, java.util.Map) 
     */
    @Override
    public XWikiURL createURL(URL url, Map<String, Object> parameters) throws InvalidURLException
    {
        XWikiURL xwikiURL;

        // Convert the URL to a URI since URI performs correctly decoding.
        // Note that this means that this method only accepts valid URLs (with proper encoding)
        URI uri;
        try {
            uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new InvalidURLException("Invalid URL [" + url + "]", e);
        }

        // Step 1: Remove the passed ignored prefix from the URL path.
        // Note that the reason is because we need to ignore the Servlet Context if this code is called in a Servlet
        // environment and since the XWiki Application can be installed in the ROOT context, as well as in any Context
        // there's no way we can guess this, and thus it needs to be passed.
        String ignorePrefix = (String) parameters.get(IGNORE_PREFIX_KEY);
        if (ignorePrefix != null && !uri.getPath().startsWith(ignorePrefix)) {
            throw new InvalidURLException("URL Path doesn't start with [" + ignorePrefix + "]");
        }
        // Note: We also remove the leading "/" after the ignored prefix.
        String path = uri.getRawPath().substring(ignorePrefix.length() + 1);

        // Step 2: Extract all segment to make it easy to decide based on their values.
        URLParsingState state = new URLParsingState();
        state.urlSegments = extractPathSegments(path);

        // Step 3: Extract the wiki name.
        // The location of the wiki name depends on whether the wiki is configured to use domain-based multiwiki or
        // path-based multiwiki. If domain-based multiwiki then extract the wiki reference from the domain, otherwise
        // extract it from the path.
        WikiReference wikiReference = extractWikiReference(uri, state);

        // Step 4: Extract the URL type and construct a XWiki URL of the corresponding type.
        // Note that the type could have been found already if the wiki was configured in path-based (in this case
        // the type is always ENTITY).
        if (state.urlType == null) {
            String typeAsString = state.urlSegments.remove(0);
            state.urlType = getXWikiURLType(typeAsString);
        }

        // Try to find a builder component matching the XWiki URL type and build the XWiki URL.
        XWikiURLBuilder builder;
        try {
            builder = this.componentManager.getInstance(XWikiURLBuilder.class, state.urlType.toString().toLowerCase());
        } catch (ComponentLookupException e) {
            throw new InvalidURLException("URL type [" + state.urlType + "] not supported yet!");
        }
        xwikiURL = builder.build(wikiReference, state.urlSegments);

        // Add the Query string parameters
        // TODO: Add special support for revisions and language since those are in XWikiEntityURL.
        if (uri.getQuery() != null) {
            for (String nameValue : Arrays.asList(uri.getQuery().split("&"))) {
                String[] pair = nameValue.split("=", 2);
                // Check if the parameter has a value or not.
                if (pair.length == 2) {
                    xwikiURL.addParameter(pair[0], pair[1]);
                } else {
                    xwikiURL.addParameter(pair[0], null);
                }
            }
        }
        
        return xwikiURL;
    }

    /**
     * Extract segments between "/" characters in the passed path. Also remove any path parameters (i.e. content
     * after ";" in a path segment; for ex ";jsessionid=...") since we don't want to have these params in the
     * segments we return and act on (otherwise we would get them in document names for example).
     *
     * Note that we only remove ";" characters when they are not URL-encoded. We want to allow the ";" character to be
     * in document names for example.
     */
    protected List<String> extractPathSegments(String rawPath)
    {
        List<String> urlSegments = new ArrayList<String>();

        // Note that we use -1 in the call below in order to get empty segments too. This is needed since in our
        // URL format "bin/view/Page" represents a Page while "bin/view/Space/" represents a Space.
        for (String pathSegment : rawPath.split("/", -1)) {

            // Remove path parameters
            String normalizedPathSegment = pathSegment.split(";", 2)[0];

            // Now let's decode it
            String decodedPathSegment;
            try {
                // Note: we decode using UTF-8 since the URI javadoc says:
                // "A sequence of escaped octets is decoded by replacing it with the sequence of characters that it
                // represents in the UTF-8 character set. UTF-8 contains US-ASCII, hence decoding has the effect of
                // de-quoting any quoted US-ASCII characters as well as that of decoding any encoded non-US-ASCII
                // characters."
                decodedPathSegment = URLDecoder.decode(normalizedPathSegment, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                // without that encoding.
                throw new RuntimeException("Failed to URL decode [" + normalizedPathSegment + "] using UTF-8.", e);
            }

            urlSegments.add(decodedPathSegment);
        }

        return urlSegments;
    }

    /**
     * Extract the name of the wiki the URL is pointing to.
     *
     * For domain-based multiwiki setups we ask a resolver to resolve the URL's host name.
     * For path-based multiwiki setup we get the path segment after the first segment, if this first segment has the
     * predefined {@link org.xwiki.url.standard.StandardURLConfiguration#getWikiPathPrefix()} value. If not then we
     * fall-back to domain-based multiwiki setups and resolve with the URL's host name.
     *
     * @return the wiki the URL is pointing to, returned as a {@link WikiReference}.
     */
    protected WikiReference extractWikiReference(URI uri, URLParsingState state)
    {
        WikiReference wikiReference = null;
        if (this.configuration.isPathBasedMultiWiki()) {
            // If the first path element isn't the value of the wikiPathPrefix configuration value then we fall back
            // to the host name. This also allows the main wiki URL to be domain-based even for a path-based multiwiki.
            if (state.urlSegments.get(0).equalsIgnoreCase(this.configuration.getWikiPathPrefix())) {
                wikiReference = this.pathBasedHostResolver.resolve(state.urlSegments.get(1));
                // Remove the first 2 segments so that when this method returns the remaining URL segments point to
                // the next meaningful item to extract, whether the wiki was domain-based or path-based. 
                state.urlSegments.remove(0);
                state.urlSegments.remove(0);
                // For path-based multiwiki the URL type is always ENTITY...
                state.urlType = XWikiURLType.ENTITY;
            }
        }
        if (wikiReference == null) {
            wikiReference = this.domainHostResolver.resolve(uri.getHost());
        }
        return wikiReference;
    }

    protected XWikiURLType getXWikiURLType(String type) throws InvalidURLException
    {
        XWikiURLType result;
        if (type.equalsIgnoreCase("bin")) {
            result = XWikiURLType.ENTITY;
        } else {
            throw new InvalidURLException("Invalid URL type [" + type + "]");
        }
        return result;
    }
}
