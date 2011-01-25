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
package com.xpn.xwiki.web;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.xwiki.container.Container;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action responsible for downloading temporary resources created by various modules. Refer JIRA issue:
 * <a>http://jira.xwiki.org/jira/browse/XWIKI-5227</a>.
 * 
 * @version $Id$
 * @since 2.4M1
 */
public class TempResourceAction extends XWikiAction
{
    /**
     * URI pattern for this action.
     */
    public static final Pattern URI_PATTERN = Pattern.compile(".*?/temp/([^/]*+)/([^/]*+)/([^/]*+)/(.*+)");

    /**
     * The URL encoding.
     */
    private static final String URL_ENCODING = "UTF-8";

    /**
     * Logging support.
     */
    private static final Log LOG = LogFactory.getLog(TempResourceAction.class);

    /**
     * Used for detecting mime-types of files.
     */
    private Tika tika = new Tika();

    /**
     * Used to resolve temporary working dir.
     */
    private Container container = Utils.getComponent(Container.class);

    /**
     * {@inheritDoc}
     */
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        String uri = request.getRequestURI();

        // Locate the temporary file.
        File tempFile = getTemporaryFile(uri, context);
        if (null == tempFile) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                "Invalid temporary resource URL");
        }

        // Write temporary file into response.
        response.setDateHeader("Last-Modified", tempFile.lastModified());
        String contentType = MimeTypes.OCTET_STREAM;
        try {
            contentType = tika.detect(tempFile);
        } catch (IOException ex) {
            LOG.warn(String.format("Unable to determine mime type for temporary resource [%s]", tempFile
                .getAbsolutePath()), ex);
        }
        response.setContentType(contentType);
        try {
            response.setContentLength((int) tempFile.length());
            IOUtils.copy(FileUtils.openInputStream(tempFile), response.getOutputStream());
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION, "Exception while sending response", e);
        }
        return null;
    }

    /**
     * Returns the temporary file corresponding to the specified URI.
     * 
     * @param uri request URI.
     * @param context xwiki context.
     * @return temporary file corresponding to the specified URI or null if no such file can be located.
     * @throws UnsupportedEncodingException
     */
    protected File getTemporaryFile(String uri, XWikiContext context)
    {
        Matcher matcher = URI_PATTERN.matcher(uri);
        File result = null;
        if (matcher.find()) {
            String wiki = context.getDatabase();
            try {
                wiki = URLEncoder.encode(wiki, URL_ENCODING);
            } catch (UnsupportedEncodingException e) {
                // This should never happen;
            }
            String space = withMinimalURLEncoding(matcher.group(1));
            String page = withMinimalURLEncoding(matcher.group(2));
            String module = withMinimalURLEncoding(matcher.group(3));
            String filePath = matcher.group(4);
            String prefix = String.format("temp/%s/%s/%s/%s/", module, wiki, space, page);
            String path = URI.create(prefix + filePath).normalize().toString();
            if (path.startsWith(prefix)) {
                result = new File(container.getApplicationContext().getTemporaryDirectory(), path);
                result = result.exists() ? result : null;
            }
        }
        return result;
    }

    /**
     * Keeps only minimal URL encoding. Currently, XWiki's URL factory over encodes the URLs in order to protect them
     * from XWiki 1.0 syntax parser.
     * 
     * @param component a URL component
     * @return the given string with minimal URL encoding
     */
    private String withMinimalURLEncoding(String component)
    {
        try {
            return URLEncoder.encode(URLDecoder.decode(component, URL_ENCODING), URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            return component;
        }
    }
}
