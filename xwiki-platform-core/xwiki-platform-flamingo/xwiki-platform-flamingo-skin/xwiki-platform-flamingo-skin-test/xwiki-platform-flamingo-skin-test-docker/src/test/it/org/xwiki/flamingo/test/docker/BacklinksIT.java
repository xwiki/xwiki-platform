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
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.integration.XWikiExecutor;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.xwiki.rendering.syntax.Syntax.XWIKI_2_0;

/**
 * @version $Id$
 * @since 14.8RC1
 */
@UITest
class BacklinksIT
{
    @Test
    void backlinksCreationSyntax20(TestUtils setup, TestReference reference, TestConfiguration testConfiguration)
        throws Exception
    {
        setup.loginAsSuperAdmin();
        DocumentReference targetDocumentReference =
            new DocumentReference("BacklinkTargetTest", reference.getLastSpaceReference());
        DocumentReference sourceDocumentReference =
            new DocumentReference("BacklinkSourceTest", reference.getLastSpaceReference());

        setup.rest().delete(targetDocumentReference);
        setup.rest().delete(sourceDocumentReference);

        // Create page listing backlinks leading to it.
        ViewPage vp = setup.createPage(targetDocumentReference,
            "{{velocity}}#foreach ($link in $doc.getBacklinks())\n$link\n#end{{/velocity}}", null,
            XWIKI_2_0.toIdString());
        // No backlinks at this stage
        assertEquals("", vp.getContent());

        // Create page pointing to the page listing the backlinks.
        setup.createPage(sourceDocumentReference,
            String.format("[[backlink>>%s]]", setup.serializeReference(targetDocumentReference)), null,
            XWIKI_2_0.toIdString());

        // Wait for the solr indexing to be completed before checking the backlinks of the target.
        new SolrTestUtils(setup, computedHostURL(testConfiguration)).waitEmpyQueue();

        vp = setup.gotoPage(targetDocumentReference);
        assertEquals(setup.serializeReference(sourceDocumentReference.getLocalDocumentReference()), vp.getContent());
    }

    private String computedHostURL(TestConfiguration testConfiguration)
    {
        ServletEngine servletEngine = testConfiguration.getServletEngine();
        return String.format("http://%s:%d%s", servletEngine.getIP(), servletEngine.getPort(),
            XWikiExecutor.DEFAULT_CONTEXT);
    }
}
