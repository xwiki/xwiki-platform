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
package org.xwiki.model;

import java.util.Map;

import org.xwiki.stability.Unstable;

/**
 *
 * @since 5.0M2
 */
@Unstable
public interface Persistable
{
    /**
     * None of the other Model API save. This allows to add/modify/delete several Entities before saving them all at
     * once, which allows for both optimizations and supporting Store working in this manner (for example SCM tools).
     */
    void save(String comment, boolean isMinorEdit, Map<String, String> extraParameters) throws ModelException;

    /**
     * Discard all uncomitted changes.
     */
    void discard();
}
