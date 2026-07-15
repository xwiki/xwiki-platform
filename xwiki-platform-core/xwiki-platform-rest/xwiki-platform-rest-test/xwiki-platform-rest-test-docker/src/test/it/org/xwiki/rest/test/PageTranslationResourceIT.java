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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageTranslationHistoryResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PageTranslationResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private String space;

    private List<String> spaces;

    private String pageName;

    private DocumentReference referenceDefault;

    private DocumentReference referenceFR;

    @BeforeEach
    @Override
    protected void setUp(TestUtils setup, TestInfo info) throws Exception
    {
        super.setUp(setup, info);

        this.wikiName = getWiki();
        this.space = getTestClassName();
        this.spaces = Arrays.asList(this.space);
        this.pageName = getTestMethodName();
        this.referenceDefault = new DocumentReference(this.wikiName, this.spaces, this.pageName);
        this.referenceFR = new DocumentReference(this.wikiName, this.spaces, this.pageName, Locale.FRENCH);

        // Clean
        getUtil().rest().delete(this.referenceFR);
        getUtil().rest().delete(this.referenceDefault);
    }

    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
    }

    @Test
    void testGETNotExistingPage() throws Exception
    {
        GetMethod getMethod = executeGet(buildURI(PageTranslationResource.class, getWiki(),
            Arrays.asList("NOTEXISTING"), "NOTEXISTING", Locale.FRENCH).toString());
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testPUTGETDELETETranslation() throws Exception
    {
        Page newPage = this.objectFactory.createPage();
        newPage.setTitle("fr titre");
        newPage.setContent("fr contenue");
        newPage.setLanguage(Locale.FRENCH.toString());

        assertFalse(getUtil().rest().exists(this.referenceDefault));

        String uri =
            buildURI(PageTranslationResource.class, getWiki(), this.spaces, this.pageName, Locale.FRENCH).toString();

        // PUT
        PutMethod putMethod = executePutXml(uri, newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        assertTrue(getUtil().rest().exists(this.referenceFR));
        assertFalse(getUtil().rest().exists(this.referenceDefault));

        // GET
        GetMethod getMethod = executeGet(uri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        // DELETE
        DeleteMethod deleteMethod = executeDelete(uri, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode(), getHttpMethodInfo(deleteMethod));

        assertFalse(getUtil().rest().exists(this.referenceDefault));
        assertFalse(getUtil().rest().exists(this.referenceFR));
    }

    @Test
    void testPageTranslationHistoryResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create a page and a translation with several versions
        Locale language = Locale.FRENCH;
        assertFalse(getUtil().rest().exists(this.referenceDefault));
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
            GetMethod getMethod = executeGet("%s?number=-1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: number=1001 should return error
            getMethod = executeGet("%s?number=1001".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());

            // Test: pagination with number=1
            getMethod = executeGet("%s?number=1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            History history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, history.getHistorySummaries().size());

            String firstVersion = history.getHistorySummaries().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet("%s?number=1&start=1".formatted(
                buildURI(PageTranslationHistoryResource.class, getWiki(), this.space, this.pageName, language)));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            history = (History) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, history.getHistorySummaries().size());
            assertNotEquals(firstVersion, history.getHistorySummaries().get(0).getVersion());
        } finally {
            // Cleanup: Delete the page.
            executeDelete(uri, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        }
    }
}
