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

import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of the possible rights.
 * @version $Id$
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

    /** Map containing all known actions. */
    private static Map<String, Right> actionMap = new HashMap();
    /** List of all rights, as strings. */
    private static List<String> allRights = new LinkedList();

    /** The numeric value of this access right. */
    private final int value;

    /** The string representation. */
    private final String stringRepr;

    /**
     * Putter to circumvent the checkstyle max number of statements.
     */
    private static class Putter
    {
        /**
         * @param key Action string.
         * @param value Right value.
         * @return This object.
         */
        Putter put(String key, Right value)
        {
            actionMap.put(key, value);
            return this;
        }
    }

    static {
        new Putter()
            .put(LOGIN.toString(), LOGIN)
            .put(VIEW.toString(), VIEW)
            .put(DELETE.toString(), DELETE)
            .put(ADMIN.toString(), ADMIN)
            .put(PROGRAM.toString(), PROGRAM)
            .put(EDIT.toString(), EDIT)
            .put(REGISTER.toString(), REGISTER)
            .put("logout", LOGIN)
            .put("loginerror", LOGIN)
            .put("loginsubmit", LOGIN)
            .put("viewrev", VIEW)
            .put("get", VIEW)
            //        actionMap.put("downloadrev", "download"); Huh??
            .put("downloadrev", VIEW)
            .put("plain", VIEW)
            .put("raw", VIEW)
            .put("attach", VIEW)
            .put("charting", VIEW)
            .put("skin", VIEW)
            .put("download", VIEW)
            .put("dot", VIEW)
            .put("svg", VIEW)
            .put("pdf", VIEW)
            .put("deleteversions", ADMIN)
            //        actionMap.put("undelete", "undelete"); Huh??
            .put("undelete", EDIT)
            .put("reset", DELETE)
            .put("commentadd", COMMENT)
            .put("redirect", VIEW)
            .put("export", VIEW)
            .put("import", ADMIN)
            .put("jsx", VIEW)
            .put("ssx", VIEW)
            .put("tex", VIEW)
            .put("unknown", VIEW)
            .put("save", EDIT)
            .put("preview", EDIT)
            .put("lock", EDIT)
            .put("cancel", EDIT)
            .put("delattachment", EDIT)
            .put("inline", EDIT)
            .put("propadd", EDIT)
            .put("propupdate", EDIT)
            .put("objectadd", EDIT)
            .put("objectremove", EDIT)
            .put("rollback", EDIT)
            .put("upload", EDIT);

        for (Right level : values()) {
            if (!level.equals(ILLEGAL)) {
                allRights.add(level.toString());
            }
        }
    }

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
     * @return The corresponding Right instance, or {@code ILLEGAL}.
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

    /**
     * Map an action represented by a string to a right.
     * @param action String representation of action.
     * @return right The corresponding Right instance, or
     * {@code ILLEGAL}.
     */
    public static Right actionToRight(String action)
    {
        Right right = actionMap.get(action);
        if (right == null) {
            return ILLEGAL;
        }
        return right;
    }

    /**
     * @return a list of the string representation of all valid rights.
     */
    public static List<String> getAllRightsAsString()
    {
        return allRights;
    }
}
