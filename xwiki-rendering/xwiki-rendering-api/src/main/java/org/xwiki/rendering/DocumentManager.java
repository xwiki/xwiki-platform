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
package org.xwiki.rendering;

/**
 * Exposes methods for managing Documents. We're creating this abstraction here in the rendering
 * component for now since there's now such abstraction in the core for the moment. When it exists
 * replace it. Note that its replacement might well be the Wiki class of the new Model or spread
 * over several classes.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public interface DocumentManager
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = DocumentManager.class.getName();

    String getDocumentContent(String documentName) throws Exception;

    boolean exists(String documentName) throws Exception;

    String getURL(String documentName, String action) throws Exception;
}
