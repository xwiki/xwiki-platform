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
package org.xwiki.platform.security.requiredrights.internal.provider;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.localization.Translation;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.FormatBlock;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.listener.Format;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Provider for a displayer for an XObject.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class XObjectDisplayerProvider extends AbstractBlockSupplierProvider<BaseObject>
{
    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private ContextualLocalizationManager contextualLocalizationManager;

    @Override
    public Supplier<Block> get(BaseObject object, Object... parameters)
    {
        // Get and thereby store the values that require a context, so we can be sure we get them in the context of
        // the document that has the object.
        XWikiContext context = this.xWikiContextProvider.get();
        BaseClass xClass = object.getXClass(context);
        List<PropertyDisplay> propertyNamesHintsValues = Arrays.stream(xClass.getProperties())
            .map(p -> (PropertyClass) p)
            .map(p -> {
                if (p instanceof TextAreaClass) {
                    // Don't use the display view for TextAreaClass, as it would execute the content.
                    return new PropertyDisplay(p.getTranslatedPrettyName(context), p.getHint(),
                        object.getStringValue(p.getName()), false);
                } else {
                    return new PropertyDisplay(p.getTranslatedPrettyName(context), p.getHint(),
                        p.displayView(p.getName(), object, context), true);
                }
            })
            .collect(Collectors.toList());

        List<PropertyDisplay> deprecatedPropertyNamesValues = xClass.getDeprecatedObjectProperties(object).stream()
            .map(p -> new PropertyDisplay(p.getName(), null, p.getValue().toString(), false))
            .collect(Collectors.toList());

        Translation removedPropertiesMessage =
            this.contextualLocalizationManager.getTranslation("core.editors.object.removeDeprecatedProperties.info");
        return () -> {
            // Display the properties
            Block propertiesBlock = renderProperties(propertyNamesHintsValues);
            Block result = new CompositeBlock(List.of(propertiesBlock));

            // Display deprecated properties
            if (!deprecatedPropertyNamesValues.isEmpty()) {
                Block deprecatedPropertiesBlock =
                    new GroupBlock(Map.of(CLASS_ATTRIBUTE, "box warningmessage deprecatedProperties"));
                if (removedPropertiesMessage != null) {
                    deprecatedPropertiesBlock.addChild(
                        new FormatBlock(List.of(removedPropertiesMessage.render(xClass.getPrettyName())), Format.BOLD));
                }
                deprecatedPropertiesBlock.addChild(renderProperties(deprecatedPropertyNamesValues));
                result.addChild(deprecatedPropertiesBlock);
            }

            return result;
        };
    }
}
