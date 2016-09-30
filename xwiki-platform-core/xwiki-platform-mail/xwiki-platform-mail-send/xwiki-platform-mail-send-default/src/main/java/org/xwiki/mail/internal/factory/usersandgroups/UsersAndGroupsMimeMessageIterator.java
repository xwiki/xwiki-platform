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
package org.xwiki.mail.internal.factory.usersandgroups;

import java.util.Iterator;
import java.util.Map;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.context.Execution;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Generate messages from a list of group references, a list of user references and a list of email addresses. Handles
 * duplicates so that an email address is sent the message only once.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
public class UsersAndGroupsMimeMessageIterator implements Iterator<MimeMessage>, Iterable<MimeMessage>
{
    private MimeMessageFactory<MimeMessage> factory;

    private Iterator<Address> addressIterator;

    private Map<String, Object> parameters;

    /**
     * @param source the list of group and user references from which to extract the list of recipients and a list of
     *        emails to send the messages to
     * @param factory the factory to use to create a single MimeMessage
     * @param parameters the parameters from which to extract the factory source and factory parameters
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *        {@link org.xwiki.model.reference.DocumentReference}
     * @param execution the component used to access the {@link com.xpn.xwiki.XWikiContext} we use to call oldcore APIs
     * @throws MessagingException if one the passed email addresses is invalid (note that we're not parsing emails in
     *         strict mode and thus it's unlikely any exception will be raised in practice)
     */
    public UsersAndGroupsMimeMessageIterator(Map<String, Object> source,
        MimeMessageFactory<MimeMessage> factory, Map<String, Object> parameters,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver, Execution execution)
        throws MessagingException
    {
        this.addressIterator = new AddressUserIterator(UsersAndGroupsSource.parse(source),
            explicitDocumentReferenceResolver, execution);
        this.factory = factory;
        this.parameters = parameters;
    }

    @Override
    public Iterator<MimeMessage> iterator()
    {
        return this;
    }

    @Override
    public boolean hasNext()
    {
        return this.addressIterator.hasNext();
    }

    @Override
    public MimeMessage next()
    {
        MimeMessage message;
        Address address = this.addressIterator.next();

        try {
            Map<String, Object> factoryParameters = (Map<String, Object>) this.parameters.get("parameters");
            message = this.factory.createMessage(this.parameters.get("source"), factoryParameters);
            message.addRecipient(Message.RecipientType.TO, address);

            // Set the Message Type if passed in parameters
            String type = (String) this.parameters.get("type");
            if (type != null) {
                ExtendedMimeMessage.wrap(message).setType(type);
            }
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to create Mime Message for recipient " + address, e);
        }

        return message;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException("remove");
    }
}
