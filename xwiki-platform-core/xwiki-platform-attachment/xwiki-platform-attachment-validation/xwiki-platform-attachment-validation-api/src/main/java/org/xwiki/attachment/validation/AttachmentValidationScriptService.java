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
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Script service for the attachment validation. Provide the attachment validation configuration values.
 *
 * @version $Id$
 * @since 14.10RC1
 */
@Component
@Singleton
@Named("attachmentValidation")
@Unstable
public class AttachmentValidationScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManager;

    @Inject
    private Logger logger;

    /**
     * @return the list of allowed attachment mimetypes. A joker (@code '*') can be used to match any media (e.g.,
     *     "image/png", "text/*")
     */
    public List<String> getAllowedMimetypes()
    {
        return getAttachmentValidationConfiguration()
            .map(AttachmentValidationConfiguration::getAllowedMimetypes)
            .orElse(List.of());
    }

    /**
     * @return the list of blocker attachment mimetype. A joker (@code '*') can be used to match any media (e.g.,
     *     "image/png", "text/*")
     */
    public List<String> getBlockerMimetypes()
    {
        return getAttachmentValidationConfiguration()
            .map(AttachmentValidationConfiguration::getBlockerMimetypes)
            .orElse(List.of());
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
