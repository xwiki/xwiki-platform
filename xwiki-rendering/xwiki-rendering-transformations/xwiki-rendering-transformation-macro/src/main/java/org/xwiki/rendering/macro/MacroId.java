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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.syntax.Syntax;

/**
 * Represents a Macro identifier. This is used when we need to pass a reference of a macro around without having to
 * pass Macro instances; it's also required when we need to create a Macro instance from an identifier.
 * <p>
 * A Macro is identified by 2 parameters:
 * <ul>
 *   <li>a string representing a technical id (eg "toc")</li>
 *   <li>an optional syntax (can be null) if the macro is only available for a given syntax</li>
 * </ul>
 * </p> 
 *
 * @version $Id$
 * @since 2.0M3
 */
public class MacroId
{
    /**
     * @see #getId()
     */
    private String id;

    /**
     * @see #getSyntax()
     */
    private Syntax syntax;

    /**
     * Constructor for macros registrered for all syntaxes.
     *
     * @param id see {@link #getId()}
     */
    public MacroId(String id)
    {
        this(id, null);
    }

    /**
     * Constructor for macros registrered for a specific syntax only.
     *
     * @param id see {@link #getId()}
     * @param syntax see {@link #getSyntax()}
     */
    public MacroId(String id, Syntax syntax)
    {
        this.id = id;
        this.syntax = syntax;
    }

    /**
     * @return the technical id of the macro (eg "toc" for the TOC Macro)
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the optional syntax (can be null) for which the macro represented by this id is available. If null
     *         then the macro is available for all syntaxes.
     */
    public Syntax getSyntax()
    {
        return this.syntax;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        return getId() + ((getSyntax() == null) ? "" : "/" + getSyntax().toIdString());
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // Random number. See http://www.technofundo.com/tech/java/equalhash.html for the detail of this
        // algorithm.
        int hash = 7;
        hash = 31 * hash + (null == getId() ? 0 : getId().hashCode());
        hash = 31 * hash + (null == getSyntax() ? 0 : getSyntax().hashCode());
        return hash;
    }

    /**
     * {@inheritDoc}
     *
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object object)
    {
        boolean result;

        // See http://www.technofundo.com/tech/java/equalhash.html for the detail of this algorithm.
        if (this == object) {
            result = true;
        } else {
            if ((object == null) || (object.getClass() != this.getClass())) {
                result = false;
            } else {
                MacroId macroId = (MacroId) object;
                result =
                    (getId() == macroId.getId() || (getId() != null && getId().equals(macroId.getId())))
                        && (getSyntax() == macroId.getSyntax() || (getSyntax() != null && getSyntax().equals(
                            macroId.getSyntax())));
            }
        }
        return result;
    }
}
