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
package org.xwiki.store;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TransactionException
 *
 * @version $Id$
 * @since 3.0M2
 */
class TransactionExceptionTest
{
    private static final String SIMPLE_TEST_OUT = """
        Caused by:
        java.lang.Exception
        \tOne exception.
        java.lang.RuntimeException
        \tNumber 2
        java.lang.OutOfMemoryError
        \tUt oh
        """;

    private static final String COMPOUND_TEST_OUT = """
        Caused by:
        java.lang.Exception
        \tsomething bad happened.
        org.xwiki.store.TransactionException
        \tCaused by:
        \tjava.lang.Exception
        \t\tOne exception.
        \tjava.lang.RuntimeException
        \t\tNumber 2
        \tjava.lang.OutOfMemoryError
        \t\tUt oh
        \t
        java.lang.Error
        \tSEGFAULT!
        """;

    private static final String NONRECOVERABLE_TEST_OUT = """
        Caused by:
        java.lang.Exception
        \tsomething bad happened.
        org.xwiki.store.TransactionException
        \tThis means there is db corruption
        \tCaused by:
        \tjava.lang.Error
        \t\tCorruption!!
        \t
        java.lang.Error
        \tSEGFAULT!
        """;

    @Test
    void emptyException()
    {
        TransactionException exception = new TransactionException(Collections.emptyList());
        assertNull(exception.getCause());
    }

    /**
     * Make sure the messages from the underlying throwables are preserved.
     */
    @Test
    void simpleExceptionTest()
    {
        TransactionException te = this.getException();
        assertFalse(te.isNonRecoverable());
        assertEquals(3, te.exceptionCount(), "Wrong number of exceptions reported");
        assertEquals(SIMPLE_TEST_OUT, te.getMessage(), "The wrong exception message was given");
        assertNotNull(te.getCause());
    }

    @Test
    void compoundExceptionTest()
    {
        TransactionException te = new TransactionException(List.of(
            new Exception("something bad happened."),
            getException(),
            new Error("SEGFAULT!")
        ));
        assertFalse(te.isNonRecoverable());
        assertEquals(5, te.exceptionCount(), "Wrong number of exceptions reported");
        assertEquals(COMPOUND_TEST_OUT, te.getMessage(), "The wrong exception message was given");
    }

    @Test
    void nonRecoverableTest()
    {
        TransactionException te = new TransactionException(List.of(
            new Exception("something bad happened."),
            new TransactionException("This means there is db corruption", List.of(new Error("Corruption!!")), true),
            new Error("SEGFAULT!")
        ));
        assertTrue(te.isNonRecoverable());
        assertEquals(3, te.exceptionCount(), "Wrong number of exceptions reported");
        assertEquals(NONRECOVERABLE_TEST_OUT, te.getMessage(), "The wrong exception message was given");
    }

    private TransactionException getException()
    {
        return new TransactionException(List.of(
            new Exception("One exception."),
            new RuntimeException("Number 2"),
            new OutOfMemoryError("Ut oh")
        ));
    }
}
