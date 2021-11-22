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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;

/**
 * Define a strategy to validate or transform an entity reference.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Role
public interface EntityNameValidation
{
    /**
     * Transforms a name such as {@link #isValid(String)} return {@code true}.
     *
     * @param name a name that should be transformed to be validated.
     * @return a new name with some changes to make it valid. Or exactly {@code name} if it was already valid.
     */
    String transform(String name);

    /**
     * Validate a name against a set of rules: usually a pattern is used to validate a name.
     *
     * @param name the name to validate.
     * @return {@code true} if it's correct according to the current policy.
     */
    boolean isValid(String name);

    /**
     * Aims at transforming an entity reference such as {@link #isValid(EntityReference)} returns true.
     * Note that this method should return exactly {@code entityReference} if {@link #isValid(EntityReference)} already
     * returns true for it.
     *
     * @param entityReference the entity reference on which to perform transformation.
     * @return a new {@link EntityReference} with the fewer possible transformation to make
     *          it pass {@link #isValid(EntityReference)}.
     */
    EntityReference transform(EntityReference entityReference);

    /**
     * Validate that the given {@link EntityReference} respects the current policy.
     *
     * @param entityReference the entity reference to check
     * @return {@code true} if the policy is respected.
     */
    boolean isValid(EntityReference entityReference);
}
