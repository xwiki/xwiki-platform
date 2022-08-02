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

package org.xwiki.annotation.maintainer;

/**
 * Used to specify if an annotation target has been altered by a document modification.
 *
 * @version $Id$
 * @since 2.3M1
 */
public enum AnnotationState
{
    /**
     * An annotation is safe when it is valid and can be rendered on the document it has been added.
     */
    SAFE,
    /**
     * An annotation is altered when the document on which it has been added changed and the annotation position could
     * not be correctly found on the updated document.
     */
    ALTERED,
    /**
     * An annotation is updated if its selected text has suffered a modification and it could be detected and recovered
     * during the maintaining process.
     */
    UPDATED
}
