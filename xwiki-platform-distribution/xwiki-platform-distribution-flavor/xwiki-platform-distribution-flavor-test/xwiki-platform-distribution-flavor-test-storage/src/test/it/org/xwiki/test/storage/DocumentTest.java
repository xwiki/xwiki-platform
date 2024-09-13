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
package org.xwiki.test.storage;

import java.util.HashMap;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.storage.framework.AbstractTest;
import org.xwiki.test.storage.framework.StoreTestUtils;

/**
 * Test saving and downloading of attachments.
 *
 * @version $Id$
 * @since 3.2M1
 */
public class DocumentTest extends AbstractTest
{
    private String spaceName;
    private String pageName;

    @Before
    public void setUp()
    {
        this.spaceName = this.getClass().getSimpleName();
        this.pageName = this.getTestMethodName();
    }

    @Test
    public void testRollback() throws Exception
    {
        final String spaceName = "DocumentTest";
        final String pageName = "testRollback";

        final String versionOne = "This is version one";
        final String versionTwo = "This is version two";

        final String pageURL = this.getAddressPrefix() + "get/" + spaceName + "/" + pageName + "?xpage=plain";

        // Delete the document if it exists.
        doPostAsAdmin(spaceName, pageName, null, "delete", "confirm=1", null);

        // Create a document. v1.1
        doPostAsAdmin(spaceName, pageName, null, "save", null,
            new HashMap<String, String>() {{
                put("content", versionOne);
            }});

        // Change the document v2.1
        doPostAsAdmin(spaceName, pageName, null, "save", null,
            new HashMap<String, String>() {{
                put("content", versionTwo);
            }});

        // Make sure it's version 2.
        Assert.assertEquals("<p>" + versionTwo + "</p>", StoreTestUtils.getPageAsString(pageURL));

        // Do a rollback. v3.1
        doPostAsAdmin(spaceName, pageName, null, "rollback", "rev=1.1&confirm=1", null);

        // Make sure it's the same as version 1.
        Assert.assertEquals("<p>" + versionOne + "</p>", StoreTestUtils.getPageAsString(pageURL));

        // Make sure the latest current version is actually v3.1
        HttpMethod ret =
            doPostAsAdmin(spaceName, pageName, null, "preview", "xpage=plain",
                new HashMap<String, String>() {{
                    put("content", "{{velocity}}$doc.getVersion(){{/velocity}}");
                }});
        Assert.assertEquals("<p>3.1</p>", new String(ret.getResponseBody(), "UTF-8"));
    }

    /**
     * check that https://jira.xwiki.org/browse/XWIKI-7943 has not regressed.
     * Jetty prevents saving of large documents unless you specify max form size on the command line.
     *
     * @since 4.1.1
     * @since 4.0.1
     */
    @Test
    public void testSaveOfThreeHundredKilobyteDocument() throws Exception
    {
        final String content = secure().nextAlphanumeric(300000);
        final HttpMethod ret =
            this.doPostAsAdmin(this.spaceName, this.pageName, null, "save", null,
                new HashMap<String, String>() {{
                    put("content", content);
                }});
        // save forwards the user to view, if it's too big, jetty gives you some error response code (400+)
        Assert.assertEquals(302, ret.getStatusCode());
    }

    @Test
    public void testDocumentIsSameAfterSaving() throws Exception
    {
        final String content =
              "{{groovy}}\n"
            + "def content = 'test content'\n"
            + "doc.setContent(content);\n"
            + "doc.saveAsAuthor();\n"
            + "println(doc.getContent());\n"
            + "{{/groovy}}";

        // Delete the document if it exists.
        doPostAsAdmin(this.spaceName, this.pageName, null, "delete", "confirm=1", null);

        // Create a document.
        doPostAsAdmin(this.spaceName, this.pageName, null, "save", null,
            new HashMap<String, String>(){{
                put("content", content);
            }});

        final String url =
            this.getAddressPrefix() + "view/" + this.spaceName + "/" + this.pageName + "?xpage=plain";

        Assert.assertEquals("<p>test content</p>", StoreTestUtils.getPageAsString(url));
    }

    @Test
    public void testOtherDocumentIsSameAfterSaving() throws Exception
    {
        final String content =
              "{{groovy}}\n"
            + "def content = 'test content'\n"
            + "otherDoc = xwiki.getDocument('" + this.spaceName + "','" + this.pageName + "x')\n"
            + "otherDoc.setContent(content);\n"
            + "otherDoc.saveAsAuthor();\n"
            + "println(otherDoc.getContent());\n"
            + "{{/groovy}}";

        // Delete the document if it exists.
        doPostAsAdmin(this.spaceName, this.pageName, null, "delete", "confirm=1", null);

        // Create a document.
        doPostAsAdmin(this.spaceName, this.pageName, null, "save", null,
            new HashMap<String, String>(){{
                put("content", content);
            }});

        final String url =
            this.getAddressPrefix() + "view/" + this.spaceName + "/" + this.pageName + "?xpage=plain";

        Assert.assertEquals("<p>test content</p>", StoreTestUtils.getPageAsString(url));
    }
}
