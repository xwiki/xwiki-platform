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
package org.xwiki.mail.internal.factory.users;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.Session;
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
    public Iterator<MimeMessage> createMessage(Session session, Object userReferencesObject,
        Map<String, Object> parameters) throws MessagingException
    {
        List<DocumentReference> userReferences = getTypedSource(userReferencesObject, List.class);
        validateParameters(parameters, HINT, SOURCE);

        // Extract from the passed parameters the MimeMessageFactory to use to create a single mail
        String factoryHint = (String) parameters.get(HINT);
        Object factorySource = parameters.get(SOURCE);

        MimeMessageFactory factory = getInternalMimeMessageFactory(factoryHint, factorySource);

        UsersMimeMessageIterator iterator = new UsersMimeMessageIterator(userReferences, factory, parameters,
            this.componentManagerProvider.get());
        return iterator;
    }

}
