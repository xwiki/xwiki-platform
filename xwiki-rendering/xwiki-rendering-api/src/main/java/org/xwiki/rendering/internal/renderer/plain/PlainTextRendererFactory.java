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
package org.xwiki.rendering.internal.renderer.plain;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.internal.renderer.AbstractPrintRendererFactory;

/**
 * Create Plain Text Renderers.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Component("plain/1.0")
public class PlainTextRendererFactory extends AbstractPrintRendererFactory
{
    /**
     * {@inheritDoc}
     * @see AbstractPrintRendererFactory#getSyntax()
     */
    public Syntax getSyntax()
    {
        return Syntax.PLAIN_1_0;
    }
}
