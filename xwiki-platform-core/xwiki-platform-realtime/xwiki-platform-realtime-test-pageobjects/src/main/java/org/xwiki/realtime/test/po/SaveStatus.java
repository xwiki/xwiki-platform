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
package org.xwiki.realtime.test.po;

/**
 * The save status values.
 * 
 * @version $Id$
 * @since 16.10.6
 * @since 17.3.0RC1
 */
public enum SaveStatus
{
    /**
     * The document has unsaved changes.
     */
    UNSAVED("dirty"),

    /**
     * The document is being saved.
     */
    SAVING("cleaning"),

    /**
     * The document doesn't have unsaved changes.
     */
    SAVED("clean");

    private final String value;

    /**
     * Create a new save status.
     * 
     * @param value the save status value
     */
    SaveStatus(String value)
    {
        this.value = value;
    }

    /**
     * @return the save status value
     */
    public String getValue()
    {
        return this.value;
    }

    /**
     * @param value the save status value
     * @return the save status corresponding to the given value
     */
    public static SaveStatus fromValue(String value)
    {
        for (SaveStatus status : SaveStatus.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }
}
