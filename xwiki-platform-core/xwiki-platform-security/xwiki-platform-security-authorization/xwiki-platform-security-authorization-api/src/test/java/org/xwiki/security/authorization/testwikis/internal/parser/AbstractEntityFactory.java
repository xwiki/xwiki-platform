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

package org.xwiki.security.authorization.testwikis.internal.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.security.authorization.testwikis.TestEntity;

/**
 * Base factory for the creation of test entities based on an XML definition.
 *
 * @param <T> Specialized type of the entity created by this factory
 *
 * @version $Id$
 * @since 5.0M2
 */
public abstract class AbstractEntityFactory<T extends TestEntity> implements EntityFactory
{
    /** Parent entity to be used for all entities created by this factory. */
    private TestEntity parent;

    /** Create a factory for the root element (no parent). */
    AbstractEntityFactory() {
    }

    /**
     * Create a factory for some types children of the given parent.
     * @param parent Parent entity to be used for all entities created by this factory.
     */
    AbstractEntityFactory(TestEntity parent) {
        setParent(parent);
    }

    /**
     * Set the parent entity to be used for all entities created by this factory. Overridden by subclasses to
     * change their parent to another entity then the default behavior (which is taking the entity represented
     * by the parent XML element).
     * @param parent the entity that will hold any entities created by this factory.
     */
    protected void setParent(TestEntity parent)
    {
        this.parent = parent;
    }

    @Override
    public void newElement(ElementParser parser, String name, Attributes attributes) throws SAXException
    {
        registerFactories(parser, getNewInstance(parser, name, parent, attributes));
    }

    /**
     * Register factories for children of entity created by this factory.
     *
     * This needs to be overridden by subclasses to define possible child element of the XML element representing the
     * current entity.
     *
     * @param parser the XML parser current parsing the XML definition.
     * @param entity the entity created for representing the current element.
     */
    protected void registerFactories(ElementParser parser, T entity)
    {
    }

    /**
     * Abstract method to be overridden by subclasses to create a new instance of an test entity representing the
     * currently parsed XML element.
     *
     * @param parser the XML parser current parsing the XML definition.
     * @param name the name of the currently parsed element.
     * @param parent the parent to be used for creating the entity.
     * @param attributes the attribute of the XML element representing this entity.
     * @return a new test entity.
     * @throws SAXException on error.
     */
    abstract T getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
        throws SAXException;
}
