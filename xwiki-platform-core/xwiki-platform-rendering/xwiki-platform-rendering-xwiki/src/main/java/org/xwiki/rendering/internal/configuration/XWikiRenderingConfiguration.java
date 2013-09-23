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
package org.xwiki.rendering.internal.configuration;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.configuration.RenderingConfiguration;

/**
 * Extends {@link RenderingConfiguration} with XWiki specific configuration properties.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
public interface XWikiRenderingConfiguration extends RenderingConfiguration
{
    /**
     * @return {@code true} to include the image dimensions extracted from the image parameters in the image URL,
     *         {@code false} otherwise; when image dimensions are included in the image URL the image can be resized on
     *         the server side before being downloaded.
     * @since 2.5M2
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
     * @since 2.5M2
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
     * @since 2.5M2
     */
    int getImageHeightLimit();
}
