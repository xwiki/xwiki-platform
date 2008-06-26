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
 *
 */

package com.xpn.xwiki.render.filter;

import java.io.IOException;
import java.io.Writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.interwiki.InterWiki;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.util.Encoder;
import org.radeox.util.StringBufferWriter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * XWikiLinkFilter finds [text] in its input and transforms this to <a href="text">...</a> if the wiki page exists. If
 * not it adds a [create text] to the output.
 */
public class XWikiLinkFilter extends LocaleRegexTokenFilter
{
    private static Log log = LogFactory.getLog(XWikiLinkFilter.class);

    /**
     * The regular expression for detecting WikiLinks. Overwrite in subclass to support other link styles like
     * OldAndUglyWikiLinking :-) /[A-Z][a-z]+([A-Z][a-z]+)+/ wikiPattern = "\\[(.*?)\\]";
     */

    @Override
    protected String getLocaleKey()
    {
        return "filter.link";
    }

    @Override
    protected void setUp(FilterContext context)
    {
        context.getRenderContext().setCacheable(true);
    }

    @Override
    public void handleMatch(StringBuffer buffer, org.radeox.regex.MatchResult result, FilterContext context)
    {
        RenderEngine engine = context.getRenderContext().getRenderEngine();

        if (engine instanceof WikiRenderEngine) {
            WikiRenderEngine wikiEngine = (WikiRenderEngine) engine;
            Writer writer = new StringBufferWriter(buffer);

            XWikiContext xcontext = (XWikiContext) context.getRenderContext().get("xcontext");
            String str = result.group(1);
            if (str != null) {
                // TODO: This line creates bug XWIKI-188. The encoder seems to be broken. Fix this!
                // The only unescaping done should be %xx => char,
                // since &#nnn; must be preserved (the active encoding cannot handle the character)
                // and + should be preserved (for "Doc.C++ examples").
                // Anyway, this unescaper only treats &#nnn;
                // trim the name and unescape it
                // str = Encoder.unescape(str.trim());
                str = str.trim();
                String text = null, href = null, target = null;
                boolean specificText = false;

                // Is there an alias like [alias|link] ?
                int pipeIndex = str.indexOf('|');
                int pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    text = str.substring(0, pipeIndex).trim();
                    str = str.substring(pipeIndex + pipeLength);
                    specificText = true;
                }

                // Is there a target like [alias|link|target] ?
                pipeIndex = str.indexOf('|');
                pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    target = str.substring(pipeIndex + pipeLength).trim();
                    str = str.substring(0, pipeIndex);
                }
                // Done splitting

                // Fill in missing components
                href = str.trim();
                if (text == null) {
                    text = href;
                }
                // Done, now print the link

                // Determine target type: external, interwiki, internal
                int protocolIndex = href.indexOf("://");
                if (((protocolIndex >= 0) && (protocolIndex < 10)) || (href.indexOf("mailto:") == 0)) {
                    // External link
                    buffer.append("<span class=\"wikiexternallink\"><a href=\"");
                    buffer.append(Utils.createPlaceholder(Util.escapeURL(href), xcontext));
                    buffer.append("\"");
                    if (target != null) {
                        buffer.append(" " + constructRelAttribute(target, xcontext));
                    } else {
                        XWiki xwiki = xcontext.getWiki();
                        String defaulttarget = xwiki.Param("xwiki.render.externallinks.defaulttarget", "");
                        if (!defaulttarget.equals("")) {
                            buffer.append(" " + constructRelAttribute(defaulttarget, xcontext));
                        }
                    }
                    buffer.append(">");
                    buffer.append(Utils.createPlaceholder(cleanText(text), xcontext));
                    buffer.append("</a></span>");
                    return;
                }

                int hashIndex = href.lastIndexOf('#');
                String hash = "";

                if (-1 != hashIndex && hashIndex != href.length() - 1) {
                    hash = href.substring(hashIndex + 1);
                    href = href.substring(0, hashIndex);
                }
                if (href.trim().equals("")) {
                    // Internal (anchor) link
                    buffer.append("<span class=\"wikilink\"><a href=\"#");
                    buffer.append(hash);
                    buffer.append("\">");
                    if (!specificText || text.length() == 0) {
                        text = Encoder.unescape(hash);
                    }
                    buffer.append(cleanText(text));
                    buffer.append("</a></span>");
                    return;
                }

                /*
                 * // We need to keep this in XWiki int colonIndex = name.indexOf(':'); // typed link ? if (-1 !=
                 * colonIndex) { // for now throw away the type information name = name.substring(colonIndex + 1); }
                 */

                int atIndex = href.lastIndexOf('@');
                // InterWiki link
                if (-1 != atIndex) {
                    String extSpace = href.substring(atIndex + 1);
                    // Kown extarnal space?
                    InterWiki interWiki = InterWiki.getInstance();
                    if (interWiki.contains(extSpace)) {
                        href = href.substring(0, atIndex);
                        try {
                            if (-1 != hashIndex) {
                                interWiki.expand(writer, extSpace, href, text, hash);
                            } else {
                                interWiki.expand(writer, extSpace, href, text);
                            }
                        } catch (IOException e) {
                            log.debug("InterWiki " + extSpace + " not found.");
                        }
                    } else {
                        buffer.append("&#91;<span class=\"error\">");
                        buffer.append(result.group(1));
                        buffer.append("?</span>&#93;");
                    }
                } else {
                    // internal link
                    if (wikiEngine.exists(href)) {
                        if (specificText == false) {
                            text = getWikiView(href);
                            wikiEngine.appendLink(buffer, href, text, hash);
                        } else {
                            wikiEngine.appendLink(buffer, href, text, hash);
                        }
                    } else if (wikiEngine.showCreate()) {
                        if (specificText == false) {
                            text = getWikiView(href);
                        }
                        wikiEngine.appendCreateLink(buffer, href, text);
                        // links with "create" are not cacheable because
                        // a missing wiki could be created
                        context.getRenderContext().setCacheable(false);
                    } else {
                        // cannot display/create wiki, so just display the text
                        buffer.append(text);
                    }
                    if (target != null) {
                        int where = buffer.lastIndexOf(" href=\"");
                        if (where >= 0) {
                            buffer.insert(where, " " + constructRelAttribute(target, xcontext));
                        }
                    }
                }
            } else {
                buffer.append(Encoder.escape(result.group(0)));
            }
        }
    }

    /**
     * Clean the text so that it won't be interpreted by radeox simple syntax
     * 
     * @param text
     * @return
     */
    private String cleanText(String text)
    {
        return Util.escapeText(text);
    }

    /**
     * Returns the view of the wiki name that is shown to the user. Overwrite to support other views for example
     * transform "WikiLinking" to "Wiki Linking". Does nothing by default.
     * 
     * @return view The view of the wiki name
     */
    protected String getWikiView(String name)
    {
        return convertWikiWords(name);
    }

    public static String convertWikiWords(String name)
    {
        try {
            name = name.substring(name.indexOf(".") + 1);
            return name.replaceAll("([a-z])([A-Z])", "$1 $2");
        } catch (Exception e) {
            return name;
        }
    }

    private String constructRelAttribute(String target, XWikiContext context)
    {
        // We prefix with "_" since a target can be any token and we need to
        // differentiate with other valid rel tokens.
        return "rel=\"_" + Utils.createPlaceholder(target, context) + "\"";
    }
}
