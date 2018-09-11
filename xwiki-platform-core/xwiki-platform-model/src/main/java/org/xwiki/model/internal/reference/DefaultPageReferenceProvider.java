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
package org.xwiki.model.internal.reference;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.PageReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Provide a complete reference based on configurable default wiki and page.
 * 
 * @version $Id$
 * @since 10.6RC1
 */
@Component
@Singleton
public class DefaultPageReferenceProvider implements Provider<PageReference>
{
    @Inject
    private EntityReferenceProvider provider;

    @Inject
    private Provider<WikiReference> wikiReferenceProvider;

    @Inject
    private EntityReferenceFactory factory;

    /**
     * We can cache the default document since it's configurable only at xwiki.properties level which require a restart
     * to be modified.
     */
    private PageReference cachedReference;

    @Override
    public PageReference get()
    {
        if (this.cachedReference == null) {
            EntityReference reference = this.provider.getDefaultReference(EntityType.PAGE);

            // Add wiki
            reference = reference.appendParent(this.wikiReferenceProvider.get());

            this.cachedReference = this.factory.getReference(new PageReference(reference));
        }

        return this.cachedReference;
    }
}
