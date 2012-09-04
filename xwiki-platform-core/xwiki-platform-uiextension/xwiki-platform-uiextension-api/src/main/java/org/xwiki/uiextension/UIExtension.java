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
package org.xwiki.uiextension;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.block.XDOM;

/**
 * Represents a User Interface Extension.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Role
public interface UIExtension
{
    /**
     * @return the unique ID of this extension
     */
    String getId();

    /**
     * @return the ID of the hook this UI extension is providing an extension for (aka an Extension Point)
     */
    String getExtensionPointId();

    /**
      * @return a map of data provided by the extension. It's up to the Extension Point to specify what data it
     *          supports
     */
    Map<String, String> getData();

    /**
     * @return the XDOM that must be rendered when this extension is displayed
     */
    XDOM getXDOM();
}
