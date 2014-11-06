package com.xpn.xwiki.internal.skin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

public class WikiSkin extends AbstractSkin
{
    private static final LocalDocumentReference SKINCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiSkins");

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public InputSource getSkinResourceInputSource(String resource)
    {
        InputSource source = null;

        XWikiDocument skinDocument = resource.equals("macros.vm") ? null : getSkinDocument(this.id);
        if (skinDocument != null) {
            source = getTemplateContentFromDocumentSkin(resource, skinDocument);
        }

        return source;
    }

    private InputSource getTemplateContentFromDocumentSkin(String template, XWikiDocument skinDocument)
    {
        if (skinDocument != null) {
            // Try parsing the object property
            BaseProperty templateProperty = getTemplatePropertyValue(template, skinDocument);
            if (templateProperty != null) {
                return new StringInputSource((String) templateProperty.getValue());
            }

            // Try parsing a document attachment
            XWikiAttachment attachment = skinDocument.getAttachment(template);
            if (attachment != null) {
                // It's impossible to know the real attachment encoding, but let's assume that they respect the
                // standard and use UTF-8 (which is required for the files located on the filesystem)
                try {
                    return new DefaultInputStreamInputSource(attachment.getContentInputStream(this.xcontextProvider
                        .get()), true);
                } catch (Exception e) {
                    this.logger.error("Faied to get attachment content [{}]", skinDocument.getDocumentReference(), e);
                }
            }
        }

        return null;
    }

    private BaseProperty getTemplatePropertyValue(String template, XWikiDocument skinDocument)
    {
        // Try parsing the object property
        BaseObject skinObject = skinDocument.getXObject(SKINCLASS_REFERENCE);
        if (skinObject != null) {
            BaseProperty templateProperty = (BaseProperty) skinObject.safeget(template);

            // If not found try by replacing '/' with '.'
            if (templateProperty == null) {
                String escapedTemplateName = StringUtils.replaceChars(template, '/', '.');
                templateProperty = (BaseProperty) skinObject.safeget(escapedTemplateName);
            }

            if (templateProperty != null) {
                Object value = templateProperty.getValue();
                if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
                    return templateProperty;
                }
            }
        }

        return null;
    }

    private XWikiDocument getSkinDocument(String skin)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            DocumentReference skinReference = this.currentMixedDocumentReferenceResolver.resolve(skin);
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null && xwiki.getStore() != null) {
                XWikiDocument doc;
                try {
                    doc = xwiki.getDocument(skinReference, xcontext);
                } catch (XWikiException e) {
                    this.logger.error("Faied to get document [{}]", skinReference, e);

                    return null;
                }
                if (!doc.isNew()) {
                    return doc;
                }
            }
        }

        return null;
    }

    private BaseObject getSkinObject(String skin)
    {
        XWikiDocument skinDocument = getSkinDocument(skin);

        return skinDocument != null ? skinDocument.getXObject(SKINCLASS_REFERENCE) : null;
    }
}
