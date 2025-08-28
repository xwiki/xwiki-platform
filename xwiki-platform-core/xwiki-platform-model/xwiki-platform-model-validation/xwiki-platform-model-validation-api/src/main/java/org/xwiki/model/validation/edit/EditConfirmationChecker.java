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
package org.xwiki.model.validation.edit;

import java.util.Optional;

import org.xwiki.component.annotation.Role;

/**
 * Provides the operation that a pre-edit checker must provide. The checks are called by the
 * {@link EditConfirmationScriptService} and aggregated in a {@link EditConfirmationCheckerResults}. The components
 * implementing this role are called in the order of their priorities. It is advised to define a priority to make the
 * order of the resulting messages deterministic.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Role
public interface EditConfirmationChecker
{
    /**
     * Checks if edit confirmation is required, or allowed, based on the provided boolean value.
     *
     * @return an {@link Optional} containing a {@link EditConfirmationCheckerResult} if the check identified a result
     *     to be displayed to the user, or {@link Optional#empty()} if no result was found
     */
    Optional<EditConfirmationCheckerResult> check();
}
