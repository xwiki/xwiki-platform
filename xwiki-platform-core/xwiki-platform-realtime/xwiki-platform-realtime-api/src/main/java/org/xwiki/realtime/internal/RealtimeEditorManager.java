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
 * Allows to probe for active realtime sessions.
 * 
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0RC1
 */
@Unstable
@Role
public interface RealtimeEditorManager
{

    /**
     * Determine the currently selected editor. Expected outputs can be but are not limited to "inplace", "inline",
     * "wysiwyg", "wiki"...
     * 
     * @return the currently selected editor.
     */
    String getSelectedEditor();

    /**
     * Determine if there is an active realtime session for the given document translation and edit mode.
     * 
     * @param target specifies which document to check
     * @param locale explicitly specifies the translation of the document to be selected. The default value will not be
     *            inferred from context when locale is set to Locale.ROOT
     * @param editor specifies with which editor the realtime session is going to run
     * @return {@code true} if there is an active session, {@code false} otherwise.
     */
    boolean sessionIsActive(DocumentReference target, Locale locale, String editor);

}
