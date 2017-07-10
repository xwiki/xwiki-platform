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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.toc.ExtendedTocMacroParameters;
import org.xwiki.rendering.macro.toc.TocMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.xpn.xwiki.XWikiContext;

/**
 * Extends the {@link TocMacro} Macro to add a {@code reference} parameter in order to be able to generate TOCs for
 * documents other than the current one.
 * <p>
 * Note that we reimplement a new macro in xwiki-platform that overrides the one in xwiki-rendering because we want
 * to still offer a TOC Macro for users of the Rendering module (when used standalone).
 *
 * @version $Id$
 * @since 9.6RC1
 */
@Component
@Named("toc")
@Singleton
public class ExtendedTocMacro extends AbstractMacro<ExtendedTocMacroParameters>
{
    /**
     * The description of the macro.
     */
    protected static final String DESCRIPTION = "Generates a Table Of Contents.";

    private TocTreeBuilder tocTreeBuilder;

    /**
     * A parser that knows how to parse plain text; this is used to transform link labels into plain text.
     */
    @Inject
    @Named("plain/1.0")
    private Parser plainTextParser;

    /**
     * Generate link label.
     */
    @Inject
    private LinkLabelGenerator linkLabelGenerator;

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    private EntityReferenceSerializer<String> entitySerializer;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public ExtendedTocMacro()
    {
        super("Table Of Contents", DESCRIPTION, ExtendedTocMacroParameters.class);

        // Make sure this macro is executed as one of the last macros to be executed since
        // other macros can generate headers which need to be taken into account by the TOC
        // macro.
        setPriority(2000);
        setDefaultCategory(DEFAULT_CATEGORY_NAVIGATION);
    }

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        this.tocTreeBuilder = new TocTreeBuilder(new TocBlockFilter(this.plainTextParser, this.linkLabelGenerator));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    @Override
    public List<Block> execute(ExtendedTocMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        TocMacroParameters.Scope resolvedScope = parameters.getScope();
        Block rootBlock = null;
        String resourceReference = null;
        if (parameters.getReference() != null) {
            // Remote TOC always has a PAGE scope since a LOCAL scope would have no meaning
            resolvedScope = TocMacroParameters.Scope.PAGE;
            // Get the referenced page's XDOM
            rootBlock = getXDOM(parameters.getReference());
            // Set the reference so that anchor links point to the document
            resourceReference = entitySerializer.serialize(parameters.getReference());
        }

        TreeParametersBuilder builder = new TreeParametersBuilder();
        TocMacroParameters tocMacroParameters = new TocMacroParameters();
        tocMacroParameters.setStart(parameters.getStart());
        tocMacroParameters.setDepth(parameters.getDepth());
        tocMacroParameters.setNumbered(parameters.isNumbered());
        tocMacroParameters.setScope(resolvedScope);

        TreeParameters treeParameters = builder.build(rootBlock, resourceReference, tocMacroParameters, context);
        return this.tocTreeBuilder.build(treeParameters);
    }

    private XDOM getXDOM(EntityReference reference) throws MacroExecutionException
    {
        XDOM xdom;
        try {
            XWikiContext context = this.xwikiContextProvider.get();
            xdom = context.getWiki().getDocument(reference, context).getXDOM();
        } catch (Exception e) {
            throw new MacroExecutionException(String.format("Failed to get XDOM for [%s]", reference), e);
        }
        return xdom;
    }
}
