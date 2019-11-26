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
package com.xpn.xwiki.internal.display;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.display.internal.DocumentContentAsyncParser;
import org.xwiki.rendering.async.internal.AsyncProperties;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.DocumentAsyncClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of {@link DocumentContentAsyncParser} for {@link XWikiDocument} instances.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class XWikiDocumentContentAsyncParser implements DocumentContentAsyncParser
{
    @Override
    public AsyncProperties getAsyncProperties(DocumentModelBridge document)
    {
        if (document instanceof XWikiDocument) {
            XWikiDocument xdocument = (XWikiDocument) document;

            BaseObject asyncObject = xdocument.getXObject(DocumentAsyncClassDocumentInitializer.CLASS_REFERENCE);
            if (asyncObject != null) {
                boolean asyncAllowed =
                    asyncObject.getIntValue(DocumentAsyncClassDocumentInitializer.XPROPERTY_ASYNC_ENABLED) == 1;
                boolean cacheAllowed =
                    asyncObject.getIntValue(DocumentAsyncClassDocumentInitializer.XPROPERTY_ASYNC_CACHED) == 1;

                Set<String> contextElements;
                if (asyncAllowed || cacheAllowed) {
                    contextElements = new HashSet<>(
                        asyncObject.getListValue(DocumentAsyncClassDocumentInitializer.XPROPERTY_ASYNC_CONTEXT));
                } else {
                    contextElements = null;
                }

                return new AsyncProperties(asyncAllowed, cacheAllowed, contextElements);
            }
        }

        return new AsyncProperties();
    }

}
