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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests validating various store APIs.
 *
 * @version $Id$
 */
@UITest
class HibernateStoreAPIIT
{
    @BeforeAll
    public void setup(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();
    }

    @Order(1)
    @Test
    void searchWithWhereParameters(TestUtils setup, TestReference testReference) throws Exception
    {
        // Make sure the page is not already there
        setup.rest().delete(testReference);

        String content = "{{groovy}}"
            + "print xwiki.getXWiki().search('select distinct doc.fullName from XWikiDocument as doc', [['doc.fullName', '"
            + setup.serializeReference(testReference.getLocalDocumentReference())
            + "']] as Object[][], xcontext.context).size()" + "{{/groovy}}";

        assertEquals("<p>0</p>", setup.executeWiki(content, Syntax.XWIKI_2_1));

        setup.rest().savePage(testReference);

        assertEquals("<p>1</p>", setup.executeWiki(content, Syntax.XWIKI_2_1));
    }
}
