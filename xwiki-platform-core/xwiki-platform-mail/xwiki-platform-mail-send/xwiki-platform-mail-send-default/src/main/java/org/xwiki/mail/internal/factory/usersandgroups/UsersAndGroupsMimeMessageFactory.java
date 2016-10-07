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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.factory.AbstractIteratorMimeMessageFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;

/**
 * Generate one {@link MimeMessage} per user found in the passed Groups and per user passed too (and preventing
 * duplicates), using the passed parameters to define the {@link MimeMessageFactory} to call to generate the message $
 * for each user. Also handles an optional list of users to exclude.
 *
 * @version $Id$
 * @since 6.4.2
 * @since 7.0M2
 */
@Component
@Named("usersandgroups")
@Singleton
public class UsersAndGroupsMimeMessageFactory extends AbstractIteratorMimeMessageFactory
{
    private static final String HINT = "hint";

    private static final String SOURCE = "source";

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    private Execution execution;

    @Override
    public Iterator<MimeMessage> createMessage(Object sourceObject, Map<String, Object> parameters)
        throws MessagingException
    {
        Map<String, Object> source = getTypedSource(sourceObject, Map.class);

        // We verify that we have both a Factory hint specified but also the source for the Factory.
        validateParameters(parameters, HINT, SOURCE);

        // Extract from the passed parameters the MimeMessageFactory to use to create a single mail
        String factoryHint = (String) parameters.get(HINT);

        MimeMessageFactory factory = getInternalMimeMessageFactory(factoryHint);

        UsersAndGroupsMimeMessageIterator iterator = new UsersAndGroupsMimeMessageIterator(source, factory, parameters,
            this.explicitDocumentReferenceResolver, this.execution);
        return iterator;
    }
}
