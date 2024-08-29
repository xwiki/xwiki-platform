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
package org.xwiki.refactoring.internal.splitter.criterion.naming;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.refactoring.splitter.criterion.naming.HeadingNameNamingCriterion;

/**
 * Naming criterion that extracts the first heading from the content and appends it to the main (base) page name.
 * 
 * @version $Id$
 * @since 14.10.2
 * @since 15.0RC1
 */
@Component
@Named("mainPageNameAndHeading")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class MainPageNameAndHeadingNamingCriterion extends HeadingNameNamingCriterion
{
    /**
     * Implicit constructor.
     */
    public MainPageNameAndHeadingNamingCriterion()
    {
        getParameters().setParameter(HeadingNameNamingCriterion.PARAM_PREPEND_BASE_PAGE_NAME, true);
    }
}
