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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceNormalizer;
import org.xwiki.model.reference.InvalidEntityReferenceException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Transforms an {@link EntityReference} into a valid and absolute reference (with all required parents filled in).
 * This implementation uses fixed default values when parts of the Reference are missing.
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultEntityReferenceNormalizer implements EntityReferenceNormalizer
{
    @Requirement
    private ModelConfiguration configuration;

    private Map<EntityType, List<EntityType>> nextAllowedEntityTypes = new HashMap<EntityType, List<EntityType>>() {{
        put(EntityType.ATTACHMENT, Arrays.asList(EntityType.DOCUMENT));
        put(EntityType.DOCUMENT, Arrays.asList(EntityType.SPACE));
        put(EntityType.SPACE, Arrays.asList(EntityType.WIKI, EntityType.SPACE));
    }};

    /**
     * {@inheritDoc}
     * @see EntityReferenceNormalizer#normalize(EntityReference)
     * @throws InvalidEntityReferenceException if the passed reference to normalize is invalid (for example if the
     *         parent references are out of order) 
     */
    public void normalize(EntityReference referenceToNormalize)
    {
        // Check all references and parent references which have a NULL name and replace them with default values
        EntityReference reference = referenceToNormalize;
        while (reference != null) {
            if (StringUtils.isEmpty(reference.getName())) {
                reference.setName(getDefaultReferenceName(reference.getType()));
            }
            // If the parent reference isn't the allowed parent then insert an allowed reference
            List<EntityType> types = nextAllowedEntityTypes.get(reference.getType());
            if (reference.getParent() != null && types != null && !types.contains(reference.getParent().getType())) {
                EntityReference newReference = new EntityReference(
                    getDefaultReferenceName(types.get(0)), types.get(0), reference.getParent());
                reference.setParent(newReference);
            } else if (reference.getParent() == null && types != null) {
                // The top reference isn't the allowed top level reference, add a parent reference
                EntityReference newReference = new EntityReference(
                    getDefaultReferenceName(types.get(0)), types.get(0));
                reference.setParent(newReference);
            } else if (reference.getParent() != null && types == null) {
                // There's a parent but not of the correct type... it means the reference is invalid
                throw new InvalidEntityReferenceException("Invalid reference [" + referenceToNormalize + "]");
            }
            reference = reference.getParent();
        }
    }

    protected String getDefaultReferenceName(EntityType type)
    {
        return this.configuration.getDefaultReferenceName(type);
    }
}
