/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * <p/>
 * This is free software;you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation;either version2.1of
 * the License,or(at your option)any later version.
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software;if not,write to the Free
 * Software Foundation,Inc.,51 Franklin St,Fifth Floor,Boston,MA
 * 02110-1301 USA,or see the FSF site:http://www.fsf.org.
 */
package com.xpn.xwiki.render.groovy;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiAttachment;

import java.net.URLClassLoader;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XWikiPageClassLoader extends URLClassLoader
{
    private static final Log LOG = LogFactory.getLog(XWikiPageClassLoader.class);

    public XWikiPageClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }

    public XWikiPageClassLoader(URL[] urls)
    {
        super(urls);
    }

    public XWikiPageClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory)
    {
        super(urls, parent, factory);
    }

    public XWikiPageClassLoader(String jarWikiPage, XWikiContext context) throws XWikiException
    {
        this(jarWikiPage, Thread.currentThread().getContextClassLoader(), context);
    }

    public XWikiPageClassLoader(String jarWikiPage, ClassLoader parent, XWikiContext context)
        throws XWikiException
    {
        super(new URL[0], parent);
        XWikiDocument doc = context.getWiki().getDocument(jarWikiPage, context);
        if (!doc.isNew()) {
            List attachList = doc.getAttachmentList();
            for (int i=0;i<attachList.size();i++) {
                XWikiAttachment attach = (XWikiAttachment) attachList.get(i);
                String filename = attach.getFilename();
                if (filename.endsWith(".jar")) {
                    String downloadURL = doc.getExternalAttachmentURL(filename, "download", context);
                    try{
                        addURL(new URL(downloadURL));
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Adding [" + downloadURL + "] JAR from page ["
                                + jarWikiPage + "] to Groovy classloader");
                        }
                    } catch (Exception e) {
                        LOG.warn("Failed to add [" + downloadURL + "] JAR from page ["
                            + jarWikiPage + "], ignoring it.");
                    }
                }
            }
        }
    }
}
