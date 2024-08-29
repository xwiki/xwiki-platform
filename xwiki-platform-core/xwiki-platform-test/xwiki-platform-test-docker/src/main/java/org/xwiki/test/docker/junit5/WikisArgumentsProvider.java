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
package org.xwiki.test.docker.junit5;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.internal.junit5.DockerTestUtils;
import org.xwiki.test.docker.internal.junit5.ExtensionInstaller;
import org.xwiki.test.docker.internal.junit5.WikiCreator;
import org.xwiki.test.ui.TestUtils;

/**
 * @version $Id$
 * @since 14.5
 */
class WikisArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<WikisSource>
{
    private static final String MAINWIKI = "xwiki";

    private List<WikiReference> wikis;

    private Set<ExtensionId> extensions;

    @Override
    public void accept(WikisSource source)
    {
        this.wikis = new ArrayList<>(source.value());

        // Add main wiki
        if (source.mainWiki()) {
            this.wikis.add(new WikiReference(MAINWIKI));
        }

        // Add subwikis
        for (int i = 0; i < source.value(); ++i) {
            this.wikis.add(new WikiReference("wiki" + (i + 1)));
        }

        // Resolve extensions
        this.extensions = new LinkedHashSet<>(source.extensions().length);
        for (String coordinate : source.extensions()) {
            this.extensions.add(ExtensionIdConverter.toExtensionId(coordinate, null));
        }
    }

    private void initWiki(WikiReference wiki, ExtensionContext context) throws Exception
    {
        WikiCreator wikiCreator = DockerTestUtils.getWikiCreator(context);
        wikiCreator.createWiki(TestUtils.SUPER_ADMIN_CREDENTIALS, wiki.getName(), false);
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception
    {
        // Create missing wikis
        for (WikiReference wiki : this.wikis) {
            if (!wiki.getName().equals(MAINWIKI)) {
                initWiki(wiki, context);
            }
        }

        // Install required extensions
        if (!this.extensions.isEmpty()) {
            List<String> namespaces = this.wikis.stream().filter(w -> !w.getName().equals(MAINWIKI))
                .map(w -> "wiki:" + w.getName()).toList();

            ExtensionInstaller extensionInstaller = DockerTestUtils.getExtensionInstaller(context);
            extensionInstaller.installExtensions(this.extensions, TestUtils.SUPER_ADMIN_CREDENTIALS,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), namespaces, false);
        }

        // Return initialized wikis
        return this.wikis.stream().map(Arguments::of);
    }
}
