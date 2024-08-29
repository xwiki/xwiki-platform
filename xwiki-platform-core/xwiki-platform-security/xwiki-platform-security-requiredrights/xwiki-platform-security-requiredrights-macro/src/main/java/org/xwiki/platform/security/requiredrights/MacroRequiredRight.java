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
package org.xwiki.platform.security.requiredrights;

import org.xwiki.stability.Unstable;

/**
 * Represents a required right for a macro. To avoid dependencies on the authorization API, this class provides
 * constants for common cases.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Unstable
public enum MacroRequiredRight
{
    /**
     * Programming right is required.
     */
    PROGRAM,

    /**
     * Programming right might be required, but a manual review is needed to confirm if the right is required.
     */
    MAYBE_PROGRAM,

    /**
     * Script right is required.
     */
    SCRIPT,

    /**
     * Script right might be required, but a manual review is needed to confirm if the right is required.
     */
    MAYBE_SCRIPT
}
