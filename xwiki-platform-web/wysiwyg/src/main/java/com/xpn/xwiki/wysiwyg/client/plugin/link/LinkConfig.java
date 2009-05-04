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
package com.xpn.xwiki.wysiwyg.client.plugin.link;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xwiki.gwt.dom.client.JavaScriptObject;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Stores the data about a link: information about the link: reference (wiki, space, page), URL and also about its
 * representation in a document (parameters, etc).
 * 
 * @version $Id$
 */
public class LinkConfig implements IsSerializable
{
    /**
     * Enumeration type to store the type of link: wiki external link, link to an existing page, link to a new page,
     * etc.
     */
    public enum LinkType
    {
        /**
         * Link types: external link (default for any unrecognized link), internal link targeting an existent page,
         * internal link targeting a new page, link targeting an attached file, external link to an email address.
         */
        EXTERNAL, NEW_WIKIPAGE, WIKIPAGE, ATTACHMENT, EMAIL
    };

    /**
     * The attribute name for the known attribute to store tooltip in.
     */
    private static final String TOOLTIP_ATTRIBUTE = "title";

    /**
     * The attribute name for the known attribute which specifies whether a link opens in a new page or not.
     */
    private static final String TARGET_ATTRIBUTE = "rel";

    /**
     * The attribute value for the {@link #TARGET_ATTRIBUTE} which specifies that a link opens in a new page.
     */
    private static final String NEW_WINDOW_TARGET_VALUE = "__blank";

    /**
     * The prefix to set for all custom parameters on serialization so that there are no conflicts with the JavaScript
     * object properties. For example, there are issues on IE browsers with the "class" property.
     */
    private static final String PARAM_PREFIX = "_x";

    /**
     * The URL of this link which can be either a relative or an absolute URL.
     * 
     * @see #reference
     */
    private String url;

    /**
     * The name of the wiki where the target document of this link is located.
     * 
     * @see #reference
     */
    private String wiki;

    /**
     * The name of the space of the target page of this link.
     * 
     * @see #reference
     */
    private String space;

    /**
     * The name of the target page of this link.
     * 
     * @see #reference
     */
    private String page;

    /**
     * The reference of the link, in the {@code wikiname:spacename.pagename} form. <br />
     * Note: this value should take priority over wiki, space, page set in this config: if the reference is set, that
     * this one should be used instead of generating another. Ideally, this should be set with the URL so that no
     * further computing is necessary for this link. If this and url are not set, they are to be computed from the wiki,
     * space, page values.
     */
    private String reference;

    /**
     * The label of this link, in text form (editable form: the one which we present to the user and allow her to edit).
     */
    private String labelText;

    /**
     * Specifies if the editable form of the label of this link is readonly or not.
     */
    private boolean readOnlyLabel;

    /**
     * The label of this link, in original (HTML) form.
     */
    private String label;

    /**
     * The type of this link.
     */
    private LinkType type;

    /**
     * The map of custom parameters set for this link. This will store all html parameters passed in wiki syntax and the
     * methods that access known parameters (title, rel) will use this underlying map. <br />
     * FIXME: this won't keep the order of the parameters, unfortunately. <br />
     * TODO: think about moving all config members to this map
     */
    private Map<String, String> parameters = new HashMap<String, String>();

    /**
     * @return the url
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url)
    {
        this.url = url;
    }

    /**
     * @return the wiki
     */
    public String getWiki()
    {
        return wiki;
    }

    /**
     * @param wiki the wiki to set
     */
    public void setWiki(String wiki)
    {
        this.wiki = wiki;
    }

    /**
     * @return the space
     */
    public String getSpace()
    {
        return space;
    }

    /**
     * @param space the space to set
     */
    public void setSpace(String space)
    {
        this.space = space;
    }

    /**
     * @return the page
     */
    public String getPage()
    {
        return page;
    }

    /**
     * @param page the page to set
     */
    public void setPage(String page)
    {
        this.page = page;
    }

