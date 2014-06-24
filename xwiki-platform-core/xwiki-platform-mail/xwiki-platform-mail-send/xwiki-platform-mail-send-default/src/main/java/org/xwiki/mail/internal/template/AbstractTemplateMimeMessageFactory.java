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
package org.xwiki.mail.internal.template;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;

/**
 * Creates mime message with the subject pre-filled with evaluated subject xproperty from an XWiki.Mail xobject in the
 * Document pointed to by the passed documentReference.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public abstract class AbstractTemplateMimeMessageFactory implements MimeMessageFactory
{
    /**
     * @return the Template Manager instance to use, this allows passing either the default component implementation or
     * a secure one for scripts
     */
    protected abstract MailTemplateManager getTemplateManager();

    @Override
    public MimeMessage createMessage(Session session, Object source, Map parameters) throws MessagingException
    {
        MimeMessage message = new MimeMessage(session);
        DocumentReference documentReference = (DocumentReference) source;

        Map<String, String> velocityVariables = (Map<String, String>) parameters.get("velocityVariables");

        String language = (String) parameters.get("language");

        String subject = getTemplateManager()
                .evaluate(documentReference, "subject", (Map<String, String>) velocityVariables, language);
        message.setSubject(subject);
        return message;
    }
}
