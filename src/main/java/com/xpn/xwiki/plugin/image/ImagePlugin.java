/**
 * XWiki
 * Copyright (C) 2006 XpertNet
 * Contact: xwiki-dev@objectweb.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * @author Xavier MOGHRABI
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

import javax.imageio.ImageIO;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheNeedsRefreshException;
import com.xpn.xwiki.cache.impl.OSCacheCache;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.PluginException;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;

public class ImagePlugin extends XWikiDefaultPlugin {
	private static final int TYPE_JPG = 1;

	private static final int TYPE_PNG = 2;

	private static final int TYPE_BMP = 3;

	private static String name = "image";

	private XWikiCache imageCache;

	private int capacity = 50;

	public ImagePlugin(String name, String className, XWikiContext context) {
		super(name, className, context);
		init(context);

	}

	/**
	 * Allow to get the plugin name
	 * 
	 * @return plugin name
	 */
	public String getName() {
		return name;
	}

	public void init(XWikiContext context) {
		super.init(context);
		try {
			String capacityParam = context.getWiki().Param("xwiki.plugin.image.cache.capacity");
			capacity = Integer.parseInt(capacityParam);
		} catch (NumberFormatException e) {
			throw e;
		} finally {
			imageCache = new OSCacheCache(capacity, true, "temp/imageCache");
		}

	}

	public void flushCache() {
		if (imageCache != null)
			imageCache.flushAll();
	}

	public XWikiAttachment downloadAttachment(XWikiAttachment image, XWikiContext context) {

		int height = 0;
		XWikiAttachment imageclone = null;
		try {
			
			height = Integer.parseInt(context.getRequest().getParameter("height"));
			
			imageclone = (XWikiAttachment) image.clone();
			String key = imageclone.getId() + "-" + TYPE_PNG + "-" + height;
			
			if (imageCache != null) {
				try {
					imageclone.setContent((byte []) imageCache.getFromCache(key));
				} catch (XWikiCacheNeedsRefreshException e) {
					try {
						imageclone = this.getImageByHeight(imageclone, height, context);
						imageCache.putInCache(key, imageclone.getContent(context));
					} catch (Exception e2) {
						imageCache.cancelUpdate(key);
						throw e2;
					}
				}
			} else {
				imageclone = this.getImageByHeight(imageclone, height, context);
			}
		} catch (Exception e) {
			imageclone = image;
		} finally {
			return imageclone;
		}
	}

	public XWikiAttachment getImageByHeight(XWikiAttachment image, int thumbnailHeight, XWikiContext context) throws Exception {
		
		if (getType(image.getMimeType(context)) == 0)
			throw new PluginException(name,  PluginException.ERROR_XWIKI_NOT_IMPLEMENTED,
					"Only JPG, PNG or BMP images are supported.");

		Toolkit tk = Toolkit.getDefaultToolkit();
		Image imgOri = tk.createImage(image.getContent(context));

		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(imgOri, 0);
		mediaTracker.waitForID(0);

		int imgOriWidth = imgOri.getWidth(null);
		int imgOriHeight = imgOri.getHeight(null);

		if (thumbnailHeight >= imgOriHeight)
			throw new PluginException(name, PluginException.ERROR_XWIKI_DIFF_METADATA_ERROR,
					"Thumbnail image not created: the height is higher than the original one.");

		double imageRatio = (double) imgOriWidth / (double) imgOriHeight;
		int thumbnailWidth = (int) (thumbnailHeight * imageRatio);

		// draw original image to thumbnail image object and
		// scale it to the new size on-the-fly
		BufferedImage imgTN = new BufferedImage(thumbnailWidth, thumbnailHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = imgTN.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		graphics2D.drawImage(imgOri, 0, 0, thumbnailWidth, thumbnailHeight, null);

		// save thumbnail image to bout
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ImageIO.write(imgTN, "PNG", bout);

		
		image.setContent(bout.toByteArray());
		
		return image;
	}

	public static int getType(String mimeType) {
	        if (mimeType.equals("image/jpg"))
	            return TYPE_JPG;
	        if (mimeType.equals("image/jpeg"))
	            return TYPE_JPG;
	        if (mimeType.equals("image/png"))
	            return TYPE_PNG;
	        if (mimeType.equals("image/bmp"))
	            return TYPE_BMP;
	        return 0;
	}

}
