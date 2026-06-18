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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
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

public class CommentsResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    @BeforeEach
    @Override
    public void setUp(TestUtils setup, TestInfo info) throws Exception
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
    public void testRepresentation() throws Exception
    {
        /* Everything is done in test methods */
    }

    @Test
    public void testPOSTComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int numberOfComments = comments.getComments().size();

        Comment comment = objectFactory.createComment();
        comment.setText("Comment");

        PostMethod postMethod = executePostXml(commentsUri, comment, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    public void testPOSTCommentWithTextPlain() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int numberOfComments = comments.getComments().size();

        PostMethod postMethod = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }

    @Test
    public void testPOSTCommentWithTextPlainNoCSRF() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int numberOfComments = comments.getComments().size();

        PostMethod postMethod = executePost(commentsUri, "Comment", MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
        assertEquals("Invalid or missing form token.", postMethod.getResponseBodyAsString());

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(numberOfComments, comments.getComments().size());
    }

    @Test
    public void testGETComment() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (Comment comment : comments.getComments()) {
            checkLinks(comment);
        }
    }

    @Test
    public void testGETCommentsAtPreviousVersions() throws Exception
    {
        String pageHistoryUri = buildURI(PageHistoryResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(pageHistoryUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        History history = (History) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (HistorySummary historySummary : history.getHistorySummaries()) {
            getMethod = executeGet(getFirstLinkByRelation(historySummary, Relations.PAGE).getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            if (getFirstLinkByRelation(page, Relations.COMMENTS) != null) {
                getMethod = executeGet(getFirstLinkByRelation(page, Relations.COMMENTS).getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
            }
        }
    }

    @Test
    public void testPOSTCommentFormUrlEncoded() throws Exception
    {
        String commentsUri = buildURI(CommentsResource.class, getWiki(), this.spaces, this.pageName).toString();

        GetMethod getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Comments comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        int numberOfComments = comments.getComments().size();

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("text", "Comment");

        PostMethod postMethod = executePostForm(commentsUri, nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        getMethod = executeGet(commentsUri);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        comments = (Comments) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(numberOfComments + 1, comments.getComments().size());
    }
}
