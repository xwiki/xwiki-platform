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
package org.xwiki.extension.security;

import java.util.Objects;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;

import static java.lang.Boolean.TRUE;
import static org.xwiki.extension.security.internal.configuration.DocConfigurationSource.XOBJECT_REFERENCE;

/**
 * Initialize the {@code XWiki.Extension.Security.Code.Config} document and make it hidden.
 *
 * @version $Id$
 * @since 15.9RC1
 * @since 15.5.3
 */
@Component
@Singleton
@Named("XWiki.Extension.Security.Code.Config")
public class ExtensionSecurityConfigMandatoryDocumentInitializer implements MandatoryDocumentInitializer
{
    @Override
    public EntityReference getDocumentReference()
    {
        return XOBJECT_REFERENCE;
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean updated = false;
        if (!Objects.equals(TRUE, document.isHidden())) {
            document.setHidden(true);
            updated = true;
        }
        return updated;
    }
}
