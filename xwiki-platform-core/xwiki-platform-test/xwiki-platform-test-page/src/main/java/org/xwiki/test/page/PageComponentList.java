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
package org.xwiki.test.page;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.xwiki.context.internal.DefaultExecution;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.display.internal.ConfiguredDocumentDisplayer;
import org.xwiki.display.internal.DefaultDisplayConfiguration;
import org.xwiki.display.internal.DefaultDocumentDisplayer;
import org.xwiki.display.internal.DocumentContentDisplayer;
import org.xwiki.display.internal.DocumentTitleDisplayer;
import org.xwiki.localization.internal.DefaultContextualLocalizationManager;
import org.xwiki.localization.internal.DefaultLocalizationManager;
import org.xwiki.localization.internal.DefaultTranslationBundleContext;
import org.xwiki.model.internal.DefaultModelContext;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.properties.internal.DefaultBeanManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.rendering.internal.macro.DefaultMacroContentParser;
import org.xwiki.rendering.internal.macro.DefaultMacroIdFactory;
import org.xwiki.rendering.internal.macro.DefaultMacroManager;
import org.xwiki.rendering.internal.macro.html.HTMLMacro;
import org.xwiki.rendering.internal.macro.html.HTMLMacroXHTMLRendererFactory;
import org.xwiki.rendering.internal.macro.include.IncludeMacro;
import org.xwiki.rendering.internal.macro.velocity.DefaultVelocityMacroConfiguration;
import org.xwiki.rendering.internal.macro.velocity.VelocityMacro;
import org.xwiki.rendering.internal.macro.velocity.filter.IndentVelocityMacroFilter;
import org.xwiki.rendering.internal.parser.DefaultContentParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextBlockParser;
import org.xwiki.rendering.internal.parser.plain.PlainTextStreamParser;
import org.xwiki.rendering.internal.parser.reference.type.URLResourceReferenceTypeParser;
import org.xwiki.rendering.internal.renderer.DefaultLinkLabelGenerator;
import org.xwiki.rendering.internal.renderer.plain.PlainTextBlockRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRenderer;
import org.xwiki.rendering.internal.renderer.plain.PlainTextRendererFactory;
import org.xwiki.rendering.internal.transformation.DefaultTransformationManager;
import org.xwiki.rendering.internal.transformation.XWikiRenderingContext;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroDocumentReferenceResolver;
import org.xwiki.rendering.internal.transformation.macro.CurrentMacroEntityReferenceResolver;
import org.xwiki.rendering.internal.transformation.macro.MacroTransformation;
import org.xwiki.rendering.internal.util.DefaultErrorBlockGenerator;
import org.xwiki.resource.internal.DefaultResourceReferenceManager;
import org.xwiki.script.internal.DefaultScriptContextManager;
import org.xwiki.script.internal.ScriptExecutionContextInitializer;
import org.xwiki.script.internal.service.DefaultScriptServiceManager;
import org.xwiki.script.internal.service.ServicesScriptContextInitializer;
import org.xwiki.sheet.internal.DefaultSheetManager;
import org.xwiki.sheet.internal.SheetDocumentDisplayer;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.rendering.velocity.StubVelocityManager;
import org.xwiki.velocity.internal.DefaultVelocityConfiguration;
import org.xwiki.velocity.internal.DefaultVelocityContextFactory;
import org.xwiki.velocity.internal.DefaultVelocityEngine;
import org.xwiki.velocity.internal.DefaultVelocityFactory;
import org.xwiki.xml.internal.html.DefaultHTMLCleaner;
import org.xwiki.xml.internal.html.filter.AttributeFilter;
import org.xwiki.xml.internal.html.filter.BodyFilter;
import org.xwiki.xml.internal.html.filter.FontFilter;
import org.xwiki.xml.internal.html.filter.LinkFilter;
import org.xwiki.xml.internal.html.filter.ListFilter;
import org.xwiki.xml.internal.html.filter.ListItemFilter;

