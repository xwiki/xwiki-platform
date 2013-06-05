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

import java.util.Arrays;
import java.util.List;

import org.xml.sax.Attributes;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestSpace;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestSpace;

/**
 * Test Space factory.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestSpaceFactory extends AbstractSecureEntityFactory<TestSpace>
{
    /**
     * Create a new factory for spaces of the given parent (wiki usually).
     * @param parent parent entity to used by this factory.
     */
    TestSpaceFactory(TestEntity parent)
    {
        setParent(parent);
    }

    @Override
    public List<String> getTagNames()
    {
        return Arrays.asList("space");
    }

    @Override
    TestSpace getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
    {
        EntityReference reference = parser.getResolver().resolve(attributes.getValue("name"),
            DefaultTestSpace.TYPE,
            parent.getReference());

        TestSpace space = (TestSpace) parent.getEntity(reference);
        if (space == null) {
            space = new DefaultTestSpace(reference, attributes.getValue("alt"), parent);
        }

        return space;
    }

    @Override
    protected void registerFactories(ElementParser parser, TestSpace entity)
    {
        super.registerFactories(parser, entity);
        parser.register(new TestDocumentFactory(entity));
    }

}
