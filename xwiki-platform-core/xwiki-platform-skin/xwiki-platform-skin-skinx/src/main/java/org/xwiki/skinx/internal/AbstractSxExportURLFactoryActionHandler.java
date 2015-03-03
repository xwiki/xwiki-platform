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
package org.xwiki.skinx.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.ExportURLFactoryActionHandler;
import com.xpn.xwiki.web.ExportURLFactoryContext;
import com.xpn.xwiki.web.sx.Extension;
import com.xpn.xwiki.web.sx.SxDocumentSource;
import com.xpn.xwiki.web.sx.SxResourceSource;
import com.xpn.xwiki.web.sx.SxSource;

/**
 * Handles SX URL rewriting, by extracting and rendering the SX content in a file on disk and generating a URL
 * pointing to it.
 *
 * @version $Id$
 * @since 6.2RC1
 */
public abstract class AbstractSxExportURLFactoryActionHandler implements ExportURLFactoryActionHandler
{
    /** If the user passes this parameter in the URL, we will look for the script in the jar files. */
    private static final String JAR_RESOURCE_REQUEST_PARAMETER = "resource";

    private static final char URL_PATH_SEPARATOR = '/';

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    protected abstract String getSxPrefix();

    protected abstract String getFileSuffix();

    /**
     * Get the type of extension, depends on the type of action.
     *
     * @return a new object which extends Extension.
     */
    public abstract Extension getExtensionType();

    @Override
    public URL createURL(String web, String name, String queryString, String anchor, String wikiId,
        XWikiContext context, ExportURLFactoryContext factoryContext) throws Exception
    {
        // Check if the current user has the right to view the SX file. We do this since this is what would happen
        // in XE when a SX action is called (check done in XWikiAction).
        // Note that we cannot just open an HTTP connection to the SX action here since we wouldn't be authenticated...
        // Thus we have to simulate the same behavior as the SX action...
        DocumentReference sxDocumentReference = new DocumentReference(wikiId, web, name);
        this.authorizationManager.checkAccess(Right.VIEW, sxDocumentReference);

        // Set the SX document as the current document in the XWiki Context since unfortunately the SxSource code
        // uses the current document in the context instead of accepting it as a parameter...
        XWikiDocument sxDocument = context.getWiki().getDocument(sxDocumentReference, context);

        Map<String, Object> backup = new HashMap<>();
        XWikiDocument.backupContext(backup, context);
        try {
            sxDocument.setAsContextDoc(context);
            return processSx(web, name, queryString, context, factoryContext);
        } finally {
            XWikiDocument.restoreContext(backup, context);
        }
    }

    private URL processSx(String web, String name, String queryString, XWikiContext context,
        ExportURLFactoryContext factoryContext) throws Exception
    {
        SxSource sxSource = null;

        // Check if we have the JAR_RESOURCE_REQUEST_PARAMETER parameter in the query string
        List<NameValuePair> params = URLEncodedUtils.parse(queryString, StandardCharsets.UTF_8);
        for (NameValuePair param : params) {
            if (param.getName().equals(JAR_RESOURCE_REQUEST_PARAMETER)) {
                sxSource = new SxResourceSource(param.getValue());
                break;
            }
        }

        if (sxSource == null) {
            sxSource = new SxDocumentSource(context, getExtensionType());
        }

        String content = getContent(sxSource, factoryContext);

        // Write the content to file
        // We need a unique name for that SSX content
        String targetPath = String.format("%s/%s/%s", getSxPrefix(), web, name);
        File targetDirectory = new File(factoryContext.getExportDir(), targetPath);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }
        File targetLocation = File.createTempFile(getSxPrefix(), "." + getFileSuffix(), targetDirectory);
        FileUtils.writeStringToFile(targetLocation, content);

        // Rewrite the URL
        StringBuilder path = new StringBuilder("file://");
        path.append(getSxPrefix());
        path.append(URL_PATH_SEPARATOR);
        path.append(encodeURLPart(web));
        path.append(URL_PATH_SEPARATOR);
        path.append(encodeURLPart(name));
        path.append(URL_PATH_SEPARATOR);
        path.append(encodeURLPart(targetLocation.getName()));

        return new URL(path.toString());
    }

    protected String getContent(SxSource sxSource, ExportURLFactoryContext factoryContext)
    {
        return sxSource.getContent();
    }

    private String encodeURLPart(String part) throws IOException
    {
        return URLEncoder.encode(part, "UTF-8");
    }
}
