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
package org.xwiki.flamingo.test.docker;

import org.junit.jupiter.api.Test;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test to ensure that the {@link org.xwiki.environment.internal.ServletEnvironmentCacheInitializer} has
 * been executed.
 *
 * @version $Id$
 */
@UITest(properties = "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:Test\\.Execute\\..*")
class ServletEnvironmentCacheIT
{
    @Test
    void testCacheInitialized(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        assertEquals("<p>Resource URL Cache is initialized.<br/></p>", setup.executeWiki("""
            {{groovy wiki="false"}}
            import org.xwiki.environment.Environment
            
            def environment = services.component.getInstance(Environment.class)
            // Load the same resource twice to check if we get the same URL instance, which is only the case when it is
            // stored in a cache.
            def url1 = environment.getResource('/WEB-INF/xwiki.properties')
            def url2 = environment.getResource('/WEB-INF/xwiki.properties')
            if (url1 == null) {
                println("Error: URL is null")
            } else if (url1 === url2) {
                println("Resource URL Cache is initialized.")
            } else {
                println("Error: URLs aren't identical, resource URL Cache is NOT initialized.")
            }
            {{/groovy}}
            """, Syntax.XWIKI_2_1));
    }
}
