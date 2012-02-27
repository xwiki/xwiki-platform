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

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.user.client.Cache.CacheCallback;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.core.client.JsonUtils;

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
     * Creates a configuration object from a DOM element.
     * 
     * @param <C> the type of configuration object returned by the read method
     * @param <E> the type of DOM element being read
     */
    public interface ConfigDOMReader<C, E extends com.google.gwt.dom.client.Element>
    {
        /**
         * Reads the configuration from the given DOM element.
         * 
         * @param element a DOM element
         * @return the newly created configuration object
         */
        C read(E element);
    }

    /**
     * Updates a DOM element from a configuration object.
     * 
     * @param <C> the type of configuration object that is used to update the DOM element
     * @param <E> the type of DOM element being updated
     */
    public interface ConfigDOMWriter<C, E extends com.google.gwt.dom.client.Element>
    {
        /**
         * Writes the given configuration to the specified DOM element.
         * 
         * @param config the configuration object used to update the DOM element
         * @param element the DOM element being updated
         */
        void write(C config, E element);
    }

    /**
     * Creates configuration objects from JSON.
     * 
     * @param <C> the type of configuration object returned by the parse method
     */
    public interface ConfigJSONParser<C>
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
    public interface ConfigJSONSerializer<C>
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
            return value != null ? property + ":" + JsonUtils.escapeValue(value.toString()) : "";
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
    protected ConfigDOMReader<C, E> configDOMReader;

    /**
     * The object used to update a DOM element from a configuration object.
     */
    protected ConfigDOMWriter<C, E> configDOMWriter;

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

    @Override
    public boolean execute(String json)
    {
        C config = configJSONParser.parse(json);
        E element = getSelectedElement();
        if (element == null) {
            // Insert a new element.
            element = newElement();
            if (!super.execute(element)) {
                return false;
            }
        }
        // Update the selected element.
        write(config, element);
        return true;
    }

    /**
     * Updates the attributes of the given element based on the given configuration object.
     * <p>
     * Note: This method was added mainly to allow derived classes to adjust the configuration object before the element
     * is updated.
     * 
     * @param config the object used to update the attributes of the given element
     * @param element the element whose attributes are being updated
     */
    protected void write(C config, E element)
    {
        configDOMWriter.write(config, element);
    }

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

    @Override
    public String getParameter()
    {
        E selectedElement = getSelectedElement();
        if (selectedElement == null) {
            return null;
        }
        return configJSONSerializer.serialize(configDOMReader.read(selectedElement));
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
     * @return a new element to be inserted
     */
    protected abstract E newElement();
}
