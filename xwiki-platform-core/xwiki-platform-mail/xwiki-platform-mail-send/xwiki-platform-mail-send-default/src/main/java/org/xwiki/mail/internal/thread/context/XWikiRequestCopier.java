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
package org.xwiki.mail.internal.thread.context;

import javax.inject.Singleton;

import org.apache.commons.lang3.exception.CloneFailedException;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletRequest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Copy a {@link XWikiRequest} instance.
 *
 * @version $Id$
 * @since 7.1M2
 * @since 6.4.5
 */
@Component
@Singleton
public class XWikiRequestCopier implements Copier<XWikiRequest>
{
    @Override
    public XWikiRequest copy(XWikiRequest originalRequest) throws CloneFailedException
    {
        if (originalRequest == null) {
            return null;
        }

        XWikiServletRequestStub dummyRequest = new XWikiServletRequestStub(originalRequest);

        return new XWikiServletRequest(dummyRequest);
    }
}
