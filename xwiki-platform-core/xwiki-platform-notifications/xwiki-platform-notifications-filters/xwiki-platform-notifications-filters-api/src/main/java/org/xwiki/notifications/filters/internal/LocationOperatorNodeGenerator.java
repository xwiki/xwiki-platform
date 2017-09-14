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
package org.xwiki.notifications.filters.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Helper to generate an Expression Node to match a given location (either a wiki, a space or a document).
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component(roles = LocationOperatorNodeGenerator.class)
@Singleton
public class LocationOperatorNodeGenerator
{
    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    /**
     * @param location the reference of a location
     * @return the AbstractOperatorNode to filter on this location
     */
    public AbstractOperatorNode generateNode(EntityReference location)
    {
        String wiki = location.extractReference(EntityType.WIKI).getName();
        String serializedLocation = localSerializer.serialize(location);

        switch (location.getType()) {
            case DOCUMENT:
                return value(EventProperty.WIKI).eq(value(wiki))
                        .and(value(EventProperty.PAGE).eq(value(serializedLocation)));

            case SPACE:
                return value(EventProperty.WIKI).eq(value(wiki))
                        .and(value(EventProperty.SPACE).startsWith(value(serializedLocation)));

            case WIKI:
                return value(EventProperty.WIKI).eq(value(wiki));

            default:
                return null;
        }
    }
}
