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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Style;
import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.Cache.CacheCallback;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.core.client.JsArrayString;

/**
 * Base class for executables that insert or update HTML elements.
 * 
 * @param <C> the type of configuration object used to update the element
 * @param <E> the type of element that is inserted or updated
 * @version $Id$
 */
public abstract class AbstractInsertElementExecutable<C, E extends com.google.gwt.dom.client.Element> extends
    InsertHTMLExecutable
{
    /**
     * Creates a configuration object from an element.
     * 
     * @param <C> the type of configuration object returned by the parse method
     * @param <E> the type of element being parsed
     */
    public static interface ConfigHTMLParser<C, E extends com.google.gwt.dom.client.Element>
    {
        /**
         * Creates a new configuration object from the given element.
         * 
         * @param element a DOM element
         * @return the newly created configuration object
         */
        C parse(E element);
    }

    /**
     * Serializes a configuration object to an HTML fragment that can be used to insert the corresponding element into
     * the edited document.
     * 
     * @param <C> the type of configuration object that is being serialized to HTML
     */
    public static interface ConfigHTMLSerializer<C>
    {
        /**
         * Serializes a configuration object to an HTML fragment that can be used to insert the corresponding element
         * into the edited document.
         * 
         * @param config the configuration object to be serialized
         * @return the HTML fragment that can be used to insert the specified element into the edited document
         */
        String serialize(C config);
    }

    /**
     * Creates configuration objects from JSON.
     * 
     * @param <C> the type of configuration object returned by the parse method
     */
    public static interface ConfigJSONParser<C>
    {
        /**
         * Creates a new configuration object from its JSON representation.
         * 
         * @param json the JSON representation of of a configuration object
         * @return a new configuration object
         */
        C parse(String json);
    }

    /**
     * Serializes a configuration object to JSON.
     * 
     * @param <C> the type of configuration object that is being serialized to JSON
     */
    public static interface ConfigJSONSerializer<C>
    {
        /**
         * Serializes the given configuration object to JSON.
         * 
         * @param config the configuration object to be serialized
         * @return the JSON serialization
         */
        String serialize(C config);
    }

    /**
     * Base class for all classes that serialize configuration objects to JSON. It includes some utility methods.
     * 
     * @param <C> the type of configuration object that is being serialized to JSON
     */
    public abstract static class AbstractConfigJSONSerializer<C> implements ConfigJSONSerializer<C>
    {
        /**
         * Serializes a property of the {@link ImageConfig}.
         * 
         * @param property the name of the property to serialize
         * @param value the value of the specified property
         * @return the {@code property:value} JSON pair if the property value is not {@code null}, the empty string
         *         otherwise
         */
        protected String serialize(String property, Object value)
        {
            return value != null ? property + ":'" + value.toString().replace("'", "\\'") + '\'' : "";
        }

        /**
         * Appends a JSON pair to the given {@link StringBuffer}.
         * 
         * @param result the string buffer where to append the given pair
         * @param pair a {@code property:value} JSON pair
         */
        protected void append(StringBuffer result, String pair)
        {
            if (!StringUtils.isEmpty(pair)) {
                if (result.length() > 0) {
                    result.append(",");
                }
                result.append(pair);
            }
        }
    }

    /**
     * The object used to extract a configuration object from an element.
     */
    protected ConfigHTMLParser<C, E> configHTMLParser;

    /**
     * The object used to serialize a configuration object to HTML.
     */
    protected ConfigHTMLSerializer<C> configHTMLSerializer;

    /**
     * The object used to serialize a configuration object to JSON.
     */
    protected ConfigJSONParser<C> configJSONParser;

    /**
     * The object used to create a configuration object from JSON.
     */
    protected ConfigJSONSerializer<C> configJSONSerializer;

    /**
     * Creates a new executable that can be used to insert HTML elements in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public AbstractInsertElementExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#execute(String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean execute(String json)
    {
        String html = configHTMLSerializer.serialize(configJSONParser.parse(json));
        E element = getSelectedElement();
        if (element == null) {
            // Insert a new element.
            return super.execute(html);
        } else {
            // Overwrite an existing element.
            Element container = Element.as(rta.getDocument().createDivElement());
            // Inner HTML listeners have to be notified in order to extract the meta data.
            container.xSetInnerHTML(html);
            merge(element, (E) container.getFirstChild());
            return true;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#isExecuted()
     */
    @Override
    public boolean isExecuted()
    {
        // NOTE: We don't use this.getClass().getName() as cache key prefix because we want to be able to compile the
        // code without class meta data information.
        return cache.get(getCacheKeyPrefix() + "#executed", new CacheCallback<Boolean>()
        {
            public Boolean get()
            {
                return getSelectedElement() != null;
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertHTMLExecutable#getParameter()
     */
    @Override
    public String getParameter()
    {
        E selectedElement = getSelectedElement();
        if (selectedElement == null) {
            return null;
        }
        return configJSONSerializer.serialize(configHTMLParser.parse(selectedElement));
    }

    /**
     * @return the text to prefix all cache keys used by this class
     */
    protected abstract String getCacheKeyPrefix();

    /**
     * @return the selected element, {@code null} if no element is selected
     */
    protected abstract E getSelectedElement();

    /**
     * Merges the given elements.
     * 
     * @param target the element that will be updated
     * @param source the element providing the change
     */
    protected void merge(E target, E source)
    {
        // Update the content.
        if (!source.getInnerHTML().equals(target.getInnerHTML())) {
            DOMUtils.getInstance().deleteNodeContents(target, 0, target.getChildCount());
            target.appendChild(Element.as(source).extractContents());
        }

        // Merge complex attributes.
        // Merge class name attribute.
        if (!target.getClassName().equals(source.getClassName())) {
            String[] targetClassNames = target.getClassName().split("\\s+");
            for (int i = 0; i < targetClassNames.length; i++) {
                source.addClassName(targetClassNames[i]);
            }
        }
        // Merge style attribute.
        if (!Element.as(target).xGetAttribute(Style.STYLE_ATTRIBUTE).equals(
            Element.as(source).xGetAttribute(Style.STYLE_ATTRIBUTE))) {
            extend((Style) source.getStyle(), (Style) target.getStyle());
        }
        // Update all attributes.
        JsArrayString attributeNames = Element.as(source).getAttributeNames();
        for (int i = 0; i < attributeNames.length(); i++) {
            String newValue = Element.as(source).xGetAttribute(attributeNames.get(i));
            Element.as(target).xSetAttribute(attributeNames.get(i), newValue);
        }
    }

    /**
     * Copies the properties from the source style to the target style only they are not defined in the target style.
     * 
     * @param target the style to be extended
     * @param source the extension source
     */
    private native void extend(Style target, Style source)
    /*-{
        for(propertyName in source) {
            if ('' + source[propertyName] != '' && '' + target[propertyName] == '') {
                target[propertyName] = source[propertyName];
            }
        }
    }-*/;
}
