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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.parser.AttachmentParser;
import org.xwiki.rendering.renderer.LinkLabelGenerator;
import org.xwiki.rendering.renderer.xhtml.XHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLLinkRenderer;
import org.xwiki.rendering.renderer.xhtml.XHTMLRendererFactory;
import org.xwiki.rendering.renderer.xhtml.DefaultXHTMLImageRenderer;
import org.xwiki.rendering.renderer.xhtml.DefaultXHTMLLinkRenderer;
import org.xwiki.rendering.wiki.WikiModel;

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
    private LinkLabelGenerator linkLabelGenerator;

    @Requirement
    private AttachmentParser attachmentParser;

    /**
     * Used to find which implementation of XHTML Link and Image Renderers to use:
     * If there's an implementation of {@link WikiModel} registered then use the 
     * Wiki Renderers, otherwise use the Simple Renderers.
     */
    @Requirement
    private ComponentManager componentManager;
    
    /**
     * {@inheritDoc}
     * @see XHTMLRendererFactory#createXHTMLLinkRenderer()
     */
    public XHTMLLinkRenderer createXHTMLLinkRenderer()
    {
        XHTMLLinkRenderer renderer;
        try {
            WikiModel wikiModel = this.componentManager.lookup(WikiModel.class);
            renderer = new DefaultXHTMLLinkRenderer(wikiModel, this.linkLabelGenerator, this.attachmentParser); 
        } catch (ComponentLookupException e) {
            // There's no WikiModel implementation available, fall back to the Simple XHTML Link Renderer
            renderer = new DefaultXHTMLLinkRenderer(this.linkLabelGenerator, this.attachmentParser); 
        }
        return renderer; 
    }
    
    /**
     * {@inheritDoc}
     * @see XHTMLRendererFactory#createXHTMLImageRenderer()
     */
    public XHTMLImageRenderer createXHTMLImageRenderer()
    {
        XHTMLImageRenderer renderer;
        try {
            WikiModel wikiModel = this.componentManager.lookup(WikiModel.class);
            renderer = new DefaultXHTMLImageRenderer(wikiModel); 
        } catch (ComponentLookupException e) {
            // There's no WikiModel implementation available, fall back to the Simple XHTML Image Renderer
            renderer = new DefaultXHTMLImageRenderer(); 
        }
        return renderer; 
    }
}
