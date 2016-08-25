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
package org.xwiki.mail.internal;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.AbstractMandatoryDocumentInitializer;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Create or update a {@code XWiki.Mail} class and document at startup. This class is used as a Mail Template.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@Component
@Named("XWiki.Mail")
@Singleton
public class MailMandatoryDocumentInitializer extends AbstractMandatoryDocumentInitializer
{
    private static final String SPACE = "XWiki";

    private static final String PAGE = "Mail";

    /**
     * Create/update the {@code XWiki.Mail} class and document.
     */
    public MailMandatoryDocumentInitializer()
    {
        super(SPACE, PAGE);
    }

    @Override
    public boolean updateDocument(XWikiDocument document)
    {
        boolean needsUpdate = false;

        BaseClass bclass = document.getXClass();

        needsUpdate |= bclass.addTextField("subject", "Subject", 40);
        needsUpdate |= bclass.addTextField("language", "Language", 5);
        needsUpdate |= bclass.addTextAreaField("text", "Text", 80, 15, TextAreaClass.EditorType.PURE_TEXT);
        needsUpdate |= bclass.addTextAreaField("html", "HTML", 80, 15, TextAreaClass.EditorType.PURE_TEXT);

        needsUpdate |= setClassDocumentFields(document, "Mail Class");

        return needsUpdate;
    }
}
