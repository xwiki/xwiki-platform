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
package org.xwiki.realtime.internal;

import java.util.Locale;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Allows to probe the realtime editing sessions.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
@Unstable
@Role
public interface RealtimeSessionManager
{
    /**
     * Determine if there is an active realtime editing session for the specified document translation that the current
     * user can join, taking into account the requested edit mode and the user editor preference. A document field (e.g.
     * the document content) can't be edited with different type of editors at the same time (e.g. WYSIWYG and Wiki).
     * The type of editor for a given field is set when the realtime session is created and can't be changed until the
     * session is closed. In order to join the session, you need to use the same editor type as the one bound to the
     * session.
     * 
     * @param documentReference the document reference to check for an active realtime editing session
     * @param locale the document translation to check for an active realtime editing session; this is the <em>real</em>
     *            locale of the target document translation
     * @return {@code true} if there is an active realtime editing session that the user can join, {@code false}
     *         otherwise
     */
    boolean canJoinSession(DocumentReference documentReference, Locale locale);
}
