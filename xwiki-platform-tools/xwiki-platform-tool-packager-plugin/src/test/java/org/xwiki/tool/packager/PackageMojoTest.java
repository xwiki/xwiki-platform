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
package org.xwiki.tool.packager;

import org.apache.velocity.VelocityContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.xwiki.tool.packager.PackageMojo}.
 *
 * @version $Id$
 * @since 6.2
 */
public class PackageMojoTest
{
    @Test
    public void replaceProperty()
    {
        VelocityContext context = new VelocityContext();
        context.put("xwikiDataDir", "/some/path");
        PackageMojo mojo = new PackageMojo();

        // We test several things here:
        // - that ${xwikiDataDir} is going to be replaced
        // - that $XWIKI_OPTS, $XWIKI_DATA_DIR and $XWIKI_PID are not going to be modified
        // - that $! is not going to be modified either
        String content = ""
            + "# Location where XWiki stores generated data and where database files are.\n"
            + "XWIKI_DATA_DIR=${xwikiDataDir}\n"
            + "XWIKI_OPTS=\"$XWIKI_OPTS -Dxwiki.data.dir=$XWIKI_DATA_DIR\"\n"
            + "XWIKI_PID=$!\n"
            + "whatever";
        String expected = ""
            + "# Location where XWiki stores generated data and where database files are.\n"
            + "XWIKI_DATA_DIR=/some/path\n"
            + "XWIKI_OPTS=\"$XWIKI_OPTS -Dxwiki.data.dir=$XWIKI_DATA_DIR\"\n"
            + "XWIKI_PID=$!\n"
            + "whatever";
        assertEquals(expected, mojo.replaceProperty(content, context));
    }
}
