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
package org.xwiki.security;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.internal.XWikiBridge;

/**
 * {@link SecurityReference} factory.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityReferenceFactory implements SecurityReferenceFactory
{
    /** Bridge to the XWiki to retrieve the main wiki reference. */
    @Inject
    private XWikiBridge wikiBridge;

    /** Cache the main wiki reference. */
    private SecurityReference mainWikiReference;

    /** @return the main wiki reference. */
    private SecurityReference getMainWikiReference()
    {
        if (mainWikiReference == null) {
            mainWikiReference = new SecurityReference(wikiBridge.getMainWikiReference());
        }
        return mainWikiReference;
    }

    @Override
    public SecurityReference newEntityReference(EntityReference reference)
    {
        return new SecurityReference(this.wikiBridge.toCompatibleEntityReference(reference), getMainWikiReference());
    }

    @Override
    public UserSecurityReference newUserReference(DocumentReference reference)
    {
        return new UserSecurityReference(reference, getMainWikiReference());
    }

    @Override
    public GroupSecurityReference newGroupReference(DocumentReference reference)
    {
        return new GroupSecurityReference(reference, getMainWikiReference());
    }
}
