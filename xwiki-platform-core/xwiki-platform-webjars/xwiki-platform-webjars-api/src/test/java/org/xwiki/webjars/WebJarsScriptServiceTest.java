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
package org.xwiki.webjars;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstalledExtension;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.webjars.internal.WebJarsResourceReference;
import org.xwiki.webjars.script.WebJarsScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.webjars.script.WebJarsScriptService}.
 *
 * @version $Id$
 * @since 6.0M1
 */
public class WebJarsScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<WebJarsScriptService> mocker =
        new MockitoComponentMockingRule<>(WebJarsScriptService.class);

    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> serializer;

    @Before
    public void setUp() throws Exception
    {
       this.serializer = this.mocker.getInstance(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
           ResourceReference.class, ExtendedURL.class));
    }

    @Test
    public void computeURLWithVersion() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        WebJarsResourceReference resourceReference = new WebJarsResourceReference("wiki:math",
            Arrays.asList("ang:ular", "2.1.11", "angular.css"));
        // Test that colon is not interpreted as groupId/artifactId separator (for backwards compatibility).
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "ang:ular", "2.1.11", "angular.css")));

        assertEquals("/xwiki/ang%3Aular/2.1.11/angular.css",
            this.mocker.getComponentUnderTest().url("ang:ular/2.1.11/angular.css"));
    }

    @Test
    public void computeURLWithoutVersion() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        InstalledExtensionRepository installedExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class);
        InstalledExtension extension = mock(InstalledExtension.class);
        when(installedExtensionRepository.getInstalledExtension("org.webjars:angular", "wiki:math")).thenReturn(
            extension);
        when(extension.getId()).thenReturn(new ExtensionId("bar", "2.1.11"));

        WebJarsResourceReference resourceReference = new WebJarsResourceReference("wiki:math",
            Arrays.asList("angular", "2.1.11", "angular.css"));
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "angular", "2.1.11", "angular.css")));

        assertEquals("/xwiki/angular/2.1.11/angular.css",
            this.mocker.getComponentUnderTest().url("angular", "angular.css"));
    }

    @Test
    public void computeURLWithoutVersionAndNoExtensionMatchingWebJarId() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        WebJarsResourceReference resourceReference =
            new WebJarsResourceReference("wiki:math", Arrays.asList("angular", "angular.css"));
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "angular", "angular.css")));

        assertEquals("/xwiki/angular/angular.css", this.mocker.getComponentUnderTest().url("angular", "angular.css"));
    }

    @Test
    public void computeURLWithParameters() throws Exception
    {
        WebJarsResourceReference resourceReference = new WebJarsResourceReference("wiki:wiki",
            Arrays.asList("angular", "2.1.11", "angular.js"));
        resourceReference.addParameter("evaluate", "true");
        resourceReference.addParameter("list", Arrays.asList("one", "two"));
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("xwiki", "angular", "2.1.11", "angular.js"),
            resourceReference.getParameters());
        when(this.serializer.serialize(resourceReference)).thenReturn(extendedURL);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("version", "2.1.11");
        params.put("evaluate", true);
        params.put("list", new String[] {"one", "two"});
        assertEquals("/xwiki/angular/2.1.11/angular.js?evaluate=true&list=one&list=two",
            this.mocker.getComponentUnderTest().url("angular", "wiki:wiki", "angular.js", params));
    }

    @Test
    public void computeJavaScriptURLWithSuffixAndNoParameters() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        WebJarsResourceReference resourceReference =
            new WebJarsResourceReference("wiki:math", Arrays.asList("angular", "angular.js"));
        resourceReference.addParameter("r", "1");
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "angular", "angular.js"), resourceReference.getParameters()));

        assertEquals("/xwiki/angular/angular.js?r=1", this.mocker.getComponentUnderTest().url("angular", "angular.js"));
    }

    @Test
    public void computeURLForBackwardCompatibilityWhenWikiIsSpecifiedAsParameter() throws Exception
    {
        InstalledExtensionRepository installedExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class);
        InstalledExtension extension = mock(InstalledExtension.class);
        when(installedExtensionRepository.getInstalledExtension("org.webjars:angular", "wiki:math")).thenReturn(
            extension);
        when(extension.getId()).thenReturn(new ExtensionId("bar", "2.1.11"));

        WebJarsResourceReference resourceReference = new WebJarsResourceReference("wiki:math",
            Arrays.asList("angular", "2.1.11", "angular.css"));
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "angular", "2.1.11", "angular.css")));

        assertEquals("/xwiki/angular/2.1.11/angular.css",
            this.mocker.getComponentUnderTest().url("angular", "angular.css",
                Collections.singletonMap("wiki", "math")));
    }

    @Test
    public void computeURLWithParametersAndWikiIsNotSpecifiedInParameter() throws Exception
    {
        WikiDescriptorManager wikiDescriptorManager = this.mocker.getInstance(WikiDescriptorManager.class);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("math");

        InstalledExtensionRepository installedExtensionRepository =
            this.mocker.getInstance(InstalledExtensionRepository.class);
        InstalledExtension extension = mock(InstalledExtension.class);
        when(installedExtensionRepository.getInstalledExtension("org.webjars:angular", "wiki:math")).thenReturn(
            extension);
        when(extension.getId()).thenReturn(new ExtensionId("bar", "2.1.11"));

        WebJarsResourceReference resourceReference = new WebJarsResourceReference("wiki:math",
            Arrays.asList("angular", "2.1.11", "angular.css"));
        when(this.serializer.serialize(resourceReference)).thenReturn(
            new ExtendedURL(Arrays.asList("xwiki", "angular", "2.1.11", "angular.css")));

        assertEquals("/xwiki/angular/2.1.11/angular.css",
            this.mocker.getComponentUnderTest().url("angular", "angular.css", Collections.<String, Object>emptyMap()));
    }
}
