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

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Create or update the Crypto.SignatureClass document.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("Crypto.SignatureClass")
@Singleton
public class SignatureClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Default constructor.
     */
    public SignatureClassDocumentInitializer()
    {
        super(DefaultSignatureStore.SIGNATURECLASS);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addTextField(DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE, "Reference", 64);
        xclass.addTextAreaField(DefaultSignatureStore.SIGNATURECLASS_PROP_SIGNATURE, "Signature", 64, 10,
            TextAreaClass.EditorType.PURE_TEXT, TextAreaClass.ContentType.PURE_TEXT);
    }
}
