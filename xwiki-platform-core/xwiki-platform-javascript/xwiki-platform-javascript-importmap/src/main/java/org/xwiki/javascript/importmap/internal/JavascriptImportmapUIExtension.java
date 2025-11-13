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
package org.xwiki.javascript.importmap.internal;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.uiextension.UIExtension;

/**
 * Retrieve the resolved importmap from {@link JavascriptImportmapResolver} and inject it in the page headers with the
 * {@code "org.xwiki.platform.html.head"} extension point.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@Component
@Singleton
@Named("JavascriptImportmap")
public class JavascriptImportmapUIExtension implements UIExtension
{
    @Inject
    private JavascriptImportmapResolver javascriptImportmapResolver;

    @Override
    public String getId()
    {
        return "org.xwiki.platform.javascript.importmap.html.head";
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.html.head";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of("order", "1000");
    }

    @Override
    public Block execute()
    {
        return this.javascriptImportmapResolver.getBlock();
    }
}
