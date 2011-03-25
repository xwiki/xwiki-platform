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
package com.xpn.xwiki.plugin;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.render.WikiSubstitution;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Plugin which allows to define a series of substitutions to apply to the content during the rendering process.
 * Substitutions are defined in the {@code Plugins.PatternPlugin} document in the main wiki (only global configuration
 * is supported at the moment), using objects of the {@code Plugins.PatternPlugin} class, which must have three fields:
 * <ul>
 * <li><tt>pattern</tt>: either a Perl regular expression replace starting with the <tt>s/</tt> command, or a plain text
 * string to replace</li>
 * <li><tt>result</tt>: the replacement string to use when the pattern is a plain text</li>
 * <li><tt>description</tt>: a short description of the substitution</li>
 * </ul>
 * <p>
 * The substitutions occur near the end of the rendering process for the xwiki/1.0 syntax only, after the content has
 * already been processed by the main rendering components (Velocity, Groovy, Radeox).
 * </p>
 * <p>
 * Another special transformation performed by the plugin is to replace the string {@code %PATTERNS%} with a table
 * listing the currently enabled substitutions.
 * </p>
 * 
 * @deprecated the plugin technology is deprecated, consider rewriting as components
 * @version $Id$
 */
@Deprecated
public class PatternPlugin extends XWikiDefaultPlugin implements EventListener
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(PatternPlugin.class);

    /** The document holding the configuration data. */
    private static final DocumentReference CONFIGURATION_DOCUMENT =
        new DocumentReference("Plugins", "PatternPlugin", "xwiki");

    /** The class used for storing the configuration. */
    private static final DocumentReference CONFIGURATION_CLASS = CONFIGURATION_DOCUMENT;

    /** Events monitored by this plugin, all changes on the configuration document. */
    private static final List<Event> EVENTS = new ArrayList<Event>()
    {
        {
            add(new DocumentCreatedEvent(CONFIGURATION_DOCUMENT));
            add(new DocumentUpdatedEvent(CONFIGURATION_DOCUMENT));
            add(new DocumentDeletedEvent(CONFIGURATION_DOCUMENT));
        }
    };

    /** The list of configured patterns to search for. */
    private List<String> patterns = new ArrayList<String>();

    /** The list of configured replacement strings. */
    private List<String> results = new ArrayList<String>();

    /** The list of configured substitution description. */
    private List<String> descriptions = new ArrayList<String>();

    /** Special wiki substitution which replaces the %PATTERNS% string with a table listing the configured patterns. */
    private WikiSubstitution patternListSubstitution;

    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     * 
     * @param name the plugin name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public PatternPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);

        // Watch for any modifications of the Plugins.PatternPlugin document.
        Utils.getComponent(ObservationManager.class).addListener(this);
        this.patternListSubstitution = new WikiSubstitution(context.getUtil(), "%PATTERNS%");
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        // If the PatternPlugin document has been modified we need to reload the patterns
        init((XWikiContext) Utils.getComponent(Execution.class).getContext().getProperty("xwikicontext"));
    }

    /**
     * Reads the configuration data and prepares the plugin.
     * 
     * @param context the current request context
     * @see XWikiPluginInterface#init(XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        XWiki xwiki = context.getWiki();
        try {
            synchronized (this.patterns) {
                this.patterns.clear();
                this.results.clear();
                this.descriptions.clear();
                XWikiDocument configurationDoc = xwiki.getDocument(CONFIGURATION_DOCUMENT, context);
                List<BaseObject> patternList = configurationDoc.getXObjects(CONFIGURATION_CLASS);
                if (patternList == null) {
                    return;
                }
                for (BaseObject pattern : patternList) {
                    if (pattern == null) {
                        continue;
                    }
                    addPattern(pattern.getStringValue("pattern"), pattern.getStringValue("result"),
                        pattern.getStringValue("description"));
                }
            }
        } catch (Exception ex) {
            LOG.error("Failed to initialize the Pattern plugin: " + ex.getMessage(), ex);
        }
    }

    /**
     * Adds a pattern to the list of transformations performed by the plugin.
     * 
     * @param pattern the regular expression to search for
     * @param result what to use instead
     * @param description the description of this transformation
     */
    public void addPattern(String pattern, String result, String description)
    {
        synchronized (this.patterns) {
            this.patterns.add(pattern);
            this.results.add(result);
            this.descriptions.add(description);
        }
    }

    /**
     * Replaces the occurrence of %PATTERNS% with the list of enabled substitutions.
     * 
     * @param content the current content being rendered
     * @param context the current request context
     * @return the resulting content after the mentioned substitution
     * @see XWikiPluginInterface#commonTagsHandler(String, XWikiContext)
     */
    @Override
    public String commonTagsHandler(String content, XWikiContext context)
    {
        String subst = getPatternList();
        subst = StringUtils.replace(subst, "$", "\\$");
        this.patternListSubstitution.setSubstitution(subst);
        return this.patternListSubstitution.substitute(content);
    }

    /**
     * Apply the currently enabled substitutions on a line of content.
     * 
     * @param line the current line being rendered on which to apply substitutions
     * @param context the current request context
     * @return the processed line
     * @see XWikiPluginInterface#outsidePREHandler(String, XWikiContext)
     */
    @Override
    public String outsidePREHandler(String line, XWikiContext context)
    {
        Util util = context.getUtil();
        String result = line;

        for (int i = 0; i < this.patterns.size(); i++) {
            String pattern = this.patterns.get(i);
            try {
                if (pattern.startsWith("s/")) {
                    result = util.substitute(pattern, result);
                } else {
                    result = StringUtils.replace(result, " " + pattern, " " + this.results.get(i));
                }
            } catch (Exception ex) {
                LOG.error("Failed to apply pattern [" + pattern + "]: " + ex.getMessage());
            }
        }

        return result;
    }

    /**
     * Generate a table listing all the enabled substitutions.
     * 
     * @return a HTML table surrounded by pre markers
     */
    private String getPatternList()
    {
        StringBuilder list = new StringBuilder();
        list.append("{pre}\n<table><thead><tr><th>Pattern</th><th>Result</th><th>Description</th></tr></thead><tbody>");
        synchronized (this.patterns) {
            for (int i = 0; i < this.patterns.size(); ++i) {
                list.append("<tr><td>");
                list.append(XMLUtils.escape(this.patterns.get(i)));
                list.append("</td><td>");
                list.append(XMLUtils.escape(this.results.get(i)));
                list.append("</td> <td>");
                list.append(XMLUtils.escape(this.descriptions.get(i)));
                list.append("</td></tr>");
            }
            list.append("</tbody></table>");
            list.append("\n{/pre}");
        }
        return list.toString();
    }
}
