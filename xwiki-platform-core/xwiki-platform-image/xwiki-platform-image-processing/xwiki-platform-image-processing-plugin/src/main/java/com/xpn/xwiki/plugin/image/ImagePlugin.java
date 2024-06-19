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
package com.xpn.xwiki.plugin.image;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class ImagePlugin extends XWikiDefaultPlugin
{
    /**
     * Logging helper object.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ImagePlugin.class);

    /**
     * The name used for retrieving this plugin from the context.
     *
     * @see XWikiPluginInterface#getName()
     */
    private static final String PLUGIN_NAME = "image";

    /**
     * Cache for already served images.
     */
    private Cache<XWikiAttachment> imageCache;

    /**
     * The size of the cache. This parameter can be configured using the key {@code xwiki.plugin.image.cache.capacity}.
     */
    private int capacity = 50;

    /**
     * Default JPEG image quality.
     */
    private float defaultQuality = 0.5f;

    /**
     * The object used to process images.
     */
    private ImageProcessor imageProcessor;

    /**
     * Creates a new instance of this plugin.
     *
     * @param name the name of the plugin
     * @param className the class name
     * @param context the XWiki context
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String, String, com.xpn.xwiki.XWikiContext)
     */
    public ImagePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);

        init(context);
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ImagePluginAPI((ImagePlugin) plugin, context);
    }

    @Override
    public String getName()
    {
        return PLUGIN_NAME;
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);

        initCache(context);

        String imageProcessorHint = context.getWiki().Param("xwiki.plugin.image.processorHint", "thumbnailator");
        this.imageProcessor = Utils.getComponent(ImageProcessor.class, imageProcessorHint);

        String defaultQualityParam = context.getWiki().Param("xwiki.plugin.image.defaultQuality");
        if (!StringUtils.isBlank(defaultQualityParam)) {
            try {
                this.defaultQuality = Math.max(0, Math.min(1, Float.parseFloat(defaultQualityParam.trim())));
            } catch (NumberFormatException e) {
                LOG.warn("Failed to parse xwiki.plugin.image.defaultQuality configuration parameter. "
                    + "Using {} as the default image quality.", this.defaultQuality);
            }
        }
    }

    /**
     * Tries to initialize the image cache. If the initialization fails the image cache remains {@code null}.
     *
     * @param context the XWiki context
     */
    private void initCache(XWikiContext context)
    {
        if (this.imageCache == null) {
            CacheConfiguration configuration = new CacheConfiguration();

            configuration.setConfigurationId("xwiki.plugin.image");

            // Set cache constraints.
            LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
            configuration.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);

            String capacityParam = context.getWiki().Param("xwiki.plugin.image.cache.capacity");
            if (!StringUtils.isBlank(capacityParam) && StringUtils.isNumeric(capacityParam.trim())) {
                try {
                    this.capacity = Integer.parseInt(capacityParam.trim());
                } catch (NumberFormatException e) {
                    LOG.warn(String.format(
                        "Failed to parse xwiki.plugin.image.cache.capacity configuration parameter. "
                            + "Using %s as the cache capacity.", this.capacity), e);
                }
            }
            lru.setMaxEntries(this.capacity);

            try {
                this.imageCache = Utils.getComponent(CacheManager.class).createNewLocalCache(configuration);
            } catch (CacheException e) {
                LOG.error("Error initializing the image cache.", e);
            }
        }
    }

    @Override
    public void flushCache()
    {
        if (this.imageCache != null) {
            this.imageCache.dispose();
        }
        this.imageCache = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Allows to scale images server-side, in order to have real thumbnails for reduced traffic. The new image
     * dimensions are passed in the request as the {@code width} and {@code height} parameters. If only one of the
     * dimensions is specified, then the other one is computed to preserve the original aspect ratio of the image.
     * </p>
     *
     * @see XWikiDefaultPlugin#downloadAttachment(XWikiAttachment, XWikiContext)
     */
    @Override
    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        if (attachment == null || !this.imageProcessor.isMimeTypeSupported(attachment.getMimeType(context))) {
            return attachment;
        }

        int height = -1;
        try {
            height = Integer.parseInt(context.getRequest().getParameter("height"));
        } catch (NumberFormatException e) {
            // Ignore.
        }

        int width = -1;
        try {
            width = Integer.parseInt(context.getRequest().getParameter("width"));
        } catch (NumberFormatException e) {
            // Ignore.
        }

        float quality = -1;
        try {
            quality = Float.parseFloat(context.getRequest().getParameter("quality"));
        } catch (NumberFormatException | NullPointerException e) {
            // Ignore.
        }

        // If no scaling is needed, return the original image.
        if (height <= 0 && width <= 0 && quality < 0) {
            return attachment;
        }

        try {
            // Transform the image attachment before is it downloaded.
            return downloadImage(attachment, width, height, quality, context);
        } catch (Exception e) {
            LOG.warn("Failed to transform image attachment {} for scaling, falling back to original attachment.",
                attachment.getFilename());
            LOG.debug("Full stack trace for image attachment scaling error: ", e);
            return attachment;
        }
    }

    /**
     * Transforms the given image (i.e. shrinks the image and changes its quality) before it is downloaded.
     *
     * @param image the image to be downloaded
     * @param width the desired image width; this value is taken into account only if it is greater than zero and
     *     less than the current image width
     * @param height the desired image height; this value is taken into account only if it is greater than zero and
     *     less than the current image height
     * @param quality the desired compression quality
     * @param context the XWiki context
     * @return the transformed image
     * @throws Exception if transforming the image fails
     */
    private XWikiAttachment downloadImage(XWikiAttachment image, int width, int height, float quality,
        XWikiContext context) throws Exception
    {
        initCache(context);

        boolean keepAspectRatio = Boolean.valueOf(context.getRequest().getParameter("keepAspectRatio"));

        XWikiAttachment thumbnail = (this.imageCache == null)
            ? shrinkImage(image, width, height, keepAspectRatio, quality, context)
            : downloadImageFromCache(image, width, height, keepAspectRatio, quality, context);

        // If the image has been transformed, update the file name extension to match the image format.
        String fileName = thumbnail.getFilename();
        String extension = StringUtils.lowerCase(StringUtils.substringAfterLast(fileName, String.valueOf('.')));
        if (thumbnail != image && !Arrays.asList("jpeg", "jpg", "png").contains(extension)) {
            // The scaled image is PNG, so correct the extension in order to output the correct MIME type.
            thumbnail.setFilename(StringUtils.substringBeforeLast(fileName, ".") + ".png");
        }
        return thumbnail;
    }

    /**
     * Downloads the given image from cache.
     *
     * @param image the image to be downloaded
     * @param width the desired image width; this value is taken into account only if it is greater than zero and
     *     less than the current image width
     * @param height the desired image height; this value is taken into account only if it is greater than zero and
     *     less than the current image height
     * @param keepAspectRatio {@code true} to preserve aspect ratio when resizing the image, {@code false}
     *     otherwise
     * @param quality the desired compression quality
     * @param context the XWiki context
     * @return the transformed image
     * @throws Exception if transforming the image fails
     */
    private XWikiAttachment downloadImageFromCache(XWikiAttachment image, int width, int height,
        boolean keepAspectRatio, float quality, XWikiContext context) throws Exception
    {
        String key = String.format("%s;%s;%s;%s;%s;%s;%s", image.getId(), image.getVersion(), image.getDate().getTime(),
            width, height, keepAspectRatio, quality);

        XWikiAttachment thumbnail = this.imageCache.get(key);
        if (thumbnail == null) {
            thumbnail = shrinkImage(image, width, height, keepAspectRatio, quality, context);
            this.imageCache.set(key, thumbnail);
        }
        return thumbnail;
    }

    /**
     * Reduces the size (i.e. the number of bytes) of an image by scaling its width and height and by reducing its
     * compression quality. This helps decreasing the time needed to download the image attachment.
     *
     * @param attachment the image to be shrunk
     * @param requestedWidth the desired image width; this value is taken into account only if it is greater than
     *     zero and less than the current image width
     * @param requestedHeight the desired image height; this value is taken into account only if it is greater than
     *     zero and less than the current image height
     * @param keepAspectRatio {@code true} to preserve the image aspect ratio even when both requested dimensions
     *     are properly specified (in this case the image will be resized to best fit the rectangle with the requested
     *     width and height), {@code false} otherwise
     * @param requestedQuality the desired compression quality
     * @param context the XWiki context
     * @return the modified image attachment
     * @throws Exception if shrinking the image fails
     */
    private XWikiAttachment shrinkImage(XWikiAttachment attachment, int requestedWidth, int requestedHeight,
        boolean keepAspectRatio, float requestedQuality, XWikiContext context) throws Exception
    {
        Image image = this.imageProcessor.readImage(attachment.getContentInputStream(context));

        // Compute the new image dimension.
        int currentWidth = image.getWidth(null);
        int currentHeight = image.getHeight(null);
        int[] dimensions =
            reduceImageDimensions(currentWidth, currentHeight, requestedWidth, requestedHeight, keepAspectRatio);

        float quality = requestedQuality;
        if (quality < 0) {
            // If no scaling is needed and the quality parameter is not specified, return the original image.
            if (dimensions[0] == currentWidth && dimensions[1] == currentHeight) {
                return attachment;
            }
            quality = this.defaultQuality;
        }

        // Scale the image to the new dimensions.
        RenderedImage shrunkImage = this.imageProcessor.scaleImage(image, dimensions[0], dimensions[1]);

        // Create an image attachment for the shrunk image.
        XWikiAttachment thumbnail = attachment.clone();
        thumbnail.loadAttachmentContent(context);

        OutputStream acos = thumbnail.getAttachment_content().getContentOutputStream();
        this.imageProcessor.writeImage(shrunkImage,
            attachment.getMimeType(context),
            quality,
            acos);

        IOUtils.closeQuietly(acos);

        return thumbnail;
    }

    /**
     * Computes the new image dimension which:
     * <ul>
     * <li>uses the requested width and height only if both are smaller than the current values</li>
     * <li>preserves the aspect ratio when width or height is not specified.</li>
     * </ul>
     *
     * @param currentWidth the current image width
     * @param currentHeight the current image height
     * @param requestedWidth the desired image width; this value is taken into account only if it is greater than
     *     zero and less than the current image width
     * @param requestedHeight the desired image height; this value is taken into account only if it is greater than
     *     zero and less than the current image height
     * @param keepAspectRatio {@code true} to preserve the image aspect ratio even when both requested dimensions
     *     are properly specified (in this case the image will be resized to best fit the rectangle with the requested
     *     width and height), {@code false} otherwise
     * @return new width and height values
     */
    private int[] reduceImageDimensions(int currentWidth, int currentHeight, int requestedWidth, int requestedHeight,
        boolean keepAspectRatio)
    {
        int[] dimensions;
        // Keep the aspect ratio when requested or width or height are missing.
        if (keepAspectRatio || requestedWidth <= 0 || requestedHeight <= 0) {
            dimensions = reduceImageDimensions(currentWidth, currentHeight, requestedWidth, requestedHeight);
        } else if (requestedWidth > currentWidth || requestedHeight > currentHeight) {
            // Resize the image to preserve the requested image ratio while preventing outscaling.
            dimensions = reduceImageDimensions(requestedWidth, requestedHeight, currentWidth, currentHeight);
        } else {
            dimensions = new int[] { requestedWidth, requestedHeight };
        }

        return dimensions;
    }

    /**
     * Reduce the image of dimension {@code baseWidth*baseHeight} so that it fits a box of dimension
     * {@code targetWidth*targetHeight} while preserving its current dimensions. If {@code targetWidth} or
     * {@code targetHeight} is 0, negative, or larger than respectively {@code baseWidth} and
     * {@code baseHeight}, it is ignored.
     *
     * @param baseWidth the current width of the image
     * @param baseHeight the current height of the image
     * @param targetWidth the requested width of the image
     * @param targetHeight the requested height of the image
     * @return the new dimensions of the image, preserving to base aspect ratio
     */
    private int[] reduceImageDimensions(int baseWidth, int baseHeight, int targetWidth, int targetHeight)
    {
        int width = baseWidth;
        int height = baseHeight;

        double aspectRatio = (double) baseWidth / (double) baseHeight;
        // Ignore the width if it is not given or too large, i.e., larger than the current width or larger than the
        // width derived from the requested height.
        if (targetWidth <= 0 || targetWidth >= baseWidth
            || (targetHeight > 0 && targetWidth > (int) (targetHeight * aspectRatio)))
        {
            // Ignore the requested width. Check the requested height.
            if (targetHeight > 0 && targetHeight < baseHeight) {
                // Reduce the height, keeping aspect ratio.
                width = (int) (targetHeight * aspectRatio);
                height = targetHeight;
            }
        } else {
            // Ignore the requested height. Reduce the width, keeping aspect ratio.
            width = targetWidth;
            height = (int) (targetWidth / aspectRatio);
        }

        return new int[] { width, height };
    }

    /**
     * @param attachment an image attachment
     * @param context the XWiki context
     * @return the width of the specified image
     * @throws IOException if reading the image from the attachment content fails
     * @throws XWikiException if reading the attachment content fails
     */
    public int getWidth(XWikiAttachment attachment, XWikiContext context) throws IOException, XWikiException
    {
        return this.imageProcessor.readImage(attachment.getContentInputStream(context)).getWidth(null);
    }

    /**
     * @param attachment an image attachment
     * @param context the XWiki context
     * @return the height of the specified image
     * @throws IOException if reading the image from the attachment content fails
     * @throws XWikiException if reading the attachment content fails
     */
    public int getHeight(XWikiAttachment attachment, XWikiContext context) throws IOException, XWikiException
    {
        return this.imageProcessor.readImage(attachment.getContentInputStream(context)).getHeight(null);
    }
}
