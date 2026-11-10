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
package org.xwiki.livedata.test.ui;

import org.junit.jupiter.api.Nested;
import org.xwiki.test.docker.junit5.UITest;

/**
 * All UI tests for the livedata extension.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.9
 */
@UITest(
    // The image plugin that resizes the images of the card covers is not registered until the server is restarted, so
    // we need to make it a core extension.
    extraJARs = {
        "org.xwiki.platform:xwiki-platform-image-processing-plugin"
    },
    resolveExtraJARs = true,
    properties = {
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.image.ImagePlugin"
    }
)
class AllIT
{
    @Nested
    class NestedLiveDataIT extends LiveDataIT
    {
    }

    @Nested
    class NestedLiveDataInplaceEditIT extends LiveDataInplaceEditIT
    {
    }
}
