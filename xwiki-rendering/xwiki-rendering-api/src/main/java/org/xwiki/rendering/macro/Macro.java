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

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @param <P> the type of the macro parameters bean
 * @version $Id$
 * @since 1.5M2
 */
@ComponentRole
public interface Macro<P> extends Comparable<Macro< ? >>
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = Macro.class.getName();

    /**
     * The priority of execution relative to the other Macros. The lowest values have the highest priorities and execute
     * first. For example a Macro with a priority of 100 will execute before one with a priority of 500.
     * 
     * @return the execution priority
     */
    int getPriority();

    /**
     * @return the macro descriptor
     */
    MacroDescriptor getDescriptor();

    /**
     * @return true if the macro can be inserted in some existing content such as a paragraph, a list item etc. For
     *         example if I have <code>== hello {{velocity}}world{{/velocity}}</code> then the Velocity macro must
     *         support the inline mode and not generate a paragraph.
     */
    boolean supportsInlineMode();

    /**
     * @param parameters the macro parameters in the forma of a bean defined by the {@link Macro} implementation
     * @param content the content of the macro
     * @param context the context of the macros transformation process
     * @return the result of the macro execution
     * @throws MacroExecutionException error when executing the macro
     */
    List<Block> execute(P parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException;
}
