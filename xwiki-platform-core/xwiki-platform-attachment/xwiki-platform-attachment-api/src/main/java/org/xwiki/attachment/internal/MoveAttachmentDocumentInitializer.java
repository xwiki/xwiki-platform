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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.CONTENT_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.SCOPE_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.UI_EXTENSION_CLASS;

/**
 * Initializes a User Interface Extension (UIX) in a document. This UIX is used to initialize a configuration that can
 * be used by Javascript scripts.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Singleton
@Named("Attachment.Code.MoveAttachment")
public class MoveAttachmentDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    private static final String UIPX_ID = "org.xwiki.platform.template.header.after";

    private static final String UIX_ID = "org.xwiki.platform.attachment.header.after.move";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public MoveAttachmentDocumentInitializer()
    {
        super(new LocalDocumentReference(Arrays.asList("Attachment", "Code"), "MoveAttachment"));
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean superUpdated = super.updateDocument(document);

        List<BaseObject> uIExtensionObjects = document.getXObjects(UI_EXTENSION_CLASS);
        boolean exists = uIExtensionObjects.stream().anyMatch(object ->
            Objects.equals(object.getStringValue(EXTENSION_POINT_ID_PROPERTY), UIPX_ID)
                && Objects.equals(object.getStringValue(ID_PROPERTY), UIX_ID));
        if (!exists) {
            try {
                EntityReference uIExtensionClass = new EntityReference(UI_EXTENSION_CLASS);
                int idx = document.createXObject(uIExtensionClass, this.contextProvider.get());
                BaseObject xObject = document.getXObject(uIExtensionClass, idx);
                xObject.setStringValue(EXTENSION_POINT_ID_PROPERTY, UIPX_ID);
                xObject.setStringValue(ID_PROPERTY, UIX_ID);
                xObject.setStringValue(CONTENT_PROPERTY, "{{velocity}}\n"
                    + "{{html clean=\"false\"}}\n"
                    + "<script id=\"attachment-move-config\" type=\"application/json\">$jsontool.serialize({\n"
                    + "  'treeWebjar': $!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', "
                    + "'require-config.min.js', {'evaluate': true})"
                    + "})</script>\n"
                    + "{{/html}}\n"
                    + "{{/velocity}}");
                xObject.setStringValue(SCOPE_PROPERTY, "wiki");
            } catch (XWikiException e) {
                this.logger.error(
                    "Error while trying to initialize a [org.xwiki.platform.template.header.after] UIX in "
                        + "[Attachment.Code.MoveAttachment].", e);
            }
        }

        Boolean hidden = document.isHidden();
        if (Boolean.FALSE.equals(hidden)) {
            document.setHidden(true);
        }

        return superUpdated || !exists || !hidden;
    }
}
