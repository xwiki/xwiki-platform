/*
 */
package org.xwiki.mail.internal.factory.users;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.factory.AbstractIteratorMimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;

/**
 * Generate one {@link MimeMessage} per passed user, using the passed parameters to define the
 * {@link org.xwiki.mail.MimeMessageFactory} to call to generate the message for each user.
 *
 * @version $Id$
 * @since 6.4.1
 * @deprecated starting with 6.4.2 this is replaced by the {@code usersandroups} Mime Message Factory
 */
@Component
@Named("users")
@Singleton
@Deprecated
public class UsersMimeMessageFactory extends AbstractIteratorMimeMessageFactory
{
    private static final String HINT = "hint";

    private static final String SOURCE = "source";

    @Override
    public Iterator<MimeMessage> createMessage(Object userReferencesObject, Map<String, Object> parameters)
        throws MessagingException
    {
        List<DocumentReference> userReferences = getTypedSource(userReferencesObject, List.class);

        // We verify that we have both a Factory hint specified but also the source for the Factory.
        validateParameters(parameters, HINT, SOURCE);

        // Extract from the passed parameters the MimeMessageFactory to use to create a single mail
        String factoryHint = (String) parameters.get(HINT);

        MimeMessageFactory factory = getInternalMimeMessageFactory(factoryHint);

        UsersMimeMessageIterator iterator = new UsersMimeMessageIterator(userReferences, factory, parameters,
            this.componentManagerProvider.get());
        return iterator;
    }

}
