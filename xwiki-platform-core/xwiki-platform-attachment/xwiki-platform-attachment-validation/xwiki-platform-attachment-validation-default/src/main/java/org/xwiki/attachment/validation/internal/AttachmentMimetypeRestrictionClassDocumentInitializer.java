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
package org.xwiki.attachment.validation.internal;

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

import static com.xpn.xwiki.objects.classes.ListClass.DISPLAYTYPE_INPUT;

/**
 * Initialize the {@code XWiki.Attachment.Validation.Code.AttachmentMimetypeRestrictionClass} XClass. This class is used
 * to configure restrictions on the kind of attachments that can be uploaded.
 *
 * @version $Id$
 * @since 14.10
 */
@Component
@Named(AttachmentMimetypeRestrictionClassDocumentInitializer.HINT)
@Singleton
public class AttachmentMimetypeRestrictionClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Hint for this class.
     */
    public static final String HINT = "XWiki.Attachment.Validation.Code.AttachmentMimetypeRestrictionClass";

    /**
     * The local reference of the redirect attachment class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(List.of("XWiki", "Attachment", "Validation", "Code"),
            "AttachmentMimetypeRestrictionClass");

    /**
     * Field name for the allowed mime types.
     */
    public static final String ALLOWED_MIMETYPES_FIELD = "allowedMimetypes";

    /**
     * Field name for the denied mime types.
     */
    public static final String BLOCKED_MIMETYPES_FIELD = "blockedMimetypes";

    /**
     * Default constructor.
     */
    public AttachmentMimetypeRestrictionClassDocumentInitializer()
    {
        super(REFERENCE);
    }

    @Override
    protected void createClass(BaseClass xclass)
    {
        xclass.addStaticListField(ALLOWED_MIMETYPES_FIELD, "Allowed Mimetypes", 1, true, "", DISPLAYTYPE_INPUT);
        xclass.addStaticListField(BLOCKED_MIMETYPES_FIELD, "Blocked Mimetypes", 1, true, "", DISPLAYTYPE_INPUT);
    }
}
