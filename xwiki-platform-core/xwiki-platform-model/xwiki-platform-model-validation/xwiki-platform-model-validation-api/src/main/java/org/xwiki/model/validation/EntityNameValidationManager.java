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
package org.xwiki.model.validation;

import java.util.Set;

import org.xwiki.component.annotation.Role;

/**
 * Manage the available {@link EntityNameValidation} components.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Role
public interface EntityNameValidationManager
{
    /**
     * @return the current strategy as defined in the {@link EntityNameValidationConfiguration}.
     */
    EntityNameValidation getEntityReferenceNameStrategy();

    /**
     * @param hint hint of the name strategy to retrieve.
     * @return the @{@link EntityNameValidation} for the given hint.
     */
    EntityNameValidation getEntityReferenceNameStrategy(String hint);

    /**
     * @return the names of available strategies.
     */
    Set<String> getAvailableEntityNameValidations();

    /**
     * Allow to reset the configuration of the strategies.
     */
    void resetStrategies();
}
