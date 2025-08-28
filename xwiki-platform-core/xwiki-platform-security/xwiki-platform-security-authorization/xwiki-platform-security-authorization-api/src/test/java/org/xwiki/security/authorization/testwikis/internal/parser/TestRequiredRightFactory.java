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

import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xwiki.model.EntityType;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestRequiredRight;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestRequiredRight;

/**
 * Test required right factory.
 *
 * @version $Id$
 */
public class TestRequiredRightFactory extends AbstractEntityFactory<TestRequiredRight>
{
    /**
     * Create a new factory for required rights of the given parent.
     *
     * @param parent parent entity to used by this factory.
     */
    public TestRequiredRightFactory(TestEntity parent)
    {
        super(parent);
    }

    @Override
    public List<String> getTagNames()
    {
        return List.of("requiredRight");
    }

    @Override
    TestRequiredRight getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
        throws SAXException
    {
        Right right = Right.toRight(attributes.getValue("type"));
        String scopeValue = attributes.getValue("scope");
        EntityType scope;
        if (scopeValue == null) {
            scope = EntityType.DOCUMENT;
        } else if ("farm".equalsIgnoreCase(scopeValue)) {
            scope = null;
        } else {
            scope = EntityType.valueOf(scopeValue.toUpperCase());
        }

        return new DefaultTestRequiredRight(right, scope, parent);
    }
}
