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
package org.xwiki.edit.internal;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.configuration.internal.AbstractDocumentConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Base class for configuration sources that read the editor bindings from {@code EditorBindingClass} objects.
 * 
 * @version $Id$
 * @since 8.2RC1
 */
public abstract class AbstractEditorBindingsSource extends AbstractDocumentConfigurationSource
{
    /**
     * The local reference of the class used to bind editors to data types.
     */
    private static final LocalDocumentReference CLASS_REFERENCE = new LocalDocumentReference("XWiki",
        "EditorBindingClass");

    @Override
    protected LocalDocumentReference getClassReference()
    {
        return CLASS_REFERENCE;
    }

    @Override
    protected Object getBaseProperty(String propertyName, boolean text) throws XWikiException
    {
        for (BaseObject baseObject : getBaseObjects()) {
            String dataType = baseObject.getStringValue("dataType");
            if (Objects.equals(dataType, propertyName)) {
                String roleHint = baseObject.getStringValue("roleHint");
                if (!StringUtils.isEmpty(roleHint)) {
                    return roleHint;
                }
            }
        }
        return null;
    }

    private List<BaseObject> getBaseObjects() throws XWikiException
    {
        DocumentReference documentReference = getFailsafeDocumentReference();
        LocalDocumentReference classReference = getFailsafeClassReference();

        if (documentReference != null && classReference != null) {
            XWikiContext xcontext = this.xcontextProvider.get();
            XWikiDocument document = xcontext.getWiki().getDocument(getDocumentReference(), xcontext);
            List<BaseObject> objects = document.getXObjects(classReference);
            if (objects != null) {
                return objects;
            }
        }

        return Collections.emptyList();
    }
}
