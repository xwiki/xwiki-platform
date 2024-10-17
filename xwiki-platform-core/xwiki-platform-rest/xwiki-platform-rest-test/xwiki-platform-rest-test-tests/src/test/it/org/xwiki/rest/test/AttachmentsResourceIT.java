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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.attachments.AttachmentHistoryResource;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.attachments.AttachmentsAtPageVersionResource;
import org.xwiki.rest.resources.attachments.AttachmentsResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;

public class AttachmentsResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.wikiName = getWiki();
        this.spaces = Arrays.asList(getTestClassName());
        this.pageName = getTestMethodName();

        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        // Create a clean test page.
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        /* Everything is done in test methods. */
    }

    @Test
    public void testPUTGETAttachments() throws Exception
    {
        /* Test normal random UUID method */
        String randomStr = String.format("%s.txt", UUID.randomUUID());
        /* Test filenames requiring url encoding */
        putAttachmentFilename(randomStr);
        putAttachmentFilename("my attach.txt");
        putAttachmentFilename("^caret.txt");
        putAttachmentFilename("#pound.txt");
        putAttachmentFilename("%percent.txt");
        putAttachmentFilename("{brace}.txt");
        putAttachmentFilename("[bracket].txt");
        /** Causes XWIKI-7874 **/
        putAttachmentFilename("plus+plus.txt");

        // Now get all the attachments.
        String attachmentsUri = buildURIForThisPage(AttachmentsResource.class);
        CloseableHttpResponse response = executeGet(attachmentsUri);
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
        assertEquals(8, attachments.getAttachments().size());

        // Clean the wiki for further tests: WikisResourceTest use a list of attachments and might fail
        // if we don't clean here.
        DocumentReference documentReference = new DocumentReference(this.wikiName, this.spaces, this.pageName);
        this.testUtils.deleteAttachement(documentReference, "my attach.txt");
        this.testUtils.deleteAttachement(documentReference, "^caret.txt");
        this.testUtils.deleteAttachement(documentReference, "#pound.txt");
        this.testUtils.deleteAttachement(documentReference, "%percent.txt");
        this.testUtils.deleteAttachement(documentReference, "{brace}.txt");
        this.testUtils.deleteAttachement(documentReference, "[bracket].txt");
        this.testUtils.deleteAttachement(documentReference, "plus+plus.txt");
    }

    protected void putAttachmentFilename(String attachmentName) throws Exception
    {
        String content = "ATTACHMENT CONTENT";
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        CloseableHttpResponse response = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getCode());

        response = executeGet(attachmentURI);
        String body = EntityUtils.toString(response.getEntity());
        assertEquals(body, HttpStatus.SC_OK, response.getCode());

        assertEquals(content, body);
    }

    @Test
    public void testPUTAttachmentNoRights() throws Exception
    {
        String attachmentName = String.format("%s.txt", UUID.randomUUID());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse response = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, putMethod.getCode());
    }

    @Test
    public void testDELETEAttachment() throws Exception
    {
        String attachmentName = String.format("%d.txt", System.currentTimeMillis());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);
        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse response = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, response.getCode());

        response = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_OK, response.getCode());

        response = executeDelete(attachmentURI, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getCode());

        response = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testDELETEAttachmentNoRights() throws Exception
    {
        String attachmentName = String.format("%d.txt", System.currentTimeMillis());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse response = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, response.getCode());

        response = executeDelete(attachmentURI);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());

        response = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_OK, response.getCode());
    }

    @Test
    public void testGETAttachmentsAtPageVersion() throws Exception
    {
        final int NUMBER_OF_ATTACHMENTS = 4;
        String[] attachmentNames = new String[NUMBER_OF_ATTACHMENTS];
        String[] pageVersions = new String[NUMBER_OF_ATTACHMENTS];

        for (int i = 0; i < NUMBER_OF_ATTACHMENTS; i++) {
            attachmentNames[i] = String.format("%s.txt", UUID.randomUUID());
        }

        String content = "ATTACHMENT CONTENT";

        // Create NUMBER_OF_ATTACHMENTS attachments
        for (int i = 0; i < NUMBER_OF_ATTACHMENTS; i++) {
            String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentNames[i]);

            try (CloseableHttpResponse response = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword())) {
                assertEquals(HttpStatus.SC_CREATED, response.getCode());

                Attachment attachment = (Attachment) this.unmarshaller.unmarshal(response.getEntity().getContent());
                pageVersions[i] = attachment.getPageVersion();
            }
        }

        // For each page version generated, check that the attachments that are supposed to be there are actually there.
        // We do the following: at pageVersion[i] we check that all attachmentNames[0..i] are there.
        for (int i = 0; i < NUMBER_OF_ATTACHMENTS; i++) {
            String attachmentsUri = buildURIForThisPage(AttachmentsAtPageVersionResource.class, pageVersions[i]);
            CloseableHttpResponse response = executeGet(attachmentsUri);
            assertEquals(HttpStatus.SC_OK, response.getCode());

            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());

            // Check that all attachmentNames[0..i] are present in the list of attachments of page at version
            // pageVersions[i]
            for (int j = 0; j <= i; j++) {
                boolean found = false;
                for (Attachment attachment : attachments.getAttachments()) {
                    if (attachment.getName().equals(attachmentNames[j])) {
                        if (attachment.getPageVersion().equals(pageVersions[i])) {
                            found = true;
                            break;
                        }
                    }
                }
                Assert.assertTrue(String.format("%s is not present in attachments list of the page at version %s",
                    attachmentNames[j], pageVersions[i]), found);
            }

            /* Check links */
            for (Attachment attachment : attachments.getAttachments()) {
                checkLinks(attachment);
            }
        }
    }

    @Test
    public void testGETAttachmentVersions() throws Exception
    {
        final int NUMBER_OF_VERSIONS = 4;
        String attachmentName = String.format("%s.txt", UUID.randomUUID());

        Map<String, String> versionToContentMap = new HashMap<>();

        // Create NUMBER_OF_ATTACHMENTS attachments
        for (int i = 0; i < NUMBER_OF_VERSIONS; i++) {
            String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);
            String content = String.format("CONTENT %d", i);
            CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            if (i == 0) {
                assertEquals(HttpStatus.SC_CREATED, putMethod.getCode());
            } else {
                assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode());
            }

            Attachment attachment = (Attachment) this.unmarshaller.unmarshal(putMethod.getEntity().getContent());

            versionToContentMap.put(attachment.getVersion(), content);
        }

        String attachmentsUri = buildURIForThisPage(AttachmentHistoryResource.class, attachmentName);
        CloseableHttpResponse response = executeGet(attachmentsUri);
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
        assertEquals(NUMBER_OF_VERSIONS, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            response = executeGet(getFirstLinkByRelation(attachment, Relations.ATTACHMENT_DATA).getHref());
            assertEquals(HttpStatus.SC_OK, response.getCode());

            assertEquals(versionToContentMap.get(attachment.getVersion()), EntityUtils.toString(response.getEntity()));
        }
    }

    @Test
    public void testPOSTAttachment() throws Exception
    {
        final String attachmentName = String.format("%s.txt", UUID.randomUUID());
        final String content = "ATTACHMENT CONTENT";

        String attachmentsUri = buildURIForThisPage(AttachmentsResource.class, attachmentName);

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addBinaryBody(attachmentName, content.getBytes());
        entityBuilder.addTextBody("XWiki-Form-Token", getFormToken(TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword()));

        HttpPost postMethod = new HttpPost(attachmentsUri);
        postMethod.setEntity(entityBuilder.build());
        postMethod.addHeader("Accept", MediaType.APPLICATION_XML);
        postMethod.addHeader("XWiki-Form-Token", getFormToken(TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword()));

        CloseableHttpResponse response = execute(postMethod, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_CREATED, response.getCode());

        this.unmarshaller.unmarshal(response.getEntity().getContent());

        Header location = response.getHeader("location");

        response = executeGet(location.getValue());
        Assert.assertEquals(getHttpResponseInfo(response), HttpStatus.SC_OK, response.getCode());

        Assert.assertEquals(content, EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testAttachmentsResourcePaginationAndErrors() throws Exception
    {
        // Setup: Add two attachments
        String attachmentName1 = "att1.txt";
        String attachmentName2 = "att2.txt";
        try {
            putAttachmentFilename(attachmentName1);
            putAttachmentFilename(attachmentName2);

            String attachmentsUri = buildURIForThisPage(AttachmentsResource.class);

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(attachmentsUri + "?number=-1");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet(attachmentsUri + "?number=1001");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet(attachmentsUri + "?number=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());

            String firstName = attachments.getAttachments().get(0).getName();

            // Test: pagination with number=1 and start=1
            response = executeGet(attachmentsUri + "?number=1&start=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());
            Assert.assertNotEquals(firstName, attachments.getAttachments().get(0).getName());
        } finally {
            // Clean up
            this.testUtils.deletePage(this.reference);
        }
    }

    @Test
    public void testAttachmentHistoryResourcePaginationAndErrors() throws Exception
    {
        try {
            // Setup: Create an attachment with multiple versions
            String attachmentName = "history.txt";
            int versionCount = 3;
            for (int i = 0; i < versionCount; i++) {
                String content = "Content version " + i;
                CloseableHttpResponse response =
                    executePut(buildURIForThisPage(AttachmentResource.class, attachmentName), content,
                        MediaType.TEXT_PLAIN,
                        TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                        TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
                if (i == 0) {
                    Assert.assertEquals(HttpStatus.SC_CREATED, response.getCode());
                } else {
                    Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());
                }
            }

            String historyUri = buildURIForThisPage(AttachmentHistoryResource.class, attachmentName);

            // Test: number=-1 should return error
            CloseableHttpResponse response = executeGet(historyUri + "?number=-1");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: number=1001 should return error
            response = executeGet(historyUri + "?number=1001");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

            // Test: pagination with number=1
            response = executeGet(historyUri + "?number=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());

            String firstVersion = attachments.getAttachments().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            response = executeGet(historyUri + "?number=1&start=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, attachments.getAttachments().size());
            Assert.assertNotEquals(firstVersion, attachments.getAttachments().get(0).getVersion());
        } finally {
            // Clean up
            this.testUtils.deletePage(this.reference);
        }
    }

    /**
     * Creates a URI to access the specified resource with the given path elements. The wiki, space and page path
     * elements are added by this method so you can skip them.
     * 
     * @param resource the resource that needs to be accessed
     * @param args the path elements
     * @return an URI to access the specified resource with the given path elements
     * @throws Exception if encoding the path elements fails
     */
    protected String buildURIForThisPage(Class<?> resource, Object... args) throws Exception
    {
        List<Object> pathElements = new ArrayList<>();
        pathElements.add(this.wikiName);
        pathElements.add(this.spaces);
        pathElements.add(this.pageName);
        pathElements.addAll(Arrays.asList(args));

        return super.buildURI(resource, pathElements.toArray());
    }
}
