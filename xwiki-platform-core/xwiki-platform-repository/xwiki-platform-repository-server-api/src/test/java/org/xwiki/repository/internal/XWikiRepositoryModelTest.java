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
package org.xwiki.repository.internal;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.extension.internal.ExtensionFactory;
import org.xwiki.extension.repository.DefaultExtensionRepositoryDescriptor;
import org.xwiki.extension.repository.ExtensionRepositoryDescriptor;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link XWikiRepositoryModel}.
 *
 * @version $Id$
 */
class XWikiRepositoryModelTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private final ExtensionFactory factory = new ExtensionFactory();

    @Test
    void toStringWithoutProperties() throws URISyntaxException
    {
        ExtensionRepositoryDescriptor repository =
            new DefaultExtensionRepositoryDescriptor("id", "maven", new URI("https://host/path?query#fragment"));

        String repositoryString = XWikiRepositoryModel.toString(repository);

        assertEquals("id:maven:https://host/path?query#fragment", repositoryString);
        assertEquals(repository, XWikiRepositoryModel.toRepositoryDescriptor(repositoryString, this.factory));
    }

    @Test
    void toStringWithProperties() throws URISyntaxException
    {
        ExtensionRepositoryDescriptor repository = new DefaultExtensionRepositoryDescriptor("id", "maven",
            new URI("https://host/path"), Map.of("maven.snapshot", "true", "maven.release", "false",
                "special key:;=|,", "special value:;=|,", "empty", ""));

        String repositoryString = XWikiRepositoryModel.toString(repository);

        assertEquals("id:maven;empty=;maven.release=false;maven.snapshot=true"
            + ";special+key%3A%3B%3D%7C%2C=special+value%3A%3B%3D%7C%2C:https://host/path", repositoryString);
        assertEquals(repository, XWikiRepositoryModel.toRepositoryDescriptor(repositoryString, this.factory));
    }

    @Test
    void toStringNull()
    {
        assertNull(XWikiRepositoryModel.toString(null));
    }

    @Test
    void toRepositoryDescriptors() throws URISyntaxException
    {
        assertEquals(List.of(new DefaultExtensionRepositoryDescriptor("id1", "xwiki", new URI("https://host1")),
            new DefaultExtensionRepositoryDescriptor("", "maven", new URI("https://host2"),
                Map.of("maven.release", "false"))),
            XWikiRepositoryModel.toRepositoryDescriptors(
                List.of("id1:xwiki:https://host1", ":maven;maven.release=false:https://host2", "id3:maven:invalid uri"),
                this.factory));

        assertEquals("Failed to parse repository descriptor [id3:maven:invalid uri]. Root cause is "
            + "[URISyntaxException: Illegal character in path at index 7: invalid uri]",
            this.logCapture.getMessage(0));
    }
}
