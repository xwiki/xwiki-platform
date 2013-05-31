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
import org.xwiki.security.authorization.testwikis.TestDefinition;
import org.xwiki.security.authorization.testwikis.TestEntity;
import org.xwiki.security.authorization.testwikis.internal.entities.DefaultTestDefinition;

/**
 * Root entity factory.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestWikisFactory extends AbstractEntityFactory<TestDefinition>
{
    /** The entity created for the root. */
    private TestDefinition wikis;

    @Override
    public List<String> getTagNames()
    {
        return Arrays.asList("wikis");
    }

    @Override
    TestDefinition getNewInstance(ElementParser parser, String name, TestEntity parent, Attributes attributes)
        throws SAXException
    {
        if (wikis != null) {
            throw new SAXException(parser.getLocatedMessage("Only one root element is allowed per file."));
        }
        wikis = new DefaultTestDefinition();
        return wikis;
    }

    @Override
    protected void registerFactories(ElementParser parser, TestDefinition entity)
    {
        parser.register(new TestWikiFactory(wikis));
    }

    /**
     * @return the entity created for the root.
     */
    TestDefinition getWikis() {
        return wikis;
    }
}
