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

package org.xwiki.rendering.internal.macro.wikibridge;

import java.util.HashMap;
import java.util.List;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacro;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A wrapper macro used for testing wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class MockWikiMacro implements WikiMacro
{
    /**
     * The internal wiki macro instance.
     */
    private WikiMacro wikiMacro;

    /**
     * The {@link ComponentManager} component.
     */
    private ComponentManager componentManager;
    
    /**
     * Creates a new mock wiki macro encapsulating the given wiki macro instance. 
     * 
     * @param wikiMacro encapsulated wiki macro instance.
     * @param componentManager component manager.
     */
    public MockWikiMacro(WikiMacro wikiMacro, ComponentManager componentManager)
    {
        this.wikiMacro = wikiMacro;
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     */
    public List<Block> execute(WikiMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Use a dummy XWikiContext.
        try {
            Execution execution = componentManager.lookup(Execution.class);
            execution.getContext().setProperty("xwikicontext", new HashMap<String, Object>());
        } catch (ComponentLookupException ex) {
            throw new MacroExecutionException(ex.getMessage(), ex);
        }
        
        return this.wikiMacro.execute(parameters, content, context);
    }

    /**
     * {@inheritDoc}    
     */
    public String getId()
    {
        return this.wikiMacro.getId();
    }

    /**
     * {@inheritDoc}
     */
    public MacroDescriptor getDescriptor()
    {
        return this.wikiMacro.getDescriptor();
    }

    /**
     * {@inheritDoc}
     */
    public int getPriority()
    {
        return this.wikiMacro.getPriority();
    }

    /**
     * {@inheritDoc}
     */
    public boolean supportsInlineMode()
    {
        return this.wikiMacro.supportsInlineMode();
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(Macro< ? > o)
    {
        return this.wikiMacro.compareTo(o);
    }
}
