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
package org.xwiki.platform.component.front.internal;

import java.util.Map;

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.uiextension.UIExtension;

import static org.xwiki.rendering.syntax.Syntax.ANNOTATED_HTML_5_0;

/**
 * UI extension that injects the client-side component manager initialization script into the HTML head. This extension
 * loads and initializes the component manager, making components available on the client-side. This initialization is
 * done on a separate UI extension instead of through importmap's eager mechanism to ensure that is called last, once
 * all the modules had a chance to register components.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Singleton
@Named("ClientSideComponentsResolver")
public class ClientSideComponentsResolverUIExtension implements UIExtension
{
    @Override
    public String getId()
    {
        return "org.xwiki.platform.component.front.html.head";
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.html.head";
    }

    @Override
    public Map<String, String> getParameters()
    {
        // One more than JavascriptImportmapUIExtension
        return Map.of("order", "1001");
    }

    @Override
    public Block execute()
    {
        return new RawBlock("""
            <script type='module'>
            import {init} from "@xwiki/platform-component-manager-default";
            init().catch((e) => console.error('Failed to initialize the component manager', e));
            </script>
            """, ANNOTATED_HTML_5_0);
    }
}
