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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;

/**
 * Generate an entity reference string that doesn't contain reference parts that are the same as either the current
 * entity in the execution context or as the passed entity reference (if any). Note that the terminal part is always
 * kept (eg the document's page for a document reference or the attachment's filename for an attachment reference).
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component
@Singleton
@Named("compact")
public class CompactStringEntityReferenceSerializer extends DefaultStringEntityReferenceSerializer
{
    @Inject
    @Named("current")
    private EntityReferenceProvider provider;

    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        StringBuilder representation = new StringBuilder();

        List<EntityReference> references = reference.getReversedReferenceChain();
        for (int i = 0; i < references.size();) {
            EntityReference currentReference = references.get(i);
            EntityType currentType = currentReference.getType();

            // Move to last element of the same type
            while (++i < references.size() && references.get(i).getType() == currentType) {
                currentReference = references.get(i);
            }

            if (shouldSerialize(currentReference, representation, currentReference == reference, parameters)) {
                serializeEntityReferenceType(currentReference, representation, currentReference == reference);
            }
        }

        return representation.toString();
    }

    /**
     * @since 7.2M2
     */
    protected boolean shouldSerialize(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        boolean shouldPrint = false;

        // Only serialize if:
        // - the current entity reference has a different value and type than the passed reference
        // - the entity type being serialized is not the last type of the chain
        // In addition an entity reference isn't printed only if all parent references are not printed either,
        // otherwise print it. For example "wiki:page" isn't allowed for a Document Reference.

        if (isLastReference || representation.length() > 0) {
            shouldPrint = true;
        } else {
            EntityReference defaultReference = resolveDefaultReference(currentReference.getType(), parameters);
            if (defaultReference == null || !equal(defaultReference, currentReference)) {
                shouldPrint = true;
            }
        }

        return shouldPrint;
    }

    /**
     * Serialize the last part of the reference (all the ending elements having the same entity type).
     * 
     * @param reference the reference to serialize
     * @since 7.2M2
     */
    protected void serializeEntityReferenceType(EntityReference reference, StringBuilder representation,
        boolean isLastReference)
    {
        EntityReference parent = reference.getParent();
        if (parent != null && parent.getType() == reference.getType()) {
            serializeEntityReferenceType(parent, representation, false);
        }

        super.serializeEntityReference(reference, representation, isLastReference);
    }

    protected boolean equal(EntityReference defaultReference, EntityReference currentReference)
    {
        EntityReference defaultReferenceIt = defaultReference;
        EntityReference currentReferenceIt = currentReference;

        for (; defaultReferenceIt != null; defaultReferenceIt = defaultReferenceIt.getParent(), currentReferenceIt =
            currentReferenceIt.getParent()) {
            if (currentReferenceIt == null || defaultReferenceIt.getType() != currentReferenceIt.getType()
                || !defaultReferenceIt.getName().equals(currentReferenceIt.getName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * @since 7.2M1
     */
    protected EntityReference resolveDefaultReference(EntityType type, Object... parameters)
    {
        EntityReference resolvedDefaultReference = null;
        if (parameters.length > 0 && parameters[0] instanceof EntityReference) {
            // Try to extract the type from the passed parameter.
            EntityReference referenceParameter = (EntityReference) parameters[0];
            EntityReference extractedReference = referenceParameter.extractReference(type);
            if (extractedReference != null) {
                resolvedDefaultReference = extractedReference;

                // Remove parent if any
                EntityReference parent = resolvedDefaultReference.getParent();
                while (parent != null && parent.getType() == resolvedDefaultReference.getType()) {
                    parent = parent.getParent();
                }
                if (parent != null) {
                    resolvedDefaultReference = resolvedDefaultReference.removeParent(parent);
                }
            }
        }

        if (resolvedDefaultReference == null) {
            resolvedDefaultReference = this.provider.getDefaultReference(type);
        }

        return resolvedDefaultReference;
    }
}
