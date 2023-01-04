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
package org.xwiki.rendering.internal.macro;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.ComponentMandatory;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.internal.transformation.macro.HTMLRawBlockFilter;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.transformation.macro.RawBlockFilterParameters;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * HTML raw block filter that considers rights of the author.
 *
 * @version $Id$
 * @since 14.7RC1
 */
@Component
@ComponentMandatory
@Singleton
@Named("html")
public class XWikiHTMLRawBlockFilter extends HTMLRawBlockFilter
{
    /**
     * Used to verify if the current doc has scripting rights.
     */
    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Override
    public RawBlock filter(RawBlock block, RawBlockFilterParameters parameters) throws MacroExecutionException
    {
        if (SyntaxType.HTML_FAMILY_TYPES.contains(block.getSyntax().getType())
            && !this.authorizationManager.hasAccess(Right.SCRIPT))
        {
            parameters.setRestricted(true);
        }

        return super.filter(block, parameters);
    }
}
