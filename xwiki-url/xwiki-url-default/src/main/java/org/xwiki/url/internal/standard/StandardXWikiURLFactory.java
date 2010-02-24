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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.url.InvalidURLException;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.XWikiURLType;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
@Component("standard")
public class StandardXWikiURLFactory implements XWikiURLFactory<URL>
{
    /**
     * @see #createURL(java.net.URL, java.util.Map)
     */
    private static final String IGNORE_PREFIX_KEY = "ignorePrefix";

    @Requirement
    private StandardURLConfiguration configuration;

    @Requirement
    private HostResolver hostResolver;

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
    public XWikiURL createURL(URL url, Map<String, Object> parameters) throws InvalidURLException
    {
        XWikiURL xwikiURL;

        // Step 1: Remove the passed ignored prefix from the URL path.
        // Note that the reason is because we need to ignore the Servlet Context if this code is called in a Servlet
        // environment and since the XWiki Application can be installed in the ROOT context, as well as in any Context
        // there's no way we can guess this, and thus it needs to be passed.
        String ignorePrefix = (String) parameters.get(IGNORE_PREFIX_KEY);
        if (ignorePrefix != null && !url.getPath().startsWith(ignorePrefix)) {
            throw new InvalidURLException("URL Path doesn't start with [" + ignorePrefix + "]");
        }
        String path = url.getPath().substring(ignorePrefix.length());

        // Step 2: Extract all segment to make it easy to decide based on their values.
        URLParsingState state = new URLParsingState();
        state.urlSegments = new ArrayList<String>(Arrays.asList(StringUtils.split(path, '/')));

        // Step 3: Extract the wiki name.
        // The location of the wiki name depends on whether the wiki is configured to use domain-based multiwiki or
        // path-based multiwiki. If domain-based multiwiki then extract the wiki reference from the domain, otherwise
        // extract it from the path.
        WikiReference wikiReference = extractWikiReference(url, state);

        // Step 4: Extract the URL type and construct a XWiki URL of the corresponding type.
        // Note that the type could have been found already if the wiki was configured in path-based (in this case
        // the type is always ENTITY).
        if (state.urlType == null) {
            String typeAsString = state.urlSegments.remove(0);
            state.urlType = getXWikiURLType(typeAsString);
        }

        if (state.urlType == XWikiURLType.ENTITY) {
            xwikiURL = buildXWikiEntityURL(wikiReference, state.urlSegments);
        } else {
            throw new InvalidURLException("URL type [" + state.urlType + "] not supported yet!");
        }

        return xwikiURL;
    }

    // TODO: handle query string
    protected XWikiEntityURL buildXWikiEntityURL(WikiReference wikiReference, List<String> urlSegments)
        throws InvalidURLException
    {
        XWikiEntityURL entityURL;

        // Extract the action as the first segment, unless the view action is the default action.
        // TODO: handle default view action
        String action = urlSegments.get(0);

        String space = urlSegments.get(1);
        String page = urlSegments.get(2);

        DocumentReference documentReference = new DocumentReference(wikiReference.getName(),
            space, page);
        // TODO: Normalize with default resolver

        if (urlSegments.size() == 4) {
            String attachment = urlSegments.get(3);
            entityURL = new XWikiEntityURL(new AttachmentReference(attachment, documentReference));
        } else if (urlSegments.size() < 4) {
            entityURL = new XWikiEntityURL(documentReference);
        } else {
            throw new InvalidURLException("Invalid number of path separators");
        }

        entityURL.setAction(action);

        return entityURL;
    }

    protected WikiReference extractWikiReference(URL url, URLParsingState state)
    {
        String host = null;
        if (this.configuration.isPathBasedMultiWikiFormat()) {
            // If the first path element isn't the value of the wikiPathPrefix configuration value then we fall back
            // to the host name. This also allows the main wiki URL to be domain-based even for a path-based multiwiki.
            if (state.urlSegments.get(0).equalsIgnoreCase(this.configuration.getWikiPathPrefix())) {
                host = state.urlSegments.get(1);
                // Remove the first 2 segments so that when this method returns the remaining URL segments point to
                // the next meaningful item to extract, whether the wiki was domain-based or path-based. 
                state.urlSegments.remove(0);
                state.urlSegments.remove(0);
                // For path-based multiwiki the URL type is always ENTITY...
                state.urlType = XWikiURLType.ENTITY;
            }
        }
        if (host == null) {
            host = url.getHost();
        }
        return this.hostResolver.resolve(host);
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
