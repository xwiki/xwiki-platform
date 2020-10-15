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
package org.xwiki.extension.index.internal;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.component.internal.WikiDeletedListener;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.extension.internal.converter.ExtensionAuthorConverter;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.search.solr.test.SolrComponentList;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SolrEventStore}, {@link EventsSolrCoreInitializer} and {@link WikiDeletedListener}.
 * 
 * @version $Id$
 */
@ComponentTest
@ComponentList({ExtensionIndexStore.class, ExtensionIndexSolrCoreInitializer.class, ExtensionIdConverter.class,
    ExtensionAuthorConverter.class})
@ReferenceComponentList
@SolrComponentList
class ExtensionIndexStoreTest
{
    @XWikiTempDir
    private File permanentDirectory;

    private ConfigurationSource mockXWikiProperties;

    private Environment mockEnvironment;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private ExtensionIndexStore indexStore;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockXWikiProperties =
            this.componentManager.registerMockComponent(ConfigurationSource.class, "xwikiproperties");
        this.mockEnvironment = this.componentManager.registerMockComponent(Environment.class);
        when(this.mockXWikiProperties.getProperty(anyString(), anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(1);
            }
        });

        when(this.mockEnvironment.getPermanentDirectory()).thenReturn(this.permanentDirectory);
        FileUtils.deleteDirectory(this.permanentDirectory);
        this.permanentDirectory.mkdirs();
    }

    // Tests

}
