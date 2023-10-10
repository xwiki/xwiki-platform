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
package org.xwiki.platform.security.requiredrights.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Analyzer that checks if an XObject would need more rights than it currently has.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(DefaultObjectRequiredRightAnalyzer.ID)
public class DefaultObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    /**
     * The id of this analyzer.
     */
    public static final String ID = "object";

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    @Named(XDOMRequiredRightAnalyzer.ID)
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private TranslationMessageSupplierProvider translationMessageSupplierProvider;

    @Inject
    private StringBlockSupplierProvider stringBlockSupplierProvider;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContentParser contentParser;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        if (object == null) {
            return List.of();
        }
        EntityReference xClassReference = object.getRelativeXClassReference();
        String className = this.compactEntityReferenceSerializer.serialize(xClassReference);

        try {
            RequiredRightAnalyzer<BaseObject> analyzer =
                this.componentManagerProvider.get().getInstance(new DefaultParameterizedType(null,
                    RequiredRightAnalyzer.class, BaseObject.class), "object/" + className);
            return analyzer.analyze(object);
        } catch (ComponentLookupException e) {
            // No analyzer found for this class, so we just check all properties.
            BaseClass xClass = object.getXClass(this.contextProvider.get());
            List<RequiredRightAnalysisResult> results = new ArrayList<>();

            for (String propertyName : object.getPropertyList()) {
                results.addAll(analyzeProperty(object, propertyName, xClass));
            }

            return results;
        }
    }

    protected List<RequiredRightAnalysisResult> analyzeProperty(BaseObject object, String propertyName,
        BaseClass xClass) throws RequiredRightsException
    {
        PropertyInterface property = xClass.getField(propertyName);
        if (property instanceof TextAreaClass) {
            TextAreaClass textAreaClass = (TextAreaClass) property;

            return analyzeTextAreaProperty(object, propertyName, textAreaClass);
        }
        return List.of();
    }

    protected List<RequiredRightAnalysisResult> analyzeTextAreaProperty(BaseObject object, String propertyName,
        TextAreaClass textAreaClass) throws RequiredRightsException
    {
        String contentTypeString = textAreaClass.getContentType();
        TextAreaClass.ContentType contentType =
            TextAreaClass.ContentType.getByValue(contentTypeString);
        PropertyInterface field = object.getField(propertyName);

        List<RequiredRightAnalysisResult> result = List.of();

        // No need to analyze restricted properties.
        if (!textAreaClass.isRestricted() && field instanceof BaseStringProperty) {
            String value = ((BaseStringProperty) field).getValue();

            if (contentType != null) {
                switch (contentType) {
                    case VELOCITY_CODE:
                        result = analyzeVelocityScriptValue(value, field.getReference(),
                            "security.requiredrights.object.velocityCodeTextArea");
                        break;
                    case VELOCITYWIKI:
                        result = analyzeVelocityScriptValue(value, field.getReference(),
                            "security.requiredrights.object.velocityWikiTextArea");
                        if (result.isEmpty()) {
                            // If there is no Velocity code, we analyze the content as wiki syntax.
                            result = analyzeWikiContent(object, value, field.getReference());
                        }
                        break;
                    case WIKI_TEXT:
                        result = analyzeWikiContent(object, value, field.getReference());
                        break;
                    default:
                        break;
                }
            }
        }

        return result;
    }

    private List<RequiredRightAnalysisResult> analyzeVelocityScriptValue(String value, EntityReference reference,
        String translationMessage)
    {
        List<RequiredRightAnalysisResult> result;
        if (VelocityUtil.containsVelocityScript(value)) {
            result = List.of(new RequiredRightAnalysisResult(reference,
                this.translationMessageSupplierProvider.get(translationMessage),
                this.stringBlockSupplierProvider.get(value),
                List.of(
                    new RequiredRightAnalysisResult.RequiredRight(Right.SCRIPT, EntityType.DOCUMENT, false),
                    new RequiredRightAnalysisResult.RequiredRight(Right.PROGRAM, EntityType.DOCUMENT, true)
                )));
        } else {
            result = List.of();
        }

        return result;
    }

    private List<RequiredRightAnalysisResult> analyzeWikiContent(BaseObject object, String value,
        EntityReference reference)
        throws RequiredRightsException
    {
        try {
            XDOM parsedContent = this.contentParser.parse(value, object.getOwnerDocument().getSyntax(),
                object.getDocumentReference());
            parsedContent.getMetaData().addMetaData(XDOMRequiredRightAnalyzer.ENTITY_REFERENCE_METADATA, reference);
            return this.xdomRequiredRightAnalyzer.analyze(parsedContent);
        } catch (ParseException | MissingParserException e) {
            throw new RequiredRightsException("Failed to parse content of object property", e);
        }
    }
}
