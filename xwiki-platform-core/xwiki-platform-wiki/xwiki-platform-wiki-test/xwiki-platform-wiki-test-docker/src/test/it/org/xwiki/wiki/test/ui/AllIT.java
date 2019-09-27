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
package org.xwiki.wiki.test.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.ExtensionOverride;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI Tests for the multi-wikis manipulations.
 *
 * @version $Id$
 */
@UITest(
    properties = {
        // TODO: Remove once https://jira.xwiki.org/browse/XWIKI-7581 is fixed
        "xwikiCfgSuperadminPassword=pass",
        // The Notifications module contributes a Hibernate mapping that needs to be added to hibernate.cfg.xml
        "xwikiDbHbmCommonExtraMappings=notification-filter-preferences.hbm.xml",
        // Disable the DW
        "xwikiPropertiesAdditionalProperties=distribution.automaticStartOnMainWiki=false"
    },
    extraJARs = {
        // It's currently not possible to install a JAR contributing a Hibernate mapping file as an Extension. Thus
        // we need to provide the JAR inside WEB-INF/lib
        "org.xwiki.platform:xwiki-platform-notifications-filters-default",
        // Required by components located in a core extensions
        "org.xwiki.platform:xwiki-platform-notifications-preferences-default",
        "org.xwiki.platform:xwiki-platform-wiki-template-default",
        // TODO: improve the docker test framework to indicate xwiki-platform-wiki-ui-wiki instead of all those jars one
        // by one
        // Needed by the subwikis
        "org.xwiki.platform:xwiki-platform-wiki-script",
        "org.xwiki.platform:xwiki-platform-wiki-user-default",
        "org.xwiki.platform:xwiki-platform-wiki-user-script"
    },
    extensionOverrides = {
        @ExtensionOverride(
            extensionId = "org.xwiki.platform:xwiki-platform-web",
            overrides = {
                // We set a default UI for the subwiki in the webapp, so that the Wiki Creation UI knows which extension
                // to install on a subwiki by default (which is something we test)
                // Otherwise the wiki creation form will display the flavor picker and the functional tests do not handle it.
                "properties=xwiki.extension.distribution.wikiui=org.xwiki.platform:xwiki-platform-wiki-ui-wiki"
            }
        )
    }
)
public class AllIT
{
    @Nested
    @DisplayName("Wiki Manager REST Panels Tests")
    class NestedWikiManagerRestIT extends WikiManagerRestIT
    {
    }

    @Nested
    @DisplayName("Wiki Template Tests")
    class NestedWikiTemplateIT extends WikiTemplateIT
    {
    }
}
