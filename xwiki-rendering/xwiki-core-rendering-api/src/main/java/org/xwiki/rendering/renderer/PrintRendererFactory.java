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

import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Make it easy to create {@link PrintRenderer}. The main reason is because Print renderers cannot be components
 * since they have some state (the {@link WikiPrinter}). Thus all dependent components required to construct
 * a Print renderer have to be passed manually to its constructor. Since the implementation of this interface is a
 * component it's automatically injected all components required to construct the Print renderers.
 *
 * @version $Id: $
 * @since 1.6M2
 */
public interface PrintRendererFactory
{
    String ROLE = PrintRendererFactory.class.getName();
    
    PrintRenderer createRenderer(Syntax targetSyntax, WikiPrinter printer);
}
