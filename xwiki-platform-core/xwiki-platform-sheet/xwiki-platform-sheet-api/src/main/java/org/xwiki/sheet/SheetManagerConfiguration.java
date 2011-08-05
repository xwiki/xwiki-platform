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
package org.xwiki.sheet;

import java.util.Properties;

import org.xwiki.component.annotation.ComponentRole;

/**
 * {@link SheetManager} configuration.
 * 
 * @version $Id$
 */
@ComponentRole
public interface SheetManagerConfiguration
{
    /**
     * @return the sheet that is used to display documents that define a class and don't explicitly include a sheet;
     *         this is not the sheet binded with the class but the sheet that displays class informations like the list
     *         of properties and their types
     */
    String getDefaultClassSheet();

    /**
     * @return the default binding between classes and their sheets; this is useful for classes that are created
     *         automatically, i.e. not imported, by other modules/components which are not aware of the sheet API; the
     *         key is the class string reference and the value is the sheet string reference; both references can be
     *         relative to the current wiki
     */
    Properties getDefaultClassSheetBinding();
}
