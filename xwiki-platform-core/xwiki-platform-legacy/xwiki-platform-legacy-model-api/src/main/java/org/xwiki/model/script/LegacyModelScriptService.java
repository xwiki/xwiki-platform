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
package org.xwiki.model.script;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;

/**
 * Legacy version of {@link ModelScriptService}, holding deprecated methods.
 *
 * @version $Id$
 * @since 17.0.0RC1
 */
@Component
@Named("model")
@Singleton
public class LegacyModelScriptService extends ModelScriptService
{
    /**
     * Get the value configured for a specific entity type, like the space name or wiki name. This doesn't return a
     * proper entity reference, but just the string value that should be used for that type of entity.
     *
     * @param type the target entity type; from Velocity it's enough to use a string with the uppercase name of the
     *            entity, like {@code 'SPACE'}
     * @param hint the hint of the value provider to use (valid hints are for example "default", "current" and
     *            "currentmixed")
     * @return the configured value for the requested entity type, for example "Main" for the default space or "WebHome"
     *         for the default space homepage
     * @since 4.3M1
     * @deprecated since 7.2M1, use {@link #getEntityReference(EntityType, String)}
     */
    @Deprecated
    public String getEntityReferenceValue(EntityType type, String hint)
    {
        if (type == null) {
            return null;
        }

        try {
            EntityReferenceValueProvider provider =
                this.componentManager.getInstance(EntityReferenceValueProvider.class, hint);
            return provider.getDefaultValue(type);
        } catch (ComponentLookupException ex) {
            return null;
        }
    }

    /**
     * Get the current value for a specific entity type, like the current space or wiki name. This doesn't return a
     * proper entity reference, but just the string value that should be used for that type of entity.
     *
     * @param type the target entity type; from Velocity it's enough to use a string with the uppercase name of the
     *            entity, like {@code 'SPACE'}
     * @return the current value for the requested entity type
     * @since 4.3M1
     * @deprecated since 7.4.1/8.0M1, use {@link #getEntityReference(EntityType)}
     */
    @Deprecated
    public String getEntityReferenceValue(EntityType type)
    {
        return getEntityReferenceValue(type, DEFAULT_RESOLVER_HINT);
    }
}
