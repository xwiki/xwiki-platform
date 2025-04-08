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
package org.xwiki.configuration.internal.test;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Utility to test all extensions of {@link AbstractDocumentConfigurationSource}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
public abstract class AbstractTestDocumentConfigurationSource
{
    @InjectMockitoOldcore
    protected MockitoOldcore oldcore;

    @InjectComponentManager
    protected MockitoComponentManager componentManager;

    @MockComponent
    protected CacheManager cacheManager;

    protected static final String CURRENT_WIKI = "currentwiki";

    protected Cache<Object> mockCache;

    protected ConverterManager mockConverter;

    protected abstract ConfigurationSource getConfigurationSource();

    protected abstract LocalDocumentReference getClassReference();

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        this.mockCache = mock();
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenReturn(this.mockCache);
    }

    public void before() throws Exception
    {
        this.mockConverter = this.componentManager.getInstance(ConverterManager.class);

        when(this.mockConverter.convert(any(Type.class), any(Object.class))).then(
            invocation -> invocation.getArguments()[1]);

        this.oldcore.getXWikiContext().setWikiId(CURRENT_WIKI);
    }

    protected void setupBaseObject(DocumentReference documentReference, Consumer<BaseObject> consumer)
        throws XWikiException
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument document = this.oldcore.getXWikiContext().getWiki().getDocument(documentReference, xcontext);
        LocalDocumentReference classReference = getClassReference();
        BaseObject baseObject = document.getXObject(classReference);
        if (baseObject == null) {
            baseObject = new BaseObject();
            baseObject.setXClassReference(classReference);
            document.addXObject(baseObject);
        }
        consumer.accept(baseObject);
        xcontext.getWiki().saveDocument(document, xcontext);
    }

    protected void removeConfigObject(DocumentReference documentReference) throws XWikiException
    {
        XWikiContext xcontext = this.oldcore.getXWikiContext();
        XWikiDocument document = xcontext.getWiki().getDocument(documentReference, xcontext);
        LocalDocumentReference classReference = getClassReference();
        document.removeXObjects(classReference);
        xcontext.getWiki().saveDocument(document, xcontext);
    }

    protected void setStringProperty(DocumentReference documentReference, String propertyName, String propertyValue)
        throws XWikiException
    {
        setupBaseObject(documentReference, baseObject -> {
            baseObject.setStringValue(propertyName, propertyValue);
        });
    }

    protected void resetCache()
    {
        this.mockCache = mock();
    }

    protected void setCache(String property, Object value)
    {
        when(this.mockCache.get(property)).thenReturn(value);
    }
}
