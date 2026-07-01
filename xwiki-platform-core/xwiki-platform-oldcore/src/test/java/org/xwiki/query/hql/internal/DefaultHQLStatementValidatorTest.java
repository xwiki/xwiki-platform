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
package org.xwiki.query.hql.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Validate {@link DefaultHQLStatementValidator}.
 * 
 * @version $Id$
 */
@ComponentTest
public class DefaultHQLStatementValidatorTest
{
    @InjectMockComponents
    private DefaultHQLStatementValidator validator;

    @Test
    void checkOrderBySafe() throws QueryException
    {
        // Valid

        this.validator.checkOrderBySafe(null, "");
        this.validator.checkOrderBySafe(null, "column");
        this.validator.checkOrderBySafe(null, "column asc");
        this.validator.checkOrderBySafe(null, "column ASC");
        this.validator.checkOrderBySafe(List.of(), "");
        this.validator.checkOrderBySafe(List.of(), "column");
        this.validator.checkOrderBySafe(List.of(), "column desc");
        this.validator.checkOrderBySafe(List.of(), "prefix.column asc");
        this.validator.checkOrderBySafe(List.of("prefix."), "");
        this.validator.checkOrderBySafe(List.of("prefix."), "prefix.column");
        this.validator.checkOrderBySafe(List.of("prefix."), "prefix.column asc");

        // Invalid

        assertThrows(QueryException.class, () -> this.validator.checkOrderBySafe(null, "column (desc)"));
        assertThrows(QueryException.class, () -> this.validator.checkOrderBySafe(List.of("prefix."), "column desc"));
        assertThrows(QueryException.class,
            () -> this.validator.checkOrderBySafe(List.of("prefix."), "otherprefix.column desc"));
    }
}
