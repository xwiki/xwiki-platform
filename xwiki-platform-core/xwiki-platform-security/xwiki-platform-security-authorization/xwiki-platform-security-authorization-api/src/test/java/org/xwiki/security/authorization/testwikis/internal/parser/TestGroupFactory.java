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
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.testwikis.TestDocument;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestDocument;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestGroupDocument;

/**
 * Factory for document representing groups.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestGroupFactory extends TestUserFactory
{
    /**
     * Create a new factory for document representing groups.
     * @param parent parent entity to used by this factory.
     */
    TestGroupFactory(TestEntity parent)
    {
       super(parent);
    }

    @Override
    public List<String> getTagNames()
    {
        return Arrays.asList("group");
    }

    @Override
    protected void registerFactories(ElementParser parser, TestDocument entity)
    {
        super.registerFactories(parser, entity);
        parser.register(new TestGroupUserFactory(entity));
    }

    @Override
    TestDocument getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
    {
        EntityReference reference = parser.getResolver().resolve(attributes.getValue("name"),
            DefaultTestDocument.TYPE,
            parent.getReference());
        EntityReference creator = parser.getResolver().resolve(attributes.getValue("creator"),
            DefaultTestDocument.TYPE,
            new EntityReference(XWikiConstants.XWIKI_SPACE, EntityType.SPACE, reference.getRoot()));

        return new DefaultTestGroupDocument(reference, creator, attributes.getValue("alt"), parent);
    }
}
