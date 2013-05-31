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
package org.xwiki.test.ui.po.editor;

import org.xwiki.test.ui.po.FormElement;

/**
 * Represents a StaticListClass property form.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class StaticListClassEditElement extends ClassPropertyEditPane
{
    /** The display type for inputting to setDisplayType. */
    public enum DisplayType
    {
        INPUT("input"),
        SELECT("select"),
        RADIO("radio"),
        CHECKBOX("checkbox");

        private final String id;

        private DisplayType(final String id)
        {
            this.id = id;
        }

        @Override
        public String toString()
        {
            return this.id;
        }
    }

    public StaticListClassEditElement(FormElement form, String propertyName)
    {
        super(form, propertyName);
    }

    public void setValues(String values)
    {
        setMetaProperty("values", values);
    }

    public void setMultiSelect(boolean isMultiSelect)
    {
        setMetaProperty("multiSelect", isMultiSelect ? "true" : "false");
    }

    public void setDisplayType(DisplayType type)
    {
        setMetaProperty("displayType", type.toString());
    }

    public void setRelationalStorage(boolean isRelationalStorage)
    {
        setMetaProperty("relationalStorage", isRelationalStorage ? "true" : "false");
    }
}
