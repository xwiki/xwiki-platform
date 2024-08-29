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

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests the sheet system.
 *
 * @version $Id$
 */
@UITest
class SheetIT
{
    @BeforeAll
    public void setup(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void sheetURLParameter(TestUtils setup, TestReference reference) throws Exception
    {
        DocumentReference pageReference = reference;
        DocumentReference sheetReference =
            new DocumentReference(pageReference.getName() + "Sheet", reference.getLastSpaceReference());

        setup.rest().savePage(reference, "content", "title");

        StringBuilder source = new StringBuilder();
        source.append("sheet content\n");
        source.append("\n");
        source.append("{{velocity}}\n");
        source.append("$doc.plainTitle\n");
        source.append("$xwiki.getDocument('" + setup.serializeReference(pageReference) + "').plainTitle\n");
        source.append("{{/velocity}}");
        setup.rest().savePage(sheetReference, source.toString(), "sheet title");

        StringBuilder result = new StringBuilder();
        result.append("sheet content\n");
        result.append("\n");
        result.append("sheet title\n");
        result.append("title");
        assertEquals(result.toString(), setup.executeAndGetBodyAsString(reference,
            Map.of("sheet", setup.serializeReference(sheetReference), "outputSyntax", "plain")));
    }
}
