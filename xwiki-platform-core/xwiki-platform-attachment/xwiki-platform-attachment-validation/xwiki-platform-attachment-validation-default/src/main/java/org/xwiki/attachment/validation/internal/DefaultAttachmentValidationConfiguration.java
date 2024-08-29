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
package org.xwiki.attachment.validation.internal;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE;
import static com.xpn.xwiki.plugin.fileupload.FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.ALLOWED_MIMETYPES_FIELD;
import static org.xwiki.attachment.validation.internal.AttachmentMimetypeRestrictionClassDocumentInitializer.BLOCKED_MIMETYPES_FIELD;

/**
 * Default implementation of the attachment configuration. Looks for the configuration:
 * <ol>
 *     <li>In the current space {@code WebPreferences}</li>
 *     <li>In {@code XWiki.XWikiPreferences}</li>
 *     <li>In {@code xwiki.properties}</li>
 * </ol>
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
public class DefaultAttachmentValidationConfiguration implements AttachmentValidationConfiguration
{
    private static final String ATTACHMENT_MIMETYPE_ALLOW_LIST_PROPERTY = "attachment.upload.allowList";

    private static final String ATTACHMENT_MIMETYPE_BLOCK_LIST_PROPERTY = "attachment.upload.blockList";

    @Inject
    @Named(DefaultAttachmentMimetypeRestrictionSpacesConfigurationSource.HINT)
    private ConfigurationSource attachmentConfigurationSource;

    @Inject
    @Named(DefaultAttachmentMimetypeRestrictionWikiConfigurationSource.HINT)
    private ConfigurationSource wikiConfigurationSource;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xWikiPropertiesConfigurationSource;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    @Override
    public List<String> getAllowedMimetypes()
    {
        return getPropertyWithFallback(ALLOWED_MIMETYPES_FIELD, ATTACHMENT_MIMETYPE_ALLOW_LIST_PROPERTY);
    }

    @Override
    public List<String> getAllowedMimetypes(DocumentReference documentReference)
    {
        return wrapWithScope(documentReference, this::getAllowedMimetypes);
    }

    @Override
    public List<String> getBlockerMimetypes()
    {
        return getPropertyWithFallback(BLOCKED_MIMETYPES_FIELD, ATTACHMENT_MIMETYPE_BLOCK_LIST_PROPERTY);
    }

    @Override
    public List<String> getBlockerMimetypes(DocumentReference documentReference)
    {
        return wrapWithScope(documentReference, this::getBlockerMimetypes);
    }

    @Override
    public long getMaxUploadSize(EntityReference entityReference)
    {
        XWikiContext context = this.contextProvider.get();
        XWikiDocument previous = context.getDoc();

        try {
            XWiki wiki = context.getWiki();
            if (entityReference != null) {
                context.setDoc(wiki.getDocument(entityReference, context));
            }
            return wiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context);
        } catch (XWikiException e) {
            this.logger.warn("Failed to resolve the entity [{}]. Cause: [{}]", entityReference, getRootCauseMessage(e));
            return UPLOAD_DEFAULT_MAXSIZE;
        } finally {
            context.setDoc(previous);
        }
    }

    private List<String> getPropertyWithFallback(String attachmentXObjectProperty, String xWikiPropertiesProperty)
    {
        return get(this.attachmentConfigurationSource, attachmentXObjectProperty)
            .or(() -> get(this.wikiConfigurationSource, attachmentXObjectProperty))
            .or(() -> get(this.xWikiPropertiesConfigurationSource, xWikiPropertiesProperty))
            .orElse(List.of());
    }

    private Optional<List<String>> get(ConfigurationSource attachmentConfigurationSource, String key)
    {
        Optional<List<String>> result;
        if (!attachmentConfigurationSource.containsKey(key)) {
            result = Optional.empty();
        } else {
            List<String> property = attachmentConfigurationSource.getProperty(key);
            if (property.isEmpty()) {
                result = Optional.empty();
            } else {
                result = Optional.of(property);
            }
        }
        return result;
    }

    private List<String> wrapWithScope(DocumentReference documentReference, Supplier<List<String>> supplier)
    {
        XWikiContext xWikiContext = this.contextProvider.get();
        XWikiDocument oldDoc = xWikiContext.getDoc();
        try {
            xWikiContext.setDoc(xWikiContext.getWiki().getDocument(documentReference, xWikiContext));
            return supplier.get();
        } catch (XWikiException e) {
            this.logger.warn("Failed to get document [{}]. Cause: [{}]", documentReference, getRootCauseMessage(e));
            return List.of();
        } finally {
            xWikiContext.setDoc(oldDoc);
        }
    }
}
