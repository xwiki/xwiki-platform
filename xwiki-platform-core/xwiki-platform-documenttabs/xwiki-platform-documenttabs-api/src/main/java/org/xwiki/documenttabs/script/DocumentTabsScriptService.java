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
package org.xwiki.documenttabs.script;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.documenttabs.DocumentTabsConfiguration;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Exposes the visibility of the tabs displayed below the content of a page, so that the skin templates and the
 * administration sheets can share the same rules.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Named("documenttabs")
@Singleton
@Unstable
public class DocumentTabsScriptService implements ScriptService
{
    @Inject
    private DocumentTabsConfiguration configuration;

    /**
     * @return {@code false} when the whole tab area must be hidden, which no individual tab can override
     * @see DocumentTabsConfiguration#areTabsDisplayed()
     */
    public boolean areTabsDisplayed()
    {
        return this.configuration.areTabsDisplayed();
    }

    /**
     * @param tabId the identifier of the tab, i.e. the identifier of its UI extension
     * @param defaultVisibility the visibility to use when no administration level defines one for that tab
     * @return {@code true} when the tab must be displayed
     * @see DocumentTabsConfiguration#isTabVisible(String, boolean)
     */
    public boolean isTabVisible(String tabId, boolean defaultVisibility)
    {
        return this.configuration.isTabVisible(tabId, defaultVisibility);
    }
}