    /**
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * @return the type
     */
    public LinkType getType()
    {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(LinkType type)
    {
        this.type = type;
    }

    /**
     * @return the labelText
     */
    public String getLabelText()
    {
        return labelText;
    }

    /**
     * @param labelText the labelText to set
     */
    public void setLabelText(String labelText)
    {
        this.labelText = labelText;
    }

    /**
     * @return the readOnlyLabel
     */
    public boolean isReadOnlyLabel()
    {
        return readOnlyLabel;
    }

    /**
     * @param readOnlyLabel the readOnlyLabel to set
     */
    public void setReadOnlyLabel(boolean readOnlyLabel)
    {
        this.readOnlyLabel = readOnlyLabel;
    }

    /**
     * @return the reference
     */
    public String getReference()
    {
        return reference;
    }

    /**
     * @param reference the reference to set
     */
    public void setReference(String reference)
    {
        this.reference = reference;
    }

    /**
     * @return {@code true} if this link is configured to open in a new window, {@code false} otherwise
     */
    public boolean isOpenInNewWindow()
    {
        String value = getParameter(TARGET_ATTRIBUTE);
        if (NEW_WINDOW_TARGET_VALUE.equals(value)) {
            return true;
        }
        return false;
    }

    /**
     * @param openInNewWindow whether this link should be opened in a new window or not
     */
    public void setOpenInNewWindow(boolean openInNewWindow)
    {
        if (openInNewWindow) {
            setParameter(TARGET_ATTRIBUTE, NEW_WINDOW_TARGET_VALUE);
        } else {
            removeParameter(TARGET_ATTRIBUTE);
        }
    }

    /**
     * @return the tooltip of this link
     */
    public String getTooltip()
    {
        return getParameter(TOOLTIP_ATTRIBUTE);
    }

    /**
     * @param tooltip the tooltip to set for this link
     */
    public void setTooltip(String tooltip)
    {
        setParameter(TOOLTIP_ATTRIBUTE, tooltip);
    }

    /**
     * @param name the name of the parameter to get
     * @return the value of the parameter
     */
    public String getParameter(String name)
    {
        return parameters.get(name);
    }

    /**
     * Sets the value of the specified parameter to the specified value and returns the old value, if any, or null
     * otherwise.
     * 
     * @param name the name of the parameter to set
     * @param value the value of the parameter to set
     * @return the old value of the set parameter, if any
     */
    public String setParameter(String name, String value)
    {
        return parameters.put(name, value);
    }

    /**
     * @param name the name of the parameter to unset
     * @return the old value of the parameter to unset, if any, or {@code null} otherwise
     */
    public String removeParameter(String name)
    {
        return parameters.remove(name);
    }

    /**
     * @return the JSON representation of this ImageConfig
     */
    public String toJSON()
    {
        // FIXME: these serializations might collide with the key names for custom parameters.
        String jsonString =
            "{ " + formatValue("reference", getReference()) + formatValue("url", getUrl())
                + formatValue("label", getLabel()) + formatValue("labeltext", getLabelText())
                + formatValue("readonlylabel", isReadOnlyLabel() ? true : null) + formatValue("type", getType());
        // serialize all parameters
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            jsonString += formatValue(PARAM_PREFIX + param.getKey(), param.getValue());
        }
        // Remove last comma
        if (jsonString.length() > 4) {
            jsonString = jsonString.substring(0, jsonString.length() - 2);
        }
        // close it and return it
        jsonString = jsonString + " }";
        return jsonString;
    }

    /**
     * Formats the passed value as a key: value JSON pair, if the key is not null. If it is, the void string is
     * returned.
     * 
     * @param key the key of the formatted pair
     * @param value the value of the formatted pair
     * @return the formatted key: value JSON pair
     */
    private String formatValue(String key, Object value)
    {
        return value != null ? key + ": '" + value.toString().replace("'", "\\'") + "', " : "";
    }

    /**
     * Fills this object with data from the passed JSON representation.
     * 
     * @param json the JSON representation of this image config object.
     */
    public void fromJSON(String json)
    {
        JavaScriptObject jsObj = JavaScriptObject.fromJson(json);
        setReference((String) jsObj.get("reference"));
        setUrl((String) jsObj.get("url"));
        setLabel((String) jsObj.get("label"));
        setLabelText((String) jsObj.get("labeltext"));
        setReadOnlyLabel(jsObj.get("readonlylabel") != null ? Boolean.parseBoolean((String) jsObj.get("readonlylabel"))
            : false);
        setType(jsObj.get("type") != null ? LinkType.valueOf((String) jsObj.get("type")) : null);
        // load the link parameters from the remaining parameters
        List<String> processedKeys = Arrays.asList("reference", "url", "label", "labeltext", "readonlylabel", "type");
        JsArrayString keys = jsObj.getKeys();
        for (int i = 0; i < keys.length(); i++) {
            if (!processedKeys.contains(keys.get(i))) {
                // add it to the parameters
                parameters.put(keys.get(i).substring(2), jsObj.get(keys.get(i)).toString());
            }
        }
    }

    /**
     * Returns an iterable type over the list of parameters for this {@link LinkConfig}. Implemented through this
     * function to hide the actual parameters map and return only a read-only iterator.
     * 
     * @return an iterable type over the this {@link LinkConfig}'s parameters collection
     */
    public Iterable<Entry<String, String>> listParameters()
    {
        return new Iterable<Entry<String, String>>()
        {
            public Iterator<Entry<String, String>> iterator()
            {
                return parameters.entrySet().iterator();
            }
        };
    }
}
