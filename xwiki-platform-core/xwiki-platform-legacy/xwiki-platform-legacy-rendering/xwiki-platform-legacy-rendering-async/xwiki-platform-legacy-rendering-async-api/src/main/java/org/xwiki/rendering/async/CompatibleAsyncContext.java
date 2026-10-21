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
package org.xwiki.rendering.async;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.Right;

/**
 * Add a backward compatibility layer to the {@link AsyncContext} interface.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public interface CompatibleAsyncContext
{
    /**
     * @param right the right needed for execution of the action
     * @param userReference the user to check the right for
     * @param entityReference the entity on which to check the right
     * @param allowed the result of the evaluation
     * @since 11.8RC1
     * @deprecated use {@link AsyncContext#useRight(Right, DocumentReference, EntityReference, Boolean)} instead,
     *     which supports indicating that the evaluation failed
     */
    @Deprecated(since = "18.10.0RC1")
    default void useRight(Right right, DocumentReference userReference, EntityReference entityReference,
        boolean allowed)
    {
        ((AsyncContext) this).useRight(right, userReference, entityReference, Boolean.valueOf(allowed));
    }
}
