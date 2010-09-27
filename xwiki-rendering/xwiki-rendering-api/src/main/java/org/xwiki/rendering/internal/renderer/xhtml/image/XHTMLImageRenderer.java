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
package org.xwiki.rendering.internal.renderer.xhtml.image;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.listener.ImageListener;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;

/**
 * Renders images as XHTML.
 * 
 * @version $Id$
 * @since 1.8RC3
 */
@ComponentRole
public interface XHTMLImageRenderer extends ImageListener
{
    /**
     * The name of the XHTML image element.
     */
    String IMG = "img";
    
    /**
     * <code>img</code> XHTML element parameter to indicate an alternate description to the image.
     */
    String ALTERNATE = "alt";
    
    /**
     * <code>img</code> XHTML element parameter to indicate where the image is located.
     */
    String SRC = "src";

    /**
     * @param printer the XHTML printer to use to output images as XHTML
     */
    void setXHTMLWikiPrinter(XHTMLWikiPrinter printer);

    /**
     * @return the XHTML printer to use to output images as XHTML
     * @since 2.0M3
     */
    XHTMLWikiPrinter getXHTMLWikiPrinter();
}
