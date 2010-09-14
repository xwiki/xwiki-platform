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
package org.xwiki.rendering.internal.wiki;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.internal.configuration.XWikiRenderingConfiguration;
import org.xwiki.rendering.wiki.WikiModel;

import com.steadystate.css.parser.CSSOMParser;

/**
 * Implementation using the Document Access Bridge ({@link DocumentAccessBridge}).
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class XWikiWikiModel implements WikiModel
{
    /**
     * The suffix used to mark an amount of pixels.
     */
    private static final String PIXELS = "px";

    /**
     * The name of the {@code width} image parameter.
     */
    private static final String WIDTH = "width";

    /**
     * The name of the {@code height} image parameter.
     */
    private static final String HEIGHT = "height";

    /**
     * The component used to access configuration parameters.
     */
    @Requirement
    private XWikiRenderingConfiguration xwikiRenderingConfiguration;

    /**
     * The component used to access the underlying XWiki model.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The component used to serialize entity references to strings.
     */
    @Requirement
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The object used to parse the CSS from the image style parameter.
     */
    private final CSSOMParser cssParser = new CSSOMParser();

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getAttachmentURL(String, String)
     */
    public String getAttachmentURL(String documentName, String attachmentName)
    {
        return this.documentAccessBridge.getAttachmentURL(documentName, attachmentName);
    }

    /**
     * Extracts the specified image dimension from the image parameters.
     * 
     * @param dimension either {@code width} or {@code height}
     * @param imageParameters the image parameters; may include the {@code width}, {@code height} and {@code style}
     *            parameters
     * @return the value of the passed dimension if it is specified in the image parameters, {@code null} otherwise
     */
    private String getImageDimension(String dimension, Map<String, String> imageParameters)
    {
        // Check first if the style parameter contains information about the given dimension. In-line style has priority
        // over the dimension parameters.
        String value = null;
        String style = imageParameters.get("style");
        if (StringUtils.isNotBlank(style)) {
            try {
                CSSStyleDeclaration sd = cssParser.parseStyleDeclaration(new InputSource(new StringReader(style)));
                value = sd.getPropertyValue(dimension);
            } catch (IOException e) {
                // Ignore the style parameter.
            }
        }
        if (StringUtils.isBlank(value)) {
            // Fall back on the value of the dimension parameter.
            value = imageParameters.get(dimension);
        }
        return value;
    }

    /**
     * Creates the query string that can be added to an image URL to resize the image on the server side.
     * 
     * @param imageParameters image parameters, including width and height then they are specified
     * @return the query string to be added to an image URL in order to resize the image on the server side
     */
    private StringBuilder getImageURLQueryString(Map<String, String> imageParameters)
    {
        String width = StringUtils.chomp(getImageDimension(WIDTH, imageParameters), PIXELS);
        String height = StringUtils.chomp(getImageDimension(HEIGHT, imageParameters), PIXELS);
        boolean useHeight = StringUtils.isNotEmpty(height) && StringUtils.isNumeric(height);
        StringBuilder queryString = new StringBuilder();
        if (StringUtils.isEmpty(width) || !StringUtils.isNumeric(width)) {
            // Width is unspecified or is not measured in pixels.
            if (useHeight) {
                // Height is specified in pixels.
                queryString.append('&').append(HEIGHT).append('=').append(height);
            } else {
                // If image width and height are unspecified or if they are not expressed in pixels then limit the image
                // size to best fit the rectangle specified in the configuration (keeping aspect ratio).
                int widthLimit = xwikiRenderingConfiguration.getImageWidthLimit();
                if (widthLimit > 0) {
                    queryString.append('&').append(WIDTH).append('=').append(widthLimit);
                }
                int heightLimit = xwikiRenderingConfiguration.getImageHeightLimit();
                if (heightLimit > 0) {
                    queryString.append('&').append(HEIGHT).append('=').append(heightLimit);
                }
                if (widthLimit > 0 && heightLimit > 0) {
                    queryString.append("&keepAspectRatio=").append(true);
                }
            }
        } else {
            // Width is specified in pixels.
            queryString.append('&').append(WIDTH).append('=').append(width);
            if (useHeight) {
                // Height is specified in pixels.
                queryString.append('&').append(HEIGHT).append('=').append(height);
            }
        }
        return queryString;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.wiki.WikiModel#getImageURL(java.lang.String, java.lang.String, java.util.Map)
     */
    public String getImageURL(String documentName, String attachmentName, Map<String, String> parameters)
    {
        String url = getAttachmentURL(documentName, attachmentName);
        if (!xwikiRenderingConfiguration.isImageDimensionsIncludedInImageURL()) {
            return url;
        }

        StringBuilder queryString = getImageURLQueryString(parameters);
        if (queryString.length() == 0) {
            return url;
        }

        // Determine the insertion point.
        int insertionPoint = url.lastIndexOf('#');
        if (insertionPoint < 0) {
            // No fragment identifier.
            insertionPoint = url.length();
        }
        if (url.lastIndexOf('?', insertionPoint) < 0) {
            // No query string.
            queryString.setCharAt(0, '?');
        }

        // Insert the query string.
        return new StringBuilder(url).insert(insertionPoint, queryString).toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#isDocumentAvailable(String)
     */
    public boolean isDocumentAvailable(String documentName)
    {
        return this.documentAccessBridge.exists(documentName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentViewURL(String, String, String)
     */
    public String getDocumentViewURL(String documentName, String anchor, String queryString)
    {
        return this.documentAccessBridge.getURL(documentName, "view", queryString, anchor);
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModel#getDocumentEditURL(String, String, String)
     */
    public String getDocumentEditURL(String documentName, String anchor, String queryString)
    {
        // Add the parent=<current document name> parameter to the query string of the edit URL so that
        // the new document is created with the current page as its parent.
        String modifiedQueryString = queryString;
        if (StringUtils.isBlank(queryString)) {
            DocumentReference reference = this.documentAccessBridge.getCurrentDocumentReference();
            if (reference != null) {
                try {
                    // Note: we encode using UTF8 since it's the W3C recommendation.
                    // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars
                    // TODO: Once the xwiki-url module is usable, refactor this code to use it and remove the need to
                    // perform explicit encoding here.
                    modifiedQueryString =
                        "parent=" + URLEncoder.encode(this.entityReferenceSerializer.serialize(reference), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                    // without that encoding.
                    throw new RuntimeException("Failed to URL encode ["
                        + this.entityReferenceSerializer.serialize(reference) + "] using UTF-8.", e);
                }
            }
        }

        return this.documentAccessBridge.getURL(documentName, "create", modifiedQueryString, anchor);
    }
}
