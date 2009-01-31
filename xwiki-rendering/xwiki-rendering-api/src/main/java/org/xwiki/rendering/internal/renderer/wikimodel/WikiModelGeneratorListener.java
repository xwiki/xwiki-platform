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
import java.util.List;
import java.util.Map;
import java.util.Collections;

import org.wikimodel.wem.IWemConstants;
import org.wikimodel.wem.IWemListener;
import org.wikimodel.wem.WikiFormat;
import org.wikimodel.wem.WikiParameter;
import org.wikimodel.wem.WikiParameters;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.Format;
import org.xwiki.rendering.listener.xml.XMLNode;

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
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.EM,
                    createWikiParameters(parameters).toList()));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRIKE,
                    createWikiParameters(parameters).toList()));
                break;
            case UNDERLINED:
                this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.INS,
                    createWikiParameters(parameters).toList()));
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
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRONG, 
                    createWikiParameters(parameters).toList()));
                break;
            case ITALIC:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.EM,
                    createWikiParameters(parameters).toList()));
                break;
            case STRIKEDOUT:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRIKE,
                    createWikiParameters(parameters).toList()));
                break;
            case UNDERLINED:
                this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.INS,
                    createWikiParameters(parameters).toList()));
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

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Don't do anything since there's no notion of Macro marker in WikiModel and anyway
        // there's nothing to render for a marker...
    }

    public void beginParagraph(Map<String, String> parameters)
    {
        this.wikimodelListener.beginParagraph(createWikiParameters(parameters));
    }

    public void beginSection(HeaderLevel level, Map<String, String> parameters)
    {
        this.wikimodelListener.beginHeader(level.getAsInt(), createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#beginXMLNode(XMLNode)
     */
    public void beginXMLNode(XMLNode node)
    {
        // TODO: Find what to do... For now don't render XML elements
    }

    public void endList(ListType listType, Map<String, String> parameters)
    {
        this.wikimodelListener.endList(createWikiParameters(parameters), false);
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

    public void endHeader(HeaderLevel level, Map<String, String> parameters)
    {
        this.wikimodelListener.endHeader(level.getAsInt(), createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Listener#endXMLNode(XMLNode)
     */
    public void endXMLNode(XMLNode node)
    {
        // TODO: Find what to do... For now don't render XML elements
    }

    public void beginLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO wait for WikiModel to support wiki syntax in links
        // See http://code.google.com/p/wikimodel/issues/detail?id=87
    }

    public void endLink(Link link, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // TODO wait for WikiModel to support wiki syntax in links
        // See http://code.google.com/p/wikimodel/issues/detail?id=87
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
        // TODO: Find what to do...
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
     * @see org.xwiki.rendering.listener.Listener#onVerbatimInline(String)
     */
    public void onVerbatimInline(String protectedString)
    {
        this.wikimodelListener.onVerbatimInline(protectedString);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#onVerbatimStandalone(String, Map)
     */
    public void onVerbatimStandalone(String protectedString, Map<String, String> parameters)
    {
        this.wikimodelListener.onVerbatimBlock(protectedString, createWikiParameters(parameters));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#beginDefinitionList()
     * @since 1.6M2
     */
    public void beginDefinitionList()
    {
        this.wikimodelListener.beginDefinitionList(createWikiParameters(Collections.<String, String>emptyMap()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.listener.Listener#endDefinitionList()
     * @since 1.6M2
     */
    public void endDefinitionList()
    {
        this.wikimodelListener.endDefinitionList(createWikiParameters(Collections.<String, String>emptyMap()));
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

    public void onImage(Image image, boolean isFreeStandingURI, Map<String, String> parameters)
    {
        // Note: This means that any WikiModel listener needs to be overridden with a XWiki specific
        // version that knows how to handle XWiki image location format.
//TODO        this.wikimodelListener.onReference("image:" + imageLocation);
    }

    private WikiParameters createWikiParameters(Map<String, String> parameters)
    {
        List<WikiParameter> wikiParams = new ArrayList<WikiParameter>();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            wikiParams.add(new WikiParameter(entry.getKey(), entry.getValue()));
        }
        
        return new WikiParameters(wikiParams);
    }
    
    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.Listener#beginError(String, String)
     * @since 1.7M3
     */
    public void beginError(String message, String description)
    {
        // Nothing to do since WikiModel doesn't support the notion of Error events.
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.listener.Listener#endError(String, String)
     * @since 1.7M3
     */
    public void endError(String message, String description)
    {
        // Nothing to do since WikiModel doesn't support the notion of Error events.
    }
}
