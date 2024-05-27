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

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.test.SolrTestUtils;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @version $Id$
 * @since 14.8RC1
 */
@UITest
class BacklinksIT
{
    @Test
    void backlinks(TestUtils setup, TestReference reference, TestConfiguration testConfiguration) throws Exception
    {
        setup.loginAsSuperAdmin();
        DocumentReference targetDocumentReference =
            new DocumentReference("BacklinkTargetTest", reference.getLastSpaceReference());
        DocumentReference sourceDocumentReference1 =
            new DocumentReference("BacklinkSourceTest1", reference.getLastSpaceReference());
        DocumentReference sourceDocumentReference2 =
            new DocumentReference("BacklinkSourceTest2", reference.getLastSpaceReference());

        setup.rest().delete(targetDocumentReference);
        setup.rest().delete(sourceDocumentReference1);
        setup.rest().delete(sourceDocumentReference2);

        // Create page listing backlinks leading to it.
        ViewPage vp = setup.createPage(targetDocumentReference,
            "{{velocity}}#foreach ($link in $doc.getBacklinks())\n$link\n#end{{/velocity}}", null);
        // No backlinks at this stage
        assertEquals("", vp.getContent());

        // Create a source page with a local link to the target page
        setup.createPage(sourceDocumentReference1,
            String.format("[[page:%s]][[%s]]",
                setup.serializeReference(reference.getLastSpaceReference()),
                setup.serializeReference(targetDocumentReference.getLocalDocumentReference())),
            null);
        // Create a source page with a link to the target page but on a different wiki
        setup.createPage(sourceDocumentReference2,
            String.format("[[page:otherwiki:%s]][[otherwiki:%s]]",
                setup.serializeReference(targetDocumentReference.getLocalDocumentReference()),
                setup.serializeReference(targetDocumentReference.getLocalDocumentReference())),
            null);

        // Wait for the solr indexing to be completed before checking the backlinks of the target.
        new SolrTestUtils(setup, testConfiguration.getServletEngine()).waitEmptyQueue();

        vp = setup.gotoPage(targetDocumentReference);

        assertEquals(setup.serializeReference(sourceDocumentReference1.getLocalDocumentReference()), vp.getContent());
    }
}
