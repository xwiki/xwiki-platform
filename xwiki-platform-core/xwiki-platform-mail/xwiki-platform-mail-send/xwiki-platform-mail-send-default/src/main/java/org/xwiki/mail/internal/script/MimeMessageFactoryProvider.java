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
package org.xwiki.mail.internal.script;

import java.lang.reflect.Type;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.mail.MimeMessageFactory;

/**
 * Get a {@link MimeMessageFactory} component instance for a Script Service, based on a component hint and a source
 * Type.
 *
 * @version $Id$
 * @since 6.4M3
 */
public final class MimeMessageFactoryProvider
{
    private MimeMessageFactoryProvider()
    {
        // Hide the default constructor for utility classes
    }

    /**
     * @param hint the component hint of the {@link org.xwiki.mail.MimeMessageFactory} component to find
     * @param returnType the type returned by the {@link org.xwiki.mail.MimeMessageFactory} to find
     * @param componentManager used to dynamically load all MimeMessageIterator
     * @return MimeMessage Factory
     * @throws ComponentLookupException when an error occurs
     */
    public static MimeMessageFactory get(String hint, Type returnType, ComponentManager componentManager)
        throws ComponentLookupException
    {
        MimeMessageFactory factory;
        // Step 1: Look for a secure version first
        try {
            factory = componentManager.getInstance(new DefaultParameterizedType(null, MimeMessageFactory.class,
                returnType), String.format("%s/secure", hint));
        } catch (ComponentLookupException e) {
            // Step 2: Look for a non secure version if a secure one doesn't exist...
            factory = componentManager.getInstance(new DefaultParameterizedType(null, MimeMessageFactory.class,
                returnType), hint);
        }

        return factory;
    }
}
