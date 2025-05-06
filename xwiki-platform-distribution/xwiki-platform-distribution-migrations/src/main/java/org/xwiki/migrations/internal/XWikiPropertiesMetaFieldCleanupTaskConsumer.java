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
package org.xwiki.migrations.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.index.IndexException;
import org.xwiki.index.TaskConsumer;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static com.xpn.xwiki.internal.mandatory.XWikiPreferencesDocumentInitializer.LOCAL_REFERENCE;

/**
 * Remove the content of the {@code meta} field from {@code XWiki.XWikiProperties} when requested by
 * {@link R170400000XWIKI23160DataMigration}. Note that this task is internal and shouldn't be used outside of
 * {@link R170400000XWIKI23160DataMigration}.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named(XWikiPropertiesMetaFieldCleanupTaskConsumer.TASK_NAME)
@Singleton
public class XWikiPropertiesMetaFieldCleanupTaskConsumer implements TaskConsumer
{
    static final String TASK_NAME = "xwiki-properties-meta-field-cleanup";

    private static final String META_FIELD = "meta";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Override
    public void consume(DocumentReference documentReference, String version) throws IndexException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        XWikiDocument document;
        try {
            document = wiki.getDocument(documentReference, context).clone();
        } catch (XWikiException e) {
            throw new IndexException("Unable to retrieve document [%s]".formatted(documentReference), e);
        }

        if (document != null) {
            BaseObject xObject = document.getXObject(LOCAL_REFERENCE);

            if (xObject != null && StringUtils.isNotEmpty(xObject.getStringValue(META_FIELD))) {
                xObject.setStringValue(META_FIELD, "");
                try {
                    wiki.saveDocument(document,
                        "[UPGRADE] empty field [%s] because it matches the default values".formatted(META_FIELD),
                        context);
                } catch (XWikiException e) {
                    throw new IndexException("Unable to save document [%s]".formatted(documentReference), e);
                }
            }
        }
    }
}
