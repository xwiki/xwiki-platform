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
package com.xpn.xwiki.render;

import java.util.StringTokenizer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.util.Util;

public class XWikiWikiBaseRenderer implements XWikiRenderer
{
    private boolean removePre = true;

    private boolean renderWiki = true;

    public XWikiWikiBaseRenderer()
    {
    }

    public XWikiWikiBaseRenderer(boolean renderWiki, boolean removePre)
    {
        this.setRemovePre(removePre);
        this.setRenderWiki(renderWiki);

    }

    public static String makeAnchor(String text, Util util)
    {
        // Remove invalid characters to create an anchor
        text = util.substitute("s/^[\\s\\#\\_]* //o", text);
        text = util.substitute("s/[\\s\\_]*$//o", text);
        text = util.substitute("s/<\\w[^>]*>//goi", text);
        text = util.substitute("s/[^a-zA-Z0-9]/_/go", text);
        text = util.substitute("s/__+/_/go", text);
        text = util.substitute("s/^(.{32})(.*)$/$1/o", text);

        return text;
    }

    public static void makeHeading(StringBuffer stringBuffer, String level, String text, Util util)
    {
        String anchor = makeAnchor(text, util);
        stringBuffer.append("<h");
        stringBuffer.append(level);
        stringBuffer.append(" id=\"");
        stringBuffer.append(anchor);
        stringBuffer.append("\" >");
        stringBuffer.append(text);
        stringBuffer.append("</h");
        stringBuffer.append(level);
        stringBuffer.append(">");
    }

    public static void internalLink(StringBuffer output, String start, String web, String topic, String link,
        String anchor, boolean doLink, XWikiContext context, Util util)
    {
        // kill spaces and Wikify page name (ManpreetSingh - 15 Sep 2000)
        topic = util.substitute("s/^\\s*//", topic);
        topic = util.substitute("s/\\s*$//", topic);
        topic = util.substitute("s/^(.)/\\U$1/", topic);
        topic = util.substitute("s/\\s([a-zA-Z0-9])/\\U$1/g", topic);

        // Add <nop> before WikiWord inside text to prevent double links
        link = util.substitute("s/(\\s)([A-Z]+[a-z]+[A-Z])/$1<nop>$2/go", link);

        // Parent Document
        XWikiDocument parentdoc = (XWikiDocument) context.get("doc");

        XWiki xwiki = context.getWiki();
        XWikiDocument doc;
        try {
            doc = xwiki.getDocument(web, topic, context);
        } catch (XWikiException e) {
            doc = new XWikiDocument();
            doc.setName(topic);
            doc.setSpace(web);
        }

        output.append(start);
        if (!doc.isNew()) {
            output.append("<a href=\"");
            output.append(doc.getURL("view", context));
            if ((anchor != null) && (!anchor.equals(""))) {
                output.append("#");
                output.append(anchor);
            }
            output.append("\">");
            output.append(link);
            output.append("</a>");
        } else if (doLink) {
            output.append("<span class=\"newtopiclink\">");
            output.append(link);
            output.append("</span><a href=\"");
            output.append(doc.getURL("edit", context));
            output.append("?parent=");
            output.append(parentdoc.getFullName());
            output.append("\">?</a>");
        } else {
            output.append(link);
        }
    }

    /*
     * } elsif( $doLink ) { $text .= "<span style='background : $newTopicBgColor;'>" . "<font
     * color=\"$newTopicFontColor\">$theLinkText</font></span>" . "<a
     * href=\"$scriptUrlPath/edit$scriptSuffix/$theWeb/$theTopic?parent=$webName.$topicName\">?</a>"; return $text; }
     * else { $text .= $theLinkText; return $text; } }
     */

    public String handleInternalTags(String content, XWikiDocument doc, XWikiContext context)
    {
        return content;
    }

    public String handleAllTags(String content, XWikiDocument doc, XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        XWikiPluginManager plugins = xwiki.getPluginManager();

        // Call it again after plugins..
        if (this.renderWiki) {
            handleInternalTags(content, doc, context);
        }

        // PLUGIN: call startRenderingHandler at the start with the full content
        content = plugins.commonTagsHandler(content, context);

        // Call it again after plugins..
        if (this.renderWiki) {
            handleInternalTags(content, doc, context);
        }

        return content;
    }

