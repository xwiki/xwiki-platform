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
package org.xwiki.extension.distribution.internal.job;

import org.xwiki.job.AbstractRequest;
import org.xwiki.job.Request;
import org.xwiki.model.reference.DocumentReference;

/**
 * @version $Id$
 * @since 4.2M3
 */
public class DistributionRequest extends AbstractRequest implements Cloneable
{
    private static final long serialVersionUID = 1L;

    /**
     * @see #getWiki()
     */
    public static final String PROPERTY_WIKI = "wiki";

    public static final String PROPERTY_USERREFERENCE = "user.reference";

    /**
     * The default constructor.
     */
    public DistributionRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 9.4RC1
     */
    public DistributionRequest(Request request)
    {
        super(request);
    }

    public void setWiki(String wiki)
    {
        setProperty(PROPERTY_WIKI, wiki);
    }

    public String getWiki()
    {
        return getProperty(PROPERTY_WIKI);
    }

    public void setUserReference(DocumentReference userReference)
    {
        setProperty(PROPERTY_USERREFERENCE, userReference);
    }

    public DocumentReference getUserReference()
    {
        return getProperty(PROPERTY_USERREFERENCE);
    }

    @Override
    public Object clone() throws CloneNotSupportedException
    {
        return super.clone();
    }
}
