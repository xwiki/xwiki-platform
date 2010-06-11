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
 *
 */
package org.xwiki.security;

/**
 * Enumeration of the possible rights.
 * @version $Id: $
 */
public enum Right
{
    /** The login access right. */
       LOGIN(0, "login"),
    /** The view access right. */
        VIEW(1, "view"), 
    /** The edit access right. */
        EDIT(2, "edit"),
    /** The delete access right. */
      DELETE(3, "delete"),
    /** The Admin access right. */
       ADMIN(4, "admin"), 
    /** The program access right. */
     PROGRAM(5, "programming"),
    /** The register access right. */
    REGISTER(6, "register"),
    /** The comment access right. */
     COMMENT(7, "comment"),
    /** Illegal value. */
     ILLEGAL(8, "illegal");

    /** The numeric value of this access right. */
    private final int value;

    /** The string representation. */
    private final String stringRepr;

    /**
     * @param value Numeric value.
     * @param stringRepr String representation of this instance.
     */
    private Right(int value, String stringRepr)
    {
        this.value = value;
        this.stringRepr = stringRepr;
    }

    /**
     * @return The numeric value of this access right.
     */
    public int getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return stringRepr;
    }

    /**
     * Convert a string to a right.
     * @param string String representation of right.
     * @return The corresponding Right instance, or Right.ILLEGAL.
     */
    public static Right toRight(String string)
    {
        for (Right right : values()) {
            if (right.stringRepr.equals(string)) {
                return right;
            }
        }
        return ILLEGAL;
    }
}
