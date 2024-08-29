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

import com.xpn.xwiki.doc.AbstractMandatoryClassInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;

import static java.util.Arrays.asList;

/**
 * Initialize the {@code Attachment.Code.RedirectAttachmentClass} XClass. This class is used to save the new location of
 * moved attachments when a redirection must be preserved.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Named(RedirectAttachmentClassDocumentInitializer.HINT)
@Singleton
public class RedirectAttachmentClassDocumentInitializer extends AbstractMandatoryClassInitializer
{
    /**
     * Hint for this class.
     */
    public static final String HINT = "Attachment.Code.RedirectAttachmentClass";

    /**
     * The local reference of the redirect attachment class.
     */
    public static final LocalDocumentReference REFERENCE =
        new LocalDocumentReference(asList("Attachment", "Code"), "RedirectClass");

    /**
     * The name of the field containing the name of the attachment before the move.
     */
    public static final String SOURCE_NAME_FIELD = "sourceName";

    /**
     * The name of the field storing the location of the attachment after the move.
     */
    public static final String TARGET_LOCATION_FIELD = "targetLocation";

    /**
     * The name of the field storing the name of the attachment after the move.
     */
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
        xclass.addTextField(SOURCE_NAME_FIELD, "Source Name", 30);
        xclass.addTextField(TARGET_LOCATION_FIELD, "Target Location", 30);
        xclass.addTextField(TARGET_NAME_FIELD, "Target Name", 30);
    }
}
