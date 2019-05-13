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
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.pages.PageTranslationResource;
import org.xwiki.rest.test.framework.AbstractHttpTest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageTranslationResourceTest extends AbstractHttpTest
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
        GetMethod getMethod = executeGet(buildURI(PageTranslationResource.class, getWiki(),
            Arrays.asList("NOTEXISTING"), "NOTEXISTING", Locale.FRENCH).toString());
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());
    }

    @Test
    public void testPUTGETDELETETranslation() throws Exception
    {
        Page newPage = this.objectFactory.createPage();
        newPage.setTitle("fr titre");
        newPage.setContent("fr contenue");
        newPage.setLanguage(Locale.FRENCH.toString());

        assertFalse(this.testUtils.rest().exists(this.referenceDefault));

        String uri =
            buildURI(PageTranslationResource.class, getWiki(), this.spaces, this.pageName, Locale.FRENCH).toString();

        // PUT
        PutMethod putMethod = executePutXml(uri, newPage, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_CREATED, putMethod.getStatusCode());
        Page modifiedPage = (Page) this.unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        assertTrue(this.testUtils.rest().exists(this.referenceFR));
        assertFalse(this.testUtils.rest().exists(this.referenceDefault));

        // GET
        GetMethod getMethod = executeGet(uri);
        assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
        modifiedPage = (Page) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals("fr titre", modifiedPage.getTitle());
        assertEquals("fr contenue", modifiedPage.getContent());
        assertEquals(Locale.FRENCH.toString(), modifiedPage.getLanguage());

        // DELETE
        DeleteMethod deleteMethod = executeDelete(uri, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(getHttpMethodInfo(deleteMethod), HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode());

        assertFalse(this.testUtils.rest().exists(this.referenceDefault));
        assertFalse(this.testUtils.rest().exists(this.referenceFR));
    }
}
