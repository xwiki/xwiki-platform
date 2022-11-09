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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
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
    private AttachmentValidationConfiguration attachmentValidationConfiguration;

    /**
     * @return the list of allowed attachment mimetype regex (e.g., "image/png", "text/.*")
     */
    public List<String> getAllowedMimetypes()
    {
        return this.attachmentValidationConfiguration.getAllowedMimetypes();
    }

    /**
     * @return the list of blocker attachment mimetype regex (e.g., "image/png", "text/.*")
     */
    public List<String> getBlockerMimetypes()
    {
        return this.attachmentValidationConfiguration.getBlockerMimetypes();
    }
}
