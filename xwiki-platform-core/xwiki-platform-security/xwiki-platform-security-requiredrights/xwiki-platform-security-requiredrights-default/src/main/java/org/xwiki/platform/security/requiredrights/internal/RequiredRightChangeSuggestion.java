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
package org.xwiki.platform.security.requiredrights.internal;

import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;

/**
 * A suggestion for an operation that removes or adds rights to a document.
 *
 * @param increasesRights if more rights are granted due to this change
 * @param rightToRemove the right to replace
 * @param rightToAdd the right to add
 * @param requiresManualReview if the analysis is certain that the right is needed/not needed or the user needs to
 * @param hasPermission if the current user has the permission to perform the proposed change manually review the
 *     analysis result to determine if the right is actually needed/not needed
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
public record RequiredRightChangeSuggestion(boolean increasesRights, DocumentRequiredRight rightToRemove,
                                            DocumentRequiredRight rightToAdd, boolean requiresManualReview,
                                            boolean hasPermission)
{
}
