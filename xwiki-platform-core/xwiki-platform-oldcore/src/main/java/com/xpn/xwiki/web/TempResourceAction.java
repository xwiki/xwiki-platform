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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.environment.Environment;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action responsible for downloading temporary resources created by various modules. The temporary resource is put in
 * the temporary directory in a directory named "temp" and in subdirectories "(module)/(wiki)/(space)/(page)/(file)"
 * where:
 * <ul>
 *   <li>(module): it's the 3rd path segment in the request URL (format: {code .../temp/1/2/3/4})</li>
 *   <li>(wiki): the name of the current wiki (extracted from the URL too)</li>
 *   <li>(space): it's the 1st path segment in the request URL (format: {code .../temp/1/2/3/4})</li>
 *   <li>(page): it's the 2nd path segment in the request URL (format: {code .../temp/1/2/3/4})</li>
 *   <li>(file): it's the 4th path segment in the request URL (format: {code .../temp/1/2/3/4})</li>
 * </ul>
 * <p/>
 * For example if the URL is {@code http://localhost:8080/xwiki/bin/temp/Main/WebHome/test/test.png} then the resource
 * will be fetched from {@code TMPDIR/temp/test/xwiki/Main/WebHome/test.png}.
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
     * The path separator.
     */
    private static final String PATH_SEPARATOR = "/";

    /**
     * The URL encoding.
     */
    private static final String URL_ENCODING = "UTF-8";

    /**
     * Logging support.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TempResourceAction.class);

    /**
     * Used for detecting mime-types of files.
     */
    private Tika tika = new Tika();

    /**
     * Used to find the temporary dir.
     */
    private Environment environment = Utils.getComponent(Environment.class);

    @Override
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
            contentType = this.tika.detect(tempFile);
        } catch (IOException ex) {
            LOGGER.warn(
                String.format("Unable to determine mime type for temporary resource [%s]", tempFile.getAbsolutePath()),
                ex);
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
     */
    protected File getTemporaryFile(String uri, XWikiContext context)
    {
        Matcher matcher = URI_PATTERN.matcher(uri);
        File result = null;
        if (matcher.find()) {
            List<String> pathSegments = new ArrayList<String>();
            // Add all the path segments.
            pathSegments.add("temp");
            // temp/module
            pathSegments.add(withMinimalURLEncoding(matcher.group(3)));
            // temp/module/wiki
            pathSegments.add(encodeURLPathSegment(context.getWikiId()));
            // temp/module/wiki/space
            pathSegments.add(withMinimalURLEncoding(matcher.group(1)));
            // temp/module/wiki/space/page
            pathSegments.add(withMinimalURLEncoding(matcher.group(2)));
            // Save the path prefix before adding the file path to be able to check if the file path tries to get out of
            // the parent folder (e.g. using '/../').
            String prefix = StringUtils.join(pathSegments, PATH_SEPARATOR);
            // temp/module/wiki/space/page/path/to/file.tmp
            for (String filePathSegment : matcher.group(4).split(PATH_SEPARATOR)) {
                pathSegments.add(withMinimalURLEncoding(filePathSegment));
            }
            String path = URI.create(StringUtils.join(pathSegments, PATH_SEPARATOR)).normalize().toString();
            if (path.startsWith(prefix)) {
                result = new File(this.environment.getTemporaryDirectory(), path);
                result = result.exists() ? result : null;
            }
        }
        return result;
    }

    /**
     * Keeps only minimal URL encoding. Currently, XWiki's URL factory over encodes the URLs in order to protect them
     * from XWiki 1.0 syntax parser.
     * <p>
     * This method also ensures that the path to the temporary file is fully encoded (has the canonical form) even if
     * the URL used to access the file is partially decoded (which can happen for instance when XWiki is behind Apache's
     * {@code mode_proxy} with {@code nocanon} option disabled).
     *
     * @param encodedPathSegment an encoded URL path segment
     * @return the given string with minimal URL encoding
     */
    private String withMinimalURLEncoding(String encodedPathSegment)
    {
        return encodeURLPathSegment(decodeURLPathSegment(encodedPathSegment));
    }

    private String encodeURLPathSegment(String segment)
    {
        try {
            return URLEncoder.encode(segment, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            return segment;
        }
    }

    private String decodeURLPathSegment(String encodedSegment)
    {
        try {
            return URLDecoder.decode(encodedSegment, URL_ENCODING);
        } catch (UnsupportedEncodingException e) {
            // This should never happen.
            return encodedSegment;
        }
    }
}
