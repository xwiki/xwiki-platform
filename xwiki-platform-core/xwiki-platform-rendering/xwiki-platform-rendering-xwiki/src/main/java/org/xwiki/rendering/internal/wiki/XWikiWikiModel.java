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

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.w3c.css.sac.InputSource;
import org.w3c.dom.css.CSSStyleDeclaration;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.listener.reference.AttachmentResourceReference;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.wiki.WikiModel;
import org.xwiki.rendering.wiki.WikiModelException;

import com.steadystate.css.parser.CSSOMParser;
import com.steadystate.css.parser.SACParserCSS3;

/**
 * Implementation using the Document Access Bridge ({@link DocumentAccessBridge}).
 *
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
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
     * The UTF-8 encoding.
     */
    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * The component used to access configuration parameters.
     */
    @Inject
    private ExtendedRenderingConfiguration extendedRenderingConfiguration;

    /**
     * The component used to access the underlying XWiki model.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to find the URL for an icon.
     */
    @Inject
    private SkinAccessBridge skinAccessBridge;

    /**
     * The component used to serialize entity references to strings.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private EntityReferenceResolver<ResourceReference> resourceReferenceEntityReferenceResolver;

    /**
     * Provides logging for this class.
     */
    @Inject
    private Logger logger;

    /**
     * The object used to parse the CSS from the image style parameter.
     * <p>
     * NOTE: We explicitly pass the CSS SAC parser because otherwise (e.g. using the default constructor)
     * {@link CSSOMParser} sets the {@code org.w3c.css.sac.parser} system property to its own implementation, i.e.
     * {@link com.steadystate.css.parser.SACParserCSS2}, affecting other components that require a CSS SAC parser (e.g.
     * PDF export).
     *
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-5625">XWIKI-5625: PDF styling doesn't work anymore</a>
     */
    private final CSSOMParser cssParser = new CSSOMParser(new SACParserCSS3());

    /**
     * {@inheritDoc}
     * 
     * @since 2.5RC1
     */
    @Override
    public String getLinkURL(ResourceReference linkReference)
    {
        // Note that we don't ask for a full URL because links should be relative as much as possible
        EntityReference attachmentReference =
            this.resourceReferenceEntityReferenceResolver.resolve(linkReference, EntityType.ATTACHMENT);

        if (attachmentReference == null) {
            throw new IllegalArgumentException(String.valueOf(attachmentReference));
        }

        return this.documentAccessBridge.getAttachmentURL(new AttachmentReference(attachmentReference),
            linkReference.getParameter(AttachmentResourceReference.QUERY_STRING), false);
    }

    /**
     * {@inheritDoc}
     * 
     * @since 2.5RC1
     */
    @Override
    public String getImageURL(ResourceReference imageReference, Map<String, String> parameters)
    {
        // Handle icon references
        if (imageReference.getType().equals(ResourceType.ICON)) {
            return this.skinAccessBridge.getIconURL(imageReference.getReference());
        }

        // Handle attachment references
        if (this.extendedRenderingConfiguration.isImageDimensionsIncludedInImageURL()) {
            Map<String, Object> urlParameters = getImageURLParameters(parameters);
            if (!urlParameters.isEmpty()) {
                // Handle scaled image attachments.
                String queryString = imageReference.getParameter(AttachmentResourceReference.QUERY_STRING);
                queryString = extendQueryString(queryString, urlParameters);
                ResourceReference scaledImageReference = imageReference.clone();
                scaledImageReference.setParameter(AttachmentResourceReference.QUERY_STRING, queryString);
                return getLinkURL(scaledImageReference);
            }
        }

        return getLinkURL(imageReference);
    }

    private EntityReference getDocumentEntityReference(ResourceReference resourceReference)
    {
        EntityReference documentReference;

        if (resourceReference.getType().equals(ResourceType.PAGE)) {
            documentReference =
                this.resourceReferenceEntityReferenceResolver.resolve(resourceReference, EntityType.PAGE);
        } else {
            documentReference =
                this.resourceReferenceEntityReferenceResolver.resolve(resourceReference, EntityType.DOCUMENT);
        }

        return documentReference;
    }

    @Override
    public boolean isDocumentAvailable(ResourceReference resourceReference)
    {
        EntityReference documentEntityReference = getDocumentEntityReference(resourceReference);

        if (documentEntityReference == null) {
            throw new IllegalArgumentException(String.valueOf(resourceReference));
        }

        DocumentReference documentReference = this.documentAccessBridge.getDocumentReference(documentEntityReference);

        try {
            return this.documentAccessBridge.exists(documentReference);
        } catch (Exception e) {
            this.logger.error("Failed to check the existence of document with reference [{}]", documentReference, e);
        }

        return false;
    }

    @Override
    public String getDocumentViewURL(ResourceReference resourceReference)
    {
        EntityReference documentReference = getDocumentEntityReference(resourceReference);

        if (documentReference == null) {
            throw new IllegalArgumentException(String.valueOf(resourceReference));
        }

        return this.documentAccessBridge.getDocumentURL(documentReference, "view",
            resourceReference.getParameter(DocumentResourceReference.QUERY_STRING),
            resourceReference.getParameter(DocumentResourceReference.ANCHOR));
    }

    @Override
    public String getDocumentEditURL(ResourceReference resourceReference)
    {
        // Add the parent=<current document name> parameter to the query string of the edit URL so that
        // the new document is created with the current page as its parent.
        String modifiedQueryString = resourceReference.getParameter(DocumentResourceReference.QUERY_STRING);
        if (StringUtils.isBlank(modifiedQueryString)) {
            DocumentReference reference = this.documentAccessBridge.getCurrentDocumentReference();
            if (reference != null) {
                try {
                    // Note 1: we encode using UTF8 since it's the W3C recommendation.
                    // See http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars

                    // Note 2: We need to be careful to use a compact serializer so that the wiki part is not
                    // part of the generated String so that when the user clicks on the link, the new page is created
                    // with a relative parent (and thus the new page can be moved from one wiki to another easily
                    // without having to change the parent reference).

                    // TODO: Once the xwiki-url module is usable, refactor this code to use it and remove the need to
                    // perform explicit encoding here.

                    modifiedQueryString = "parent="
                        + URLEncoder.encode(this.compactEntityReferenceSerializer.serialize(reference), UTF8.name());
                } catch (UnsupportedEncodingException e) {
                    // Not supporting UTF-8 as a valid encoding for some reasons. We consider XWiki cannot work
                    // without that encoding.
                    throw new RuntimeException("Failed to URL encode [" + reference + "] using UTF-8.", e);
                }
            }
        }

        EntityReference documentReference = getDocumentEntityReference(resourceReference);

        if (documentReference == null) {
            throw new IllegalArgumentException(String.valueOf(resourceReference));
        }

        return this.documentAccessBridge.getDocumentURL(documentReference, "create", modifiedQueryString,
            resourceReference.getParameter(DocumentResourceReference.ANCHOR));
    }

    @Override
    public XDOM getXDOM(ResourceReference resourceReference) throws WikiModelException
    {
        // Currently we only support getting the XDOM for a document
        EntityReference entityReference = getDocumentEntityReference(resourceReference);

        if (entityReference == null) {
            throw new IllegalArgumentException(String.valueOf(resourceReference));
        }

        try {
            return this.documentAccessBridge.getTranslatedDocumentInstance(entityReference).getXDOM();
        } catch (Exception e) {
            throw new WikiModelException(String.format("Failed to get XDOM for [%s]", resourceReference), e);
        }
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
                CSSStyleDeclaration sd = this.cssParser.parseStyleDeclaration(new InputSource(new StringReader(style)));
                value = sd.getPropertyValue(dimension);
            } catch (Exception e) {
                // Ignore the style parameter but log a warning to let the user know.
                this.logger.warn("Failed to parse CSS style [{}]. Root cause is: [{}]", style,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        if (StringUtils.isBlank(value)) {
            // Fall back on the value of the dimension parameter.
            value = imageParameters.get(dimension);
        }
        return value;
    }

    /**
     * Creates the parameters that can be added to an image URL to resize the image on the server side.
     *
     * @param imageParameters image parameters, including width and height when they are specified
     * @return the parameters to be added to an image URL in order to resize the image on the server side
     */
    private Map<String, Object> getImageURLParameters(Map<String, String> imageParameters)
    {
        String width = StringUtils.removeEnd(getImageDimension(WIDTH, imageParameters), PIXELS);
        String height = StringUtils.removeEnd(getImageDimension(HEIGHT, imageParameters), PIXELS);
        boolean useHeight = StringUtils.isNotEmpty(height) && StringUtils.isNumeric(height);
        Map<String, Object> queryString = new LinkedHashMap<String, Object>();
        if (StringUtils.isEmpty(width) || !StringUtils.isNumeric(width)) {
            // Width is unspecified or is not measured in pixels.
            if (useHeight) {
                // Height is specified in pixels.
                queryString.put(HEIGHT, height);
            } else {
                // If image width and height are unspecified or if they are not expressed in pixels then limit the image
                // size to best fit the rectangle specified in the configuration (keeping aspect ratio).
                int widthLimit = this.extendedRenderingConfiguration.getImageWidthLimit();
                if (widthLimit > 0) {
                    queryString.put(WIDTH, widthLimit);
                }
                int heightLimit = this.extendedRenderingConfiguration.getImageHeightLimit();
                if (heightLimit > 0) {
                    queryString.put(HEIGHT, heightLimit);
                }
                if (widthLimit > 0 && heightLimit > 0) {
                    queryString.put("keepAspectRatio", true);
                }
            }
        } else {
            // Width is specified in pixels.
            queryString.put(WIDTH, width);
            if (useHeight) {
                // Height is specified in pixels.
                queryString.put(HEIGHT, height);
            }
        }
        return queryString;
    }

    private String extendQueryString(String queryString, Map<String, Object> parameters)
    {
        List<NameValuePair> pairs = new ArrayList<NameValuePair>(URLEncodedUtils.parse(queryString, UTF8, '&'));
        // Exclude the parameters that are already on the query string.
        for (NameValuePair pair : pairs) {
            parameters.remove(pair.getName());
        }
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            pairs.add(new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())));
        }
        return URLEncodedUtils.format(pairs, UTF8);
    }
}
