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

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputStreamInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.skin.Resource;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
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
    private static final String LOGO = "logo";

    private static final String SVG_EXTENSION = ".svg";

    @Inject
    private Provider<XWikiContext> xwikiContextProvider;

    @Inject
    @Named("all")
    private ConfigurationSource configurationSource;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private InternalSkinManager internalSkinManager;

    /**
     * @return an attachment holding the logo of the wiki
     * @throws Exception if an error happens
     */
    public Attachment getLogo() throws Exception
    {
        XWikiContext context = xwikiContextProvider.get();

        String colorTheme = configurationSource.getProperty("colorTheme");
        if (StringUtils.isNotBlank(colorTheme)) {
            DocumentReference colorThemeRef = documentReferenceResolver.resolve(colorTheme);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(colorThemeRef, context);
            String logo = doc.getStringValue(LOGO);
            if (StringUtils.isBlank(logo)) {
                logo = doc.getStringValue("logoImage");
            }
            if (isLogoAttachementValid(doc, logo)) {
                XWikiAttachment attachment = doc.getAttachment(logo);

                return toLogoAttachment(attachment, context);
            }
        }

        String skin = configurationSource.getProperty("skin");
        if (StringUtils.isNotBlank(skin)) {
            DocumentReference skinRef = documentReferenceResolver.resolve(skin);
            XWiki xwiki = context.getWiki();
            XWikiDocument doc = xwiki.getDocument(skinRef, context);
            String logo = doc.getStringValue(LOGO);
            if (isLogoAttachementValid(doc, logo)) {
                XWikiAttachment attachment = doc.getAttachment(logo);

                return toLogoAttachment(attachment, context);
            }
        }

        return newLogoAttachment("image/png", this.internalSkinManager.getCurrentSkin(true).getResource("logo.png"),
            context);
    }

    private Attachment toLogoAttachment(XWikiAttachment attachment, XWikiContext xcontext) throws XWikiException
    {
        // Make sure attachment content is loaded
        attachment.loadAttachmentContent(xcontext);

        // Make sure the attachment have the right name
        if (!attachment.getFilename().equals(LOGO)) {
            return newLogoAttachment(attachment.getMimeType(xcontext),
                (XWikiAttachmentContent) attachment.getAttachment_content().clone(), xcontext);
        } else {
            return new Attachment(attachment.getDoc().newDocument(xcontext), attachment, xcontext);
        }
    }

    private Attachment newLogoAttachment(String mimeType, XWikiAttachmentContent content, XWikiContext xcontext)
    {
        XWikiAttachment attachment = new XWikiAttachment(null, LOGO);
        attachment.setAttachment_content(content);
        attachment.setMimeType(mimeType);

        return new Attachment(null, attachment, xcontext);
    }

    private Attachment newLogoAttachment(String mimeType, Resource sourceImageIS, XWikiContext xcontext)
        throws Exception
    {
        InputSource inputSource = sourceImageIS.getInputSource();

        if (inputSource instanceof InputStreamInputSource) {
            try (InputStream inputStream = ((InputStreamInputSource) inputSource).getInputStream()) {
                XWikiAttachmentContent content = new XWikiAttachmentContent();
                content.setContent(inputStream);

                return newLogoAttachment(mimeType, content, xcontext);
            }
        }

        throw new NotificationException("Unsupported logo input source [" + inputSource + "]");
    }

    private boolean isLogoAttachementValid(XWikiDocument doc, String logo)
    {
        return StringUtils.isNotBlank(logo) && !logo.endsWith(SVG_EXTENSION) && doc.getAttachment(logo) != null;
    }
}
