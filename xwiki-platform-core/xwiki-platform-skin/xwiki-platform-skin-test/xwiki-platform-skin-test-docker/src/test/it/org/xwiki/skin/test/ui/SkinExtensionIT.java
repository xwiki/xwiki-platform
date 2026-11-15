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
package org.xwiki.skin.test.ui;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Verify the scopes in which a JavaScript Skin Extension is loaded, depending on its {@code use} property.
 *
 * @version $Id$
 */
@UITest
class SkinExtensionIT
{
    private static final String JSX_CLASS = "XWiki.JavaScriptExtension";

    private static final String USE = "use";

    private static final String MARKER = "JSX active";

    /**
     * Adds the marker to the page content, so that the test can see whether the extension was loaded. Note that the
     * extension is deferred by default, so it runs once the page content is parsed.
     */
    private static final String JSX_CODE =
        "document.getElementById('xwikicontent').append('%s');".formatted(MARKER);

    @Test
    @Order(1)
    void javaScriptExtensionUse(TestUtils setup, TestReference testReference)
    {
        // The "always" scope requires the page holding the extension to be saved with wiki admin rights.
        setup.loginAsSuperAdmin();

        DocumentReference otherPage = new DocumentReference("OtherPage", testReference.getLastSpaceReference());
        setup.createPage(otherPage, "other page content");
        setup.createPage(testReference, "extension page content");
        setup.addObject(testReference, JSX_CLASS, "code", JSX_CODE, USE, "always");

        // An extension used always is loaded on every page.
        assertThat(setup.gotoPage(testReference).getContent(), containsString(MARKER));
        assertThat(setup.gotoPage(otherPage).getContent(), containsString(MARKER));

        // An extension used on the current page is loaded only on the page holding it.
        setup.updateObject(testReference, JSX_CLASS, 0, USE, "currentPage");
        assertThat(setup.gotoPage(testReference).getContent(), containsString(MARKER));
        assertThat(setup.gotoPage(otherPage).getContent(), not(containsString(MARKER)));

        // An extension used on demand is loaded only when requested, even on the page holding it.
        setup.updateObject(testReference, JSX_CLASS, 0, USE, "onDemand");
        assertThat(setup.gotoPage(testReference).getContent(), not(containsString(MARKER)));
        assertThat(setup.gotoPage(otherPage).getContent(), not(containsString(MARKER)));
    }
}
