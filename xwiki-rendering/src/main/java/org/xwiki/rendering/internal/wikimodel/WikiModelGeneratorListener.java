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
package org.xwiki.rendering.internal.wikimodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    public void beginBold()
    {
        this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.STRONG));
    }

    public void beginItalic()
    {
        this.wikimodelListener.beginFormat(new WikiFormat(IWemConstants.EM));
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

    public void beginParagraph()
    {
        this.wikimodelListener.beginParagraph(null);
    }

    public void beginSection(SectionLevel level)
    {
        this.wikimodelListener.beginHeader(level.getAsInt(), null);
    }

    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        // TODO: Find what to do... For now don't render XML elements
    }

    public void endBold()
    {
        this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.STRONG));
    }

    public void endItalic()
    {
        this.wikimodelListener.endFormat(new WikiFormat(IWemConstants.EM));
    }

    public void beginUnderline()
    {
        // TODO: Not supported by wikimodel yet.
        // See http://code.google.com/p/wikimodel/issues/detail?id=31
    }

    public void endUnderline()
    {
        // TODO: Not supported by wikimodel yet.
        // See http://code.google.com/p/wikimodel/issues/detail?id=31
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

    public void endParagraph()
    {
        this.wikimodelListener.endParagraph(null);
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
        List<WikiParameter> wikiParams = new ArrayList<WikiParameter>();
        wikiParams.add(new WikiParameter("", ""));
        WikiParameters linkParameters = new WikiParameters(wikiParams);

        WikiReference wikiReference = new WikiReference(link.getReference(), link.getLabel(), linkParameters);
        this.wikimodelListener.onReference(wikiReference);
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
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
}
