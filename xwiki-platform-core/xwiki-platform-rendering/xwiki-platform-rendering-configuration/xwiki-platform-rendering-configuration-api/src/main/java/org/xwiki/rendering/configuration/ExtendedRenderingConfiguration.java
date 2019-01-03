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
package org.xwiki.rendering.configuration;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.stability.Unstable;

/**
 * Extends {@link RenderingConfiguration} with XWiki-specific configuration properties.
 * 
 * @version $Id$
 * @since 8.2M1
 */
@Role
public interface ExtendedRenderingConfiguration
{
    /**
     * @return {@code true} to include the image dimensions extracted from the image parameters in the image URL,
     *         {@code false} otherwise; when image dimensions are included in the image URL the image can be resized on
     *         the server side before being downloaded.
     */
    boolean isImageDimensionsIncludedInImageURL();

    /**
     * One way to improve page load speed is to resize images on the server side just before rendering the page. The
     * rendering module can use the image width provided by the user to scale the image. When the user doesn't specify
     * the image width the rendering module can limit the width of the image based on this configuration parameter.
     * <p>
     * The default value is {@code -1} which means image width is not limited by default. Use a value greater than 0 to
     * limit the image width (pixels). Note that the aspect ratio is kept even when both the width and the height of the
     * image are limited.
     * 
     * @return the maximum image width when there's no user supplied width
     * @see #isImageDimensionsIncludedInImageURL()
     */
    int getImageWidthLimit();

    /**
     * One way to improve page load speed is to resize images on the server side just before rendering the page. The
     * rendering module can use the image height provided by the user to scale the image. When the user doesn't specify
     * the image height the rendering module can limit the height of the image based on this configuration parameter.
     * <p>
     * The default value is {@code -1} which means image height is not limited by default. Use a value greater than 0 to
     * limit the image height (pixels). Note that the aspect ratio is kept even when both the width and the height of
     * the image are limited.
     * 
     * @return the maximum image height when there's no user supplied height
     * @see #isImageDimensionsIncludedInImageURL()
     */
    int getImageHeightLimit();

    /**
     * @return the list of Rendering Syntaxes that should be used for the current wiki (i.e. that should be proposed to
     *         the user when editing wiki pages).
     */
    List<Syntax> getConfiguredSyntaxes();

    /**
     * @return the list of Rendering Syntaxes that should not be used for the current wiki (i.e. that should not be
     *         proposed to the user when editing wiki pages). The reason is that we want by default that all syntaxes
     *         are enabled (for example so that when installing a new Syntax throught the Extension Manager it's active
     *         by default)
     */
    List<Syntax> getDisabledSyntaxes();

    /**
     * @return the default Syntax to use when creating new content (Documents, etc).
     * @since 11.0RC1
     */
    @Unstable
    default Syntax getDefaultContentSyntax()
    {
        return Syntax.XWIKI_2_1;
    }
}
