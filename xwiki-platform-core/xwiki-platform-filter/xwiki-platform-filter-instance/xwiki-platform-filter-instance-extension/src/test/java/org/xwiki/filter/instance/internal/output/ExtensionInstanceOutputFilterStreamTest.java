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
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.EmptyExtension;
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

/**
 * Validate {@link ExtensionInstanceOutputProperties}.
 * 
 * @version $Id$
 */
@AllComponents
public class ExtensionInstanceOutputFilterStreamTest
{
    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule();

    private BeanInputFilterStreamFactory<FilterXMLInputProperties> xmlInputFilterStreamFactory;

    private BeanOutputFilterStreamFactory<InstanceOutputProperties> outputFilterStreamFactory;

    private InstalledExtensionRepository installedExtensionRepository;

    private ExtensionRepositoryManager extensionRepositoryMock;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.extensionRepositoryMock =
            this.repositoryUtil.getComponentManager().registerMockComponent(ExtensionRepositoryManager.class);
        doThrow(ResolveException.class).when(this.extensionRepositoryMock).resolve((ExtensionId) any());
    }

    // Tests

    @Before
    public void before() throws Exception
    {
        this.xmlInputFilterStreamFactory =
            this.repositoryUtil.getComponentManager().getInstance(InputFilterStreamFactory.class,
                FilterStreamType.FILTER_XML.serialize());
        this.outputFilterStreamFactory =
            this.repositoryUtil.getComponentManager().getInstance(OutputFilterStreamFactory.class,
                FilterStreamType.XWIKI_INSTANCE.serialize());

        this.installedExtensionRepository =
            this.repositoryUtil.getComponentManager().getInstance(InstalledExtensionRepository.class);
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
    public void testImportExtensionId() throws FilterException, ResolveException, UnsupportedEncodingException
    {
        doReturn(new EmptyExtension(new ExtensionId("extensionid1", "version1"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid1", "version1"));
        doReturn(new EmptyExtension(new ExtensionId("extensionid2", "version2"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid2", "version2"));

        importFromXML("extensionid");

        Assert.assertNull(this.installedExtensionRepository.getInstalledExtension("extensionid1", null));
        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid1", "namespace1"));
        Assert.assertEquals("version1",
            this.installedExtensionRepository.getInstalledExtension("extensionid1", "namespace1").getId().getVersion()
                .getValue());

        Assert.assertNull(this.installedExtensionRepository.getInstalledExtension("extensionid2", null));
        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid2", "wiki:wiki2"));
        Assert.assertEquals("version2",
            this.installedExtensionRepository.getInstalledExtension("extensionid2", "wiki:wiki2").getId().getVersion()
                .getValue());
    }

    @Test
    public void testImportExtensionIdWithoutNamespace() throws FilterException, ResolveException,
        UnsupportedEncodingException
    {
        doReturn(new EmptyExtension(new ExtensionId("extensionid", "version"), "test")).when(
            this.extensionRepositoryMock).resolve(new ExtensionId("extensionid", "version"));

        importFromXML("extensionidwithoutnamespace");

        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", null));
        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace"));
        Assert.assertEquals("version",
            this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace").getId().getVersion()
                .getValue());
    }
}
