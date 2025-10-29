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
package org.xwiki.rest.internal;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.web.XWikiContextInitializationFilter;

/**
 * Filter in charge of initializing the XWiki context before the handling of a REST request.
 * 
 * @version $Id$
 * @since 19.10.0RC1
 */
public class XWikiRESTContextInitializationFilter extends XWikiContextInitializationFilter
{
    @Override
    protected void authenticate(XWikiContext context, HttpServletRequest request) throws XWikiException
    {
        // Before running the authentication, switch the the right context wiki (if the path if of the form
        // /wikis/{wikiid}[/*])
        String path = request.getPathInfo();
        if (path != null) {
            String wiki = extractWiki(path);
            if (wiki != null) {
                context.setWikiId(wiki);
            }
        }

        super.authenticate(context, request);
    }

    private String extractWiki(String path)
    {
        String[] elements = StringUtils.split(path, '/');

        if (elements.length >= 2 && elements[0].equals("wikis")) {
            return elements[1];
        }

        return null;
    }
}
