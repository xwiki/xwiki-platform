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
package org.xwiki.skinx;

import java.util.Map;

import javax.inject.Inject;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.skinx.SkinExtensionPluginApi;

/**
 * The abstract implementation of the wrapper around the skinx plugins. Provides the mechanism needed to grab the skin
 * extensions plugins and call the {@code use} methods on them, subclasses only need to provide the name of the skin
 * extension through their {@link SkinExtension} role hint.
 * 
 * @version $Id$
 * @since 1.20
 */
public abstract class AbstractWrapperSkinExtension implements SkinExtension
{
    /** Execution context handler, needed for accessing the XWikiContext. */
    @Inject
    private Execution execution;

    @Override
    public void use(String resource)
    {
        getSkinExtensionPluginApi().use(resource);
    }

    @Override
    public void use(String resource, Map<String, Object> parameters)
    {
        getSkinExtensionPluginApi().use(resource, parameters);
    }

    /**
     * @return the {@link SkinExtensionPluginApi} in the running wiki
     */
    private SkinExtensionPluginApi getSkinExtensionPluginApi()
    {
        XWikiContext xwikiContext = (XWikiContext) execution.getContext().getProperty("xwikicontext");
        XWiki wiki = xwikiContext.getWiki();
        return (SkinExtensionPluginApi) wiki.getPluginApi(getName(), xwikiContext);
    }

    /**
     * @return the name of the skin extension (e.g. ssx, jsfx, etc) to wrap
     */
    public String getName()
    {
        return getClass().getAnnotation(Component.class).value();
    }
}
