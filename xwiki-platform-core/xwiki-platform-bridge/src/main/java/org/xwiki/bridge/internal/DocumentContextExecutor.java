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
package org.xwiki.bridge.internal;

import java.util.concurrent.Callable;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;

/**
 * Executes a {@link Callable} with the given document in context.
 *
 * @version $Id$
 * @since 14.10.6
 * @since 15.2RC1
 */
@Role
public interface DocumentContextExecutor
{
    /**
     * Execute the passed {@link Callable} with the given document in context.
     *
     * @param callable the task to execute
     * @param document the document to put in context
     * @return computed result
     * @param <V> the result type of method {@code call}
     */
    <V> V call(Callable<V> callable, DocumentModelBridge document) throws Exception;
}
