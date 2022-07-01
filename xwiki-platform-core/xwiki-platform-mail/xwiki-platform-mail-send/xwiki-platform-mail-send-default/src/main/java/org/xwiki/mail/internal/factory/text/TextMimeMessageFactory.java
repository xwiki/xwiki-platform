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
package org.xwiki.mail.internal.factory.text;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeBodyPartFactory;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.factory.AbstractMimeMessageFactory;
import org.xwiki.properties.ConverterManager;

/**
 * A basic {@link MimeMessageFactory} which is taken a {@link String} as source which represents the content of the
 * message.
 *
 * @version $Id$
 * @since 14.6RC1
 * @since 14.4.3
 * @since 13.10.8
 */
@Component
@Singleton
@Named("text")
public class TextMimeMessageFactory extends AbstractMimeMessageFactory<MimeMessage>
{
    @Inject
    private ConverterManager converterManager;

    @Inject
    private MimeBodyPartFactory<String> mimeBodyPartFactory;

    @Override
    public MimeMessage createMessage(Object source, Map<String, Object> parameters) throws MessagingException
    {
        // This whole code has been inspired by the implementation of
        // org.xwiki.mail.internal.factory.template.AbstractTemplateMimeMessageFactory

        // Note: We don't create a Session here ATM since it's not required. The returned MimeMessage will be
        // given a valid Session when it's deserialized from the mail content store for sending.
        ExtendedMimeMessage message = new ExtendedMimeMessage();

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
            message.setType(type);
        }

        // Handle the subject. Get it from the parameters.
        String subject = (String) parameters.get("subject");
        message.setSubject(subject);

        // Add a default body part taken from the template.
        Multipart multipart = new MimeMultipart("mixed");
        multipart.addBodyPart(this.mimeBodyPartFactory.create((String) source, parameters));
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
