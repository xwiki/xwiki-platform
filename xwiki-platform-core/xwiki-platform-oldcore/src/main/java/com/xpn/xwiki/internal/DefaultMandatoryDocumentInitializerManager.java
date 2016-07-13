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
package com.xpn.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.MandatoryDocumentInitializerManager;

/**
 * Default implementation of {@link MandatoryDocumentInitializerManager}.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class DefaultMandatoryDocumentInitializerManager implements MandatoryDocumentInitializerManager
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localSerializer;

    @Inject
    private Logger logger;

    @Override
    public MandatoryDocumentInitializer getMandatoryDocumentInitializer(DocumentReference documentReference)
    {
        ComponentManager componentManager = this.componentManagerProvider.get();

        String fullReference = this.serializer.serialize(documentReference);

        try {
            if (componentManager.hasComponent(MandatoryDocumentInitializer.class, fullReference)) {
                return componentManager.getInstance(MandatoryDocumentInitializer.class, fullReference);
            } else {
                String localReference = this.localSerializer.serialize(documentReference);

                if (componentManager.hasComponent(MandatoryDocumentInitializer.class, localReference)) {
                    return componentManager.getInstance(MandatoryDocumentInitializer.class, localReference);
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to initialize component [{}] for document", MandatoryDocumentInitializer.class,
                documentReference, e);
        }

        return null;
    }
}
