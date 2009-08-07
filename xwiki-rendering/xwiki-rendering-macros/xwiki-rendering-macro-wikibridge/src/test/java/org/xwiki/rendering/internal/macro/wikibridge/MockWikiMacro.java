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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameterDescriptor;
import org.xwiki.rendering.macro.wikibridge.WikiMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A wrapper macro used for testing a {@link DefaultWikiMacro} instance.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component("testwikimacro")
public class MockWikiMacro implements Macro<WikiMacroParameters>, Initializable
{
    /**
     * The internal {@link DefaultWikiMacro} instance.
     */
    private DefaultWikiMacro wikiMacro;

    /**
     * The {@link ComponentManager} component.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The {@link Execution} component.
     */
    @Requirement
    private Execution execution;

    /**
     * {@inheritDoc}
     */
    public void initialize() throws InitializationException
    {
        // Require two parameters, one mandatory and one optional.
        WikiMacroParameterDescriptor param1 = new WikiMacroParameterDescriptor("param1", "This is param1", true);
        WikiMacroParameterDescriptor param2 = new WikiMacroParameterDescriptor("param2", "This is param2", true);
        List<WikiMacroParameterDescriptor> params = new ArrayList<WikiMacroParameterDescriptor>();
        params.add(param1);
        params.add(param2);

        // Initialize the internal WikiMacro instance.
        WikiMacroDescriptor descriptor =
            new WikiMacroDescriptor("Test Wiki Macro", "Test", new DefaultContentDescriptor(false), params);
        this.wikiMacro =
            new DefaultWikiMacro("xwiki:Main.TestWikiMacro", "testwikimacro", true, descriptor,
                "This is **testwikimacro**", "xwiki/2.0", componentManager);

        // Set a dummy XWikiContext.
        execution.getContext().setProperty("xwikicontext", new HashMap<String, Object>());
    }

    /**
     * {@inheritDoc}
     */
    public List<Block> execute(WikiMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        return this.wikiMacro.execute(parameters, content, context);
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
