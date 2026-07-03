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
package org.xwiki.uiextension.macro;

import org.xwiki.properties.annotation.PropertyDescription;
import org.xwiki.properties.annotation.PropertyMandatory;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.internal.macro.UIExtensionsMacro;

/**
 * Parameters for the {@link UIExtensionsMacro} Macro.
 *
 * @version $Id$
 * @since 14.0RC1
 */
public class UIExtensionsMacroParameters
{
    private String extensionPoint;

    /**
     * @param extensionPoint the ID of the Extension Point to retrieve the {@link UIExtension}s for
     */
    @PropertyDescription("The ID of the Extension Point to retrieve the {@link UIExtension}s for.")
    @PropertyMandatory
    public void setExtensionPoint(String extensionPoint)
    {
        this.extensionPoint = extensionPoint;
    }

    /**
     * @return the ID of the Extension Point to retrieve the {@link UIExtension}s for
     */
    public String getExtensionPoint()
    {
        return this.extensionPoint;
    }
}
