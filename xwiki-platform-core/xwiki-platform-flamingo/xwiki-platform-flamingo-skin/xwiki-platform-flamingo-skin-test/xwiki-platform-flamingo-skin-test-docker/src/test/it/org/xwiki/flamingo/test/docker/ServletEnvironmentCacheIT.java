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
            import org.xwiki.environment.internal.ServletEnvironment
            
            def environment = services.component.getInstance(Environment.class)
            if (!(environment instanceof ServletEnvironment)) {
                println("Expected ServletEnvironment, got: " + environment.getClass().getName())
            }
            if (environment.resourceURLCache == null) {
                println("Error: Resource URL Cache is null.")
            } else {
                println("Resource URL Cache is initialized.")
            }
            {{/groovy}}
            """, Syntax.XWIKI_2_1));
    }
}
