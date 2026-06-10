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
package org.xwiki.filter.instance.internal.output;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.EmptyExtension;
import org.xwiki.extension.test.MockitoRepositoryUtilsExtension;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.filterxml.input.FilterXMLInputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultURLInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.output.ExtensionInstanceOutputProperties;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Validate {@link ExtensionInstanceOutputProperties}.
 *
 * @version $Id$
 */
@ComponentTest
@AllComponents
@ExtendWith(MockitoRepositoryUtilsExtension.class)
class ExtensionInstanceOutputFilterStreamTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private BeanInputFilterStreamFactory<FilterXMLInputProperties> xmlInputFilterStreamFactory;

    private BeanOutputFilterStreamFactory<InstanceOutputProperties> outputFilterStreamFactory;

    private InstalledExtensionRepository installedExtensionRepository;

    private ExtensionRepositoryManager extensionRepositoryMock;

    @AfterComponent
    void afterComponent() throws Exception
    {
        this.extensionRepositoryMock =
            this.componentManager.registerMockComponent(ExtensionRepositoryManager.class);
        doThrow(ResolveException.class).when(this.extensionRepositoryMock).resolve((ExtensionId) any());
    }

    // Tests

    @BeforeEach
    void before() throws Exception
    {
        this.xmlInputFilterStreamFactory = this.componentManager.getInstance(InputFilterStreamFactory.class,
            FilterStreamType.FILTER_XML.serialize());
        this.outputFilterStreamFactory = this.componentManager.getInstance(OutputFilterStreamFactory.class,
            FilterStreamType.XWIKI_INSTANCE.serialize());

        this.installedExtensionRepository = this.componentManager.getInstance(InstalledExtensionRepository.class);
    }

    protected void importFromXML(String resource) throws FilterException
    {
        importFromXML(resource, null);
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties) throws FilterException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceOutputProperties();
            instanceProperties.setVerbose(false);
        }

        OutputFilterStream outputFilterStream =
            this.outputFilterStreamFactory.createOutputFilterStream(instanceProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        FilterXMLInputProperties properties = new FilterXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputFilterStream inputFilterStream = this.xmlInputFilterStreamFactory.createInputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        try {
            inputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close input wiki stream", e);
        }
        try {
            outputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close output wiki stream", e);
        }
    }

    // tests

    @Test
    void importExtensionId() throws FilterException, ResolveException
    {
        doReturn(new EmptyExtension(new ExtensionId("extensionid1", "version1"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid1", "version1"));
        doReturn(new EmptyExtension(new ExtensionId("extensionid2", "version2"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid2", "version2"));

        importFromXML("extensionid");

        assertNull(this.installedExtensionRepository.getInstalledExtension("extensionid1", null));
        assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid1", "namespace1"));
        assertEquals("version1",
            this.installedExtensionRepository.getInstalledExtension("extensionid1", "namespace1").getId().getVersion()
                .getValue());

        assertNull(this.installedExtensionRepository.getInstalledExtension("extensionid2", null));
        assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid2", "wiki:wiki2"));
        assertEquals("version2",
            this.installedExtensionRepository.getInstalledExtension("extensionid2", "wiki:wiki2").getId().getVersion()
                .getValue());
    }

    @Test
    void importExtensionIdWithoutNamespace() throws FilterException, ResolveException
    {
        doReturn(new EmptyExtension(new ExtensionId("extensionid", "version"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid", "version"));

        importFromXML("extensionidwithoutnamespace");

        assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", null));
        assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace"));
        assertEquals("version",
            this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace").getId().getVersion()
                .getValue());
    }
}
