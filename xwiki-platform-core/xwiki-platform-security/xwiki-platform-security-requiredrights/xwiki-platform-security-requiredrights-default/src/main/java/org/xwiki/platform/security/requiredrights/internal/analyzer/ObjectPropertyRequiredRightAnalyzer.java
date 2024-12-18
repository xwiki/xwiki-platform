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
package org.xwiki.platform.security.requiredrights.internal.analyzer;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.internal.provider.BlockSupplierProvider;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ContentParser;
import org.xwiki.rendering.parser.MissingParserException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.velocity.internal.util.VelocityDetector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Helper component for analyzing {@link BaseObject} objects and their properties.
 *
 * @version $Id$
 */
@Component(roles = ObjectPropertyRequiredRightAnalyzer.class)
@Singleton
public class ObjectPropertyRequiredRightAnalyzer
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private VelocityDetector velocityDetector;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    @Named("stringCode")
    private BlockSupplierProvider<String> stringCodeBlockSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseObject> objectBlockSupplierProvider;

    @Inject
    private RequiredRightAnalyzer<XDOM> xdomRequiredRightAnalyzer;

    @Inject
    private ContentParser contentParser;

    /**
     * Create a {@link RequiredRightAnalysisResult} for the given object.
     *
     * @param object the object to create the result for
     * @param right the required right
     * @param translationMessage the translation message key
     * @param messageParameters the parameters for the translation message
     * @return the created result
     */
    public RequiredRightAnalysisResult createObjectResult(BaseObject object, RequiredRight right,
        String translationMessage, Object... messageParameters)
    {
        return new RequiredRightAnalysisResult(object.getReference(),
            this.translationMessageSupplierProvider.get(translationMessage, messageParameters),
            this.objectBlockSupplierProvider.get(object),
            List.of(right));
    }

    /**
     * Analyze all properties of the given object and add a result for the object itself.
     *
     * @param object the object to analyze
     * @param right the required right
     * @param translationMessage the translation message key
     * @param messageParameters the parameters for the translation message
     * @return the analysis results
     * @throws RequiredRightsException if an error occurs during the analysis
     */
    public List<RequiredRightAnalysisResult> analyzeAllPropertiesAndAddObjectResult(BaseObject object,
        RequiredRight right, String translationMessage, Object... messageParameters) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results = analyzeAllProperties(object);
        results.add(createObjectResult(object, right, translationMessage, messageParameters));
        return results;
    }

    /**
     * Analyze all properties of the given object.
     *
     * @param object the object to analyze
     * @return the analysis results
     * @throws RequiredRightsException if an error occurs during the analysis
     */
    public List<RequiredRightAnalysisResult> analyzeAllProperties(BaseObject object) throws RequiredRightsException
    {
        // No analyzer found for this class, so we just check all properties.
        BaseClass xClass = object.getXClass(this.contextProvider.get());
        List<RequiredRightAnalysisResult> results = new ArrayList<>();

        for (String propertyName : object.getPropertyList()) {
            results.addAll(analyzeProperty(object, propertyName, xClass));
        }

        return results;
    }

    /**
     * Analyze the given property of the given object.
     *
     * @param object the object to analyze
     * @param propertyName the name of the property to analyze
     * @param xClass the class of the object
     * @return the analysis results
     * @throws RequiredRightsException if an error occurs during the analysis
     */
    public List<RequiredRightAnalysisResult> analyzeProperty(BaseObject object, String propertyName,
        BaseClass xClass) throws RequiredRightsException
    {
        PropertyInterface property = xClass.getField(propertyName);
        if (property instanceof TextAreaClass) {
            TextAreaClass textAreaClass = (TextAreaClass) property;
            return analyzeTextAreaProperty(object, propertyName, textAreaClass);
        }
        return List.of();
    }

    private List<RequiredRightAnalysisResult> analyzeTextAreaProperty(BaseObject object, String propertyName,
        TextAreaClass textAreaClass) throws RequiredRightsException
    {
        String contentTypeString = textAreaClass.getContentType();
        TextAreaClass.ContentType contentType =
            TextAreaClass.ContentType.getByValue(contentTypeString);
        if (contentType == null) {
            // Default to wiki text like TextAreaClass does.
            contentType = TextAreaClass.ContentType.WIKI_TEXT;
        }
        PropertyInterface field = object.getField(propertyName);

        List<RequiredRightAnalysisResult> result = List.of();

        // No need to analyze restricted properties.
        if (!textAreaClass.isRestricted() && field instanceof BaseStringProperty) {
            BaseStringProperty baseStringProperty = (BaseStringProperty) field;
            String value = baseStringProperty.getValue();

            if (StringUtils.isNotBlank(value)) {
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
                    case PURE_TEXT:
                        break;
                    default:
                        result = analyzeWikiContent(object, value, field.getReference());
                        break;
                }
            }
        }

        return result;
    }

    /**
     * Analyze the given value as a Velocity script.
     *
     * @param value the value to analyze
     * @param reference the reference of the property
     * @param translationMessage the translation message key
     * @return the analysis results
     */
    public List<RequiredRightAnalysisResult> analyzeVelocityScriptValue(String value, EntityReference reference,
        String translationMessage)
    {
        List<RequiredRightAnalysisResult> result;
        if (this.velocityDetector.containsVelocityScript(value)) {
            result = List.of(new RequiredRightAnalysisResult(reference,
                this.translationMessageSupplierProvider.get(translationMessage),
                this.stringCodeBlockSupplierProvider.get(value),
                RequiredRight.SCRIPT_AND_MAYBE_PROGRAM));
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
