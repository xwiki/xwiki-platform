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
package org.xwiki.export.pdf.internal;

import org.xwiki.component.annotation.Role;

/**
 * Used to record the skin extensions (JavaScript, CSS) required while rendering some content.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Role
public interface RequiredSkinExtensionsRecorder
{
    /**
     * Start recording.
     */
    void start();

    /**
     * Stop recording.
     * 
     * @return the HTML that needs to be placed in the page head in order to pull the skin extensions (JavaScript, CSS)
     *         that were required while rendering the content, since the last call to {@link #start()}
     */
    String stop();
}
