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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 */
@Deprecated
public class ImagePluginAPI extends PluginApi<ImagePlugin>
{
    /** Logging helper object. */
    private static final Logger LOG = LoggerFactory.getLogger(ImagePluginAPI.class);

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /**
     * Creates a new instance of this plugin API.
     *
     * @param imagePlugin the underlying image plugin that is exposed by this API
     * @param context the XWiki context
     */
    public ImagePluginAPI(ImagePlugin imagePlugin, XWikiContext context)
    {
        super(imagePlugin, context);
    }

    /**
     * Detects the height of an image attached to a wiki page.
     *
     * @param pageName the name of a wiki page
     * @param attachmentName the name of an image attached to the specified page
     * @return the height of the specified image
     */
    public int getHeight(String pageName, String attachmentName)
    {
        try {
            return getProtectedPlugin().getHeight(getAttachment(pageName, attachmentName), getXWikiContext());
        } catch (Exception e) {
            LOG.error(String.format("Failed to detect the height of %s attached to %s.", attachmentName, pageName), e);
            return -1;
        }
    }

    /**
     * Detects the width of an image attached to a wiki page.
     *
     * @param pageName the name of a wiki page
     * @param attachmentName the name of an image attached to the specified page
     * @return the width of the specified image
     */
    public int getWidth(String pageName, String attachmentName)
    {
        try {
            return getProtectedPlugin().getWidth(getAttachment(pageName, attachmentName), getXWikiContext());
        } catch (Exception e) {
            LOG.error(String.format("Failed to detect the width of %s attached to %s.", attachmentName, pageName), e);
            return -1;
        }
    }

    /**
     * @param pageName the name of a wiki page
     * @param attachmentName the name of an attachment of the specified page
     * @return the specified attachment
     * @throws XWikiException if retrieving the attachment fails
     */
    private XWikiAttachment getAttachment(String pageName, String attachmentName) throws XWikiException
    {
        DocumentReference documentReference = this.currentMixedDocumentReferenceResolver.resolve(pageName);
        XWikiDocument document = getXWikiContext().getWiki().getDocument(documentReference, getXWikiContext());
        return document.getAttachment(attachmentName);
    }
}
