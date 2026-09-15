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
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentsResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    @BeforeEach
    @Override
    protected void setUp(TestUtils setup, TestInfo info) throws Exception
    {
        super.setUp(setup, info);

        this.wikiName = getWiki();
        this.spaces = Arrays.asList(getTestClassName());
        this.pageName = getTestMethodName();

        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        // Create a clean test page.
        getUtil().rest().delete(this.reference);
        getUtil().rest().savePage(this.reference);
    }

    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
        /* Everything is done in test methods. */
    }

    @Test
    void testPUTGETAttachments() throws Exception
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
        CloseableHttpResponse getMethod = executeGet(attachmentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
        assertEquals(8, attachments.getAttachments().size());

        // Clean the wiki for further tests: WikisResourceTest use a list of attachments and might fail
        // if we don't clean here.
        DocumentReference documentReference = new DocumentReference(this.wikiName, this.spaces, this.pageName);
        getUtil().deleteAttachement(documentReference, "my attach.txt");
        getUtil().deleteAttachement(documentReference, "^caret.txt");
        getUtil().deleteAttachement(documentReference, "#pound.txt");
        getUtil().deleteAttachement(documentReference, "%percent.txt");
        getUtil().deleteAttachement(documentReference, "{brace}.txt");
        getUtil().deleteAttachement(documentReference, "[bracket].txt");
        getUtil().deleteAttachement(documentReference, "plus+plus.txt");
    }

    protected void putAttachmentFilename(String attachmentName) throws Exception
    {
        String content = "ATTACHMENT CONTENT";
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        CloseableHttpResponse getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getCode(), getHttpResponseInfo(getMethod));

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        assertEquals(content, EntityUtils.toString(getMethod.getEntity()));
    }

    @Test
    void testPUTAttachmentNoRights() throws Exception
    {
        String attachmentName = String.format("%s.txt", UUID.randomUUID());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getCode(), getHttpResponseInfo(getMethod));

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, putMethod.getCode(), getHttpResponseInfo(putMethod));
    }

    @Test
    void testDELETEAttachment() throws Exception
    {
        String attachmentName = String.format("%d.txt", System.currentTimeMillis());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);
        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        CloseableHttpResponse getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        CloseableHttpResponse deleteMethod =
            executeDelete(attachmentURI, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteMethod.getCode(), getHttpResponseInfo(deleteMethod));

        getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getCode(), getHttpResponseInfo(getMethod));
    }

    @Test
    void testDELETEAttachmentNoRights() throws Exception
    {
        String attachmentName = String.format("%d.txt", System.currentTimeMillis());
        String attachmentURI = buildURIForThisPage(AttachmentResource.class, attachmentName);

        String content = "ATTACHMENT CONTENT";

        CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        CloseableHttpResponse deleteMethod = executeDelete(attachmentURI);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, deleteMethod.getCode(), getHttpResponseInfo(deleteMethod));

        CloseableHttpResponse getMethod = executeGet(attachmentURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));
    }

    @Test
    void testGETAttachmentsAtPageVersion() throws Exception
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

            CloseableHttpResponse putMethod = executePut(attachmentURI, content, MediaType.TEXT_PLAIN,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));

            Attachment attachment = (Attachment) this.unmarshaller.unmarshal(putMethod.getEntity().getContent());
            pageVersions[i] = attachment.getPageVersion();
        }

        // For each page version generated, check that the attachments that are supposed to be there are actually there.
        // We do the following: at pageVersion[i] we check that all attachmentNames[0..i] are there.
        for (int i = 0; i < NUMBER_OF_ATTACHMENTS; i++) {
            String attachmentsUri = buildURIForThisPage(AttachmentsAtPageVersionResource.class, pageVersions[i]);
            CloseableHttpResponse getMethod = executeGet(attachmentsUri);
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());

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
                assertTrue(found, String.format("%s is not present in attachments list of the page at version %s",
                    attachmentNames[j], pageVersions[i]));
            }

            /* Check links */
            for (Attachment attachment : attachments.getAttachments()) {
                checkLinks(attachment);
            }
        }
    }

    @Test
    void testGETAttachmentVersions() throws Exception
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
                assertEquals(HttpStatus.SC_CREATED, putMethod.getCode(), getHttpResponseInfo(putMethod));
            } else {
                assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode(), getHttpResponseInfo(putMethod));
            }

            Attachment attachment = (Attachment) this.unmarshaller.unmarshal(putMethod.getEntity().getContent());

            versionToContentMap.put(attachment.getVersion(), content);
        }

        String attachmentsUri = buildURIForThisPage(AttachmentHistoryResource.class, attachmentName);
        CloseableHttpResponse getMethod = executeGet(attachmentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
        assertEquals(NUMBER_OF_VERSIONS, attachments.getAttachments().size());

        for (Attachment attachment : attachments.getAttachments()) {
            getMethod = executeGet(getFirstLinkByRelation(attachment, Relations.ATTACHMENT_DATA).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            assertEquals(versionToContentMap.get(attachment.getVersion()), EntityUtils.toString(getMethod.getEntity()));
        }
    }

    @Test
    void testPOSTAttachment() throws Exception
    {
        final String attachmentName = String.format("%s.txt", UUID.randomUUID());
        final String content = "ATTACHMENT CONTENT";

        String attachmentsUri = buildURIForThisPage(AttachmentsResource.class, attachmentName);

        MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
        entityBuilder.addBinaryBody(attachmentName, content.getBytes(), ContentType.DEFAULT_BINARY, attachmentName);

        HttpPost postMethod = new HttpPost(attachmentsUri);
        postMethod.setEntity(entityBuilder.build());
        postMethod.addHeader("XWiki-Form-Token", getFormToken(TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword()));

        CloseableHttpResponse postResponse = execute(postMethod, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postResponse.getCode(), getHttpResponseInfo(postResponse));

        this.unmarshaller.unmarshal(postResponse.getEntity().getContent());

        Header location = postResponse.getHeader("location");

        CloseableHttpResponse getMethod = executeGet(location.getValue());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        assertEquals(content, EntityUtils.toString(getMethod.getEntity()));
    }

    @Test
    void testAttachmentsResourcePaginationAndErrors() throws Exception
    {
        // Setup: Add two attachments
        String attachmentName1 = "att1.txt";
        String attachmentName2 = "att2.txt";
        try {
            putAttachmentFilename(attachmentName1);
            putAttachmentFilename(attachmentName2);

            String attachmentsUri = buildURIForThisPage(AttachmentsResource.class);

            // Test: number=-1 should return error
            CloseableHttpResponse getMethod = executeGet(attachmentsUri + "?number=-1");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(getMethod.getEntity()));

            // Test: number=1001 should return error
            getMethod = executeGet(attachmentsUri + "?number=1001");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(getMethod.getEntity()));

            // Test: pagination with number=1
            getMethod = executeGet(attachmentsUri + "?number=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(1, attachments.getAttachments().size());

            String firstName = attachments.getAttachments().get(0).getName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(attachmentsUri + "?number=1&start=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(1, attachments.getAttachments().size());
            assertNotEquals(firstName, attachments.getAttachments().get(0).getName());
        } finally {
            // Clean up
            getUtil().deletePage(this.reference);
        }
    }

    @Test
    void testAttachmentHistoryResourcePaginationAndErrors() throws Exception
    {
        try {
            // Setup: Create an attachment with multiple versions
            String attachmentName = "history.txt";
            int versionCount = 3;
            for (int i = 0; i < versionCount; i++) {
                String content = "Content version " + i;
                CloseableHttpResponse putMethod =
                    executePut(buildURIForThisPage(AttachmentResource.class, attachmentName), content,
                        MediaType.TEXT_PLAIN,
                        TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                        TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
                if (i == 0) {
                    assertEquals(HttpStatus.SC_CREATED, putMethod.getCode());
                } else {
                    assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode());
                }
            }

            String historyUri = buildURIForThisPage(AttachmentHistoryResource.class, attachmentName);

            // Test: number=-1 should return error
            CloseableHttpResponse getMethod = executeGet(historyUri + "?number=-1");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(getMethod.getEntity()));

            // Test: number=1001 should return error
            getMethod = executeGet(historyUri + "?number=1001");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(getMethod.getEntity()));

            // Test: pagination with number=1
            getMethod = executeGet(historyUri + "?number=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            Attachments attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(1, attachments.getAttachments().size());

            String firstVersion = attachments.getAttachments().get(0).getVersion();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(historyUri + "?number=1&start=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            attachments = (Attachments) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(1, attachments.getAttachments().size());
            assertNotEquals(firstVersion, attachments.getAttachments().get(0).getVersion());
        } finally {
            // Clean up
            getUtil().deletePage(this.reference);
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
