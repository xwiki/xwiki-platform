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
package org.xwiki.rest.test;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageTranslationHistoryResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotEquals;

public class PageTranslationResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private String space;

    private List<String> spaces;

    private String pageName;

    private DocumentReference referenceDefault;

    private DocumentReference referenceFR;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.wikiName = getWiki();
        this.space = getTestClassName();
        this.spaces = Arrays.asList(this.space);
        this.pageName = getTestMethodName();
        this.referenceDefault = new DocumentReference(this.wikiName, this.spaces, this.pageName);
        this.referenceFR = new DocumentReference(this.wikiName, this.spaces, this.pageName, Locale.FRENCH);

        // Clean
        this.testUtils.rest().delete(this.referenceFR);
        this.testUtils.rest().delete(this.referenceDefault);
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
    }

    @Test
    public void testGETNotExistingPage() throws Exception
    {
        CloseableHttpResponse response = executeGet(buildURI(PageTranslationResource.class, getWiki(),
            Arrays.asList("NOTEXISTING"), "NOTEXISTING", Locale.FRENCH));
        assertEquals(getHttpResponseInfo(response), HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testPUTGETDELETETranslation() throws Exception
    {
        Page newPage = this.objectFactory.createPage();
        newPage.setTitle("fr titre");
        newPage.setContent("fr contenue");
        newPage.setLanguage(Locale.FRENCH.toString());

        assertFalse(this.testUtils.rest().exists(this.referenceDefault));

        String uri = buildURI(PageTranslationResource.class, getWiki(), this.spaces, this.pageName, Locale.FRENCH);

        // PUT
        CloseableHttpResponse response = executePutXml(uri, newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(getHttpResponseInfo(response), HttpStatus.SC_CREATED, response.getCode());
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        assertTrue(this.testUtils.rest().exists(this.referenceFR));
        assertFalse(this.testUtils.rest().exists(this.referenceDefault));

        // GET
        response = executeGet(uri);
        assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());
        modifiedPage = (Page) this.unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        // DELETE
        response = executeDelete(uri, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(getHttpResponseInfo(response), HttpStatus.SC_NO_CONTENT, response.getCode());

        assertFalse(this.testUtils.rest().exists(this.referenceDefault));
        assertFalse(this.testUtils.rest().exists(this.referenceFR));
    }

    @Test
    public void testPageTranslationHistoryResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create a page and a translation with several versions
        Locale language = Locale.FRENCH;
        assertFalse(this.testUtils.rest().exists(this.referenceDefault));
        String uri = buildURI(PageTranslationResource.class, getWiki(), this.space, this.pageName, language);

        try {
            // Save translation versions using PUT to the translation resource
            Page translationPage = this.objectFactory.createPage();
            translationPage.setContent("v1fr");
            translationPage.setTitle("title1fr");
            executePutXml(uri, translationPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

            translationPage.setContent("v2fr");
            translationPage.setTitle("title2fr");
            executePutXml(uri, translationPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

            translationPage.setContent("v3fr");
            translationPage.setTitle("title3fr");
            executePutXml(uri, translationPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet("%s?number=-1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(400, response.getCode());
            assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet("%s?number=1001".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(400, response.getCode());
            assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet("%s?number=1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(HttpStatus.SC_OK, response.getCode());
            History history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());
            assertEquals(1, history.getHistorySummaries().size());

            String firstVersion = history.getHistorySummaries().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            response = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(HttpStatus.SC_OK, response.getCode());
            history = (History) this.unmarshaller.unmarshal(response.getEntity().getContent());
            assertEquals(1, history.getHistorySummaries().size());
            assertNotEquals(firstVersion, history.getHistorySummaries().get(0).getVersion());
        } finally {
            // Cleanup: Delete the page.
            executeDelete(uri, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        }
    }
}
