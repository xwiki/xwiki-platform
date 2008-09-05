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
package org.xwiki.rendering.internal.parser.wikimodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.wikimodel.wem.IWemConstants;
import org.wikimodel.wem.IWemListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.wikimodel.wem.WikiReference;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Format;

/**
 * Map XWiki Listener events on to WikiModel events.
 * 
 * @version $Id: $
 * @since 1.5RC1
 */
public class WikiModelGeneratorListener implements Listener
{
    private IWemListener wikimodelListener;

    public WikiModelGeneratorListener(IWemListener wikimodelListener)
    {
        this.wikimodelListener = wikimodelListener;
    }

    public void beginDocument()
    {
        this.wikimodelListener.beginDocument();
    }

    public void endDocument()
    {
        this.wikimodelListener.endDocument();
    }

    /**
     * {@inheritDoc}
     *
     * @see Listener#beginFormat(org.xwiki.rendering.listener.Format)
     */
    public void beginFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRONG));
                break;
            case ITALIC:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.EM));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRIKE));
                break;
            case UNDERLINED:
                // TODO: Not supported by wikimodel yet.
                // See http://code.google.com/p/wikimodel/issues/detail?id=31
                break;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see Listener#endFormat(org.xwiki.rendering.listener.Format)
     */
    public void endFormat(Format format)
    {
        switch(format)
        {
            case BOLD:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRONG));
                break;
            case ITALIC:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.EM));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRIKE));
                break;
            case UNDERLINED:
                // TODO: Not supported by wikimodel yet.
                // See http://code.google.com/p/wikimodel/issues/detail?id=31
                break;
        }
    }

    public void beginList(ListType listType)
    {
        this.wikimodelListener.beginList(null, false);
    }

    public void beginListItem()
    {
        this.wikimodelListener.beginListItem();
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Don't do anything since there's no notion of Macro marker in WikiModel and anyway
        // there's nothing to render for a marker...
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.wikimodelListener.beginParagraph(createWikiParameters(parameters));
    }

    public void beginSection(SectionLevel level)
    {
        this.wikimodelListener.beginHeader(level.getAsInt(), null);
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        // TODO: Find what to do... For now don't render XML elements
    }

    public void endList(ListType listType)
    {
        this.wikimodelListener.endList(null, false);
    }

    public void endListItem()
    {
        this.wikimodelListener.endListItem();
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Don't do anything since there's no notion of Macro marker in WikiModel and anyway
        // there's nothing to render for a marker...
    }

    public void endParagraph(Map<String, String> parameters)
    {
        this.wikimodelListener.endParagraph(createWikiParameters(parameters));
    }

    public void endSection(SectionLevel level)
    {
        this.wikimodelListener.endHeader(level.getAsInt(), null);
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        // TODO: Find what to do... For now don't render XML elements
    }

    public void onEscape(String escapedString)
    {
        this.wikimodelListener.onEscape(escapedString);
    }

    public void onLineBreak()
    {
        this.wikimodelListener.onLineBreak();
    }

    public void onLink(Link link)
    {
        WikiReference wikiReference = new WikiReference(link.getReference(), link.getLabel(),
            createWikiParameters(Collections.<String, String>emptyMap()));
        this.wikimodelListener.onReference(wikiReference);
    }

    public void onInlineMacro(String name, Map<String, String> parameters, String content)
    {
        // Don't do anything since macros have already been transformed so this method
        // should not be called.
    }

    public void onStandaloneMacro(String name, Map<String, String> parameters, String content)
    {
        // Don't do anything since macros have already been transformed so this method
        // should not be called.
    }

    public void onNewLine()
    {
        this.wikimodelListener.onNewLine();
    }

    public void onSpace()
    {
        this.wikimodelListener.onSpace(" ");
    }

    public void onSpecialSymbol(String symbol)
    {
        this.wikimodelListener.onSpecialSymbol(symbol);
    }

    public void onWord(String word)
    {
        this.wikimodelListener.onWord(word);
    }

    public void onId(String name)
    {
        // TODO: Find what to do...
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onHorizontalLine() 
     */
    public void onHorizontalLine()
    {
        this.wikimodelListener.onHorizontalLine();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onEmptyLines(int)
     */
    public void onEmptyLines(int count)
    {
        this.wikimodelListener.onEmptyLines(count);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        this.wikimodelListener.onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(String)
     */
    public void onVerbatimStandalone(String protectedString)
    {
        this.wikimodelListener.onVerbatimBlock(protectedString);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        this.wikimodelListener.beginDefinitionList(createWikiParameters(Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        this.wikimodelListener.endDefinitionList(createWikiParameters(Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionTerm()
     * @since 1.6M2
     */
    public void beginDefinitionTerm()
    {
        this.wikimodelListener.beginDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionDescription()
     * @since 1.6M2
     */
    public void beginDefinitionDescription()
    {
        this.wikimodelListener.beginDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endDefinitionTerm()
     * @since 1.6M2
     */
    public void endDefinitionTerm()
    {
        this.wikimodelListener.endDefinitionTerm();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endDefinitionDescription()
     * @since 1.6M2
     */
    public void endDefinitionDescription()
    {
        this.wikimodelListener.endDefinitionDescription();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void beginQuotation(Map<String, String> parameters)
    {
        this.wikimodelListener.beginQuotation(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endQuotation(java.util.Map)
     * @since 1.6M2
     */
    public void endQuotation(Map<String, String> parameters)
    {
        this.wikimodelListener.endQuotation(createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#beginQuotationLine()
     * @since 1.6M2
     */
    public void beginQuotationLine()
    {
        this.wikimodelListener.beginQuotationLine();
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.rendering.listener.Listener#endQuotationLine() 
     * @since 1.6M2
     */
    public void endQuotationLine()
    {
        this.wikimodelListener.endQuotationLine();
    }

    private WikiParameters createWikiParameters(Map<String, String> parameters)
    {
        List<WikiParameter> wikiParams = new ArrayList<WikiParameter>();
        for (String key: parameters.keySet()) {
            wikiParams.add(new WikiParameter(key, parameters.get(key)));
        }
        return new WikiParameters(wikiParams);        
    }
}
