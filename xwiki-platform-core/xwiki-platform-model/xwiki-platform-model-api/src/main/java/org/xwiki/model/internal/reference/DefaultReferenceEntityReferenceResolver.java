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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Resolve an {@link org.xwiki.model.reference.EntityReference} into a valid and absolute reference (with all required
 * parents filled in). See {@link DefaultEntityReferenceValueProvider} for the behavior used when Reference values are
 * not defined in the passed reference.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named("default")
@Singleton
public class DefaultReferenceEntityReferenceResolver extends AbstractReferenceEntityReferenceResolver
{
    /**
     * Entity reference value provider used to provide default value.
     */
    @Inject
    private EntityReferenceValueProvider provider;

    @Override
    protected String getDefaultValue(EntityType type, Object... parameters)
    {
        return this.provider.getDefaultValue(type);
    }
}
