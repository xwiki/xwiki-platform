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

import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.xwiki.localization.LocaleUtils;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.internal.ExtendedMimeMessage;
import org.xwiki.mail.internal.factory.AbstractMimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;

/**
 * Creates a Mime Message with the subject pre-filled with the evaluated "subject" xproperty found in an "XWiki.Mail"
 * xobject located in the template document pointed to by the passed reference.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public abstract class AbstractTemplateMimeMessageFactory extends AbstractMimeMessageFactory<MimeMessage>
{
    @Inject
    private ConverterManager converterManager;

    /**
     * @return the Template Manager instance to use, this allows passing either the default component implementation or
     * a secure one for scripts
     */
    protected abstract MailTemplateManager getTemplateManager();

    /**
     * @return the Body Part Factory instance to use, this allows passing either the default component implementation or
     * a secure one for scripts
     */
    protected abstract MimeBodyPartFactory<DocumentReference> getMimeBodyPartFactory();

    @Override
    public MimeMessage createMessage(Session session, Object templateReferenceObject,
        Map<String, Object> parameters) throws MessagingException
    {
        DocumentReference templateReference = getTypedSource(templateReferenceObject, DocumentReference.class);
        MimeMessage message = new ExtendedMimeMessage(session);

        // Handle optional "from" address.
        Address from = this.converterManager.convert(Address.class, parameters.get("from"));
        if (from != null) {
            message.setFrom(from);
        }

        // Handle optional "to", "cc" and "bcc" addresses.
        setRecipient(message, Message.RecipientType.TO, parameters.get("to"));
        setRecipient(message, Message.RecipientType.CC, parameters.get("cc"));
        setRecipient(message, Message.RecipientType.BCC, parameters.get("bcc"));

        // Handle optional "type" parameter to set the mail type
        // Set the Message type if passed in parameters
        String type = (String) parameters.get("type");
        if (type != null) {
            message.addHeader("X-MailType", type);
        }

        // Handle the subject. Get it from the template
        Map<String, String> velocityVariables = (Map<String, String>) parameters.get("velocityVariables");
        String language = (String) parameters.get("language");
        Locale locale = LocaleUtils.toLocale(language);
        String subject = getTemplateManager().evaluate(templateReference, "subject", velocityVariables, locale);
        message.setSubject(subject);

        // Add a default body part taken from the template.
        Multipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(getMimeBodyPartFactory().create(templateReference, parameters));
        message.setContent(multipart);

        return message;
    }

    private void setRecipient(MimeMessage message, Message.RecipientType type, Object value)
        throws MessagingException
    {
        Address[] addresses = this.converterManager.convert(Address[].class, value);
        if (addresses != null) {
            message.setRecipients(type, addresses);
        }
    }
}
