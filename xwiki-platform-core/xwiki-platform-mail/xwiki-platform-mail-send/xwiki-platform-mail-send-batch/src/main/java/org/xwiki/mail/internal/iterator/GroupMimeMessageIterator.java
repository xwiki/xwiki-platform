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

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Generate messages from a group reference.
 *
 * @version $Id$
 * @since 6.4M2
 */
public class GroupMimeMessageIterator extends AbstractMessageIterator
{
    private static final String USER_SPACE = "XWiki";

    private static final EntityReference GROUPS_CLASS =
        new EntityReference("XWikiGroups", EntityType.DOCUMENT, new EntityReference(USER_SPACE, EntityType.SPACE));

    @Inject
    private Execution execution;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    private DocumentReference groupReference;

    /**
     * @param groupReference the group that contains list of recipients
     * @param factory to create MimeMessage
     * @param parameters the parameters from which to extract the session, source and the headers
     * @throws MessagingException when an error occurs when retrieving the number of users
     */
    public GroupMimeMessageIterator(DocumentReference groupReference, MimeMessageFactory factory,
        Map<String, Object> parameters) throws MessagingException
    {
        this.factory = factory;
        this.parameters = parameters;
        this.groupReference = groupReference;

        XWikiContext context = getXWikiContext();
        try {
            this.iteratorSize = context.getWiki().getDocument(groupReference, context).getXObjects(GROUPS_CLASS).size();
        } catch (XWikiException e) {
            throw new MessagingException(String.format(
                "Failed to find number of [%s] objects in group Document [%s]", GROUPS_CLASS, groupReference), e);
        }
    }

    @Override protected MimeMessage createMessage() throws MessagingException
    {
        DocumentReference groupsClassReference = this.resolver.resolve(GROUPS_CLASS);
        String userFullName =
            this.documentAccessBridge.getProperty(this.groupReference, groupsClassReference, this.position, "member")
                .toString();

        DocumentReference userReference = documentReferenceResolver.resolve(userFullName);

        String email = this.documentAccessBridge.getProperty(userReference, new DocumentReference(userReference
            .getWikiReference().getName(), USER_SPACE, "XWikiUsers"), "email").toString();

        Map<String, Object> parameters = (Map<String, Object>) this.parameters.get("parameters");
        Session session = (Session) this.parameters.get("session");

        MimeMessage mimeMessage = this.factory.createMessage(session, this.parameters.get("source"), parameters);
        mimeMessage.addRecipient(Message.RecipientType.TO, InternetAddress.parse(email)[0]);

        return mimeMessage;
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
