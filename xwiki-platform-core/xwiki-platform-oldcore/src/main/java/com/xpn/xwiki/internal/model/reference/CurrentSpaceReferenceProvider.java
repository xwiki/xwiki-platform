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
package com.xpn.xwiki.internal.model.reference;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Provide complete current document reference.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("current")
@Singleton
public class CurrentSpaceReferenceProvider implements Provider<SpaceReference>
{
    @Inject
    @Named("current")
    private Provider<WikiReference> wikiReferenceProvider;

    @Inject
    @Named("current")
    private EntityReferenceProvider provider;

    @Override
    public SpaceReference get()
    {
        EntityReference currentWiki = this.wikiReferenceProvider.get();
        EntityReference currentSpace = this.provider.getDefaultReference(EntityType.SPACE);

        // The provided space entity has no wiki parent set. Re-create the space reference chain and set the current
        // wiki as the root.
        EntityReference result = currentWiki;
        for (EntityReference spaceReference : currentSpace.getReversedReferenceChain()) {
            result = new SpaceReference(spaceReference.getName(), result);
        }

        return (SpaceReference) result;
    }
}
