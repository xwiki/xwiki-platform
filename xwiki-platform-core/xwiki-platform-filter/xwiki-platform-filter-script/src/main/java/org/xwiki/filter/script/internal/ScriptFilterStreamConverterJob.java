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
package org.xwiki.filter.script.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filter.internal.job.FilterStreamConverterJob;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Override {@link FilterStreamConverterJob} to set current wiki/user to search for extension in the right
 * {@link org.xwiki.component.manager.ComponentManager}.
 * 
 * @version $Id$
 * @since 6.2M1
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(FilterStreamConverterJob.JOBTYPE)
public class ScriptFilterStreamConverterJob extends FilterStreamConverterJob implements Initializable
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private String contextWiki;

    private DocumentReference contextUser;

    @Override
    public void initialize() throws InitializationException
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            this.contextWiki = xcontext.getWikiId();
            this.contextUser = xcontext.getUserReference();
        }
    }

    @Override
    public void run()
    {
        // Set proper context wiki and user
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            xcontext.setWikiId(this.contextWiki);
            xcontext.setUserReference(this.contextUser);
        }

        super.run();
    }
}
