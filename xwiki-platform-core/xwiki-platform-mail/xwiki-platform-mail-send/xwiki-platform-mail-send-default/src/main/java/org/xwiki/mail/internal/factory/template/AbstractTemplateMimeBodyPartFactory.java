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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.factory.AbstractMimeBodyPartFactory;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Creates an Body Part from a Document Reference pointing to a Document containing an XWiki.Mail XObject (the first one
 * found is used).
 *
 * @version $Id$
 * @since 6.1RC1
 */
public abstract class AbstractTemplateMimeBodyPartFactory extends AbstractMimeBodyPartFactory<DocumentReference>
{
    private static final String ATTACHMENT_PROPERTY_NAME = "attachments";

    private static final String INCLUDE_TEMPLATE_ATTACHMENTS_PROPERTY_NAME = "includeTemplateAttachments";

    @Inject
    @Named("text/html")
    private MimeBodyPartFactory<String> htmlBodyPartFactory;

    @Inject
    private DocumentAccessBridge bridge;

    @Inject
    private AttachmentConverter attachmentConverter;

    /**
     * @return the Template Manager instance to use, this allows passing either the default component implementation or
     * a secure one for scripts
     */
    protected abstract MailTemplateManager getTemplateManager();

    @Override
    public MimeBodyPart create(DocumentReference documentReference, Map<String, Object> parameters)
        throws MessagingException
    {
        Map<String, Object> velocityVariables = (Map<String, Object>) parameters.get("velocityVariables");

        Object localeValue = parameters.get("language");

        String textContent = getTemplateManager().evaluate(documentReference, "text", velocityVariables, localeValue);
        String htmlContent = getTemplateManager().evaluate(documentReference, "html", velocityVariables, localeValue);

        Map<String, Object> htmlParameters = new HashMap<>();
        htmlParameters.put("alternate", textContent);

        // Handle attachments:
        // - if the user has passed an "attachments" property with a list of attachment then add them
        // - if the user has set the "includeTemplateAttachments" property then add all attachments found in the
        //   template document too
        List<Attachment> attachments = new ArrayList<>();
        List<Attachment> parameterAttachments = (List<Attachment>) parameters.get(ATTACHMENT_PROPERTY_NAME);
        if (parameterAttachments != null) {
            attachments.addAll(parameterAttachments);
        }
        Boolean includeTemplateAttachments = (Boolean) parameters.get(INCLUDE_TEMPLATE_ATTACHMENTS_PROPERTY_NAME);
        if (includeTemplateAttachments != null && includeTemplateAttachments) {
            try {
                List<XWikiAttachment> xwikiAttachments =
                    ((XWikiDocument) this.bridge.getDocumentInstance(documentReference)).getAttachmentList();
                attachments.addAll(this.attachmentConverter.convert(xwikiAttachments));
            } catch (Exception e) {
                throw new MessagingException(
                    String.format("Failed to include attachments from the Mail Template [%s]", documentReference),
                    e);
            }
        }
        if (!attachments.isEmpty()) {
            htmlParameters.put(ATTACHMENT_PROPERTY_NAME, attachments);
        }

        return this.htmlBodyPartFactory.create(htmlContent, htmlParameters);
    }
}
