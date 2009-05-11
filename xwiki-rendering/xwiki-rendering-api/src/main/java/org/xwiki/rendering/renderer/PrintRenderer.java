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
package org.xwiki.rendering.renderer;

import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * A Print Renderer is a {@link Renderer} that outputs its results to a {@link WikiPrinter}.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public interface PrintRenderer extends Renderer
{
    /**
     * @return the printer to which events generate results in. For example the XHTML print renderer outputs XHTML to a
     *         {@link WikiPrinter} and the resulting XHTML can be retrieved by calling
     *         {@link org.xwiki.rendering.renderer.printer.WikiPrinter#toString()}
     */
    WikiPrinter getPrinter();
}
