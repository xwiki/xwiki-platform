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
package org.xwiki.rendering.internal.executor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.executor.ContentExecutor;
import org.xwiki.rendering.executor.ContentExecutorException;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationException;

/**
 * Execute the content by running the Macro Transformation.
 *
 * @version $Id$
 * @since 8.4RC1
 */
@Component
@Singleton
public class MacroContentExecutor implements ContentExecutor<MacroTransformationContext>
{
    @Inject
    private ContentParser contentParser;

    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    @Override
    public XDOM execute(String content, Syntax syntax, MacroTransformationContext macroContext)
        throws ParseException, MissingParserException, ContentExecutorException
    {
        XDOM xdom = this.contentParser.parse(content, syntax);
        executeContent(xdom, macroContext);
        return xdom;
    }

    @Override
    public XDOM execute(String content, Syntax syntax, EntityReference source, MacroTransformationContext macroContext)
        throws ParseException, MissingParserException, ContentExecutorException
    {
        XDOM xdom = this.contentParser.parse(content, syntax, source);
        executeContent(xdom, macroContext);
        return xdom;
    }

    private void executeContent(XDOM xdom, MacroTransformationContext macroContext) throws ContentExecutorException
    {
        try {
            this.macroTransformation.transform(xdom, macroContext.getTransformationContext());
        } catch (TransformationException e) {
            throw new ContentExecutorException("Failed to execute content", e);
        }
    }
}
