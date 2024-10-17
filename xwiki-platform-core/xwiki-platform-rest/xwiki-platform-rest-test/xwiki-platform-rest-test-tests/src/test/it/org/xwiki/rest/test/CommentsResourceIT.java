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
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.Before;
import org.junit.Test;
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

import static org.junit.Assert.assertEquals;

public class CommentsResourceIT extends AbstractHttpIT
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
        /* Everything is done in test methods */
    }

    @Test
    public void testPOSTComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        Comments comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        Comment comment = objectFactory.createComment();
        comment.setText("Comment");

        CloseableHttpResponse postResponse = executePostXml(commentsUri, comment,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postResponse.getCode());

        getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    public void testPOSTCommentWithTextPlain() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        Comments comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        CloseableHttpResponse postResponse = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postResponse.getCode());

        getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    public void testPOSTCommentWithTextPlainNoCSRF() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        Comments comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        CloseableHttpResponse postResponse = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postResponse.getCode());
        assertEquals("Invalid or missing form token.", EntityUtils.toString(postResponse.getEntity()));

        getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        assertEquals(numberOfComments, comments.getComments().size());
    }

    @Test
    public void testGETComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        Comments comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        for (Comment comment : comments.getComments()) {
            checkLinks(comment);
        }
    }

    @Test
    public void testGETCommentsAtPreviousVersions() throws Exception
    {
        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(pageHistoryUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        History history = (History) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getResponse = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getResponse.getCode());

            Page page = (Page) unmarshaller.unmarshal(getResponse.getEntity().getContent());

            if (getFirstLinkByRelation(page, Relations.COMMENTS) != null) {
                getResponse = executeGet(getFirstLinkByRelation(page, Relations.COMMENTS).getHref());
                assertEquals(HttpStatus.SC_OK, getResponse.getCode());
            }
        }
    }

    @Test
    public void testPOSTCommentFormUrlEncoded() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName);

        CloseableHttpResponse getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        Comments comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        int numberOfComments = comments.getComments().size();

        CloseableHttpResponse postResponse =
            executePostForm(commentsUri, List.of(new BasicNameValuePair("text", "Comment")),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postResponse.getCode());

        getResponse = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getResponse.getCode());

        comments = (Comments) unmarshaller.unmarshal(getResponse.getEntity().getContent());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }
}
