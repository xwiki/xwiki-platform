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
package org.xwiki.webjars.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * Make it easy to use WebJars in scripts. For example it can compute an XWiki WebJars URL.
 *
 * @version $Id$
 * @since 6.0M1
 */
@Component
@Named("webjars")
@Singleton
public class WebJarsScriptService implements ScriptService
{
    /**
     * @todo finish implementing URL serialization in the URL module to avoid this dependency on oldcore.
     */
    @Inject
    private Execution execution;

    /**
     * Compute an XWiki WebJAR URL
     * (of the form {@code http://server/bin/webjars/resource/path?value=(resource name)}.
     *
     * @param resourceName the resource asked (eg {@code angular/2.1.11/angular.js"})
     * @return the computed URL
     */
    public String url(String resourceName)
    {
        XWikiContext xcontext = getXWikiContext();
        XWikiURLFactory urlFactory = xcontext.getURLFactory();
        return urlFactory.getURL(
            urlFactory.createURL("resources", "path", "webjars", String.format("value=%s", resourceName), null,
                xcontext),
            xcontext);
    }

    private XWikiContext getXWikiContext()
    {
        XWikiContext xwikiContext =
            (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        if (xwikiContext == null) {
            throw new RuntimeException("The XWiki Context is not available in the Execution Context");
        }
        return xwikiContext;
    }
}
