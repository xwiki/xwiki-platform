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
package org.xwiki.repository.job;

import java.util.List;

import org.xwiki.extension.ExtensionId;
import org.xwiki.job.Request;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to export an XWiki extension and its dependencies as a XIP package.
 * 
 * @version $Id$
 * @since 18.5.0RC1
 */
@Unstable
public class XIPExportJobRequest extends AbstractCheckRightsRequest
{
    /**
     * The property that stores the id of the extension to export.
     */
    private static final String PROPERTY_EXTENSION = "extension";

    /**
     * The property that stores the XWiki version where the XIP package is intended to be used.
     */
    private static final String PROPERTY_XWIKI_VERSION = "xwikiVersion";

    /**
     * The property that stores the list of core extensions to exclude from the XIP package. These extensions are
     * supposed to be provided by the target XWiki instance.
     */
    private static final String PROPERTY_CORE_EXTENSIONS = "coreExtensions";

    /**
     * Default constructor.
     */
    public XIPExportJobRequest()
    {
    }

    /**
     * Copy constructor.
     *
     * @param request the request to copy
     */
    public XIPExportJobRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the id of the extension to export
     */
    public ExtensionId getExtension()
    {
        return getProperty(PROPERTY_EXTENSION);
    }

    /**
     * Sets the id of the extension to export.
     *
     * @param extension the id of the extension to export
     */
    public void setExtension(ExtensionId extension)
    {
        setProperty(PROPERTY_EXTENSION, extension);
    }

    /**
     * @return the XWiki version where the XIP package is intended to be used
     */
    public String getXWikiVersion()
    {
        return getProperty(PROPERTY_XWIKI_VERSION);
    }

    /**
     * Sets the XWiki version where the XIP package is intended to be used.
     *
     * @param xwikiVersion the XWiki version where the XIP package is intended to be used
     */
    public void setXWikiVersion(String xwikiVersion)
    {
        setProperty(PROPERTY_XWIKI_VERSION, xwikiVersion);
    }

    /**
     * @return the list of core extensions to excludes from the XIP package
     */
    public List<ExtensionId> getCoreExtensions()
    {
        return getProperty(PROPERTY_CORE_EXTENSIONS);
    }

    /**
     * Sets the list of core extensions to exclude from the XIP package.
     *
     * @param coreExtensions the list of core extensions to exclude from the XIP package
     */
    public void setCoreExtensions(List<ExtensionId> coreExtensions)
    {
        setProperty(PROPERTY_CORE_EXTENSIONS, coreExtensions);
    }
}
