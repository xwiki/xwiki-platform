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
package org.xwiki.mail.internal.factory.group;

import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.factory.AbstractMessageIterator;
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
 * @since 6.4M3
 * @deprecated starting with 6.4.2 this is replaced by the {@code usersandroups} Mime Message Factory
 */
@Deprecated
public class GroupMimeMessageIterator extends AbstractMessageIterator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupMimeMessageIterator.class);

    private static final String USER_SPACE = "XWiki";

    private static final EntityReference GROUPS_CLASS =
        new EntityReference("XWikiGroups", EntityType.DOCUMENT, new EntityReference(USER_SPACE, EntityType.SPACE));

    private DocumentAccessBridge documentAccessBridge;

    private DocumentReferenceResolver<String> stringResolver;

    private DocumentReference groupReference;

    private ComponentManager componentManager;

    /**
     * @param groupReference the group that contains list of recipients
     * @param factory the factory to use to create a single MimeMessage
     * @param parameters the parameters from which to extract the session, source and the headers
     * @param componentManager used to dynamically load components
     * @throws MessagingException when an error occurs when retrieving the number of users
     */
    public GroupMimeMessageIterator(DocumentReference groupReference,
        MimeMessageFactory<MimeMessage> factory, Map<String, Object> parameters,
        ComponentManager componentManager) throws MessagingException
    {
        this.factory = factory;
        this.parameters = parameters;
        this.groupReference = groupReference;
        this.componentManager = componentManager;

        XWikiContext context = getXWikiContext();
        try {
            this.iteratorSize = context.getWiki().getDocument(groupReference, context).getXObjects(GROUPS_CLASS).size();
        } catch (XWikiException e) {
            throw new MessagingException(String.format(
                "Failed to find number of [%s] objects in group Document [%s]", GROUPS_CLASS, groupReference), e);
        }
        this.documentAccessBridge = getAccessBridge();

        this.stringResolver = getResolver();
    }

    @Override
    protected MimeMessage createMessageInternal() throws MessagingException
    {
        MimeMessage mimeMessage;

        DocumentReference groupsClassReference = this.stringResolver.resolve(USER_SPACE + ".XWikiGroups");

        String userFullName = this.documentAccessBridge.getProperty(this.groupReference, groupsClassReference,
            this.position, "member").toString();

        DocumentReference userReference = this.stringResolver.resolve(userFullName);

        // If the user has no email address then return a null Mime Message so that it's skipped
        Object emailObject = this.documentAccessBridge.getProperty(userReference, new DocumentReference(userReference
            .getWikiReference().getName(), USER_SPACE, "XWikiUsers"), "email");
        if (emailObject != null) {
            String email = emailObject.toString();

            Map<String, Object> parameters = (Map<String, Object>) this.parameters.get("parameters");

            // Note: We don't create a Session here ATM since it's not required. The returned MimeMessage will be
            // given a valid Session when it's deserialized from the mail content store for sending.
            mimeMessage = this.factory.createMessage(null, this.parameters.get("source"), parameters);
            mimeMessage.addRecipients(Message.RecipientType.TO, email);
        } else {
            getLogger().warn("User [{}] has no email defined. Email has not been sent to that user.", userReference);
            mimeMessage = null;
        }

        return mimeMessage;
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    private DocumentAccessBridge getAccessBridge() throws MessagingException
    {
        DocumentAccessBridge accessBridge;
        try {
            accessBridge = this.componentManager.getInstance(DocumentAccessBridge.class);
        } catch (ComponentLookupException e) {
            throw new MessagingException("Failed to find default Document bridge ", e);
        }
        return accessBridge;
    }

    private XWikiContext getXWikiContext() throws MessagingException
    {
        XWikiContext xWikiContext;
        try {
            Execution execution = this.componentManager.getInstance(Execution.class);
            xWikiContext = (XWikiContext) execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        } catch (ComponentLookupException e) {
            throw new MessagingException("Failed to find default Execution context", e);
        }
        return xWikiContext;
    }

    private DocumentReferenceResolver<String> getResolver() throws MessagingException
    {
        DocumentReferenceResolver<String> resolver;
        try {
            resolver = this.componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING, "current");
        } catch (ComponentLookupException e) {
            throw new MessagingException("Failed to find default Document resolver", e);
        }
        return resolver;
    }
}
