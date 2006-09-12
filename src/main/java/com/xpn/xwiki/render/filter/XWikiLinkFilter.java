/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
 * @author sdumitriu
 */


package com.xpn.xwiki.render.filter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.api.engine.RenderEngine;
import org.radeox.api.engine.WikiRenderEngine;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.interwiki.InterWiki;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.util.Encoder;
import org.radeox.util.StringBufferWriter;

import java.io.IOException;
import java.io.Writer;


/*
* LinkTestFilter finds [text] in its input and transforms this
* to <a href="text">...</a> if the wiki page exists. If not
* it adds a [create text] to the output.
*
* @author stephan
* @team sonicteam
* @version $Id: XWikiLinkFilter.java 448 2005-01-26 08:37:17Z ldubost $
*/

public class XWikiLinkFilter extends LocaleRegexTokenFilter {

    private static Log log = LogFactory.getLog(XWikiLinkFilter.class);


    /**
     * The regular expression for detecting WikiLinks.
     * Overwrite in subclass to support other link styles like
     * OldAndUglyWikiLinking :-)
     *
     * /[A-Z][a-z]+([A-Z][a-z]+)+/
     * wikiPattern = "\\[(.*?)\\]";
     */

    protected String getLocaleKey() {
        return "filter.link";
    }

    protected void setUp(FilterContext context) {
        context.getRenderContext().setCacheable(true);
    }

    public void handleMatch(StringBuffer buffer, org.radeox.regex.MatchResult result, FilterContext context) {
        RenderEngine engine = context.getRenderContext().getRenderEngine();

        if (engine instanceof WikiRenderEngine) {
            WikiRenderEngine wikiEngine = (WikiRenderEngine) engine;
            Writer writer = new StringBufferWriter(buffer);

            String name = result.group(1);
            if (name != null) {

            	// TODO: This line creates bug XWIKI-188. The encoder seems to be broken. Fix this!
                // trim the name and unescape it
                name = Encoder.unescape(name.trim());

                // Is there an alias like [alias|link] ?
                int pipeIndex = name.indexOf('|');
                int pipeLength = 1;
                if (pipeIndex==-1)
                    pipeIndex = name.indexOf('>');
                if (pipeIndex==-1)  {
                    pipeIndex = name.indexOf("&gt;");
                    pipeLength = 4;
                }
                String alias ="";
                if (-1 != pipeIndex) {
                    alias = name.substring(0, pipeIndex);
                    name = name.substring(pipeIndex + pipeLength);
                }

                int protocolIndex = name.indexOf("://");
                if (((protocolIndex>=0)&&(protocolIndex<10))
                    ||(name.indexOf("mailto:")==0)) {
                    // External link
                    String view = name;
                        if (-1 != pipeIndex) {
                            view = alias;
                        }

                    buffer.append("<span class=\"wikiexternallink\"><a href=\"");
                    buffer.append(name.trim());
                    buffer.append("\">");
                    buffer.append(Encoder.toEntity(view.charAt(0)) + view.substring(1));
                    buffer.append("</a></span>");
                    return;
                }


                int hashIndex = name.lastIndexOf('#');

                String hash = "";
                if (-1 != hashIndex && hashIndex != name.length() -1) {
                    hash = name.substring(hashIndex + 1);
                    name = name.substring(0, hashIndex);
                }

                /*
                // We need to keep this in XWiki
                int colonIndex = name.indexOf(':');
                // typed link ?
                if (-1 != colonIndex) {
                    // for now throw away the type information
                    name = name.substring(colonIndex + 1);
                }
                */

                int atIndex = name.lastIndexOf('@');
                // InterWiki link ?
                if (-1 != atIndex) {
                    String extSpace = name.substring(atIndex + 1);
                    // known extarnal space ?
                    InterWiki interWiki = InterWiki.getInstance();
                    if (interWiki.contains(extSpace)) {
                        String view = name;
                        if (-1 != pipeIndex) {
                            view = alias;
                        }

                        name = name.substring(0, atIndex);
                        try {
                            if (-1 != hashIndex) {
                                interWiki.expand(writer, extSpace, name, view, hash);
                            } else {
                                interWiki.expand(writer, extSpace, name, view);
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

                    if (wikiEngine.exists(name)) {
                        String view = getWikiView(name);
                        if (-1 != pipeIndex) {
                            view = alias;
                        }
                        // Do not add hash if an alias was given
                        if (-1 != hashIndex ) {
                            wikiEngine.appendLink(buffer, name, view, hash);
                        } else {
                            wikiEngine.appendLink(buffer, name, view);
                        }
                    } else if (wikiEngine.showCreate()) {
                        String view = getWikiView(name);
                        if (-1 != pipeIndex) {
                            view = alias;
                        }
                        wikiEngine.appendCreateLink(buffer, name, view);
                        // links with "create" are not cacheable because
                        // a missing wiki could be created
                        context.getRenderContext().setCacheable(false);
                    } else {
                        // cannot display/create wiki, so just display the text
                        buffer.append(name);
                    }
                }
            } else {
                buffer.append(Encoder.escape(result.group(0)));
            }
        }
    }

    /**
     * Returns the view of the wiki name that is shown to the
     * user. Overwrite to support other views for example
     * transform "WikiLinking" to "Wiki Linking".
     * Does nothing by default.
     *
     * @return view The view of the wiki name
     */

    protected String getWikiView(String name) {
        return convertWikiWords(name);
    }

    public static String convertWikiWords(String name) {
        try {
            name = name.substring(name.indexOf(".") + 1);
            return name.replaceAll("([a-z])([A-Z])", "$1 $2");
        } catch (Exception e) {
            return name;
        }
    }

}
