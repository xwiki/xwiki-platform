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

/**
 * The different types of Print Renderers.
 *
 * @version $Id: $ 
 * @since 1.6M2
 */
public enum PrintRendererType
{
    XHTML("xhtml"), XWIKISYNTAX("xwiki"), EVENTS("event"), TEX("tex");

    private String id;

    private PrintRendererType(String id)
    {
        this.id = id;
    }

    public static PrintRendererType fromString(String id)
    {
        PrintRendererType result;
        if (id.equalsIgnoreCase(PrintRendererType.XHTML.id)) {
            result = PrintRendererType.XHTML;
        } else if (id.equalsIgnoreCase(PrintRendererType.XWIKISYNTAX.id)) {
            result = PrintRendererType.XWIKISYNTAX;
        } else if (id.equalsIgnoreCase(PrintRendererType.EVENTS.id)) {
            result = PrintRendererType.EVENTS;
        } else if (id.equalsIgnoreCase(PrintRendererType.TEX.id)) {
            result = PrintRendererType.TEX;
        } else {
            throw new RuntimeException("Invalid Print Renderer type [" + id + "]");
        }

        return result;
    }
}
