/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 21:00:48
 */

package com.xpn.xwiki.render;

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import org.apache.oro.text.regex.*;
import org.apache.ecs.html.A;
import java.util.StringTokenizer;

public class XWikiWikiBaseRenderer implements XWikiRenderer {


    public XWikiWikiBaseRenderer() {
    }

    public static String makeAnchor(String text, Util util) {
        // Remove invalid characters to create an anchor
        text = util.substitute("s/^[\\s\\#\\_]* //o", text);
        text = util.substitute("s/[\\s\\_]*$//o", text);
        text = util.substitute("s/<\\w[^>]*>//goi", text);
        text = util.substitute("s/[^a-zA-Z0-9]/_/go", text);
        text = util.substitute("s/__+/_/go", text);
        text = util.substitute("s/^(.{32})(.*)$/$1/o", text);
        return text;
    }

    public static void makeHeading(StringBuffer stringBuffer, String level, String text,  Util util) {

        String anchor = makeAnchor(text, util);
        stringBuffer.append("<a name=\"");
        stringBuffer.append(anchor);
        stringBuffer.append("\" /><h");
        stringBuffer.append(level);
        stringBuffer.append(">");
        stringBuffer.append(text);
        stringBuffer.append("</h");
        stringBuffer.append(level);
        stringBuffer.append(">");
    }

    public static void internalLink(StringBuffer output, String start, String web, String topic,
                                      String link, String anchor, boolean doLink, XWikiContext context, Util util) {
        // kill spaces and Wikify page name (ManpreetSingh - 15 Sep 2000)
        topic = util.substitute("s/^\\s*//", topic);
        topic = util.substitute("s/\\s*$//", topic);
        topic = util.substitute("s/^(.)/\\U$1/", topic);
        topic = util.substitute("s/\\s([a-zA-Z0-9])/\\U$1/g", topic);

        // Add <nop> before WikiWord inside text to prevent double links
        link =  util.substitute("s/(\\s)([A-Z]+[a-z]+[A-Z])/$1<nop>$2/go", link);

        // Parent Document
        XWikiDocInterface parentdoc = (XWikiDocInterface) context.get("doc");

        // TODO: check if the document exists
        XWiki xwiki = (XWiki) context.get("xwiki");
        XWikiDocInterface doc;
        try {
         doc = xwiki.getDocument(web, topic);
        } catch (XWikiException e) {
         doc = new XWikiSimpleDoc();
         doc.setName(topic);
         doc.setWeb(web);
        }

        output.append(start);
        if (!doc.isNew()) {
            output.append("<a href=\"");
            output.append(doc.getActionUrl("view", context));
            if ((anchor != null)&&(!anchor.equals(""))) {
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
            output.append(doc.getActionUrl("edit", context));
            output.append("?parent=");
            output.append(doc.getFullName());
            output.append("\">?</a>");
        } else {
            output.append(link);
        }
    }

    /*
           } elsif( $doLink ) {
               $text .= "<span style='background : $newTopicBgColor;'>"
                     .  "<font color=\"$newTopicFontColor\">$theLinkText</font></span>"
                     .  "<a href=\"$scriptUrlPath/edit$scriptSuffix/$theWeb/$theTopic?parent=$webName.$topicName\">?</a>";
               return $text;

           } else {
               $text .= $theLinkText;
               return $text;
           }
       }
   */

    public String render(String content, XWikiDocInterface doc, XWikiContext context) {
        boolean insidePRE = false;
        boolean insideVERBATIM = false;
        boolean insideTABLE = false;
        boolean noAutoLink = false;
        Util util = context.getUtil();
        ListSubstitution ls = new ListSubstitution(util);

        content = util.substitute("s/\\r//go", content);
        content = util.substitute("s/\\\\\\n//go", content);
        content = util.substitute("s/(\\|$)/$1 /", content);

        // Initialization of input and output omitted
        StringBuffer output = new StringBuffer();
        String line;
        StringTokenizer tokens = new StringTokenizer(content,"\n");
        while(tokens.hasMoreTokens()) {
            line = tokens.nextToken();

            // Changing state..
            if (util.match("m|<pre>|i", line)) {
                insidePRE = true;
            }
            if (util.match("m|</pre>|i", line)) {
                insidePRE = false;
            }

            if (insidePRE || insideVERBATIM) {
                if (insideVERBATIM) {
                    line = handleVERBATIM(line, util);
                }
                // TODO call plugin insidePREHandler
            }
            else {
                // TODO call plugin outsidePREHandler
                line = handleHeadings(line, util);
                line = handleHR(line, util);
                line = handleEmphasis(line, util);
                line = handleWikiNames(line, util, context);
                line = handleList(ls, output, line, util);
                if (line!=null) {
                    // continue other substitutions
                }
            }
            if (line!=null) {
            output.append(line);
            output.append("\n");
            }
        }
        ls.dumpCurrentList(output, true);
        return output.toString();
    }

    private String handleList(ListSubstitution ls, StringBuffer output, String line, Util util) {
        String result = ls.handleList(line);
        ls.dumpCurrentList(output, false);
        return result;
    }

    private String handleVERBATIM(String line, Util util) {
        line = util.substitute("s/\\&/&amp;/go", line);
        line = util.substitute("s/\\</&amp;/go", line);
        line = util.substitute("s/\\>/&amp;/go", line);
        line = util.substitute("s/\\&lt;pre\\&gt;/<pre>/go", line);
        return line;
    }

    private String handleEmphasis(String line, Util util) {
        // Bold/Italic/...
        line = util.substitute("s/(.*)/\n$1\n/o", line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_BOLDFIXED,line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_STRONGITALIC,line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_STRONG,line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_ITALIC,line);
        line = FormattingSubstitution.substitute(util, FormattingSubstitution.TYPE_FIXED,line);
        line = util.substitute("s/\n//go", line);
        return line;
    }

    private String handleHR(String line,  Util util) {
        // Substitute <HR>
        line = util.substitute("s/^---+/<hr \\/>/o", line);
        return line;
    }

    private String handleHeadings(String line, Util util) {
        // Substiture headers
        line = HeadingSubstitution.substitute(util, "^---+(\\++|\\#+)\\s+(.+)\\s*$",
                HeadingSubstitution.DA, line);
        line = HeadingSubstitution.substitute(util, "^\\t(\\++|\\#+)\\s+(.+)\\s*$",
                HeadingSubstitution.DA, line);
        line = HeadingSubstitution.substitute(util, "^<h([1-6])>\\s*(.+?)\\s*</h[1-6]>",
                HeadingSubstitution.HT, line);
        return line;
    }

    private String handleWikiNames(String line, Util util, XWikiContext context) {
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_ONE, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_TWO, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_THREE, util, line);
        line = WikiNameSubstitution.substitute(context, WikiNameSubstitution.TYPE_FOUR, util, line);
        return line;
    }

}
