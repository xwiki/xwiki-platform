/*
 */
package org.xwiki.mail.internal.factory.users;

import java.util.List;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.factory.AbstractMessageIterator;
import org.xwiki.model.reference.DocumentReference;

/**
 *  Generate messages from a user references.
 *
 * @version $Id$
 * @since 6.4M3
 * @deprecated starting with 6.4.2 this is replaced by the {@code usersandroups} Mime Message Factory
 */
@Deprecated
public class UsersMimeMessageIterator extends AbstractMessageIterator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UsersMimeMessageIterator.class);

    private DocumentAccessBridge documentAccessBridge;

    private final List<DocumentReference> users;

    private ComponentManager componentManager;

    /**
     * @param userReferences the list of recipients
     * @param factory to create MimeMessage
     * @param parameters the parameters from which to extract the session, source and the headers
     * @param componentManager used to dynamically load components
     * @throws MessagingException when an error occurs
     */
    public UsersMimeMessageIterator(List<DocumentReference> userReferences,
        MimeMessageFactory<MimeMessage> factory, Map<String, Object> parameters,
        ComponentManager componentManager) throws MessagingException
    {
        this.factory = factory;
        this.parameters = parameters;
        this.iteratorSize = userReferences.size();
        this.users = userReferences;
        this.componentManager = componentManager;
        this.documentAccessBridge = getAccessBridge();
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

    @Override
    protected ExtendedMimeMessage createMessageInternal() throws MessagingException
    {
        ExtendedMimeMessage mimeMessage;

        DocumentReference userReference = users.get(this.position);

        // If the user has no email address then return a null Mime Message so that it's skipped
        Object emailObject = this.documentAccessBridge.getProperty(userReference, new DocumentReference(userReference
            .getWikiReference().getName(), "XWiki", "XWikiUsers"), "email");
        if (emailObject != null) {
            String email = emailObject.toString();

            Map<String, Object> parameters = (Map<String, Object>) this.parameters.get("parameters");

            mimeMessage =
                ExtendedMimeMessage.wrap(this.factory.createMessage(this.parameters.get("source"), parameters));
            mimeMessage.addRecipient(Message.RecipientType.TO, InternetAddress.parse(email)[0]);
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
}
