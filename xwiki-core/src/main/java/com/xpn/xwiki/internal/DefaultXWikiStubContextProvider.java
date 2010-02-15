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
package com.xpn.xwiki.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Default implementation of XWikiStubContextProvider.
 * 
 * @todo make DefaultXWikiStubContextProvider able to generate a stub context from scratch some way, it will need some
 *       refactor around XWiki class for this to be possible. The current limitation is that without a first request
 *       this provider is unusable.
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultXWikiStubContextProvider extends AbstractLogEnabled implements XWikiStubContextProvider
{
    /**
     * The initial stub XWikiContext.
     */
    private XWikiContext stubContext;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.XWikiStubContextProvider#initialize(com.xpn.xwiki.XWikiContext)
     */
    public void initialize(XWikiContext context)
    {
        this.stubContext = (XWikiContext) context.clone();

        // We are sure the context request is a real servlet request
        // So we force the dummy request with the current host
        XWikiServletRequestStub dummy = new XWikiServletRequestStub();
        if (context.getRequest() != null) {
            dummy.setHost(context.getRequest().getHeader("x-forwarded-host"));
            dummy.setScheme(context.getRequest().getScheme());
        }
        XWikiServletRequest request = new XWikiServletRequest(dummy);
        this.stubContext.setRequest(request);

        this.stubContext.setCacheDuration(0);

        this.stubContext.setUser(null);
        this.stubContext.setLanguage(null);
        this.stubContext.setDatabase(context.getMainXWiki());
        this.stubContext.setDoc(new XWikiDocument());

        this.stubContext.flushClassCache();
        this.stubContext.flushArchiveCache();

        getLogger().debug("Stub context initialized.");
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.util.XWikiStubContextProvider#createStubContext()
     */
    public XWikiContext createStubContext()
    {
        // TODO: we need to find a way to create a usable XWikiContext from scratch even if it will not contains
        // information related to the URL
        return this.stubContext == null ? null : (XWikiContext) this.stubContext.clone();
    }
}
