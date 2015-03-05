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
package org.xwiki.mail.internal.factory.template;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Internal component to convert from {@link com.xpn.xwiki.api.Attachment} to {@link com.xpn.xwiki.doc.XWikiAttachment}.
 * Ideally this shouldn't be needed but at the moment our
 * {@link org.xwiki.mail.internal.factory.attachment.AttachmentMimeBodyPartFactory} only accepts an
 * {@link com.xpn.xwiki.api.Attachment} instance.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Singleton
public class DefaultAttachmentConverter implements AttachmentConverter
{
    @Inject
    private Execution execution;

    @Override
    public List<Attachment> convert(List<XWikiAttachment> attachments)
    {
        XWikiContext xwikiContext = getXWikiContext();
        List<Attachment> attachmentList = new ArrayList<>();
        for (XWikiAttachment attachment : attachments) {
            attachmentList.add(new Attachment(
                new Document(attachment.getDoc(), xwikiContext), attachment, xwikiContext));
        }
        return attachmentList;
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
