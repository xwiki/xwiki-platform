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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.StringWriter;
import java.io.StringReader;

import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.VelocityManager;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class VelocityMacro extends AbstractMacro
{
    private static final String DESCRIPTION = "Executes a Velocity script.";

    /**
     * Injected by the Component Manager.
     */
    private VelocityManager velocityManager;

    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // TODO: Use an I8N service to translate the descriptions in several languages
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getDescription()
     */
    public String getDescription()
    {
        // TODO: Use an I8N service to translate the description in several languages
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#getAllowedParameters()
     */
    public Map<String, MacroParameterDescriptor< ? >> getAllowedParameters()
    {
        // We send a copy of the map and not our map since we don't want it to be modified.
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Macro#execute(Map, String, org.xwiki.rendering.block.XDOM)
     */
    public List<Block> execute(Map<String, String> parameters, String content, XDOM dom) throws MacroExecutionException
    {
        // 1) Run Velocity on macro block content
        StringWriter writer = new StringWriter();

        try {
            this.velocityManager.getVelocityEngine().evaluate(this.velocityManager.getVelocityContext(), writer,
                "velocity macro", content);

        } catch (XWikiVelocityException e) {
            throw new MacroExecutionException("Failed to evaluate Velocity Macro for content [" + content + "]", e);
        }

        String velocityResult = writer.toString();

        // 2) Run the wiki syntax parser on the velocity-rendered content
        XDOM parsedDom;
        try {
            parsedDom = this.parser.parse(new StringReader(velocityResult));
        } catch (ParseException e) {
            throw new MacroExecutionException("Failed to parse content [" + velocityResult + "] with Syntax parser ["
                + this.parser.getSyntax() + "]", e);
        }

        return parsedDom.getChildren();
    }
}
