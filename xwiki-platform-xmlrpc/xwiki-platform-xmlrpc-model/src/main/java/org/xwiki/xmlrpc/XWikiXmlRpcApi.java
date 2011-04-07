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
 *
 */
package org.xwiki.xmlrpc;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The XWiki XML RPC API.
 * 
 * @version $Id$
 */
public interface XWikiXmlRpcApi
{
    /* Authentication */
    public String login(String username, String password) throws Exception;

    public Boolean logout(String token) throws Exception;

    /* General */
    public Map/* ServerInfo */getServerInfo(String token) throws Exception;

    /* Spaces */
    public List/* List<SpaceSummary> */getSpaces(String token) throws Exception;

    public Map/* Space */getSpace(String token, String spaceKey) throws Exception;

    public Map/* Space */addSpace(String token, Map spaceMap) throws Exception;

    public Boolean removeSpace(String token, String spaceKey) throws Exception;

    /* Pages */
    public List/* List<PageSummary> */getPages(String token, String spaceKey) throws Exception;

    public Map/* XWikiPage */getPage(String token, String pageId) throws Exception;

    public Map/* XWikiPage */storePage(String token, Map pageMap) throws Exception;

    public Map/* XWikiPage */storePage(String token, Map pageMap, boolean checkVersion) throws Exception;

    public Boolean removePage(String token, String pageId) throws Exception;

    public List/* List<XWikiPageHistorySummary> */getPageHistory(String token, String pageId) throws Exception;

    public String renderContent(String token, String space, String pageId, String content) throws Exception;

    public String renderPageContent(String token, String pageId, String content, String sourceSyntaxId,
        String targetSyntaxId) throws Exception;

    public String getRenderedContent(String token, String pageId, String syntaxId) throws Exception;

    public String convert(String token, String source, String initialSyntaxId, String targetSyntaxId) throws Exception;

    public List<String> getInputSyntaxes(String token) throws Exception;

    public List<String> getOutputSyntaxes(String token) throws Exception;

    /* Comments */
    public List/* List<Comment> */getComments(String token, String pageId) throws Exception;

    public Map/* Comment */getComment(String token, String commentId) throws Exception;

    public Map/* Comment */addComment(String token, Map commentMap) throws Exception;

    public Boolean removeComment(String token, String commentId) throws Exception;

    /* Attachments */
    public List/* List<Attachment> */getAttachments(String token, String pageId) throws Exception;

    public Map/* Attachmnent */addAttachment(String token, Integer contentId, Map attachmentMap, byte[] attachmentData)
        throws Exception;

    public byte[] getAttachmentData(String token, String pageId, String fileName, String versionNumber)
        throws Exception;

    public Boolean removeAttachment(String token, String pageId, String fileName) throws Exception;

    /* Classes */
    public List/* List<XWikiClassSummary> */getClasses(String token) throws Exception;

    public Map/* XWikiClass */getClass(String token, String className) throws Exception;

    /* Objects */
    public List/* List<XWikiObjectSummary> */getObjects(String token, String pageId) throws Exception;

    public Map/* XWikiObject */getObject(String token, String pageId, String className, Integer id) throws Exception;

    public Map/* XWikiObject */getObject(String token, String pageId, String guid) throws Exception;

    public Map/* XWikiObject */storeObject(String token, Map objectMap) throws Exception;

    public Map/* XWikiObject */storeObject(String token, Map objectMap, boolean checkVersion) throws Exception;

    public Boolean removeObject(String token, String pageId, String className, Integer id) throws Exception;

    /* Search */
    public List/* List<SerarchResult> */search(String token, String query, int maxResults) throws Exception;

    public List/* List<XWikiPageHistorySummary> */getModifiedPagesHistory(String token, Date date, int numberOfResults,
        int start, boolean fromLatest) throws Exception;
}
