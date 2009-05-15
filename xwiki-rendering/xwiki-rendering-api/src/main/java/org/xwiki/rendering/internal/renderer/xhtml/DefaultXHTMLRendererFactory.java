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
package org.xwiki.rendering.internal.renderer.xhtml;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.renderer.xhtml.XHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLLinkRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLRendererFactory;
import org.xwiki.rendering.renderer.xhtml.XWikiXHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XWikiXHTMLLinkRenderer;

/**
 * Default implementation of {@link XHTMLRendererFactory} that uses component injection to
 * get the constructor parameters to pass to the underlying XHTML Link and Image Renderers. 
 *  
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class DefaultXHTMLRendererFactory implements XHTMLRendererFactory
{
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    @Requirement
    private LinkLabelGenerator linkLabelGenerator;

    @Requirement
    private AttachmentParser attachmentParser;

    @Requirement
    private DocumentNameSerializer documentNameSerializer;
    
    /**
     * {@inheritDoc}
     * @see XHTMLRendererFactory#createXHTMLLinkRenderer()
     */
    public XHTMLLinkRenderer createXHTMLLinkRenderer()
    {
        return new XWikiXHTMLLinkRenderer(this.documentAccessBridge,
            this.linkLabelGenerator, this.attachmentParser, this.documentNameSerializer);        
    }
    
    /**
     * {@inheritDoc}
     * @see XHTMLRendererFactory#createXHTMLImageRenderer()
     */
    public XHTMLImageRenderer createXHTMLImageRenderer()
    {
        return new XWikiXHTMLImageRenderer(this.documentAccessBridge);
    }
}
