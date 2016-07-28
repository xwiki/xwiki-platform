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
package org.xwiki.ratings.internal;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.ratings.RatingsConfiguration;
import org.xwiki.ratings.RatingsManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
/**
 * Unit tests for {@link org.xwiki.ratings.internal.DefaultRatingsConfiguration}.
 *
 * @version $Id$
 * @since 8.1M2
 */
public class DefaultRatingsConfigurationTest
{
    @Rule
    public MockitoComponentMockingRule<RatingsConfiguration> mocker = 
        new MockitoComponentMockingRule<RatingsConfiguration>(DefaultRatingsConfiguration.class);
    
    private XWikiContext xcontext;
    
    @Before
    public void configure() throws Exception
    {
        xcontext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);
        
        XWiki wiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(wiki);
    }
    
    @Test
    public void getSpaceConfigurationDocument() throws Exception
    {        
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        
        XWikiDocument spaceConfigurationDocument = mock(XWikiDocument.class, "Space Configuration Document");
        
        DocumentReference configurationDocumentReference = new DocumentReference("wiki", "Space1", "WebPreferences");
        when(xcontext.getWiki().getDocument((EntityReference)configurationDocumentReference, xcontext)).thenReturn(spaceConfigurationDocument);
        
        BaseObject configurationObject = mock(BaseObject.class);
        when(spaceConfigurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE)).thenReturn(configurationObject);
              
        assertEquals(spaceConfigurationDocument, mocker.getComponentUnderTest().getConfigurationDocument(documentReference));    
    }
    
    @Test
    public void getWikiConfigurationDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        
        XWikiDocument wikiConfigurationDocument = mock(XWikiDocument.class, "Wiki Configuration Document");
        when(xcontext.getWiki().getDocument(RatingsManager.RATINGS_CONFIG_GLOBAL_REFERENCE, xcontext)).thenReturn(wikiConfigurationDocument);
        
        assertEquals(wikiConfigurationDocument, mocker.getComponentUnderTest().getConfigurationDocument(documentReference));
    }
    
    @Test
    public void getConfigurationParameter() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        
        XWikiDocument spaceConfigurationDocument = mock(XWikiDocument.class, "Space Configuration Document");
        
        DocumentReference configurationDocumentReference = new DocumentReference("wiki", "Space1", "WebPreferences");
        when(xcontext.getWiki().getDocument((EntityReference)configurationDocumentReference, xcontext)).thenReturn(spaceConfigurationDocument);
        
        BaseObject configurationObject = mock(BaseObject.class);
        when(spaceConfigurationDocument.getXObject(RatingsManager.RATINGS_CONFIG_CLASSREFERENCE)).thenReturn(configurationObject);
        
        BaseProperty displayRatings = mock(BaseProperty.class);
        when(configurationObject.get("displayRatings")).thenReturn(displayRatings);
        when(displayRatings.getValue()).thenReturn("1");
        
        assertEquals("1", mocker.getComponentUnderTest().getConfigurationParameter(documentReference, "displayRatings", "1"));
    }
    
    @Test
    public void getConfigurationParameterDefaultValue() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Space1", "Space2"), "Page");
        
        XWikiDocument spaceConfigurationDocument = mock(XWikiDocument.class, "Space Configuration Document");
        
        DocumentReference configurationDocumentReference = new DocumentReference("wiki", "Space1", "WebPreferences");
        when(xcontext.getWiki().getDocument((EntityReference)configurationDocumentReference, xcontext)).thenReturn(spaceConfigurationDocument);
        
        assertEquals("1", mocker.getComponentUnderTest().getConfigurationParameter(documentReference, "displayRatings", "1"));
    }
}