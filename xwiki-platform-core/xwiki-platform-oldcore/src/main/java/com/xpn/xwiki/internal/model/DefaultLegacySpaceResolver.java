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
package com.xpn.xwiki.internal.model;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Uses a {@code relative} Entity Reference Resolver to resolve the String representing one or serveral space names.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class DefaultLegacySpaceResolver implements LegacySpaceResolver
{
    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeEntityReferenceResolver;

    @Override
    public List<String> resolve(String spaceReferenceRepresentation)
    {
        List<String> spaceNames = new ArrayList<>();
        // Parse the spaces list into Space References
        EntityReference spaceReference =
            this.relativeEntityReferenceResolver.resolve(spaceReferenceRepresentation, EntityType.SPACE);
        for (EntityReference reference : spaceReference.getReversedReferenceChain()) {
            spaceNames.add(reference.getName());
        }
        return spaceNames;
    }
}
