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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.User;

import com.xpn.xwiki.XWikiContext;

/**
 * Resolves the current logged-in user. This is a convenience resolver since the current user should be retrieved from
 * the Execution Context instead.
 *
 * @version $Id$
 * @since 12.2RC1
 */
@Component
@Named("org.xwiki.user.CurrentUserReference")
@Singleton
public class CurrentUserResolver extends AbstractDocumentUserResolver<CurrentUserReference>
{
    @Inject
    private Execution execution;

    @Override
    public User resolve(CurrentUserReference unused, Object... parameters)
    {
        return resolveUser(new DocumentUserReference(getXWikiContext().getUserReference()));
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
