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
package org.xwiki.attachment.script;

import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.attachment.MoveAttachmentRequest;
import org.xwiki.attachment.internal.AttachmentsManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiException;

/**
 * TODO: document me.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named("attachment")
public class AttachmentScriptService implements ScriptService
{
    @Inject
    private AttachmentsManager attachmentsManager;

    // TODO: add parameters and document
    // TODO: do we keep everything in this module?
    public MoveAttachmentRequest createRenameRequest(Collection<EntityReference> sources, EntityReference destination)
    {
        MoveAttachmentRequest moveAttachmentRequest = new MoveAttachmentRequest();
        moveAttachmentRequest.setEntityReferences(sources);
        moveAttachmentRequest.setProperty("destination", destination);
        return moveAttachmentRequest;
    }

    /**
     * @param wikiName the name of the wiki
     * @param spaceName the name of the space(s) (e.g., "Main" or "Main.Sub")
     * @param pageName the name of the page
     * @param attachmentName the name of the attachment
     * @return {@code true} if the attachment exists or has existed (but was moved with a redirection) at the requested
     *     location.
     */
    public boolean attachmentExists(String wikiName, String spaceName, String pageName, String attachmentName)
    {
        try {
            return this.attachmentsManager.exists(
                new AttachmentReference(attachmentName, new DocumentReference(wikiName, spaceName, pageName)));
        } catch (XWikiException e) {
            // TODO: check best practices for exception is SS.
            return false;
        }
    }
}
