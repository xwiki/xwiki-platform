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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Plugin which transforms simple wiki tables into HTML tables. The syntax is:
 * 
 * <pre>
 * |cell 1|cell 2|...|cell n|
 * </pre>
 * 
 * The first row is considered a table header. The table starts automatically with the first line that respects this
 * format, and ends automatically at the first line that doesn't. It is possible to configure the table with:
 * <ul>
 * <li>an XML identifier</li>
 * <li>extra CSS classnames</li>
 * <li>inter-cell spacing</li>
 * <li>an optional border</li>
 * <li>intra-cell padding</li>
 * <li>a different background color for the first (header) row</li>
 * <li>a different background color for the following (data) rows</li>
 * </ul>
 * These parameters can be defined either in an object attached to <tt>xwiki:Plugins.TablePlugin</tt> (the classname of
 * the object doesn't matter), or in an optional explicit table marker, with the following syntax:
 * 
 * <pre>
 * %TABLE{tableclass="some more classnames" cellpadding="5"}%
 * </pre>
 * 
 * The parameter values must be surrounded by quotes. If such a table start marker is encountered between two table row
 * lines, the previous table is automatically ended and flushed. The settings specified in a marker are preserved for
 * the rest of the document, or until another table marker is encountered.
 * 
 * @version $Id$
 * @deprecated Very early method of converting a table from wiki syntax to HTML. It outputs verbose, hard to configure,
 *             non-XHTML syntax. Native table renderers are implemented in all the syntaxes and should be used instead.
 */
@Deprecated
public class TablePlugin extends XWikiDefaultPlugin
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(TablePlugin.class);

    /** The context key under which the table parameters are stored. */
    private static final String PARAMETERS_CONTEXT_KEY = "TablePluginParams";

    // Property keys, used as keys in the parameters map.
    /** Property key: identifier. */
    private static final String KEY_ID = "table_id";

    /** Property key: CSS classname. */
    private static final String KEY_CLASSNAME = "table_class";

    /** Property key: cell spacing. */
    private static final String KEY_SPACING = "cell_spacing";

    /** Property key: border. */
    private static final String KEY_BORDER = "table_border";

    /** Property key: cell padding. */
    private static final String KEY_PADDING = "cell_padding";

    /** Property key: header cell background color. */
    private static final String KEY_HEADER_BACKGROUND = "header_bg";

    /** Property key: data cell background color. */
    private static final String KEY_DATA_BACKGROUND = "data_bg";

    /** Property key: inside or outside table. */
    private static final String KEY_INSIDE = "inside_table";

    /** Property key: table data. */
    private static final String KEY_DATA = "current_table";

    /** All property keys. */
    private static final List<String> KEYS;

    // Property names, used as object property names in the global configuration in xwiki:Plugins.TablePlugin,
    // and as setting names in the optional table marker.
    /** Table property name: identifier. A valid HTML identifier. */
    private static final String PROPERTY_ID = "tableid";

    /** Table property name: CSS classname. A valid list of CSS classnames. */
    private static final String PROPERTY_CLASSNAME = "tableclass";

    /** Table property name: cell spacing. A positive integer. */
    private static final String PROPERTY_SPACING = "cellspacing";

    /** Table property name: border. 0 or 1 */
    private static final String PROPERTY_BORDER = "tableborder";

    /** Table property name: cell padding. A positive integer. */
    private static final String PROPERTY_PADDING = "cellpadding";

    /** Table property name: header cell background color. A valid CSS color. */
    private static final String PROPERTY_HEADER_BACKGROUND = "headerbg";

    /** Table property name: data cell background color. A valid CSS color. */
    private static final String PROPERTY_DATA_BACKGROUND = "databg";

    /** All property names. */
    private static final List<String> PROPERTIES;

    // Regular expressions
    /** Regular expression matching a table start. */
    private static final Pattern TABLE_START = Pattern.compile("^\\s*+%TABLE\\{(.*?)\\}%\\s*+$");

    /** Regular expression matching a table row. */
    private static final Pattern TABLE_ROW = Pattern.compile("^\\s*+\\|.*\\|\\s*+$");

    /** Part of the regular expression that is used for extracting property values from the inline table settings. */
    private static final String PROPERTY_PATTERN_SUFFIX = "\\s*+=\\s*+\"([^\"]*+)\"";

    /** Map of precompiled patterns for extracting the possible table properties from the inline table settings. */
    private static final Map<String, Pattern> PROPERTY_PATTERNS;

    static {
        KEYS = new ArrayList<String>();
        KEYS.add(KEY_ID);
        KEYS.add(KEY_CLASSNAME);
        KEYS.add(KEY_SPACING);
        KEYS.add(KEY_BORDER);
        KEYS.add(KEY_PADDING);
        KEYS.add(KEY_HEADER_BACKGROUND);
        KEYS.add(KEY_DATA_BACKGROUND);

        PROPERTIES = new ArrayList<String>();
        PROPERTIES.add(PROPERTY_ID);
        PROPERTIES.add(PROPERTY_CLASSNAME);
        PROPERTIES.add(PROPERTY_SPACING);
        PROPERTIES.add(PROPERTY_BORDER);
        PROPERTIES.add(PROPERTY_PADDING);
        PROPERTIES.add(PROPERTY_HEADER_BACKGROUND);
        PROPERTIES.add(PROPERTY_DATA_BACKGROUND);

        PROPERTY_PATTERNS = new HashMap<String, Pattern>();
        for (String property : PROPERTIES) {
            PROPERTY_PATTERNS.put(property, Pattern.compile(property + PROPERTY_PATTERN_SUFFIX));
        }
    }

    // Default parameters
    /** The HTML identifier (the {@code id} attribute). */
    private String defaultTableId = "xwikitableid";

    /** The CSS classname (the {@code class} attribute). */
    private String defaultTableClass = "xwikitableclass";

    /** The intercell spacing (the {@code cellspacing} attribute). */
    private String defaultCellSpacing = "1";

    /** Whether or not to draw borders in the table (the {@code border} attribute). */
    private String defaultTableBorder = "1";

    /** The inner cell padding (the {@code cellpadding} attribute). */
    private String defaultCellPadding = "1";

    /** The background color for the table header cells (the {@code bgcolor} attribute). */
    private String defaultHeaderBackground = "#DDDDDD";

    /** The background color for the table data cells (the {@code bgcolor} attribute). */
    private String defaultDataBackground = "#CCCCCC";

    /**
     * The mandatory plugin constructor, this is the method called (through reflection) by the plugin manager.
     * 
     * @param name the plugin name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public TablePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
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
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument doc =
                xwiki.getDocument(new DocumentReference(context.getMainXWiki(), "Plugins", "TablePlugin"), context);
            BaseObject pluginConfiguration = doc.getXObject();

            if (pluginConfiguration != null) {
                if (pluginConfiguration.get(PROPERTY_ID) != null) {
                    this.defaultTableId = pluginConfiguration.get(PROPERTY_ID).toString();
                }
                if (pluginConfiguration.get(PROPERTY_CLASSNAME) != null) {
                    this.defaultTableClass = pluginConfiguration.get(PROPERTY_CLASSNAME).toString();
                }
                if (pluginConfiguration.get(PROPERTY_SPACING) != null) {
                    this.defaultCellSpacing = pluginConfiguration.get(PROPERTY_SPACING).toString();
                }
                if (pluginConfiguration.get(PROPERTY_BORDER) != null) {
                    this.defaultTableBorder = pluginConfiguration.get(PROPERTY_BORDER).toString();
                }
                if (pluginConfiguration.get(PROPERTY_PADDING) != null) {
                    this.defaultCellPadding = pluginConfiguration.get(PROPERTY_PADDING).toString();
                }
                if (pluginConfiguration.get(PROPERTY_HEADER_BACKGROUND) != null) {
                    this.defaultHeaderBackground = pluginConfiguration.get(PROPERTY_HEADER_BACKGROUND).toString();
                }
                if (pluginConfiguration.get(PROPERTY_DATA_BACKGROUND) != null) {
                    this.defaultDataBackground = pluginConfiguration.get(PROPERTY_DATA_BACKGROUND).toString();
                }
            }
        } catch (Exception ex) {
            LOG.warn("Failed to read the TablePlugin configuration: " + ex.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#outsidePREHandler(String, XWikiContext)
     */
    @Override
    public String outsidePREHandler(String line, XWikiContext context)
    {
        String result = line;
        Map<String, Object> params = getParams(context);

        Matcher m = TABLE_START.matcher(line);
        if (m.find()) {
            if (Boolean.TRUE == params.get(KEY_INSIDE)) {
                result = emitTable(params);
            } else {
                result = "";
            }
            processExplicitTableParameters(m.group(1), context);
            return result;
        }

        m = TABLE_ROW.matcher(line);
        if (m.matches()) {
            processTR(line, context);
            params.put(KEY_INSIDE, Boolean.TRUE);
            result = "";
        } else if (Boolean.TRUE == params.get(KEY_INSIDE)) {
            result = emitTable(params) + line;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiPluginInterface#endRenderingHandler(String, XWikiContext)
     */
    @Override
    public String endRenderingHandler(String content, XWikiContext context)
    {
        // If there's still an unfinished table, flush it.
        Map<String, Object> params = getParams(context);
        if (Boolean.TRUE == params.get(KEY_INSIDE)) {
            return content + emitTable(params);
        }
        return content;
    }

    /**
     * Get the current table parameters and data. The parameters are computed as custom overrides, on top of default
     * values defined in the {@code xwiki:Plugins.TablePlugin} document, on top of a default set of values. The table
     * data contains the cell data definition (as a vector of vectors of string), as well as a marker specifying whether
     * we're currently parsing a wiki table or not.
     * 
     * @param context the current request context
     * @return the table parameters
     */
    public Map<String, Object> getParams(XWikiContext context)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) context.get(PARAMETERS_CONTEXT_KEY);
        if (params == null) {
            params = new HashMap<String, Object>();
            context.put(PARAMETERS_CONTEXT_KEY, params);
            params.put(KEY_ID, this.defaultTableId);
            params.put(KEY_CLASSNAME, this.defaultTableClass);
            params.put(KEY_SPACING, this.defaultCellSpacing);
            params.put(KEY_BORDER, this.defaultTableBorder);
            params.put(KEY_PADDING, this.defaultCellPadding);
            params.put(KEY_HEADER_BACKGROUND, this.defaultHeaderBackground);
            params.put(KEY_DATA_BACKGROUND, this.defaultDataBackground);
            params.put(KEY_DATA, new ArrayList<List<String>>());
            params.put(KEY_INSIDE, Boolean.FALSE);
        }
        return params;
    }

    /**
     * Extract the parameters passed in the table start line and override the default parameters.
     * 
     * @param settings the table settings to parse
     * @param context the current request context
     */
    private void processExplicitTableParameters(String settings, XWikiContext context)
    {
        // Reinitialize parameters
        context.remove(PARAMETERS_CONTEXT_KEY);
        Map<String, Object> params = getParams(context);

        for (int i = 0; i < PROPERTIES.size(); ++i) {
            String property = PROPERTIES.get(i);
            String key = KEYS.get(i);
            String value = extractProperty(property, settings, context);
            if (value != null) {
                params.put(key, value);
            }
        }
    }

    /**
     * From a list of {@code name="value"} pairs of settings, extract the value of a certain property, if any. If the
     * property is defined more than once, then the first value is returned. If the property is not defined at all,
     * {@code null} is returned.
     * 
     * @param name the property to extract
     * @param text the text in which to search
     * @param context the current request context
     * @return the specified value for this property, or {@code null} if not defined
     */
    private String extractProperty(String name, String text, XWikiContext context)
    {
        Matcher m = PROPERTY_PATTERNS.get(name).matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    /**
     * Split a line identified as a table row into individual cells, and store the new data in the currently
     * accumulating table.
     * 
     * @param line the line to process
     * @param context the current request context.
     */
    private void processTR(String line, XWikiContext context)
    {
        String[] cells = line.trim().split("\\|");
        List<List<String>> currentTable = getCurrentTable(getParams(context));
        List<String> row = new ArrayList<String>(cells.length);
        // Start from 1 since the first value is the initial empty string before the first |
        for (int i = 1; i < cells.length; ++i) {
            row.add(cells[i]);
        }
        currentTable.add(row);
    }

    /**
     * Generate the currently accumulated table as HTML, and reset the parameters accordingly: empty the accumulated
     * data, mark that the rendering is outside tables.
     * 
     * @param params the table parameters
     * @return the generated HTML table
     */
    private String emitTable(Map<String, Object> params)
    {
        List<List<String>> currentTable = getCurrentTable(params);
        StringBuilder text = new StringBuilder();
        text.append("<table border=\"" + params.get(KEY_BORDER)
            + "\" id=\"" + params.get(KEY_ID)
            + "\" class=\"" + params.get(KEY_CLASSNAME)
            + "\" cellspacing=\"" + params.get(KEY_SPACING)
            + "\" cellpadding=\"" + params.get(KEY_PADDING) + "\">\n");
        String bgColor = (String) params.get(KEY_HEADER_BACKGROUND);
        for (List<String> row : currentTable) {
            text.append("<tr>");
            for (String cell : row) {
                text.append("<td bgcolor=\"" + bgColor + "\">" + cell + "</td>\n");
            }
            text.append("</tr>\n");
            bgColor = (String) params.get(KEY_DATA_BACKGROUND);
        }
        text.append("</table>\n");
        currentTable.clear();
        params.put(KEY_INSIDE, Boolean.FALSE);
        return text.toString();
    }

    /**
     * Get the current table from the context.
     * 
     * @param params the current table parameters
     * @return the current table being accumulated
     */
    @SuppressWarnings("unchecked")
    private List<List<String>> getCurrentTable(Map<String, Object> params)
    {
        return (List<List<String>>) params.get(KEY_DATA);
    }
}
