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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * Integration test to verify that the form token is correctly injected in JavaScript requests.
 *
 * @version $Id$
 */
@UITest
class FormTokenInjectionIT
{
    @Test
    @Order(1)
    void simpleRESTPost(TestUtils setup, TestReference reference) throws Exception
    {
        setup.loginAsSuperAdmin();
        setup.deletePage(reference);

        String content = "{{html clean=\"false\"}}"
            + "<div id='results'></div>"
            + "<script>"
            + IOUtils.toString(
            Objects.requireNonNull(getClass().getResourceAsStream("/FormTokenInjectionIT/testCode.js")),
            StandardCharsets.UTF_8)
            + "</script>"
            + "{{/html}}";
        ViewPage viewPage = setup.createPage(reference, content);

        List<String> expectedMessages = List.of(
            "Simple POST: 201",
            "Only Request: 201",
            "Request with init: 201",
            "Simple with array headers: 201",
            "Request with init body",
            "Request Body",
            "Simple with array headers body"
        );

        // Wait until the page content contains the last message to ensure the script has been fully executed.
        setup.getDriver().waitUntilCondition(
            driver -> viewPage.getContent().contains(expectedMessages.get(expectedMessages.size() - 1))
        );

        String pageContent = viewPage.getContent();

        assertAll(expectedMessages.stream().map(expected -> (() -> assertThat(pageContent, containsString(expected)))));
    }
}
