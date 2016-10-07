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
package org.xwiki.repository.test;

import java.io.File;

import org.xwiki.extension.AbstractRemoteExtension;
import org.xwiki.extension.ExtensionId;

/**
 * @version $Id$
 * @since 4.2M1
 */
public class TestExtension extends AbstractRemoteExtension
{
    public final static String FOLDERNAME_EXETENSIONS = "target/extensions/";

    protected TestExtension(ExtensionId id, String type, File file)
    {
        super(null, id, type);

        setName(id.getId());

        setFile(new TestExtensionFile(file));
    }

    @Override
    public TestExtensionFile getFile()
    {
        return (TestExtensionFile) super.getFile();
    }
}
