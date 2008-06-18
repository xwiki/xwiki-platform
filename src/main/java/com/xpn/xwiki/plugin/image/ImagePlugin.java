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
 *
 */

package com.xpn.xwiki.plugin.image;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.PluginException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class ImagePlugin extends XWikiDefaultPlugin
{
    /**
     * Log4J logger object to log messages in this class.
     */
    protected static final Log LOG = LogFactory.getLog(ImagePlugin.class);

    public static final int TYPE_JPG = 1;

    public static final int TYPE_PNG = 2;

    public static final int TYPE_BMP = 3;

    /**
     * The name used for retrieving this plugin from the context.
     * 
     * @see XWikiPluginInterface#getName()
     */
    public static String PLUGIN_NAME = "image";

    /**
     * Cache for already served images.
     */
    private Cache<byte[]> imageCache;

    /**
     * The size of the cache. This parameter can be configured using the key <tt>xwiki.plugin.image.cache.capacity</tt>.
     */
    private int capacity = 50;

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public ImagePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getPluginApi(XWikiPluginInterface, XWikiContext)
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new ImagePluginAPI((ImagePlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#getName()
     */
    public String getName()
    {
        return PLUGIN_NAME;
    }

    public void init(XWikiContext context)
    {
        super.init(context);
        initCache(context);
    }

    public void initCache(XWikiContext context)
    {
        CacheConfiguration configuration = new CacheConfiguration();

        configuration.setConfigurationId("xwiki.plugin.image");

        // Set folder o store cache
        File tempDir = context.getWiki().getTempDirectory(context);
        File imgTempDir = new File(tempDir, configuration.getConfigurationId());
        try {
            imgTempDir.mkdirs();
        } catch (Exception ex) {
            LOG.warn("Cannot create temporary files", ex);
        }
        configuration.put("cache.path", imgTempDir.getAbsolutePath());

        // Set cache constraints
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        String capacityParam = "";
        try {
            capacityParam = context.getWiki().Param("xwiki.plugin.image.cache.capacity");
            if ((capacityParam != null) && (!capacityParam.equals(""))) {
                capacity = Integer.parseInt(capacityParam);
            }
        } catch (NumberFormatException ex) {
            LOG.error("Error in ImagePlugin reading capacity: " + capacityParam, ex);
        }
        lru.setMaxEntries(capacity);

        try {
            imageCache = context.getWiki().getLocalCacheFactory().newCache(configuration);
        } catch (CacheException e) {
            LOG.error("Error initializing the image cache", e);
        }
    }

    public void flushCache()
    {
        if (imageCache != null) {
            imageCache.removeAll();
        }
        imageCache = null;
    }

    public XWikiAttachment downloadAttachment(XWikiAttachment attachment, XWikiContext context)
    {
        int height = 0;
        int width = 0;
        XWikiAttachment attachmentClone = null;

        if (!attachment.isImage(context)) {
            return attachment;
        }

        String sheight = context.getRequest().getParameter("height");
        String swidth = context.getRequest().getParameter("width");

        if ((sheight == null || sheight.length() == 0) && (swidth == null || swidth.length() == 0)) {
            return attachment;
        }

        if (imageCache == null) {
            initCache(context);
        }

        try {
            if (sheight != null) {
                height = Integer.parseInt(sheight);
            }
            if (swidth != null) {
                width = Integer.parseInt(swidth);
            }

            attachmentClone = (XWikiAttachment) attachment.clone();
            String key =
                attachmentClone.getId() + "-" + attachmentClone.getVersion() + "-" + TYPE_PNG + "-" + width + "-"
                    + height;

            if (imageCache != null) {
                byte[] data = imageCache.get(key);

                if (data != null) {
                    attachmentClone.setContent(data);
                } else {
                    if (width == 0) {
                        attachmentClone = this.getImageByHeight(attachmentClone, height, context);
                    } else if (height == 0) {
                        attachmentClone = this.getImageByWidth(attachmentClone, width, context);
                    } else {
                        attachmentClone = this.getImage(attachmentClone, width, height, context);
                    }

                    imageCache.set(key, attachmentClone.getContent(context));
                }
            } else {
                attachmentClone = this.getImageByHeight(attachmentClone, height, context);
            }
        } catch (Exception e) {
            attachmentClone = attachment;
        }
        return attachmentClone;
    }

    public XWikiAttachment getImageByHeight(XWikiAttachment attachment, int thumbnailHeight, XWikiContext context)
        throws Exception
    {

        if (getType(attachment.getMimeType(context)) == 0) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Only JPG, PNG or BMP images are supported.");
        }

        Image imgOri = getImage(attachment, context);

        int imgOriWidth = imgOri.getWidth(null);
        int imgOriHeight = imgOri.getHeight(null);

        if (thumbnailHeight >= imgOriHeight) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                "Thumbnail image not created: the height is higher than the original one.");
        }

        double imageRatio = (double) imgOriWidth / (double) imgOriHeight;
        int thumbnailWidth = (int) (thumbnailHeight * imageRatio);
        createThumbnail(thumbnailWidth, thumbnailHeight, imgOri, attachment);
        return attachment;
    }

    public XWikiAttachment getImage(XWikiAttachment attachment, int thumbnailWidth, int thumbnailHeight,
        XWikiContext context) throws Exception
    {

        if (getType(attachment.getMimeType(context)) == 0) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Only JPG, PNG or BMP images are supported.");
        }

        Image imgOri = getImage(attachment, context);

        int imgOriWidth = imgOri.getWidth(null);
        int imgOriHeight = imgOri.getHeight(null);

        if (thumbnailHeight >= imgOriHeight) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                "Thumbnail image not created: the height is higher than the original one.");
        }

        if (thumbnailWidth >= imgOriWidth) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                "Thumbnail image not created: the width is higher than the original one.");
        }

        createThumbnail(thumbnailWidth, thumbnailHeight, imgOri, attachment);
        return attachment;
    }

    private Image getImage(XWikiAttachment attachment, XWikiContext context) throws XWikiException,
        InterruptedException
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Image imgOri = tk.createImage(attachment.getContent(context));

        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(imgOri, 0);
        mediaTracker.waitForID(0);
        return imgOri;
    }

    public XWikiAttachment getImageByWidth(XWikiAttachment attachment, int thumbnailWidth, XWikiContext context)
        throws Exception
    {

        if (getType(attachment.getMimeType(context)) == 0) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Only JPG, PNG or BMP images are supported.");
        }

        Image imgOri = getImage(attachment, context);
        int imgOriWidth = imgOri.getWidth(null);
        int imgOriHeight = imgOri.getHeight(null);

        if (thumbnailWidth >= imgOriWidth) {
            throw new PluginException(PLUGIN_NAME, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                "Thumbnail image not created: the width is higher than the original one.");
        }

        double imageRatio = (double) imgOriWidth / (double) imgOriHeight;
        int thumbnailHeight = (int) (thumbnailWidth / imageRatio);

        createThumbnail(thumbnailWidth, thumbnailHeight, imgOri, attachment);
        return attachment;
    }

    private void createThumbnail(int thumbnailWidth, int thumbnailHeight, Image imgOri, XWikiAttachment attachment)
        throws IOException
    {
        // draw original image to thumbnail image object and
        // scale it to the new size on-the-fly
        BufferedImage imgTN = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = imgTN.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.drawImage(imgOri, 0, 0, thumbnailWidth, thumbnailHeight, null);

        // save thumbnail image to bout
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ImageIO.write(imgTN, "PNG", bout);
        attachment.setContent(bout.toByteArray());
    }

    public static int getType(String mimeType)
    {
        if (mimeType.equals("image/jpg") || mimeType.equals("image/jpeg")) {
            return TYPE_JPG;
        }
        if (mimeType.equals("image/png")) {
            return TYPE_PNG;
        }
        if (mimeType.equals("image/bmp")) {
            return TYPE_BMP;
        }
        return 0;
    }

    public int getWidth(XWikiAttachment attachment, XWikiContext context) throws InterruptedException, XWikiException
    {
        Image imgOri = getImage(attachment, context);
        return imgOri.getWidth(null);
    }

    public int getHeight(XWikiAttachment attachment, XWikiContext context) throws InterruptedException, XWikiException
    {
        Image imgOri = getImage(attachment, context);
        return imgOri.getHeight(null);
    }
}
