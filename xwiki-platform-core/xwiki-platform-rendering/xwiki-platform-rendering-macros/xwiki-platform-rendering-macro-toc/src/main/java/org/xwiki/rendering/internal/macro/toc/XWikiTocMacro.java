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
package org.xwiki.rendering.internal.macro.toc;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.toc.XWikiTocMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

/**
 * Generate a Table Of Contents based on the document sections.
 * <p>
 * We override the default Table of Contents macro because we want to associate a {@code DocumentReference} picker to
 * the {@code reference} parameter (using the {@code PropertyDisplayType} annotation).
 * 
 * @version $Id$
 * @since 11.5RC1
 */
@Component
@Named("toc")
@Singleton
public class XWikiTocMacro extends AbstractTocMacro<XWikiTocMacroParameters>
{
    @Inject
    @Named("ssrx")
    private SkinExtension ssfx;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public XWikiTocMacro()
    {
        super(XWikiTocMacroParameters.class);
    }

    @Override
    public List<Block> execute(XWikiTocMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        this.ssfx.use("toc.css");
        return super.execute(parameters, content, context);
    }
}
