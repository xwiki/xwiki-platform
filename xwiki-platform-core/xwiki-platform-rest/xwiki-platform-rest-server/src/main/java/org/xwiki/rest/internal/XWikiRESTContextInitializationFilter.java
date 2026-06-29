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

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

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
    @Inject
    private WikiDescriptorManager wikis;

    private WikiDescriptorManager getWikiDescriptorManager(ServletContext context)
        throws WikiManagerException, ComponentLookupException
    {
        if (this.wikis == null) {
            ComponentManager componentManager =
                (ComponentManager) context.getAttribute(org.xwiki.component.manager.ComponentManager.class.getName());

            this.wikis = componentManager.getInstance(WikiDescriptorManager.class);
        }

        return this.wikis;
    }

    @Override
    protected void authenticate(XWikiContext context, HttpServletRequest request) throws XWikiException
    {
        // Before running the authentication, switch the the right context wiki (if the path if of the form
        // /wikis/{wikiid}[/*])
        String path = request.getPathInfo();
        if (path != null) {
            String wiki = extractWiki(path);
            if (wiki != null) {
                try {
                    if (getWikiDescriptorManager(request.getServletContext()).getById(wiki) != null) {
                        context.setWikiId(wiki);
                    }
                } catch (Exception e) {
                    throw new XWikiException("Failed to check if the wiki [" + wiki + "] exists", e);
                }
            }
        }

        super.authenticate(context, request);
    }

    private String extractWiki(String path)
    {
        String[] elements = StringUtils.split(path, '/');

        if (elements.length >= 2 && "wikis".equals(elements[0])) {
            return elements[1];
        }

        return null;
    }
}
