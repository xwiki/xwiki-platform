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
package org.xwiki.rendering.block;

import org.xwiki.rendering.listener.Listener;

import java.util.Map;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class MacroBlock extends AbstractBlock
{
    private String name;

    private Map<String, String> parameters;

    private String content;

    public MacroBlock(String name, Map<String, String> parameters)
    {
        this(name, parameters, null);
    }
    
    public MacroBlock(String name, Map<String, String> parameters, String content)
    {
        this.name = name;
        this.parameters = parameters;
        this.content = content;
    }

    public void traverse(Listener listener)
    {
        // Don't do anything here since we want the Macro Transformer component to take in charge
        // Macro execution. This is because Macro execution is a complex process that involves:
        // * computing the order in which the macros should be evaluated. For example the TOC macro
        //   should evaluate last since other macros can contribute section blocks.
        // * some macros need to modify blocks in the XDOM object
        // * macro execution is a multi-pass process
        // In essence the Macro Transformer will replace all MacroBlock blocks with other Blocks
        // generated from the execution of the Macros when XDOM.traverse() is called there
        // won't be any MacroBlock.traverse() method called at all.

        // Note: We're calling the event to let other listener downstream decide what to do with it.
        // In practice as described above this method will never get called when the whole rendering
        // process is executed. This does get called during our unit tests though.
        listener.onMacro(getName(), getParameters(), getContent());
    }

    public String getName()
    {
        return this.name;
    }

    public Map<String, String> getParameters()
    {
        return this.parameters;
    }

    public String getContent()
    {
        return this.content;
    }
}
