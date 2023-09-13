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

import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalysisResult;
import org.xwiki.platform.security.requiredrights.RequiredRightAnalyzer;
import org.xwiki.platform.security.requiredrights.RequiredRightsException;
import org.xwiki.security.authorization.Right;

/**
 * Analyzer that checks if a string potentially contains a velocity script. The check is done independently of the
 * rights.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named(StringVelocityRequiredRightAnalyzer.ID)
public class StringVelocityRequiredRightAnalyzer implements RequiredRightAnalyzer<String>
{
    /**
     * The id of this analyzer.
     */
    public static final String ID = "string/velocity";

    @Override
    public List<RequiredRightAnalysisResult> analyze(String object) throws RequiredRightsException
    {
        List<RequiredRightAnalysisResult> result;

        if (StringUtils.containsAny(object, "#", "$")) {
            result = List.of(new RequiredRightAnalysisResult(ID, "security.requiredrights.velocity", List.of(object),
                Right.SCRIPT, EntityType.DOCUMENT));
        } else {
            result = List.of();
        }

        return result;
    }
}
