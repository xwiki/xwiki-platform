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
package org.xwiki.rendering.internal.renderer;

import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Common code for {@link org.xwiki.rendering.renderer.PrintRendererFactory}, implements the logic to lookup and call
 * the matching Print Renderer.
 * 
 * @version $Id$
 * @since 2.0M3
 */
public abstract class AbstractPrintRendererFactory implements PrintRendererFactory
{
    /**
     * Used to lookup the {@link PrintRenderer}.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see PrintRendererFactory#createRenderer(org.xwiki.rendering.renderer.printer.WikiPrinter)
     */
    public PrintRenderer createRenderer(WikiPrinter printer)
    {
        PrintRenderer renderer;
        try {
            renderer = this.componentManager.lookup(PrintRenderer.class, getSyntax().toIdString());
        } catch (ComponentLookupException e) {
            throw new RuntimeException("Failed to create [" + getSyntax().toString() + "] renderer", e);
        }

        renderer.setPrinter(printer);

        return renderer;
    }
}
