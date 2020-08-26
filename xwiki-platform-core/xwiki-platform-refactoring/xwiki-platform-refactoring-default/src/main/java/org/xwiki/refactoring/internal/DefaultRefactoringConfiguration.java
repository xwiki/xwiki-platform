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
package org.xwiki.refactoring.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.refactoring.RefactoringConfiguration;

/**
 * Default implementation of {@link RefactoringConfiguration}.
 *
 * @version $Id$
 * @since 12.8RC1
 */
@Component
@Singleton
public class DefaultRefactoringConfiguration implements RefactoringConfiguration
{
    @Inject
    private ConfigurationSource configurationSource;

    @Override
    public boolean canSkipRecyclebin()
    {
        // check if the wiki is configured to allow the recyclebin to be skipped, and if the user has the right to do
        // so (ie, is an advanced user).
        return this.configurationSource.getProperty("refactoring.recyclebin.skip", Boolean.FALSE);
    }
}
