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
package org.xwiki.platform.svg.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.svg.SVGRasterizer;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.url.ExtendedURL;

/**
 * Utilities for working with SVG images.
 *
 * @version $Id$
 * @since 8.0M1
 */
@Component
@Named("svg")
@Singleton
@Unstable("New API introduced in 8.0")
public class SVGScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    private SVGRasterizer component;

    @Inject
    @Named("standard/tmp")
    private ResourceReferenceSerializer<TemporaryResourceReference, ExtendedURL> serializer;

    /**
     * Rasterize an image as PNG into a temporary resource belonging to the current document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @return URL pointing to the temporary resource where the PNG is stored
     */
    public ExtendedURL rasterizeToTemporaryResource(String content)
    {
        return rasterizeToTemporaryResource(content, 0, 0);
    }

    /**
     * Rasterize an image as PNG into a temporary resource belonging to the current document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return URL pointing to the temporary resource where the PNG is stored
     */
    public ExtendedURL rasterizeToTemporaryResource(String content, int width, int height)
    {
        try {
            return this.serializer.serialize(this.component.rasterizeToTemporaryResource(content, width, height));
        } catch (Exception ex) {
            this.logger.warn("Failed to rasterize SVG image to temporary resource: {}",
                ExceptionUtils.getRootCauseMessage(ex));
        }
        return null;
    }

    /**
     * Rasterize an image as PNG into a temporary resource belonging to the current document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @param targetContext the document which will "own" the new temporary resource
     * @return URL pointing to the temporary resource where the PNG is stored
     */
    public ExtendedURL rasterizeToTemporaryResource(String content, DocumentReference targetContext)
    {
        return rasterizeToTemporaryResource(content, 0, 0, targetContext);
    }

    /**
     * Rasterize an image as PNG as into temporary resource belonging to the current document, accessible through the
     * "tmp" resource URL handler.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @param targetContext the document which will "own" the new temporary resource
     * @return URL pointing to the temporary resource where the PNG is stored
     */
    public ExtendedURL rasterizeToTemporaryResource(String content, int width, int height,
        DocumentReference targetContext)
    {
        try {
            ExtendedURL result = this.serializer
                .serialize(this.component.rasterizeToTemporaryResource(content, width, height, targetContext));
            return result;
        } catch (Exception ex) {
            this.logger.warn("Failed to rasterize SVG image to temporary resource in context [{}]: {}", targetContext,
                ExceptionUtils.getRootCauseMessage(ex));
        }
        return null;
    }

    /**
     * Rasterize an image as PNG into the current response.
     *
     * @param content the SVG image
     * @return {@code true} if the image was successfully rasterized and written to the response, {@code false} in case
     *         of exceptions
     */
    public boolean rasterizeToResponse(String content)
    {
        return rasterizeToResponse(content, 0, 0);
    }

    /**
     * Rasterize an image as PNG into the current response.
     *
     * @param content the SVG image
     * @param width the desired width of the raster image, in pixels; if 0 or a negative number, the image's native size
     *            is used
     * @param height the desired height of the raster image, in pixels; if 0 or a negative number, the image's native
     *            size is used
     * @return {@code true} if the image was successfully rasterized and written to the response, {@code false} in case
     *         of exceptions
     */
    public boolean rasterizeToResponse(String content, int width, int height)
    {
        try {
            this.component.rasterizeToResponse(content, width, height);
            return true;
        } catch (Exception ex) {
            this.logger.warn("Failed to rasterize SVG image to response: {}", ExceptionUtils.getRootCauseMessage(ex));
        }
        return false;
    }
}
