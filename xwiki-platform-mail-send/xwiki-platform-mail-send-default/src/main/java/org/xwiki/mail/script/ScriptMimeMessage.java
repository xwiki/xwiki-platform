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
package org.xwiki.mail.script;

import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MimeBodyPartFactory;

/**
 * Extends {@link javax.mail.internet.MimeMessage} to add helper APIs to add body part content and to send the message.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ScriptMimeMessage extends MimeMessage
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScriptMimeMessage.class);

    private ComponentManager componentManager;

    private MailSender mailSender;

    private Execution execution;

    private Multipart multipart;

    public ScriptMimeMessage(Session session, MailSender mailSender, Execution execution,
        ComponentManager componentManager)
    {
        super(session);

        this.mailSender = mailSender;
        this.execution = execution;
        this.componentManager = componentManager;
    }

    public void addPart(String mimeType, Object content, Map<String, Object> parameters) throws MessagingException
    {
        MimeBodyPartFactory factory = getBodyPartFactory(mimeType, content.getClass());

        // If this is the first Part we create the MultiPart object.
        if (this.multipart == null) {
            this.multipart = new MimeMultipart("mixed");
        }
        MimeBodyPart part = factory.create(content, parameters);
        this.multipart.addBodyPart(part);
    }

    public void send()
    {
        try {
            this.mailSender.send(this, this.session);
        } catch (MessagingException e) {
            // Save the exception for reporting through the script services's getError() API
            this.execution.getContext().setProperty(MailSenderScriptService.ERROR_KEY, e);
        }
    }

    public void waitTillSent(long timeout)
    {
        this.mailSender.waitTillSent(timeout);
    }

    private MimeBodyPartFactory getBodyPartFactory(String mimeType, Class contentClass) throws MessagingException
    {
        MimeBodyPartFactory factory;
        try {
            // Look for a specific MimeBodyPartFactory for the passed Mime Type and Content type.
            factory = this.componentManager.getInstance(new DefaultParameterizedType(null, MimeBodyPartFactory.class,
                contentClass),  mimeType);
        } catch (ComponentLookupException e) {
            // No factory found for the passed Mime Type and type of Content.
            // If the content class is of type String then we default to the default MimeBodyPartFactory for String
            // content.
            try {
                factory = this.componentManager.getInstance(
                    new DefaultParameterizedType(null, MimeBodyPartFactory.class, String.class));
            } catch (ComponentLookupException ee) {
                // This shouldn't happen, if it does then it's an error and we want that error to bubble up till the
                // user since it would be pretty bad to send an email with some missing body part!
                throw new MessagingException(String.format(
                    "Failed to find default Mime Body Part Factory for mime type [%s] and Content type [%s]",
                        mimeType, contentClass.getName()), e);
            }
        }
        return factory;
    }
}