    @Override
    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context)
    {
        boolean insidePRE = false;
        boolean insideVERBATIM = false;
        Util util = context.getUtil();
        ListSubstitution ls = new ListSubstitution(util);
        XWiki xwiki = context.getWiki();
        XWikiPluginManager plugins = xwiki.getPluginManager();

        if (this.renderWiki) {
            content = util.substitute("s/\\r//go", content);
            content = util.substitute("s/\\\\\\n//go", content);
            content = util.substitute("s/(\\|$)/$1 /", content);
        }

        // Initialization of input and output omitted
        StringBuffer output = new StringBuffer();
        String line;

        // Start by handling all tags (plugins + internal)
        content = handleAllTags(content, doc, context);

        // Remove the content that is inside "{pre}"
        PreTagSubstitution preTagSubst = new PreTagSubstitution(util, isRemovePre());
        content = preTagSubst.substitute(content);

        // PLUGIN: call startRenderingHandler at the start with the full content
        content = plugins.startRenderingHandler(content, context);

        StringTokenizer tokens = new StringTokenizer(content, "\n");
        while (tokens.hasMoreTokens()) {
            line = tokens.nextToken();

            // Changing state..
            if (util.match("m|{pre}|i", line)) {
                if (this.renderWiki) {
                    line = util.substitute("s/{pre}//i", line);
                }
                insidePRE = true;
            }
            if (util.match("m|{/pre}|i", line)) {
                if (this.renderWiki) {
                    line = util.substitute("s/{\\/pre}//i", line);
                }
                insidePRE = false;
            }

            if (insidePRE || insideVERBATIM) {
                if (insideVERBATIM) {
                    if (this.renderWiki) {
                        line = handleVERBATIM(line, util);
                    }
                }

                // PLUGIN: call insidePREHandler with the current line
                line = plugins.insidePREHandler(line, context);
            } else {
                // PLUGIN: call insidePREHandler with the current line
                line = plugins.outsidePREHandler(line, context);
                if (this.renderWiki) {
                    line = handleHeadings(line, util);
                    line = handleHR(line, util);
                    line = handleEmphasis(line, util);
                    line = handleWikiNames(line, util, context);
                    line = handleList(ls, output, line, util);
                }

                if (line != null) {
                    // continue other substitutions
                }
            }
            if (line != null) {
                output.append(line);
                // Make sure not to add an extra new line at the end
                if (tokens.hasMoreTokens()) {
                    output.append("\n");
                }
            }
        }
        if (this.renderWiki) {
            ls.dumpCurrentList(output, true);
        }

        // PLUGIN: call endRenderingHandler at the end with the full content
        String result = output.toString();
        result = plugins.endRenderingHandler(result, context);

        return preTagSubst.insertNonWikiText(result);
    }

    @Override
    public void flushCache()
    {
        // To change body of implemented methods use File | Settings | File Templates.
    }

    private String handleList(ListSubstitution ls, StringBuffer output, String line, Util util)
    {
        String result = ls.handleList(line);
        ls.dumpCurrentList(output, false);

        return result;
    }

    private String handleVERBATIM(String line, Util util)
    {
        line = util.substitute("s/\\&/&amp;/go", line);
        line = util.substitute("s/\\</&amp;/go", line);
        line = util.substitute("s/\\>/&amp;/go", line);
        line = util.substitute("s/\\&lt;pre\\&gt;/{pre}/go", line);

        return line;
    }

    private String handleEmphasis(String line, Util util)
    {
        // Bold/Italic/...
        line = util.substitute("s/(.*)/\n$1\n/o", line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_BOLDFIXED, line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_STRONGITALIC, line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_STRONG, line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_ITALIC, line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_FIXED, line);
        line = util.substitute("s/\n//go", line);

        return line;
    }

    private String handleHR(String line, Util util)
    {
        // Substitute <HR>
        line = util.substitute("s/^---+/<hr \\/>/o", line);

        return line;
    }

    private String handleHeadings(String line, Util util)
    {
        // Substiture headers
        line = HeadingSubstitution.substitute(util, "^---+(\\++|\\#+)\\s+(.+)\\s*$", HeadingSubstitution.DA, line);
        line = HeadingSubstitution.substitute(util, "^\\t(\\++|\\#+)\\s+(.+)\\s*$", HeadingSubstitution.DA, line);
        line = HeadingSubstitution.substitute(util, "^<h([1-6])>\\s*(.+?)\\s*</h[1-6]>", HeadingSubstitution.HT, line);

        return line;
    }

    private String handleWikiNames(String line, Util util, XWikiContext context)
    {
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_ONE, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_TWO, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_THREE, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_FOUR, util, line);

        return line;
    }

    public boolean isRemovePre()
    {
        return this.removePre;
    }

    public void setRemovePre(boolean removePre)
    {
        this.removePre = removePre;
    }

    public boolean isRenderWiki()
    {
        return this.renderWiki;
    }

    public void setRenderWiki(boolean renderWiki)
    {
        this.renderWiki = renderWiki;
    }

    @Override
    public String convertMultiLine(String macroname, String params, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        return allcontent;
    }

    @Override
    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        return allcontent;
    }
}
