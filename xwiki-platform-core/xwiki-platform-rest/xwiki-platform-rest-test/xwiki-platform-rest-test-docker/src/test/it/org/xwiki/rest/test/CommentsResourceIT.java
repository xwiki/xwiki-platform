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

import javax.ws.rs.core.MediaType;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.rest.model.jaxb.HistorySummary;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.resources.comments.CommentsResource;
import org.xwiki.rest.resources.pages.PageHistoryResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommentsResourceIT extends AbstractHttpIT
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
        /* Everything is done in test methods */
    }

    @Test
    void testPOSTComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        Comment comment = objectFactory.createComment();
        comment.setText("Comment");

        CloseableHttpResponse postMethod =
            executePostXml(commentsUri, comment, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    void testPOSTCommentWithTextPlain() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        CloseableHttpResponse postMethod = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    void testPOSTCommentWithTextPlainNoCSRF() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        CloseableHttpResponse postMethod = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postMethod.getCode(), getHttpResponseInfo(postMethod));
        assertEquals("Invalid or missing form token.", EntityUtils.toString(postMethod.getEntity()));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(numberOfComments, comments.getComments().size());
    }

    @Test
    void testGETComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        for (Comment comment : comments.getComments()) {
            checkLinks(comment);
        }
    }

    @Test
    void testGETCommentsAtPreviousVersions() throws Exception
    {
        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(pageHistoryUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        History history = (History) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());

            if (getFirstLinkByRelation(page, Relations.COMMENTS) != null) {
                getMethod = executeGet(getFirstLinkByRelation(page, Relations.COMMENTS).getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));
            }
        }
    }

    @Test
    void testPOSTCommentFormUrlEncoded() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        CloseableHttpResponse getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new BasicNameValuePair("text", "Comment");

        CloseableHttpResponse postMethod = executePostForm(commentsUri, nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }
}
