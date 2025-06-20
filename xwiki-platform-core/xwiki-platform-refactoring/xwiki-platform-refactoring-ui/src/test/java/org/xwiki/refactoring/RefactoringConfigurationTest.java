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
package org.xwiki.refactoring;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;

import com.xpn.xwiki.internal.store.StoreConfiguration;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

/**
 * Unit tests for testing the {@code Refactoring.Code.RefactoringConfiguration} wiki page.
 *
 * @version $Id$
 * @since 13.2RC1
 * @since 12.10.6
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
class RefactoringConfigurationTest extends PageTest
{
    private static final DocumentReference REFACTORING_CONFIGURATION_REFERENCE =
        new DocumentReference("xwiki", asList("Refactoring", "Code"), "RefactoringConfiguration");

    @MockComponent
    private StoreConfiguration storeConfiguration;

    @Test
    void verifyFormXRedirectField() throws Exception
    {
        setOutputSyntax(Syntax.HTML_5_0);

        // Activates the recyclebin feature, allowing the tested form to be displayed.
        when(this.storeConfiguration.isRecycleBinEnabled()).thenReturn(true);

        // Checks that the xredirect URL is relative.
        assertThat(renderHTMLPage(REFACTORING_CONFIGURATION_REFERENCE).getElementsByAttributeValue("name", "xredirect")
            .eachAttr("value"), contains("/xwiki/bin/Main/WebHome"));
    }
}
