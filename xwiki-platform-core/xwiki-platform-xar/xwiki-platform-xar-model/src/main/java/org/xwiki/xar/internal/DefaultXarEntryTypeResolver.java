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
package org.xwiki.xar.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.xar.XarEntry;
import org.xwiki.xar.XarEntryType;
import org.xwiki.xar.XarEntryTypeResolver;

/**
 * Default implementation of {@link XarEntryTypeResolver}.
 * 
 * @version $Id$
 * @since 10.3RC1
 */
public class DefaultXarEntryTypeResolver implements XarEntryTypeResolver
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private XarEntryType defaultType;

    @Inject
    private Logger logger;

    @Override
    public XarEntryType resolve(XarEntry entry, boolean fallbackOnDefault)
    {
        ComponentManager componentManager = this.componentManagerProvider.get();

        // Try to find a component specific to the entity type
        if (entry != null && entry.getEntryType() != null
            && componentManager.hasComponent(XarEntryType.class, entry.getEntryType())) {
            try {
                return componentManager.getInstance(XarEntryType.class, entry.getEntryType());
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup XAR entry type for name [{}]", entry.getEntryType(), e);
            }
        }

        // Try to find a component specific to the document reference
        String reference = this.referenceSerializer.serialize(entry);
        String referencehint = DOCUMENT_PREFIX + reference;
        if (componentManager.hasComponent(XarEntryType.class, referencehint)) {
            try {
                return componentManager.getInstance(XarEntryType.class, referencehint);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to lookup XAR entry type for reference [{}]", reference, e);
            }
        }

        return fallbackOnDefault ? this.defaultType : null;
    }

    @Override
    public XarEntryType getDefault()
    {
        return this.defaultType;
    }
}
