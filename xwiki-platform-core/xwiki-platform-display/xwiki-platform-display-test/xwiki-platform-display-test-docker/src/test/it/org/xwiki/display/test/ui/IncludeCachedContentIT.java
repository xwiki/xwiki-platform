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
package org.xwiki.display.test.ui;

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
 * Verify that the content of a document whose rendering result can be cached is executed in the current context when
 * it's included in the current context.
 *
 * @version $Id$
 */
@UITest
class IncludeCachedContentIT
{
    @BeforeAll
    void beforeAll(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void includeCachedContentInCurrentContext(TestUtils setup, TestReference reference) throws Exception
    {
        DocumentReference includedReference = new DocumentReference("Included", reference.getLastSpaceReference());
        setup.rest().savePage(includedReference, "Value: {{velocity}}$!request.value{{/velocity}}", "");
        setup.rest().addObject(includedReference, "XWiki.DocumentAsyncClass", "async_cached", 1);

        DocumentReference includingReference = new DocumentReference("Including", reference.getLastSpaceReference());
        setup.rest().savePage(includingReference,
            String.format("{{include reference=\"%s\"/}}", setup.serializeReference(includedReference)), "");

        String path = String.format("bin/get/%s/%s/Including",
            reference.getLastSpaceReference().getParent().getName(), reference.getLastSpaceReference().getName());
        assertEquals("Value: one", getContent(setup, path, "one"));
        assertEquals("Value: two", getContent(setup, path, "two"));
    }

    private String getContent(TestUtils setup, String path, String value) throws Exception
    {
        return setup.getString(path, Map.of("value", value, "outputSyntax", "plain")).trim();
    }
}
