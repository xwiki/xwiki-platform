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
package org.xwiki.gwt.wysiwyg.client.syntax.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidator;
import org.xwiki.gwt.wysiwyg.client.syntax.SyntaxValidatorManager;


/**
 * The default implementation for the {@link SyntaxValidatorManager}. We don't plan to provide another implementation.
 * 
 * @version $Id$
 */
public class DefaultSyntaxValidatorManager implements SyntaxValidatorManager
{
    /**
     * The map of known syntax validators, each associated to a specific syntax (the key).
     */
    private final Map<String, SyntaxValidator> validators = new HashMap<String, SyntaxValidator>();

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidatorManager#addSyntaxValidator(SyntaxValidator)
     */
    public SyntaxValidator addSyntaxValidator(SyntaxValidator validator)
    {
        return (SyntaxValidator) validators.put(validator.getSyntax(), validator);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidatorManager#getSyntaxValidator(String)
     */
    public SyntaxValidator getSyntaxValidator(String syntax)
    {
        return (SyntaxValidator) validators.get(syntax);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidatorManager#removeSyntaxValidator(String)
     */
    public SyntaxValidator removeSyntaxValidator(String syntax)
    {
        return (SyntaxValidator) validators.remove(syntax);
    }
}
