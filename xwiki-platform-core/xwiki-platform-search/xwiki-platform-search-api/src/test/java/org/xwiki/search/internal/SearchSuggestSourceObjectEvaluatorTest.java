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
package org.xwiki.search.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.evaluation.ObjectEvaluatorException;
import org.xwiki.evaluation.ObjectPropertyEvaluator;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link SearchSuggestSourceObjectEvaluator}.
 *
 * @version $Id$
 */
@ComponentTest
public class SearchSuggestSourceObjectEvaluatorTest
{
    @InjectMockComponents
    private SearchSuggestSourceObjectEvaluator searchSuggestSourceObjectEvaluator;

    @MockComponent
    @Named("velocity")
    private ObjectPropertyEvaluator velocityObjectPropertyEvaluator;

    @Mock
    private BaseObject baseObject;

    @BeforeEach
    void setUp() throws ObjectEvaluatorException
    {
        when(this.baseObject.getStringValue("icon")).thenReturn("unevaluated icon");
        when(this.baseObject.getStringValue("name")).thenReturn("unevaluated name");

        Map<String, String> evaluatedVelocityProperties = new HashMap<>();
        evaluatedVelocityProperties.put("icon", "evaluated icon");
        evaluatedVelocityProperties.put("name", "evaluated name");

        when(this.velocityObjectPropertyEvaluator.evaluateProperties(this.baseObject, "name", "icon"))
            .thenReturn(evaluatedVelocityProperties);
    }

    @Test
    void checkEvaluationThroughPropertyEvaluator() throws ObjectEvaluatorException
    {
        Map<String, String> evaluationResults = this.searchSuggestSourceObjectEvaluator.evaluate(this.baseObject);
        verify(this.velocityObjectPropertyEvaluator).evaluateProperties(this.baseObject, "name", "icon");
        assertEquals("evaluated icon", evaluationResults.get("icon"));
        assertEquals("evaluated name", evaluationResults.get("name"));
    }
}
