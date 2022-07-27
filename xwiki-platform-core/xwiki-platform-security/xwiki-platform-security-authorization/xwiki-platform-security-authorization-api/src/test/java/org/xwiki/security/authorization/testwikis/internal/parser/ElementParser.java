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

package org.xwiki.security.authorization.testwikis.internal.parser;

import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.authorization.testwikis.TestDefinition;

/**
 * Interface implemented by the WML parser and provided to factories.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface ElementParser
{
    /**
     * Register a factory for current parsing level.
     * @param handler a factory.
     */
    void register(EntityFactory handler);

    /**
     * Format a message (like String.format) and add the current parser location at the end.
     * @param format a format string
     * @param objects arguments for the format string
     * @return a formatted string with parser location at the end.
     */
    String getLocatedMessage(String format, Object... objects);

    /**
     * @return the root entity created for the current parsing (available only during parsing)
     */
    TestDefinition getWikis();

    /**
     * @return an entity resolver that could be used by factories (available only during parsing)
     */
    EntityReferenceResolver<String> getResolver();

    /**
     * @return an entity serializer that could be used by factories (available only during parsing)
     */
    EntityReferenceSerializer<String> getSerializer();
}
