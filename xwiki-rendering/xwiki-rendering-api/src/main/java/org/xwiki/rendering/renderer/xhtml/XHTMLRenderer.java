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
package org.xwiki.rendering.renderer.xhtml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.xwiki.rendering.listener.ListType;
import org.xwiki.rendering.listener.SectionLevel;
import org.xwiki.rendering.listener.Link;
import org.xwiki.rendering.renderer.Renderer;
import org.xwiki.rendering.utils.AnchorIdGenerator;
import org.xwiki.rendering.DocumentManager;

/**
 * Generates XHTML from a {@link org.xwiki.rendering.block.XDOM} object being traversed.
 *
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLRenderer implements Renderer
{
    private PrintWriter writer;

    private DocumentManager documentManager;

    /**
     * A temporary service offering methods manipulating XWiki Documents that are needed to output the 
     * correct XHTML. For example this is used to verify if a document exists when computing the HREF 
     * attribute for a link. It's temporary because the current Document services have not yet been 
     * rewritten with the new architecture. This bridge allows us to be independent of the XWiki Core 
     * module, thus preventing a cyclic dependency.
     */
    private XHTMLLinkRenderer linkRenderer;

    private AnchorIdGenerator idGenerator;
    
    /**
     * Used to save the original Writer when we redirect all outputs to a new writer to compute a 
     * section title. We need to do this since the XHTML we generate for a section title contains
     * a unique id that we generate based on the section title and the events for the section
     * title are generated after the beginSection() event.
     */
    private PrintWriter originalWriter;

    /**
     * The temporary writer used to redirect all outputs when computing the section title.
     * @see #originalWriter
     */
    private Writer sectionTitleWriter;
    
    /**
     * @param writer the stream to write the XHTML output to
     * @param documentManager see {@link #documentManager}
     */
    public XHTMLRenderer(Writer writer, DocumentManager documentManager)
    {
        this.writer = new PrintWriter(writer);
        this.documentManager = documentManager;
        this.linkRenderer = new XHTMLLinkRenderer(documentManager);
    }

    /**
     * {@inheritDoc}
     * @see Renderer#beginDocument()
     */
    public void beginDocument()
    {
    	// Use a new generator for each document being processed since the id generator is stateful and 
    	// remembers the generated ids.
        this.idGenerator = new AnchorIdGenerator();
	}

    /**
     * {@inheritDoc}
     * @see Renderer#endDocument()
     */
	public void endDocument()
	{
    	// Don't do anything
	}

    /**
     * {@inheritDoc}
     * @see Renderer#beginBold()
     */
	public void beginBold()
    {
        write("<strong>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#beginItalic()
     */
    public void beginItalic()
    {
        write("<em class=\"italic\">");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#beginParagraph()
     */
    public void beginParagraph()
    {
        write("<p>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#endBold()
     */
    public void endBold()
    {
        write("</strong>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#endItalic()
     */
    public void endItalic()
    {
        write("</em>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#endParagraph()
     */
    public void endParagraph()
    {
        write("</p>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#onLineBreak()
     */
    public void onLineBreak()
    {
        write("<br/>");
    }

    /**
     * {@inheritDoc}
     * @see Renderer#onNewLine()
     */
    public void onNewLine()
    {
        // Voluntarily do nothing since we want the same behavior as HTML.
    }

    /**
     * {@inheritDoc}
     * @see Renderer#onLink(Link)
     */
    public void onLink(Link link)
    {
        write(this.linkRenderer.renderLink(link));
    }

    public void onMacro(String name, Map<String, String> parameters, String content)
    {
        // Do nothing since macro output depends on Macro execution which transforms the macro
        // into a set of other events.
    }

    public void beginSection(SectionLevel level)
    {
    	// Don't output anything yet since we need the section title to generate the unique XHTML id attribute.
    	// Thus we're doing the output in the endSection() event.

    	// Redirect all output to our writer
    	this.originalWriter = getWriter();
    	this.sectionTitleWriter = new StringWriter();
    	this.setWriter(new PrintWriter(this.sectionTitleWriter));
    }
    
    private void processBeginSection(SectionLevel level, String sectionTitle)
    {
        int levelAsInt = level.getAsInt();
        write("<h" + levelAsInt + " id=\"" + this.idGenerator.generateUniqueId(sectionTitle) + "\">");
        // We generate a span so that CSS rules have a hook to perform some magic that wouldn't work on just a H
        // element. Like some IE6 magic and others.
        write("<span>");
    }

    public void endSection(SectionLevel level)
    {
    	String sectionTitle = this.sectionTitleWriter.toString();
    	getWriter().close();
    	setWriter(this.originalWriter);
    	processBeginSection(level, sectionTitle);
    	write(sectionTitle);

    	int levelAsInt = level.getAsInt();
        write("</span>");
        write("</h" + levelAsInt + ">");
    }

    public void onWord(String word)
    {
		write(word);
    }

    public void onSpace()
    {
        write(" ");
    }

    public void onSpecialSymbol(String symbol)
    {
        write(StringEscapeUtils.escapeHtml(symbol));
    }

    public void onEscape(String escapedString)
    {
        // Note: for single character escapes it would have been nicer to use XML entites. However
        // Wikimodel doesn't support that since its XHTML parser uses a XML parser and entities are
        // resolved internally by XML parsers so there's no way for wikimodel to know about them.
        // TODO: The syntax below is yet to be confirmed and should be considered temporary for now
        write("<pre><![CDATA[" + escapedString + "]]></pre>");
    }

    public void beginList(ListType listType)
    {
        if (listType == ListType.BULLETED) {
            write("<ul class=\"star\">");
        } else {
            write("<ol>");
        }
    }

    public void beginListItem()
    {
        write("<li>");
    }

    public void endList(ListType listType)
    {
        if (listType == ListType.BULLETED) {
            write("</ul>");
        } else {
            write("</ol>");
        }
    }

    public void endListItem()
    {
        write("</li>");
    }
    
    public void beginXMLElement(String name, Map<String, String> attributes)
    {
        write("<" + name);
        for (String attributeName: attributes.keySet()) {
            write(" " + attributeName + "=\"" + attributes.get(attributeName) + "\"");
        }
        write(">");
    }

    public void endXMLElement(String name, Map<String, String> attributes)
    {
        write("</" + name + ">");
    }

    public void beginMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    public void endMacroMarker(String name, Map<String, String> parameters, String content)
    {
        // Ignore macro markers, nothing to do.
    }

    protected void write(String text)
    {
        this.writer.write(text);
    }

    private PrintWriter getWriter()
    {
    	return this.writer;
    }
    
    private void setWriter(PrintWriter writer)
    {
    	this.writer = writer;
    }
    
}
