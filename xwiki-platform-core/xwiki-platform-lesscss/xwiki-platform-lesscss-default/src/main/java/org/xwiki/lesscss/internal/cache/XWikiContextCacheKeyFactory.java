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
package org.xwiki.lesscss.internal.cache;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Serialize to a string (a cache key) the current XWikiContext object with a selection of fields that the LESS cache 
 * must handle.
 *  
 * @version $Id$
 */
@Component(roles = XWikiContextCacheKeyFactory.class)
@Singleton
public class XWikiContextCacheKeyFactory
{     
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * @return the cache key corresponding to the current XWikiContext state
     */
    public String getCacheKey()
    {
        XWikiContext xcontext = xcontextProvider.get();
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        
        // We serialize the class name of the current URLFactory.
        // Ex: - during HTML export, ExportURLFactory is used.
        //     - for the standard 'view' action, XWikiDefaultURLFactory is used
        //     - ...
        String urlFactoryName = urlFactory.getClass().getName();
        
        // We generate a fake URL with the current URL factory so that we take care of the internal state of that object
        // in our cache key.
        // Ex: - if the request comes from a file located in subdirectory, the generated URL will be:
        //       '../style.css'
        //     - if the request comes form a file located in a deeper subdirectory, the generated URL will be:
        //       '../../style.css'
        // It is clear that we cannot cache the same results from a request coming from a subdirectory or an other, but
        // we have no API to get the internal state of the URL Factory. So we use this 'trick' to handle it.
        URL urlFactoryGeneratedURL = urlFactory.createSkinURL("style.css", "skin", xcontext);
        
        return String.format("XWikiContext[URLFactory[%s, %s]]", urlFactoryName, urlFactoryGeneratedURL.toString());
    }
}
