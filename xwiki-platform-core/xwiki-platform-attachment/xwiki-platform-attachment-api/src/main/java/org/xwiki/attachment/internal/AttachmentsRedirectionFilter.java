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
package org.xwiki.attachment.internal;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.resource.ResourceReferenceManager;
import org.xwiki.resource.entity.EntityResourceReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.redirection.RedirectionFilter;
import com.xpn.xwiki.web.XWikiRequest;

import static com.xpn.xwiki.web.DownloadAction.ACTION_NAME;

/**
 * Check and proceed to a redirection for an attachment if needed. Attachment redirections are persisted in {@code
 * Attachment.Code.RedirectAttachmentClass} XObjects.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named("Attachment.Code.RedirectAttachmentClass")
public class AttachmentsRedirectionFilter implements RedirectionFilter
{
    @Inject
    private ResourceReferenceManager resourceReferenceManager;

    @Inject
    private AttachmentsManager attachmentsManager;
    
    @Override
    public boolean redirect(XWikiContext context) throws XWikiException
    {
        boolean redirect = false;
        if (Objects.equals(context.getAction(), ACTION_NAME)) {
            AttachmentReference attachmentReference = getAttachmentReference();
            Optional<AttachmentReference> redirection = this.attachmentsManager.getRedirection(attachmentReference);
            if (redirection.isPresent()) {
                doRedirect(context, redirection.get());
                redirect = true;
            }
        }
        return redirect;
    }

    private void doRedirect(XWikiContext context, AttachmentReference attachment) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        String url = context.getWiki().getURL(attachment, context.getAction(),
            request.getQueryString(), null, context);

        // Send the redirection
        try {
            context.getResponse().sendRedirect(url);
        } catch (IOException e) {
            throw new XWikiException(String.format("Failed to redirect to attachment [%s].", attachment), e);
        }
    }

    private AttachmentReference getAttachmentReference()
    {
        EntityResourceReference entityResource =
            (EntityResourceReference) this.resourceReferenceManager.getResourceReference();
        return new AttachmentReference(entityResource.getEntityReference().extractReference(EntityType.ATTACHMENT));
    }
}
