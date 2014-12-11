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
package org.xwiki.mail.internal.iterator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;

/**
 *  Generate messages from a user references.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class UsersMimeMessageIterator extends AbstractMessageIterator
{
    private static final String USER_SPACE = "XWiki";

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    private final List<DocumentReference> users;

    /**
     * @param userReferences the list of recipients
     * @param factory to create MimeMessage
     * @param parameters the parameters from which to extract the session, source and the headers
     */
    public UsersMimeMessageIterator(List<DocumentReference> userReferences, MimeMessageFactory factory,
        Map<String, Object> parameters)
    {
        this.factory = factory;
        this.parameters = parameters;
        this.iteratorSize = userReferences.size();
        this.users = userReferences;
    }

    @Override protected MimeMessage createMessage() throws MessagingException
    {
        DocumentReference userReference = users.get(this.position);

        String email = this.documentAccessBridge.getProperty(userReference, new DocumentReference(userReference
            .getWikiReference().getName(), USER_SPACE, "XWikiUsers"), "email").toString();

        Map<String, Object> parameters = (Map<String, Object>) this.parameters.get("parameters");
        Session session = (Session) this.parameters.get("session");

        MimeMessage mimeMessage = this.factory.createMessage(session, this.parameters.get("source"), parameters);
        mimeMessage.addRecipient(Message.RecipientType.TO, InternetAddress.parse(email)[0]);

        return mimeMessage;
    }
}
