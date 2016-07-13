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
package org.xwiki.mail.internal.factory.message;

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.internal.factory.AbstractMimeMessageFactory;

/**
 * This factory is useful when used by another MimeMessageFactory which delegates the creation of MimeMessages
 * to another MimeMessageFactory, such as with a UserAndGroup Iterator MimeMessageFactory. For example:
 *
 * <code>
 * {{velocity}}
 * ## Create a mime message, the way you like it, adding any part you like, without recipient.
 * #set ($message = $services.mailsender.createMessage('localhost@xwiki.org', null, 'SendMimeMessageToGroup'))
 * #set ($discard = $message.addPart('text/plain', 'text content'))
 *
 * ## Use the mime message cloning factory as message factory to duplicate the created message
 * #set ($parameters = {'hint' : 'message', 'source' : $message})
 *
 * #set ($source = {'groups' : [$services.model.createDocumentReference('', 'XWiki', 'XWikiAllGroup')]})
 *
 * #set ($messages = $services.mailsender.createMessages('usersandgroups', $source, $parameters))
 * #set ($result = $services.mailsender.send($messages, 'database'))
 * {{/velocity}}
 * </code>
 *
 * Since 7.4.1, messages generated all receive the same MessageId header since these are clone of the same message.
 * This behavior allow sending newsletter/mailing list message independently to many subscribers
 * while allowing them to interact on the same thread of message, since all message are identified to be the same.
 *
 * @version $Id$
 * @since 7.1M2
 */
@Component
@Named("message")
@Singleton
public class MessageMimeMessageFactory extends AbstractMimeMessageFactory<MimeMessage>
{
    @Override
    public MimeMessage createMessage(Object source, Map<String, Object> parameters)
        throws MessagingException
    {
        if (!(source instanceof MimeMessage)) {
            throw new MessagingException(
                String.format("Failed to create mime message from source [%s]", source.getClass()));
        }

        return new ExtendedMimeMessage((MimeMessage) source);
    }
}
