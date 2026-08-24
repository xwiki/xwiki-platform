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
package org.xwiki.attachment.validation;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

/**
 * Script service for the attachment validation. Provide the attachment validation configuration values.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Singleton
@Named("attachmentValidation")
public class AttachmentValidationScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private Logger logger;

    /**
     * @return the list of allowed attachment mimetypes of the current document. A joker (@code '*') can be used to
     *     match any media (e.g., "image/png", "text/*")
     */
    public List<String> getAllowedMimetypes()
    {
        return getAttachmentValidationConfiguration()
            .map(AttachmentValidationConfiguration::getAllowedMimetypes)
            .orElse(List.of());
    }

    /**
     * @param documentReference the reference of a document
     * @return the list of allowed attachment mimetypes of the provided document. A joker (@code '*') can be used to
     *     match any media (e.g., "image/png", "text/*")
     * @since 14.10.2
     * @since 15.0RC1
     */
    public List<String> getAllowedMimetypes(DocumentReference documentReference)
    {
        return getAttachmentValidationConfiguration()
            .map(attachmentValidationConfiguration -> attachmentValidationConfiguration.getAllowedMimetypes(
                documentReference))
            .orElse(List.of());
    }

    /**
     * @return the list of blocker attachment mimetype of the current document. A joker (@code '*') can be used to match
     *     any media (e.g., "image/png", "text/*")
     */
    public List<String> getBlockerMimetypes()
    {
        return getAttachmentValidationConfiguration()
            .map(AttachmentValidationConfiguration::getBlockerMimetypes)
            .orElse(List.of());
    }

    /**
     * @param documentReference the reference of a document
     * @return the list of blocker attachment mimetypes of the provided document. A joker (@code '*') can be used to
     *     match any media (e.g., "image/png", "text/*")
     * @since 14.10.2
     * @since 15.0RC1
     */
    public List<String> getBlockerMimetypes(DocumentReference documentReference)
    {
        return getAttachmentValidationConfiguration()
            .map(attachmentValidationConfiguration -> attachmentValidationConfiguration.getBlockerMimetypes(
                documentReference))
            .orElse(List.of());
    }

    /**
     * @param entityReference the entity reference to use as the context when resolving the configuration, or the
     *     current entity of {@code null}
     * @return the maximum file size allowed for a given entity, in bytes
     * @since 15.5RC1
     * @since 14.10.13
     */
    public long getUploadMaxSize(EntityReference entityReference)
    {
        return getAttachmentValidationConfiguration()
            .map(attachmentValidationConfiguration -> attachmentValidationConfiguration
                .getMaxUploadSize(entityReference))
            .orElse(0L);
    }

    /**
     * @return the maximum file size allowed for the current document, in bytes
     * @since 15.5RC1
     * @since 14.10.13
     */
    public long getUploadMaxSize()
    {
        return getUploadMaxSize(null);
    }

    private Optional<AttachmentValidationConfiguration> getAttachmentValidationConfiguration()
    {
        try {
            AttachmentValidationConfiguration attachmentValidationConfiguration =
                this.componentManager.get().getInstance(AttachmentValidationConfiguration.class);
            return Optional.of(attachmentValidationConfiguration);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to retrieve an instance of [{}] with hint [default].",
                AttachmentValidationConfiguration.class.getName());
            return Optional.empty();
        }
    }
}
