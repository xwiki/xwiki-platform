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
package org.xwiki.crypto.store.wiki.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Create or update the Crypto.PrivateKeyClass document.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("Crypto.PrivateKeyClass")
@Singleton
public class PrivateKeyClassDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    /**
     * Default constructor.
     */
    public PrivateKeyClassDocumentInitializer()
    {
        super(X509KeyWikiStore.PRIVATEKEYCLASS);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        BaseClass bclass = document.getXClass();

        boolean needsUpdate = bclass.addTextAreaField(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, "Private Key", 64, 10);

        needsUpdate |= setClassDocumentFields(document, "Private Key Class");

        return needsUpdate;
    }
}
