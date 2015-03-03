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
package org.xwiki.mail.internal.factory;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeMessageFactory;

/**
 * Helper class for locating {@link org.xwiki.mail.MimeMessageFactory} and validating passed parameters.
 *
 * @version $Id$
 * @since 6.4.1
 */
public abstract class AbstractIteratorMimeMessageFactory extends AbstractMimeMessageFactory<Iterator<MimeMessage>>
{
    /**
     * The component manager instance to use to locate components dynamically.
     */
    @Inject
    @Named("context")
    protected Provider<ComponentManager> componentManagerProvider;

    protected MimeMessageFactory getInternalMimeMessageFactory(String hint, Object source)
        throws MessagingException
    {
        try {
            return this.componentManagerProvider.get().getInstance(new DefaultParameterizedType(null,
                MimeMessageFactory.class, MimeMessage.class), hint);
        } catch (ComponentLookupException e) {
            throw new MessagingException(String.format("Failed to find a [%s<%s, MimeMessage>] for hint [%s]",
                MimeMessageFactory.class.getSimpleName(), source.getClass().getSimpleName(), hint));
        }
    }

    /**
     * Verify the parameters exist for the passed parameter names.
     *
     * @param parameters the list of parameters to check
     * @param names the list of mandatory parameter names to verify
     * @throws MessagingException when a mandatory parameter doesn't exist
     */
    protected void validateParameters(Map<String, Object> parameters, String... names)
        throws MessagingException
    {
        if (parameters == null) {
            throw new MessagingException("You must pass parameters for this Mime Message Factory to work!");
        }

        for (String name : names) {
            Object parameterValue = parameters.get(name);
            if (parameterValue == null) {
                throw new MessagingException(String.format("The parameter [%s] is mandatory.", name));
            }
        }
    }
}
