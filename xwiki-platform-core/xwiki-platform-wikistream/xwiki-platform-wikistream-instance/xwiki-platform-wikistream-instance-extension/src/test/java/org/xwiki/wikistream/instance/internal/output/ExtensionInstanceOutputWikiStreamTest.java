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
package org.xwiki.wikistream.instance.internal.output;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.test.EmptyExtension;
import org.xwiki.extension.test.MockitoRepositoryUtilsRule;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.output.ExtensionInstanceOutputProperties;
import org.xwiki.wikistream.instance.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.input.WikiXMLInputProperties;

/**
 * Validate {@link ExtensionInstanceOutputProperties}.
 * 
 * @version $Id$
 */
@AllComponents
public class ExtensionInstanceOutputWikiStreamTest
{
    @Rule
    public MockitoRepositoryUtilsRule repositoryUtil = new MockitoRepositoryUtilsRule();

    private BeanInputWikiStreamFactory<WikiXMLInputProperties> xmlInputWikiStreamFactory;

    private BeanOutputWikiStreamFactory<InstanceOutputProperties> outputWikiStreamFactory;

    private InstalledExtensionRepository installedExtensionRepository;

    private ExtensionRepositoryManager extensionRepositoryMock;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.extensionRepositoryMock =
            this.repositoryUtil.getComponentManager().registerMockComponent(ExtensionRepositoryManager.class);
        Mockito.doThrow(ResolveException.class).when(this.extensionRepositoryMock).resolve(Mockito.<ExtensionId> any());
    }

    // Tests

    @Before
    public void before() throws Exception
    {
        this.xmlInputWikiStreamFactory =
            this.repositoryUtil.getComponentManager().getInstance(InputWikiStreamFactory.class,
                WikiStreamType.WIKI_XML.serialize());
        this.outputWikiStreamFactory =
            this.repositoryUtil.getComponentManager().getInstance(OutputWikiStreamFactory.class,
                WikiStreamType.XWIKI_INSTANCE.serialize());

        this.installedExtensionRepository =
            this.repositoryUtil.getComponentManager().getInstance(InstalledExtensionRepository.class);
    }

    protected void importFromXML(String resource) throws WikiStreamException
    {
        importFromXML(resource, null);
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties)
        throws WikiStreamException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceOutputProperties();
            instanceProperties.setVerbose(false);
        }

        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.createOutputWikiStream(instanceProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.xmlInputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());

        try {
            inputWikiStream.close();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close input wiki stream", e);
        }
        try {
            outputWikiStream.close();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close output wiki stream", e);
        }
    }

    // tests

    @Test
    public void testImportExtensionId() throws WikiStreamException, ParseException, ResolveException,
        UnsupportedEncodingException
    {
        Mockito.doReturn(new EmptyExtension(new ExtensionId("extensionid1", "version1"), "test"))
            .when(this.extensionRepositoryMock).resolve(new ExtensionId("extensionid1", "version1"));
        Mockito.doReturn(new EmptyExtension(new ExtensionId("extensionid2", "version2"), "test"))
            .when(this.extensionRepositoryMock).resolve(new ExtensionId("extensionid2", "version2"));

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
    public void testImportExtensionIdWithoutNamespace() throws WikiStreamException, ParseException, ResolveException,
        UnsupportedEncodingException
    {
        Mockito.doReturn(new EmptyExtension(new ExtensionId("extensionid", "version"), "test"))
            .when(this.extensionRepositoryMock).resolve(new ExtensionId("extensionid", "version"));

        importFromXML("extensionidwithoutnamespace");

        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", null));
        Assert.assertNotNull(this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace"));
        Assert.assertEquals("version",
            this.installedExtensionRepository.getInstalledExtension("extensionid", "namespace").getId().getVersion()
                .getValue());
    }
}
