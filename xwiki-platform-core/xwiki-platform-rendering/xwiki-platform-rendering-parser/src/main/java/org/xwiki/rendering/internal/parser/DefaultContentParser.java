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
package org.xwiki.rendering.internal.parser;

import java.io.StringReader;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Default implementation of {@link ContentParser}.
 * 
 * @version $Id$
 * @since 6.0M2
 */
@Component
@Singleton
public class DefaultContentParser implements ContentParser
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public XDOM parse(String content, Syntax syntax) throws ParseException, MissingParserException
    {
        Parser parser = getParser(syntax);
        try {
            return parser.parse(new StringReader(content == null ? "" : content));
        } catch (StackOverflowError | Exception e) {
            // All exceptions as well as stack overflow errors are captured and wrapped in parse exceptions to make sure
            // that they are handled correctly by the callers. Without this, some parsing issues can be badly handled,
            // leading to instability issues.
            throw new ParseException(String.format("Failed to parse with syntax [%s].", syntax.toIdString()), e);
        }
    }

    @Override
    public XDOM parse(String content, Syntax syntax, EntityReference source) throws ParseException,
        MissingParserException
    {
        XDOM xdom = parse(content, syntax);
        if (source != null) {
            xdom.getMetaData().addMetaData(MetaData.SOURCE, serializer.serialize(source));
        }
        return xdom;
    }

    /**
     * Return a parser for the given syntax.
     * 
     * @param syntax the syntax.
     * @return a parser.
     * @throws MissingParserException when no parser where found for the given syntax.
     * @since 6.0M2
     */
    private Parser getParser(Syntax syntax) throws MissingParserException
    {
        try {
            return this.componentManagerProvider.get().getInstance(Parser.class, syntax.toIdString());
        } catch (ComponentLookupException e) {
            throw new MissingParserException(syntax, e);
        }
    }
}
