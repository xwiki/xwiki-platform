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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

@Component
@Named("macromapping")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class XWikiMacrosMappingRenderer implements XWikiRenderer, Initializable, Disposable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiMacrosMappingRenderer.class);

    /**
     * Regex pattern for matching macros that are written on single line.
     */
    private static final Pattern SINGLE_LINE_MACRO_PATTERN = Pattern.compile("\\{(\\w+)(:(.+))?\\}");

    /**
     * Regex pattern for matching macros that span several lines (i.e. macros that have a body block). Note that we're
     * using the {@link Pattern#DOTALL} flag to tell the compiler that "." should match any characters, including new
     * lines.
     */
    private static final Pattern MULTI_LINE_MACRO_PATTERN = Pattern.compile("\\{(\\w+)(:(.+?))?\\}(.+?)\\{\\1\\}",
        Pattern.DOTALL);

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

    @Inject
    private ObservationManager observationManager;

    @Inject
    private Provider<XWikiRenderingEngine> engineProvider;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    protected Map<String, String> macros_libraries;

    protected Map<String, XWikiVirtualMacro> macros_mappings;

    public XWikiMacrosMappingRenderer()
    {

    }

    /**
     * @deprecated since 6.1M2, lookup component with role {@link XWikiRenderer} and hint <code>macromapping</code>
     *             instead
     */
    @Deprecated
    public XWikiMacrosMappingRenderer(XWiki xwiki, XWikiContext context)
    {
        this.observationManager = Utils.getComponent(ObservationManager.class);
        this.xcontextProvider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);

        try {
            initialize();
        } catch (InitializationException e) {
            LOGGER.error("Failed to initialize", e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        loadPreferences();

        // Add a notification rule if the preference property plugin is modified
        this.observationManager.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                loadPreferences();
            }

            @Override
            public String getName()
            {
                return NAME;
            }

            @Override
            public List<Event> getEvents()
            {
                return EVENTS;
            }
        });
    }

    @Override
    public String getId()
    {
        return "mapping";
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.observationManager.removeListener(NAME);
    }

    /**
     * @deprecated since 6.1M2
     */
    @Deprecated
    public void loadPreferences(XWiki xwiki, XWikiContext context)
    {
        loadPreferences();
    }

    public void loadPreferences()
    {
        this.macros_libraries = new HashMap<String, String>();
        this.macros_mappings = new HashMap<String, XWikiVirtualMacro>();

        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null && xcontext.getWiki() != null) {
            String[] macrolanguages =
                StringUtils.split(xcontext.getWiki()
                    .getXWikiPreference("macros_languages", "velocity,groovy", xcontext), ", ");
            for (String language : macrolanguages) {
                this.macros_libraries
                    .put(
                        language,
                        xcontext.getWiki().getXWikiPreference("macros_" + language,
                            "XWiki." + language.substring(0, 1).toUpperCase() + language.substring(1) + "Macros",
                            xcontext));
            }

            String macrosmapping = xcontext.getWiki().getMacroList(xcontext);
            String[] mappings = StringUtils.split(macrosmapping, "\r\n");
            for (String mapping : mappings) {
                try {
                    XWikiVirtualMacro macro = new XWikiVirtualMacro(mapping);
                    if (!macro.getName().equals("")) {
                        if (!macro.getFunctionName().equals("")) {
                            this.macros_mappings.put(macro.getName(), macro);
                        } else {
                            this.macros_mappings.remove(macro.getName());
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error reading macro mapping " + mapping, e);
                }
            }
        }
    }

    @Override
    public String render(String content, XWikiDocument contentdoc, XWikiDocument doc, XWikiContext context)
    {
        if (this.macros_libraries == null) {
            loadPreferences();
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
                result.append(this.engineProvider.get()
                    .convertSingleLine(macroname, params, allcontent, macro, context));
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
                result.append(this.engineProvider.get()
                    .convertMultiLine(macroname, params, data, allcontent, macro, context));
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

    @Override
    public void flushCache()
    {
        this.macros_libraries = null;
        this.macros_mappings = null;
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
