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
package org.xwiki.bridge;

import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Compatibility interface for the {@link DocumentAccessBridge} class. The methods of this class are woven on
 * {@link DocumentAccessBridge} by {@link DocumentAccessBridgeAspect}.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */

public interface CompatibilityDocumentAccessBridge
{
    /**
     * @return true if the current document's author has programming rights.
     * @deprecated since 6.1RC1, use {@link ContextualAuthorizationManager#hasAccess(Right)} instead
     */
    @Deprecated(since = "6.1RC1")
    boolean hasProgrammingRights();
}
