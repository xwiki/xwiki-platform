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
package com.xpn.xwiki.render.filter;

import java.text.MessageFormat;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.VelocityContext;
import org.radeox.api.engine.context.InitialRenderContext;
import org.radeox.api.engine.context.RenderContext;
import org.radeox.filter.CacheFilter;
import org.radeox.filter.context.FilterContext;
import org.radeox.filter.regex.LocaleRegexTokenFilter;
import org.radeox.regex.MatchResult;
import org.xwiki.rendering.util.IdGenerator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.render.XWikiRadeoxRenderEngine;
import com.xpn.xwiki.util.TOCGenerator;

/**
 * A customized version of Radeox Heading Filter
 */
public class XWikiHeadingFilter extends LocaleRegexTokenFilter implements CacheFilter
{
    private static final Log LOG = LogFactory.getLog(XWikiHeadingFilter.class);

    private final String TOC_NUMBERED = "tocNumbered";

    private final String TOC_DATA = "tocData";

    private MessageFormat formatter;

    private int sectionNumber = 0;

    protected String getLocaleKey()
    {
        return "filter.heading";
    }

    public void handleMatch(StringBuffer buffer, MatchResult result, FilterContext context)
    {
        buffer.append(handleMatch(result, context));
    }

    public void setInitialContext(InitialRenderContext context)
    {
        super.setInitialContext(context);
        String outputTemplate = outputMessages.getString(getLocaleKey() + ".print");
        formatter = new MessageFormat("");
        formatter.applyPattern(outputTemplate);
    }

    public String handleMatch(MatchResult result, FilterContext context)
    {
        String id = null;
        String title = result.group(0);
        String level = result.group(1);
        int level_i = (level.length() + 1) / 2;
        String hlevel = (level_i <= 6 ? level_i : 6) + "";
        String text = result.group(3);
        String numbering = "";

        RenderContext rcontext = context.getRenderContext();
        XWikiContext xcontext = ((XWikiRadeoxRenderEngine) rcontext.getRenderEngine()).getXWikiContext();
        VelocityContext vcontext = (VelocityContext) xcontext.get("vcontext");
        XWikiDocument doc = xcontext.getDoc();

        LOG.debug("Processing '" + text + "'");
        // generate unique ID of the heading
        IdGenerator idGenerator = (IdGenerator) xcontext.get("headingsIdGenerator");
        if (idGenerator == null) {
            idGenerator = new IdGenerator();
            xcontext.put("headingsIdGenerator", idGenerator);
        }

        id = idGenerator.generateUniqueId("H", text);
        LOG.debug("Generated heading id '" + id + "'");

        // add numbering if the flag is set

        if (xcontext.containsKey(TOC_NUMBERED) && ((Boolean) xcontext.get(TOC_NUMBERED)).booleanValue()) {
            // This is the old place where the data was placed, but this requires programming
            // rights. Instead, we now use vcontext.
            if (xcontext.containsKey(TOC_DATA)) {
                Map tocEntry = (Map) ((Map) xcontext.get(TOC_DATA)).get(id);
                if (tocEntry != null) {
                    numbering = (String) tocEntry.get(TOCGenerator.TOC_DATA_NUMBERING) + " ";
                }
            } else if (vcontext != null && vcontext.containsKey(TOC_DATA)) {
                Map tocEntry = (Map) ((Map) vcontext.get(TOC_DATA)).get(id);
                if (tocEntry != null) {
                    numbering = (String) tocEntry.get(TOCGenerator.TOC_DATA_NUMBERING) + " ";
                }
            }
        }

        String heading = formatter.format(new Object[] {id, numbering, text, hlevel});

        // Only show the section edit button for view action and when the user has edit rights on
        // the current document
        boolean showEditButton = false;
        if (xcontext.getWiki().hasSectionEdit(xcontext) && ("view".equals(xcontext.getAction()))) {
            try {
                // TODO: The user should always be set and never be null when this code gets
                // executed. Unfortunately this is currently happening. It should be set to XWiki
                // Guest immediatly in the initialization phase.
                // TODO: Similarly the current document should never be null when this code gets
                // executed as it would mean we're trying to render the headings for a null
                // document and that doesn't make sense...
                if ((doc != null)
                    && ((xcontext.getUser() != null) && xcontext.getWiki().getRightService().hasAccessLevel("edit",
                        xcontext.getUser(), doc.getFullName(), xcontext))) {
                    showEditButton = true;
                }
            } catch (XWikiException e) {
                // TODO: Remove this try/catch block by removing the throw exception on
                // hasAccessLevel() as it never throws any exception...
            }
        }

        Object beforeAction = xcontext.get("action");
        if (showEditButton) {
            if (beforeAction != null) {
                if (!beforeAction.toString().equals("HeadingFilter")) {
                    xcontext.put("action", "HeadingFilter");
                    sectionNumber = 0;
                }
            }

            if (level.equals("1") || level.equals("1.1")) {
                // This check is needed so that only the document content generates sectionedit
                // links.
                // TODO: Find a better way to make this check, as this prevents generating links for
                // titles that are transformed by velocity (1.1 about $doc.fullName) or by radeox
                // (1.1 This is *important*).
                if (doc != null && doc.getContent().indexOf(title.trim()) != -1) {
                    // TODO: This is unstable, meaning that it works in the current skin, but it might
                    // fail if there are other headings processed before the document content.
                    sectionNumber++;
                    StringBuffer editparams = new StringBuffer();
                    if (xcontext.getWiki().getEditorPreference(xcontext).equals("wysiwyg")) {
                        editparams.append("xpage=wysiwyg&amp;section=").append(sectionNumber);
                    } else {
                        editparams.append("section=").append(sectionNumber);
                    }
                    try {
                        if ((xcontext.getWiki().isMultiLingual(xcontext)) && (doc.getRealLanguage(xcontext) != null)) {
                            editparams.append("&amp;language=").append(doc.getRealLanguage(xcontext));
                        }
                    } catch (XWikiException e) {
                    }

                    String url = doc.getURL("edit", editparams.toString(), xcontext);
                    return heading + "<span class='edit_section'>&#91;"
                        + "<a style='text-decoration: none;' title='Edit section: " + text.replaceAll("'", "&#39;")
                        + "' href='" + url + "'>" + "edit" + "</a>&#93;</span>";
                }
            }
        }

        return heading;
    }
}
