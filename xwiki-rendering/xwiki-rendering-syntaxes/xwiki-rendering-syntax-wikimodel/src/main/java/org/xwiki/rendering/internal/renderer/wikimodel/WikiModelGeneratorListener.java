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
package org.xwiki.rendering.internal.renderer.wikimodel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.wikimodel.wem.IWemConstants;
import org.wikimodel.wem.IWemListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.xwiki.rendering.internal.parser.wikimodel.DefaultXWikiGeneratorListener;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Map XWiki Listener events on to WikiModel events.
 * 
 * @version $Id$
 * @since 1.5RC1
 */
public class WikiModelGeneratorListener implements Listener
{
    private IWemListener wikimodelListener;

    private int docLevel = 1;

    private Stack<Context> context = new Stack<Context>();

    private class Context
    {
        int headerLevel;
    }

    public WikiModelGeneratorListener(IWemListener wikimodelListener)
    {
        this.wikimodelListener = wikimodelListener;
    }

    private Context getContext()
    {
        return this.context.peek();
    }

    private Context pushContext()
    {
        return this.context.push(new Context());
    }

    private Context popContext()
    {
        return this.context.pop();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDocument(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void beginDocument(MetaData metaData)
    {
        pushContext();

        this.wikimodelListener.beginDocument(WikiParameters.EMPTY);
        this.wikimodelListener.beginSection(this.docLevel++, getContext().headerLevel++, WikiParameters.EMPTY);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDocument(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void endDocument(MetaData metaData)
    {
        this.wikimodelListener.endSection(this.docLevel--, getContext().headerLevel, WikiParameters.EMPTY);
        this.wikimodelListener.endDocument(WikiParameters.EMPTY);

        popContext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginGroup(Map)
     */
    public void beginGroup(Map<String, String> parameters)
    {
        this.wikimodelListener.beginDocument(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endGroup(Map)
     */
    public void endGroup(Map<String, String> parameters)
    {
        this.wikimodelListener.endDocument(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginFormat(Format, Map)
     */
    public void beginFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRONG,
                    createWikiParameters(parameters).toList()));
                break;
            case ITALIC:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.EM, createWikiParameters(parameters)
                    .toList()));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRIKE,
                    createWikiParameters(parameters).toList()));
                break;
            case UNDERLINED:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.INS, createWikiParameters(parameters)
                    .toList()));
                break;
            case NONE:
                this.wikimodelListener.beginFormat(new WikiFormat(createWikiParameters(parameters).toList()));
                break;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endFormat(Format, Map)
     */
    public void endFormat(Format format, Map<String, String> parameters)
    {
        switch (format) {
            case BOLD:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRONG, createWikiParameters(parameters)
                    .toList()));
                break;
            case ITALIC:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.EM, createWikiParameters(parameters)
                    .toList()));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRIKE, createWikiParameters(parameters)
                    .toList()));
                break;
            case UNDERLINED:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.INS, createWikiParameters(parameters)
                    .toList()));
                break;
            case NONE:
                this.wikimodelListener.endFormat(new WikiFormat(createWikiParameters(parameters).toList()));
                break;
        }
    }

    public void beginList(ListType listType, Map<String, String> parameters)
    {
        this.wikimodelListener.beginList(createWikiParameters(parameters), false);
    }

    public void beginListItem()
    {
        this.wikimodelListener.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Don't do anything since there's no notion of Macro marker in WikiModel and anyway
        // there's nothing to render for a marker...
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.wikimodelListener.beginParagraph(createWikiParameters(parameters));
    }

    public void beginSection(Map<String, String> parameters)
    {
        this.wikimodelListener
            .beginSection(this.docLevel, getContext().headerLevel++, createWikiParameters(parameters));
    }

    public void beginHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.wikimodelListener.beginHeader(level.getAsInt(), createWikiParameters(parameters));
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.wikimodelListener.endList(createWikiParameters(parameters), false);
    }

    public void endListItem()
    {
        this.wikimodelListener.endListItem();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content, boolean isInline)
    {
        // Don't do anything since there's no notion of Macro marker in WikiModel and anyway
        // there's nothing to render for a marker...
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.wikimodelListener.endParagraph(createWikiParameters(parameters));
    }

    public void endSection(Map<String, String> parameters)
    {
        this.wikimodelListener
            .beginSection(this.docLevel, getContext().headerLevel--, createWikiParameters(parameters));
    }

    public void endHeader(HeaderLevel level, String id, Map<String, String> parameters)
    {
        this.wikimodelListener.endHeader(level.getAsInt(), createWikiParameters(parameters));
    }

    public void beginLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO wait for WikiModel to support wiki syntax in links
        // See http://code.google.com/p/wikimodel/issues/detail?id=87
    }

    public void endLink(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO wait for WikiModel to support wiki syntax in links
        // See http://code.google.com/p/wikimodel/issues/detail?id=87
    }

    public void onMacro(String id, Map<String, String> parameters, String content, boolean isInline)
    {
        if (isInline) {
            this.wikimodelListener.onMacroInline(id, createWikiParameters(parameters), content);
        } else {
            this.wikimodelListener.onMacroBlock(id, createWikiParameters(parameters), content);
        }
    }

    public void onNewLine()
    {
        // TODO: Decide when to generate a line break and when to generate a new line
        this.wikimodelListener.onNewLine();
    }

    public void onSpace()
    {
        this.wikimodelListener.onSpace(" ");
    }

    public void onSpecialSymbol(char symbol)
    {
        this.wikimodelListener.onSpecialSymbol("" + symbol);
    }

    public void onWord(String word)
    {
        this.wikimodelListener.onWord(word);
    }

    public void onId(String name)
    {
        this.wikimodelListener.onExtensionBlock(DefaultXWikiGeneratorListener.EXT_ID, createWikiParameters(Collections
            .singletonMap("name", name)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onRawText(String, org.xwiki.rendering.syntax.Syntax)
     */
    public void onRawText(String text, Syntax syntax)
    {
        // Nothing to do since wikimodel doesn't support raw content.
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine(Map)
     */
    public void onHorizontalLine(Map<String, String> parameters)
    {
        this.wikimodelListener.onHorizontalLine(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        this.wikimodelListener.onEmptyLines(count);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatim(String, boolean, Map)
     */
    public void onVerbatim(String protectedString, boolean isInline, Map<String, String> parameters)
    {
        if (isInline) {
            // TODO: we're currently not handling any inline verbatim parameters (we don't have support for this in
            // XWiki Blocks for now).
            this.wikimodelListener.onVerbatimInline(protectedString, WikiParameters.EMPTY);
        } else {
            this.wikimodelListener.onVerbatimBlock(protectedString, createWikiParameters(parameters));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void beginDefinitionList(Map<String, String> parameters)
    {
        this.wikimodelListener.beginDefinitionList(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList(java.util.Map)
     * @since 2.0RC1
     */
    public void endDefinitionList(Map<String, String> parameters)
    {
        this.wikimodelListener.endDefinitionList(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        this.wikimodelListener.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        this.wikimodelListener.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.wikimodelListener.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.wikimodelListener.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        this.wikimodelListener.beginQuotation(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.wikimodelListener.endQuotation(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        this.wikimodelListener.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine()
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        this.wikimodelListener.endQuotationLine();
    }

    public void beginTable(Map<String, String> parameters)
    {
        this.wikimodelListener.beginTable(createWikiParameters(parameters));
    }

    public void beginTableCell(Map<String, String> parameters)
    {
        this.wikimodelListener.beginTableCell(false, createWikiParameters(parameters));
    }

    public void beginTableHeadCell(Map<String, String> parameters)
    {
        this.wikimodelListener.beginTableCell(true, createWikiParameters(parameters));
    }

    public void beginTableRow(Map<String, String> parameters)
    {
        this.wikimodelListener.beginTableRow(createWikiParameters(parameters));
    }

    public void endTable(Map<String, String> parameters)
    {
        this.wikimodelListener.endTable(createWikiParameters(parameters));
    }

    public void endTableCell(Map<String, String> parameters)
    {
        this.wikimodelListener.endTableCell(false, createWikiParameters(parameters));
    }

    public void endTableHeadCell(Map<String, String> parameters)
    {
        this.wikimodelListener.endTableCell(true, createWikiParameters(parameters));
    }

    public void endTableRow(Map<String, String> parameters)
    {
        this.wikimodelListener.endTableRow(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * @see Listener#onImage(org.xwiki.rendering.listener.reference.ResourceReference, boolean, java.util.Map)
     * @since 2.5RC1
     */
    public void onImage(ResourceReference reference, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Note: This means that any WikiModel listener needs to be overridden with a XWiki specific
        // version that knows how to handle XWiki image location format.
        // TODO this.wikimodelListener.onReference("image:" + imageLocation);
    }

    /**
     * {@inheritDoc}
     * @see Listener#beginMetaData(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void beginMetaData(MetaData metadata)
    {
        // WikiModel has a notion of Property but it's different from XWiki's notion of MetaData. We could map some
        // specific metadata as WikiModel's property but it's not important since it would be useful only to benefit
        // from WikiModel's Renderer implementations and such implementation won't use XWiki's metadata anyway.
    }

    /**
     * {@inheritDoc}
     * @see Listener#endMetaData(org.xwiki.rendering.listener.MetaData)
     * @since 3.0M2
     */
    public void endMetaData(MetaData metadata)
    {
        // WikiModel has a notion of Property but it's different from XWiki's notion of MetaData. We could map some
        // specific metadata as WikiModel's property but it's not important since it would be useful only to benefit
        // from WikiModel's Renderer implementations and such implementation won't use XWiki's metadata anyway.
    }

    private WikiParameters createWikiParameters(Map<String, String> parameters)
    {
        List<WikiParameter> wikiParams = new ArrayList<WikiParameter>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            wikiParams.add(new WikiParameter(entry.getKey(), entry.getValue()));
        }

        return new WikiParameters(wikiParams);
    }
}
