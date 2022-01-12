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
package org.xwiki.attachment.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * TODO: document me.
 *
 * @version $Id$
 * @since X.Y.X
 */
@Component
@Named("Attachment.Code.RedirectAttachmentClass")
@Singleton
public class RedirectAttachmentClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * The local reference of the redirect attachment class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(XWiki.SYSTEM_SPACE, "RedirectClass");

    public static final String SOURCE_NAME_FIELD = "sourceName";

    public static final String TARGET_LOCATION_FIELD = "targetLocation";

    public static final String TARGET_NAME_FIELD = "targetName";

    /**
     * Default constructor.
     */
    public RedirectAttachmentClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        // TODO: What's the max length of an attachment name?
        xclass.addTextField(SOURCE_NAME_FIELD, "Source Name", Integer.MAX_VALUE);
        xclass.addTextField(TARGET_LOCATION_FIELD, "Target Location", Integer.MAX_VALUE);
        xclass.addTextField(TARGET_NAME_FIELD, "Target Name", Integer.MAX_VALUE);
    }
}
