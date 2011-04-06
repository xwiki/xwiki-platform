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

package com.xpn.xwiki.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

public class XWikiMacrosMappingRenderer implements XWikiRenderer, EventListener
{
    private static final Log log = LogFactory.getLog(XWikiMacrosMappingRenderer.class);

    /**
     * Regex pattern for matching macros that are written on single line.
     */
    private static final Pattern SINGLE_LINE_MACRO_PATTERN = Pattern.compile("\\{(\\w+)(:(.+))?\\}");

    /**
     * Regex pattern for matching macros that span several lines (i.e. macros that have a body block). Note that we're
     * using the {@link Pattern#DOTALL} flag to tell the compiler that "." should match any characters, including new
     * lines.
     */
    private static final Pattern MULTI_LINE_MACRO_PATTERN =
        Pattern.compile("\\{(\\w+)(:(.+?))?\\}(.+?)\\{\\1\\}", Pattern.DOTALL);

    /**
     * The name of the listener.
     */
    private static final String NAME = "XWikiMacrosMappingRenderer";

    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentUpdatedEvent(new FixedNameEventFilter("xwiki:XWiki.WikiPreferences")));
            add(new DocumentCreatedEvent(new FixedNameEventFilter("xwiki:XWiki.XWikiPreferences")));
        }
    };

    protected Map<String, String> macros_libraries = null;

    protected Map<String, XWikiVirtualMacro> macros_mappings = null;

    public XWikiMacrosMappingRenderer(XWiki xwiki, XWikiContext context)
    {
        loadPreferences(xwiki, context);

        // Add a notification rule if the preference property plugin is modified
        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    public void loadPreferences(XWiki xwiki, XWikiContext context)
    {
        this.macros_libraries = new HashMap<String, String>();
        this.macros_mappings = new HashMap<String, XWikiVirtualMacro>();

        if ((xwiki != null) && (context != null)) {
            String[] macrolanguages =
                StringUtils.split(xwiki.getXWikiPreference("macros_languages", "velocity,groovy", context), ", ");
            for (int i = 0; i < macrolanguages.length; i++) {
                String language = macrolanguages[i];
                this.macros_libraries.put(language, xwiki.getXWikiPreference("macros_" + language, "XWiki."
                    + language.substring(0, 1).toUpperCase() + language.substring(1) + "Macros", context));
            }

            String macrosmapping = xwiki.getMacroList(context);
            String[] mappings = StringUtils.split(macrosmapping, "\r\n");
            for (int i = 0; i < mappings.length; i++) {
                try {
                    XWikiVirtualMacro macro = new XWikiVirtualMacro(mappings[i]);
                    if (!macro.getName().equals("")) {
                        if (!macro.getFunctionName().equals("")) {
                            this.macros_mappings.put(macro.getName(), macro);
                        } else {
                            this.macros_mappings.remove(macro.getName());
                        }
                    }
                } catch (Exception e) {
                    log.error("Error reading macro mapping " + mappings[i], e);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.render.XWikiRenderer#render(java.lang.String, com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context)
    {
        if (this.macros_libraries == null) {
            loadPreferences(context.getWiki(), context);
        }

        content = convertSingleLines(content, context);
        content = convertMultiLines(content, context);

        return content;
    }

    private String convertSingleLines(String content, XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        Matcher m = SINGLE_LINE_MACRO_PATTERN.matcher(content);
        int current = 0;
        while (m.find()) {
            result.append(content.substring(current, m.start()));
            current = m.end();
            String macroname = m.group(1);
            String params = m.group(3);
            String allcontent = m.group(0);

            XWikiVirtualMacro macro = this.macros_mappings.get(macroname);
            if ((macro != null) && (macro.isSingleLine())) {
                result.append(context.getWiki().getRenderingEngine().convertSingleLine(macroname, params, allcontent,
                    macro, context));
            } else {
                result.append(allcontent);
            }
        }
        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }

    private String convertMultiLines(String content, XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        Matcher m = MULTI_LINE_MACRO_PATTERN.matcher(content);
        int current = 0;
        while (m.find()) {
            result.append(content.substring(current, m.start()));
            current = m.end();
            String macroname = m.group(1);
            String params = m.group(3);
            String data = m.group(4);
            String allcontent = m.group(0);

            XWikiVirtualMacro macro = this.macros_mappings.get(macroname);
            if ((macro != null) && (macro.isMultiLine())) {
                result.append(context.getWiki().getRenderingEngine().convertMultiLine(macroname, params, data,
                    allcontent, macro, context));
            } else {
                result.append(allcontent);
            }
        }
        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.render.XWikiRenderer#flushCache()
     */
    public void flushCache()
    {
        this.macros_libraries = null;
        this.macros_mappings = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.render.XWikiRenderer#convertMultiLine(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String, com.xpn.xwiki.render.XWikiVirtualMacro, com.xpn.xwiki.XWikiContext)
     */
    public String convertMultiLine(String macroname, String params, String data, String allcontent,
        XWikiVirtualMacro macro, XWikiContext context)
    {
        return allcontent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.render.XWikiRenderer#convertSingleLine(java.lang.String, java.lang.String, java.lang.String,
     *      com.xpn.xwiki.render.XWikiVirtualMacro, com.xpn.xwiki.XWikiContext)
     */
    public String convertSingleLine(String macroname, String params, String allcontent, XWikiVirtualMacro macro,
        XWikiContext context)
    {
        return allcontent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext context = (XWikiContext) data;

        loadPreferences(context.getWiki(), context);
    }
}
