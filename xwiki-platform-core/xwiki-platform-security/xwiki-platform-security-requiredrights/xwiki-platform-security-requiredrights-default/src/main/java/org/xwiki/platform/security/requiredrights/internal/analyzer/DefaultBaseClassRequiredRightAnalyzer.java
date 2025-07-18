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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.platform.security.requiredrights.display.BlockSupplierProvider;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * Required rights analyzer for XClass definitions.
 *
 * @since 15.10.16
 * @since 16.4.7
 * @since 16.10.2
 * @version $Id$
 */
@Component
@Singleton
public class DefaultBaseClassRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseClass>
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("translation")
    private BlockSupplierProvider<String> translationMessageSupplierProvider;

    @Inject
    private BlockSupplierProvider<BaseCollection<? extends EntityReference>> baseCollectionBlockSupplierProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseClass xClass) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> results = new ArrayList<>();

        // Analyze required rights for XClass definitions. We're mainly concerned about custom displayers and
        // database list properties.
        for (Object field : xClass.getFieldList()) {
            if (field instanceof PropertyClass propertyClass) {
                ComponentManager componentManager = this.componentManagerProvider.get();

                Class<? super PropertyClass> superClass = PropertyClass.class.getSuperclass();
                for (Class<?> clazz = propertyClass.getClass(); !clazz.equals(superClass);
                    clazz = clazz.getSuperclass()) {
                    DefaultParameterizedType roleType =
                        new DefaultParameterizedType(null, RequiredRightAnalyzer.class, clazz);
                    if (componentManager.hasComponent(roleType)) {
                        try {
                            RequiredRightAnalyzer<PropertyClass> analyzer = componentManager.getInstance(roleType);
                            results.addAll(analyzer.analyze(propertyClass));
                        } catch (Exception e) {
                            results.add(new RequiredRightAnalysisResult(propertyClass.getReference(),
                                this.translationMessageSupplierProvider.get(
                                    "security.requiredrights.class.errorAnalyzingProperty",
                                    ExceptionUtils.getRootCauseMessage(e)),
                                this.baseCollectionBlockSupplierProvider.get(propertyClass),
                                List.of(RequiredRight.MAYBE_PROGRAM)));
                        }
                        break;
                    }
                }
            }
        }

        return results;
    }

}
