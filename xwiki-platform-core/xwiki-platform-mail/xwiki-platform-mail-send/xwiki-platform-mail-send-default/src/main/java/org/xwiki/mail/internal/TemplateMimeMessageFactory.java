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
package org.xwiki.mail.internal;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;

import static java.util.Arrays.asList;

/**
 * Creates mime message with the subject pre-filled with evaluated subject xproperty from an XWiki.Mail xobject in the
 * Document pointed to by the passed documentReference.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("template")
@Singleton
public class TemplateMimeMessageFactory implements MimeMessageFactory
{
    @Inject
    private DefaultMailTemplateManager mailTemplateManager;

    @Override
    public MimeMessage createMessage(Session session, Object... parameters) throws MessagingException
    {
        MimeMessage message = new MimeMessage(session);

        DocumentReference documentReference = (DocumentReference) parameters[0];
        Map<String, String> data = Collections.emptyMap();

        if (asList(parameters).size() == 3) {

            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress((String) parameters[1]));
            data = (Map<String, String>) parameters[2];

        } else if (asList(parameters).size() == 4) {

            message.setFrom(new InternetAddress((String) parameters[1]));

            message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress((String) parameters[2]));

            data = (Map<String, String>) parameters[3];
        }

        String subject = this.mailTemplateManager.evaluate(documentReference, "subject", data);
        message.setSubject(subject);

        return message;
    }
}
