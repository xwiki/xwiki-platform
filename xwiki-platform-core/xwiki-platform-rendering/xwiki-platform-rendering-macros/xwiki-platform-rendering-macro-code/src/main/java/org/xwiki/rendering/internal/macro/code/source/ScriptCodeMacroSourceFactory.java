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
package org.xwiki.rendering.internal.macro.code.source;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.code.source.CodeMacroSource;
import org.xwiki.rendering.macro.code.source.CodeMacroSourceFactory;
import org.xwiki.rendering.macro.source.MacroContentSourceReference;
import org.xwiki.rendering.macro.source.MacroContentWikiSource;
import org.xwiki.rendering.macro.source.MacroContentWikiSourceFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Get content from a script binding.
 * 
 * @version $Id$
 * @since 15.0RC1
 * @since 14.10.2
 */
@Component
@Singleton
@Named(MacroContentSourceReference.TYPE_SCRIPT)
public class ScriptCodeMacroSourceFactory implements CodeMacroSourceFactory
{
    @Inject
    @Named(MacroContentSourceReference.TYPE_SCRIPT)
    private MacroContentWikiSourceFactory wikiFactory;

    @Override
    public CodeMacroSource getContent(MacroContentSourceReference reference, MacroTransformationContext context)
        throws MacroExecutionException
    {
        MacroContentWikiSource content = this.wikiFactory.getContent(reference, context);

        return new CodeMacroSource(content.getReference(), content.getContent(), null);
    }
}
