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
package org.xwiki.rendering.internal.macro.cache;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.MetaDataBlock;
import org.xwiki.rendering.block.match.MetadataBlockMatcher;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.macro.MacroContentParser;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.cache.CacheMacroParameters;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Compute the key under which the result of a cache macro call is stored.
 * <p>
 * The caches are shared by every rendering of the instance, so the key needs to hold everything that gives the cached
 * content its rights and its shape: the document the macro call comes from, the author whose rights the content is
 * executed with, whether the execution is restricted, and whether the result is inline. The identifier part of the
 * key alone is not enough, since it is chosen by the macro call and the same identifier can be used from different
 * contexts.
 *
 * @version $Id$
 * @since 16.10.20
 * @since 17.10.15
 * @since 18.4.8
 * @since 18.10.0RC1
 */
@Component(roles = CacheEntryKeyGenerator.class)
@Singleton
public class CacheEntryKeyGenerator
{
    /**
     * The parser used to parse the id parameter.
     */
    @Inject
    private MacroContentParser contentParser;

    /**
     * Renders the optional id parameter as plain text to use the result as part of the cache key.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    /**
     * Used to get the author whose rights the cached content is executed with.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to serialize the author reference.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * @param parameters the parameters of the macro call
     * @param content the content of the macro call
     * @param context the context of the macro execution
     * @return the key under which the result of this macro call is cached
     * @throws MacroExecutionException when the id parameter cannot be parsed
     */
    public String generate(CacheMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DocumentReference authorReference = this.documentAccessBridge.getCurrentAuthorReference();

        StringBuilder builder = new StringBuilder();
        appendElement(builder, getCurrentSource(context));
        appendElement(builder,
            authorReference != null ? this.entityReferenceSerializer.serialize(authorReference) : null);
        appendElement(builder, String.valueOf(context.getTransformationContext().isRestricted()));
        appendElement(builder, String.valueOf(context.isInline()));
        appendElement(builder, getIdentifier(parameters, content, context));

        return builder.toString();
    }

    /**
     * @param parameters the parameters of the macro call
     * @param content the content of the macro call
     * @param context the context of the macro execution
     * @return the part of the key chosen by the macro call: the id parameter when there is one, the content otherwise
     * @throws MacroExecutionException when the id parameter cannot be parsed
     */
    private String getIdentifier(CacheMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String identifier;

        if (parameters.getId() != null) {
            // Consider that the id contains wiki syntax and parse it with the same wiki parser than the current
            // transformation is using and render the result as plain text.
            WikiPrinter printer = new DefaultWikiPrinter();
            this.plainTextBlockRenderer.render(this.contentParser.parse(parameters.getId(), context, true, false),
                printer);
            identifier = printer.toString();
        } else {
            identifier = content;
        }

        return identifier;
    }

    /**
     * Append one element to the key being built, prefixed by its length so that two different lists of elements
     * cannot produce the same key.
     *
     * @param builder the key being built
     * @param element the element to append, possibly null
     */
    private static void appendElement(StringBuilder builder, String element)
    {
        builder.append(element != null ? element.length() : -1).append(':').append(element).append('\n');
    }

    /**
     * @param context the context of the macro execution
     * @return the reference of the document the macro call comes from, or null when it cannot be determined
     */
    private String getCurrentSource(MacroTransformationContext context)
    {
        String currentSource = context.getId();

        MacroBlock currentMacroBlock = context.getCurrentMacroBlock();

        if (currentMacroBlock != null) {
            MetaDataBlock metaDataBlock =
                currentMacroBlock.getFirstBlock(new MetadataBlockMatcher(MetaData.SOURCE), Axes.ANCESTOR_OR_SELF);

            if (metaDataBlock != null) {
                currentSource = (String) metaDataBlock.getMetaData().getMetaData(MetaData.SOURCE);
            }
        }

        return currentSource;
    }
}
