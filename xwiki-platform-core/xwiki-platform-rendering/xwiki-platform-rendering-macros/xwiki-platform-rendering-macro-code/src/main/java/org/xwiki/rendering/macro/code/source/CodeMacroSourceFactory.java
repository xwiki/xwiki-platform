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
package org.xwiki.rendering.macro.code.source;

import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.stability.Unstable;

/**
 * Fetch a content to highlight based on its reference.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Role
@Unstable
public interface CodeMacroSourceFactory
{
    /**
     * @param reference the reference of the content
     * @param context the context of the code macro execution
     * @return the content to highlight
     * @throws MacroExecutionException when failing to get the content
     * @since 15.1RC1
     * @since 14.10.5
     */
    CodeMacroSource getContent(MacroContentSourceReference reference, MacroTransformationContext context)
        throws MacroExecutionException;
}
