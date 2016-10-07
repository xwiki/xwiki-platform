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
package org.xwiki.rendering.macro.context;

/**
 * Defines the strategy to use for setting the Transformation Context for the Context Macro. Namely this controls
 * the context for the macros located inside the Context Macro. For example some macros will use as input the other
 * XDOM elements before or after the current Macro block that they correspond to. For example the TOC macro executes
 * late (with a low priority) so that all other macros have a chance to execute and then it looks for all Heading
 * Blocks in the XDOM, from the root.
 *
 * @version $Id$
 * @since 8.3
 * @since 8.4RC1
 */
public enum TransformationContextMode
{
    /**
     * The XDOM on which the macros in the Context macro execute is the current document's XDOM.
     */
    CURRENT,

    /**
     * The XDOM on which the macros in the Context macro execute is the referenced document's XDOM (but without
     * transformations applied to that XDOM).
     */
    DOCUMENT,

    /**
     * The XDOM on which the macros in the Context macro execute is the referenced document's XDOM but with
     * transformations applied to that XDOM. IMPORTANT: This can be dangerous since it means executing macros, and thus
     * also script macros defined in the referenced document. To be used with caution since that have side effects.
     */
    TRANSFORMATIONS
}
