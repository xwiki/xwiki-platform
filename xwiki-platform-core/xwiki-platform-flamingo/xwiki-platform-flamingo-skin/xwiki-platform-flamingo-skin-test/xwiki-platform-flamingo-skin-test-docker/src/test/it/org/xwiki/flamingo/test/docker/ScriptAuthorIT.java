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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Make sure a script end up being executed by the right author in various use cases.
 *
 * @version $Id$
 */
@UITest
class ScriptAuthorIT
{
    @BeforeAll
    public void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @AfterAll
    public void afterAll(TestUtils setup)
    {
        // Make sure go back to the initial state regarding the current user
        setup.setSession(null);
    }

    @Test
    @Order(1)
    void renderTitleInModifiedDocument(TestUtils setup, TestReference reference) throws Exception
    {
        // Give a page programming right
        setup.loginAsSuperAdmin();
        DocumentReference programmingReference = reference;
        setup.rest().savePage(programmingReference);

        // Write a script without programming right
        setup.createUser("renderTitleInModifiedDocument", "renderTitleInModifiedDocument", null);
        setup.setGlobalRights(null, "renderTitleInModifiedDocument", "script", true);
        setup.login("renderTitleInModifiedDocument", "renderTitleInModifiedDocument");
        DocumentReference scriptReference =
            new DocumentReference("Script", programmingReference.getLastSpaceReference());
        StringBuilder source = new StringBuilder();
        source.append("{{velocity}}\n");
        source.append("#set($main = $xwiki.getDocument('" + setup.serializeReference(programmingReference) + "'))\n");
        source.append("$main.setTitle('$doc.document.authors.contentAuthor')\n");
        source.append("$main.getPlainTitle()\n");
        source.append("{{/velocity}}");
        setup.rest().savePage(scriptReference, source.toString(), "sheet title");

        StringBuilder result = new StringBuilder();
        result.append("<p>$doc.document.authors.contentAuthor</p>");
        assertEquals(result.toString(), setup.executeAndGetBodyAsString(scriptReference, null));
    }
}
