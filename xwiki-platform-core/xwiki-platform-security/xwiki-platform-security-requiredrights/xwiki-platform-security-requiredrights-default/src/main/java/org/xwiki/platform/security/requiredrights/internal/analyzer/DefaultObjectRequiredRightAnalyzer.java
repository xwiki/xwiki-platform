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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.platform.security.requiredrights.RequiredRight;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;

import com.xpn.xwiki.objects.BaseObject;

/**
 * Analyzer that checks if an XObject would need more rights than it currently has.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class DefaultObjectRequiredRightAnalyzer implements RequiredRightAnalyzer<BaseObject>
{
    @Inject
    private ObjectPropertyRequiredRightAnalyzer objectPropertyRequiredRightAnalyzer;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @Inject
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public List<RequiredRightAnalysisResult> analyze(BaseObject object) throws RequiredRightsException
    {
        if (object == null) {
            return List.of();
        }

        try {
            return analyzeWithException(object);
        } catch (Exception e) {
            return List.of(this.objectPropertyRequiredRightAnalyzer.createObjectResult(object,
                RequiredRight.MAYBE_PROGRAM, "security.requiredrights.object.error",
                ExceptionUtils.getRootCauseMessage(e)));
        }
    }

    private List<RequiredRightAnalysisResult> analyzeWithException(BaseObject object)
        throws RequiredRightsException
    {
        EntityReference xClassReference = object.getRelativeXClassReference();
        String className = this.compactEntityReferenceSerializer.serialize(xClassReference);
        try {
            RequiredRightAnalyzer<BaseObject> analyzer =
                this.componentManagerProvider.get().getInstance(new DefaultParameterizedType(null,
                    RequiredRightAnalyzer.class, BaseObject.class), className);
            return analyzer.analyze(object);
        } catch (ComponentLookupException e) {
            return this.objectPropertyRequiredRightAnalyzer.analyzeAllProperties(object);
        }
    }
}
