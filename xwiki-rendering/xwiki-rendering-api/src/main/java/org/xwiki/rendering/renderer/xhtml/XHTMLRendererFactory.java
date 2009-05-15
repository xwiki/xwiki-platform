package org.xwiki.rendering.renderer.xhtml;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Make it easy to generate an XHTML Link or Image Renderer without having to pass any parameter.
 * Of course it's still possible to create a new instance by hand using new, thus controlling
 * completely the passed parameters to the constructor. 
 *  
 * @version $Id$
 * @since 2.0M1
 */
@ComponentRole
public interface XHTMLRendererFactory
{
    /**
     * @return XXX
     */
    XHTMLImageRenderer createXHTMLImageRenderer();

    /**
     * @return XXX
     */
    XHTMLLinkRenderer createXHTMLLinkRenderer();
}
