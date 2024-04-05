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
package org.xwiki.model.validation.edit.internal;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResults;

/**
 * Manage the interactions with the {@link EditConfirmationChecker} components.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Role
public interface EditConfirmationCheckersManager
{
    /**
     * @return the aggregated results of the {@link EditConfirmationChecker} components
     */
    EditConfirmationCheckerResults check();

    /**
     * Force the results, meaning that any result matching the result at the time it was forced will be ignored, unless
     * {@link EditConfirmationCheckerResult#isError()} returns {@code true}.
     */
    void force();
}
