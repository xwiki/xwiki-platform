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
package org.xwiki.gwt.wysiwyg.client.syntax;

/**
 * Utility class for registering and retrieving syntax validators for different syntaxes.
 * 
 * @version $Id$
 */
public interface SyntaxValidatorManager
{
    /**
     * Adds a new syntax validator and binds it to the syntax specified by {@link SyntaxValidator#getSyntax()} method.
     * 
     * @param validator the syntax validator to be added.
     * @return The syntax validator previously binded to the implied syntax, or <code>null</code> if there was no such
     *         validator.
     */
    SyntaxValidator addSyntaxValidator(SyntaxValidator validator);

    /**
     * Returns the syntax validator for the given syntax.
     * 
     * @param syntax The syntax whose validator is needed.
     * @return The validator binded to the given syntax.
     */
    SyntaxValidator getSyntaxValidator(String syntax);

    /**
     * Removes the validator associated with the given syntax identifier.
     * 
     * @param syntax The syntax identifier.
     * @return The syntax validator being removed.
     */
    SyntaxValidator removeSyntaxValidator(String syntax);
}
