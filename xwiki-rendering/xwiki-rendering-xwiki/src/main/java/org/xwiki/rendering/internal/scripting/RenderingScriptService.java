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
package org.xwiki.rendering.internal.scripting;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.service.ScriptService;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides Rendering-specific Scripting APIs.
 * 
 * @version $Id$
 * @since 2.3M1
 */
@Component("rendering")
public class RenderingScriptService implements ScriptService
{
    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * @return the list of syntaxes for which a Parser is available
     */
    public List<Syntax> getAvailableParserSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<Syntax>();
        try {
            for (Parser parser : this.componentManager.lookupList(Parser.class)) {
                syntaxes.add(parser.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // Failed to lookup parsers, consider there are no syntaxes available
        }

        return syntaxes;
    }

    /**
     * @return the list of syntaxes for which a Renderer is available
     */
    public List<Syntax> getAvailableRendererSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<Syntax>();
        try {
            for (PrintRendererFactory factory : this.componentManager.lookupList(PrintRendererFactory.class)) {
                syntaxes.add(factory.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // Failed to lookup renderers, consider there are no syntaxes available
        }

        return syntaxes;
    }
}
