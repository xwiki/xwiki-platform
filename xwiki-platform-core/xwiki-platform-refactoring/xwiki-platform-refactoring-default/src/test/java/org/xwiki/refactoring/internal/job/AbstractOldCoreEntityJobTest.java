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
package org.xwiki.refactoring.internal.job;

import javax.inject.Provider;

import org.junit.Before;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.*;

/**
 * Base class for writing unit tests for jobs extending {@link AbstractOldCoreEntityJob}.
 * 
 * @version $Id$
 */
public abstract class AbstractOldCoreEntityJobTest
{
    protected XWikiContext xcontext = mock(XWikiContext.class);

    protected AuthorizationManager authorization;

    @Before
    public void configure() throws Exception
    {
        XWiki xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = getMocker().getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        this.authorization = getMocker().getInstance(AuthorizationManager.class);

        EntityReferenceProvider defaultEntityReferenceProvider = getMocker().getInstance(EntityReferenceProvider.class);
        when(defaultEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(
            new EntityReference("WebHome", EntityType.DOCUMENT, null));
    }

    protected abstract MockitoComponentMockingRule<Job> getMocker();

    protected Job run(EntityRequest request) throws Exception
    {
        Job job = getMocker().getComponentUnderTest();
        job.initialize(request);
        job.run();
        return job;
    }
}
