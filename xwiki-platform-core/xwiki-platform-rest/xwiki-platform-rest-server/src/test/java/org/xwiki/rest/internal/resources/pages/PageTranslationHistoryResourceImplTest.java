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
package org.xwiki.rest.internal.resources.pages;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.History;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

/**
 * Unit tests for {@link PageTranslationHistoryResourceImpl}.
 *
 * @version $Id$
 */
@ComponentTest
@ReferenceComponentList
class PageTranslationHistoryResourceImplTest extends AbstractPageHistoryResourceImplTest
{
    @InjectMockComponents
    private PageTranslationHistoryResourceImpl pageTranslationHistoryResource;

    @Override
    protected History getTranslationHistory() throws XWikiRestException
    {
        return this.pageTranslationHistoryResource.getPageTranslationHistory(WIKI_NAME, SPACE_URL, PAGE_NAME,
            getLanguage(), START, LIMIT, "order", false);
    }

    @Override
    protected String getLanguage()
    {
        return "language";
    }

    @Override
    void injectURIInfo() throws IllegalAccessException
    {
        injectURIInfo(this.pageTranslationHistoryResource);
    }
}
