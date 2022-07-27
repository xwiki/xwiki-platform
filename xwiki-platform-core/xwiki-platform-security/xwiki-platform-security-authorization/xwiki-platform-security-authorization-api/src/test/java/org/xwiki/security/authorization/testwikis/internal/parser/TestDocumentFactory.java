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

/**
 * Test document factory.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestDocumentFactory extends AbstractSecureEntityFactory<TestDocument>
{
    /**
     * Create a new factory for document of the given parent (a space usually).
     * @param parent parent entity to used by this factory.
     */
    TestDocumentFactory(TestEntity parent)
    {
        setParent(parent);
    }

    @Override
    public List<String> getTagNames()
    {
        return Arrays.asList("document");
    }

    @Override
    TestDocument getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
    {
        EntityReference reference = parser.getResolver().resolve(attributes.getValue("name"),
            DefaultTestDocument.TYPE,
            parent.getReference());
        String creatorString = attributes.getValue("creator");
        EntityReference creator = (creatorString) != null ? parser.getResolver().resolve(creatorString,
            DefaultTestDocument.TYPE, new EntityReference(XWikiConstants.XWIKI_SPACE, EntityType.SPACE,
            reference.getRoot()))
            : null;


        return new DefaultTestDocument(reference, creator, attributes.getValue("alt"), parent);
    }
}
