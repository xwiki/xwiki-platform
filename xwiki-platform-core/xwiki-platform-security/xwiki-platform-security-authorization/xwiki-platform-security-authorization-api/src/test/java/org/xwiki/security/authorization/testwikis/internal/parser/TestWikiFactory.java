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
import org.xml.sax.SAXException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.TestWiki;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestDocument;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestWiki;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestDefinition;

/**
 * Test wiki factory.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestWikiFactory extends AbstractSecureEntityFactory<TestWiki>
{
    /**
     * Create a new factory for wikis of the given parent (usually a root entity).
     * @param parent parent entity to used by this factory.
     */
    TestWikiFactory(TestEntity parent)
    {
        setParent(parent);
    }

    @Override
    public List<String> getTagNames()
    {
        return Arrays.asList("wiki");
    }

    @Override
    TestWiki getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
        throws SAXException
    {
        EntityReference reference = new WikiReference(attributes.getValue("name"));
        boolean isMainWiki = "true".equals(attributes.getValue("mainWiki"));
        String ownerString = attributes.getValue("owner");
        EntityReference owner = (ownerString) != null ? parser.getResolver().resolve(ownerString,
            DefaultTestDocument.TYPE, new EntityReference(XWikiConstants.XWIKI_SPACE, EntityType.SPACE, reference))
            : null;

        if (isMainWiki && ((DefaultTestDefinition) parent).getMainWiki() != null) {
            throw new SAXException(parser.getLocatedMessage("Only one main wiki may be defined."));
        }

        return new DefaultTestWiki(reference, isMainWiki, owner, attributes.getValue("alt"), parent);
    }

    @Override
    protected void registerFactories(ElementParser parser, TestWiki entity)
    {
        super.registerFactories(parser, entity);
        parser.register(new TestSpaceFactory(entity));
        parser.register(new TestUserFactory(entity));
        parser.register(new TestGroupFactory(entity));
    }
}
