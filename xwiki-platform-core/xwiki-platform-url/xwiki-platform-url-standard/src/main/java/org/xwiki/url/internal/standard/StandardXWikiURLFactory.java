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
import org.xwiki.url.URLCreationException;
import org.xwiki.url.UnsupportedURLException;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.XWikiURL;
import org.xwiki.url.XWikiURLFactory;
import org.xwiki.url.internal.ExtendedURL;

import java.net.URL;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Parses a URL written in the "standard" format and generate a {@link XWikiURL} out of it. The "standard" format is
 * defined in {@link ExtendedURLXWikiEntityURLFactory}.
 *
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Named("standard")
@Singleton
public class StandardXWikiURLFactory implements XWikiURLFactory<URL, XWikiURL>
{
    /**
     * @see #createURL(java.net.URL, java.util.Map)
     */
    private static final String IGNORE_PREFIX_KEY = "ignorePrefix";

    /**
     * Used to parse a URL representing an Entity URL.
     */
    @Inject
    @Named("standard")
    private XWikiURLFactory<ExtendedURL, XWikiEntityURL> entityURLFactory;

    /**
     * Used to know if the wiki is in path-based configuration or not.
     */
    @Inject
    private StandardURLConfiguration configuration;

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
    public XWikiURL createURL(URL url, Map<String, Object> parameters)
        throws URLCreationException, UnsupportedURLException
    {
        XWikiURL xwikiURL;

        // Step 1: Use an Extended URL in to get access to the URL path segments.
        // Note that we also remove the passed ignore prefix from the segments if it has been specified.
        // The reason is because we need to ignore the Servlet Context if this code is called in a Servlet
        // environment and since the XWiki Application can be installed in the ROOT context, as well as in any Context
        // there's no way we can guess this, and thus it needs to be passed.
        String ignorePrefix = (String) parameters.get(IGNORE_PREFIX_KEY);
        ExtendedURL extendedURL = new ExtendedURL(url, ignorePrefix);

        // Step 2: Find out what type of URL we have and call the appropriate factories
        // At the moment we consider that all URL givent to this module are Entity URLs and that the filtering is done
        // by rules in the web.xml so that for example resources, skins or REST URLs do not reach here.
        xwikiURL = this.entityURLFactory.createURL(extendedURL, parameters);

        return xwikiURL;
    }
}
