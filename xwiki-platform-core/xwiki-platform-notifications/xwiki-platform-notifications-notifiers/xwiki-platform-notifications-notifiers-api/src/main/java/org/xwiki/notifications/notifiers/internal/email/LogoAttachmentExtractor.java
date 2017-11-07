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
package org.xwiki.notifications.notifiers.internal.email;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.skin.Resource;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.skin.InternalSkinManager;

/**
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = LogoAttachmentExtractor.class)
@Singleton
public class LogoAttachmentExtractor
{
    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Used to get file resources.
     */
    @Inject
    private Environment environment;


    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    @Named("all")
    private ConfigurationSource configurationSource;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private InternalSkinManager internalSkinManager;

    public Attachment getLogo() throws Exception
    {
        XWikiContext context = xwikiContextProvider.get();

        String colorTheme = configurationSource.getProperty("colorTheme");
        if (StringUtils.isNotBlank(colorTheme)) {
            DocumentReference colorThemeRef = documentReferenceResolver.resolve(colorTheme);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(colorThemeRef, context);
            String logo = doc.getStringValue("logo");
            if (StringUtils.isBlank(logo)) {
                logo = doc.getStringValue("logoImage");
            }
            if (StringUtils.isNotBlank(logo) && !logo.endsWith(".svg") && doc.getAttachment(logo) != null) {
                XWikiAttachment attachment = doc.getAttachment(logo);
                attachment.setFilename("logo");
                return new Attachment(new Document(doc, context), attachment, context);
            }
        }

        String skin = configurationSource.getProperty("skin");
        if (StringUtils.isNotBlank(skin)) {
            DocumentReference skinRef = documentReferenceResolver.resolve(skin);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(skinRef, context);
            String logo = doc.getStringValue("logo");
            if (StringUtils.isNotBlank(logo) && !logo.endsWith(".svg") && doc.getAttachment(logo) != null) {
                XWikiAttachment attachment = doc.getAttachment(logo);
                attachment.setFilename("logo");
                return new Attachment(new Document(doc, context), attachment, context);
            }
        }

        XWikiAttachment fakeAttachment = new XWikiAttachment();
        Resource sourceImageIS = internalSkinManager.getCurrentSkin(true).getResource("logo.png");
        InputStream inputStream = environment.getResourceAsStream(sourceImageIS.getPath());
        fakeAttachment.setAttachment_content(new XWikiAttachmentContent());
        fakeAttachment.getAttachment_content().setContent(inputStream);
        fakeAttachment.setFilename("logo");
        return new Attachment(null, fakeAttachment, context);

    }
}