import com.xpn.xwiki.doc.DefaultDocumentAccessBridge;
import com.xpn.xwiki.internal.localization.XWikiLocalizationContext;
import com.xpn.xwiki.internal.sheet.ClassSheetBinder;
import com.xpn.xwiki.internal.sheet.DefaultModelBridge;
import com.xpn.xwiki.internal.sheet.DocumentSheetBinder;
import com.xpn.xwiki.internal.skin.DefaultSkinManager;
import com.xpn.xwiki.internal.skin.InternalSkinConfiguration;
import com.xpn.xwiki.internal.skin.InternalSkinManager;
import com.xpn.xwiki.internal.skin.WikiSkinUtils;
import com.xpn.xwiki.objects.meta.BooleanMetaClass;
import com.xpn.xwiki.objects.meta.NumberMetaClass;
import com.xpn.xwiki.objects.meta.StaticListMetaClass;
import com.xpn.xwiki.objects.meta.StringMetaClass;
import com.xpn.xwiki.objects.meta.TextAreaMetaClass;
import com.xpn.xwiki.render.XWikiScriptContextInitializer;
import com.xpn.xwiki.test.component.XWikiDocumentFilterUtilsComponentList;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Pack of default Component implementations that are needed for rendering wiki pages.
 *
 * @version $Id$
 * @since 7.3M1
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD, ANNOTATION_TYPE })
@ComponentList({
    // Rendering
    XWikiRenderingContext.class,
    DefaultTransformationManager.class,
    StubRenderingConfiguration.class,
    DefaultLinkLabelGenerator.class,
    DefaultContentParser.class,
    URLResourceReferenceTypeParser.class,
    DefaultMacroContentParser.class,

    //Resource
    DefaultResourceReferenceManager.class,

    // Plain Syntax
    PlainTextBlockParser.class,
    PlainTextRendererFactory.class,
    PlainTextBlockRenderer.class,
    PlainTextStreamParser.class,
    PlainTextRenderer.class,

    // Transformation
    MacroTransformation.class,
    DefaultMacroManager.class,
    DefaultMacroIdFactory.class,
    DefaultErrorBlockGenerator.class,

    // Properties
    DefaultBeanManager.class,
    DefaultConverterManager.class,
    EnumConverter.class,
    ConvertUtilsConverter.class,

    // Display
    ConfiguredDocumentDisplayer.class,
    DefaultDisplayConfiguration.class,
    DefaultDocumentDisplayer.class,
    DocumentTitleDisplayer.class,
    DocumentContentDisplayer.class,
    SheetDocumentDisplayer.class,

    // Sheet
    DefaultSheetManager.class,
    DefaultModelBridge.class,
    DocumentSheetBinder.class,
    ClassSheetBinder.class,

    // Model
    DefaultDocumentAccessBridge.class,
    DefaultModelContext.class,

    // Velocity
    DefaultScriptContextManager.class,
    DefaultVelocityFactory.class,
    DefaultVelocityConfiguration.class,
    DefaultVelocityEngine.class,
    DefaultVelocityContextFactory.class,
    StubVelocityManager.class,

    // Skin
    DefaultSkinManager.class,
    InternalSkinManager.class,
    InternalSkinConfiguration.class,
    WikiSkinUtils.class,

    // Velocity Macro
    VelocityMacro.class,
    DefaultVelocityMacroConfiguration.class,
    IndentVelocityMacroFilter.class,

    // HTML Cleaner
    DefaultHTMLCleaner.class,
    ListFilter.class,
    ListItemFilter.class,
    FontFilter.class,
    BodyFilter.class,
    AttributeFilter.class,
    LinkFilter.class,

    // HTML Macro
    HTMLMacro.class,
    HTMLMacroXHTMLRendererFactory.class,

    // Include Macro
    IncludeMacro.class,

    // Execution
    DefaultExecutionContextManager.class,
    DefaultExecution.class,

    // Script
    ScriptExecutionContextInitializer.class,
    XWikiScriptContextInitializer.class,
    ServicesScriptContextInitializer.class,
    DefaultScriptServiceManager.class,

    // Observation
    DefaultObservationManager.class,

    // Localization
    DefaultContextualLocalizationManager.class,
    DefaultLocalizationManager.class,
    DefaultTranslationBundleContext.class,
    XWikiLocalizationContext.class,

    // Property Class Providers (needed when the page has xobjects)
    StaticListMetaClass.class,
    TextAreaMetaClass.class,
    StringMetaClass.class,
    BooleanMetaClass.class,
    NumberMetaClass.class,

    // Macro Resolver/Serializer
    CurrentMacroDocumentReferenceResolver.class,
    CurrentMacroEntityReferenceResolver.class,
})
@Inherited
@XWikiDocumentFilterUtilsComponentList
public @interface PageComponentList
{
}
