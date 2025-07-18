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
package com.xpn.xwiki.objects.classes;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.ecs.xhtml.input;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.velocity.tools.EscapeTool;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.xml.XMLAttributeValueFilter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

import static org.apache.commons.lang.StringEscapeUtils.escapeJavaScript;

public class StringClass extends PropertyClass
{
    private static final long serialVersionUID = 1L;

    private static final String XCLASSNAME = "string";

    public StringClass(String name, String prettyname, PropertyMetaClass wclass)
    {
        super(name, prettyname, wclass);
        setSize(30);
    }

    public StringClass(PropertyMetaClass wclass)
    {
        this(XCLASSNAME, "String", wclass);
    }

    public StringClass()
    {
        this(null);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public boolean isPicker()
    {
        return (getIntValue("picker") == 1);
    }

    public void setPicker(boolean picker)
    {
        setIntValue("picker", picker ? 1 : 0);
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();
        property.setValue(value);
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new StringProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        input input = new input();
        input.setAttributeFilter(new XMLAttributeValueFilter());
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toText());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());
        /* This is a text alternative fallback to explain what the input is about. 
         If the input has already been labelled in another way, this fallback will be ignored by Assistive Techs.
         */
        input.addAttribute("aria-label", localizePlainOrKey("core.model.xclass.editClassProperty.textAlternative",
            this.getTranslatedPrettyName(context)));

        if (isPicker()) {
            displayPickerEdit(input);
        }
        buffer.append(input);
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        BaseProperty property = (BaseProperty) object.safeget(name);
        if (property != null) {
            buffer.append(XMLUtils.escapeElementText(property.toText()));
        }
    }

    private void displayPickerEdit(input input)
    {
        input.setClass("suggested");
        XWikiContext xWikiContext = getXWikiContext();
        XWiki xwiki = xWikiContext.getWiki();
        String path = xwiki.getURL(new LocalDocumentReference("Main", "WebHome"), "view", xWikiContext);
        String stringBuilder = String.format("%s?%s&", path, new EscapeTool().url(getParametersMap()));
        input.setOnFocus(String.format("new ajaxSuggest(this, {script:\"%s\", varname:\"input\"} )",
            escapeJavaScript(stringBuilder)));
    }

    private Map<String, String> getParametersMap()
    {
        String dash = "-";
        // Using a linked hash map to keep the order of the keys stable when generating the query parameters, which 
        // is especially handy for testing, but could be useful in other scenarios.
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("xpage", "suggest");
        parameters.put("classname", getObject().getName());
        parameters.put("fieldname", getName());
        parameters.put("firCol", dash);
        parameters.put("secCol", dash);
        return parameters;
    }
}
