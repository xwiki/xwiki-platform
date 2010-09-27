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
package org.xwiki.rendering.internal.renderer.xwiki21;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.renderer.AbstractBlockRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;

/**
 * Renders a {@link org.xwiki.rendering.block.Block} in XWiki Syntax 2.1.
 *
 * @version $Id$
 * @since 2.5M2
 */
@Component("xwiki/2.1")
public class XWikiSyntaxBlockRenderer extends AbstractBlockRenderer
{
    /**
     * @see #getPrintRendererFactory()  
     */
    @Requirement("xwiki/2.1")
    private PrintRendererFactory xwikiSyntaxRendererFactory;

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.internal.renderer.AbstractBlockRenderer#getPrintRendererFactory()
     */
    @Override
    protected PrintRendererFactory getPrintRendererFactory()
    {
        return this.xwikiSyntaxRendererFactory;
    }
}
