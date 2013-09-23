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

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for TransactionException
 *
 * @version $Id$
 * @since 3.0M2
 */
public class TransactionExceptionTest
{
    private static final String SIMPLE_TEST_OUT =
        "Caused by:\n"
      + "java.lang.Exception\n"
      + "\t" + "One exception.\n"
      + "java.lang.RuntimeException\n"
      + "\t" + "Number 2\n"
      + "java.lang.OutOfMemoryError\n"
      + "\t" + "Ut oh\n";

    private static final String COMPOUND_TEST_OUT =
        "Caused by:\n"
      + "java.lang.Exception\n"
      + "\t" + "something bad happened.\n"
      + "org.xwiki.store.TransactionException\n"
      + "\t" + "Caused by:\n"
      + "\t" + "java.lang.Exception\n"
      + "\t" + "\t" + "One exception.\n"
      + "\t" + "java.lang.RuntimeException\n"
      + "\t" + "\t" + "Number 2\n"
      + "\t" + "java.lang.OutOfMemoryError\n"
      + "\t" + "\t" + "Ut oh\n"
      + "\t" + "\n"
      + "java.lang.Error\n"
      + "\t" + "SEGFAULT!\n";

    private static final String NONRECOVERABLE_TEST_OUT =
        "Caused by:\n"
      + "java.lang.Exception\n"
      + "\t" + "something bad happened.\n"
      + "org.xwiki.store.TransactionException\n"
      + "\t" + "This means there is db corruption\n"
      + "\t" + "Caused by:\n"
      + "\t" + "java.lang.Error\n"
      + "\t" + "\t" + "Corruption!!\n"
      + "\t" + "\n"
      + "java.lang.Error\n"
      + "\t" + "SEGFAULT!\n";

    /**
     * Make sure the messages from the underlying throwables are preserved.
     */
    @Test
    public void simpleExceptionTest()
    {
        TransactionException te = this.getException();
        Assert.assertFalse(te.isNonRecoverable());
        Assert.assertEquals("Wrong number of exceptions reported", 3, te.exceptionCount());
        Assert.assertEquals("The wrong exception message was given", SIMPLE_TEST_OUT, te.getMessage());
    }

    @Test
    public void compoundExceptionTest()
    {
        TransactionException te = new TransactionException(new ArrayList<Throwable>() {{
            add(new Exception("something bad happened."));
            add(getException());
            add(new Error("SEGFAULT!"));
        }});
        Assert.assertFalse(te.isNonRecoverable());
        Assert.assertEquals("Wrong number of exceptions reported", 5, te.exceptionCount());
        Assert.assertEquals("The wrong exception message was given", COMPOUND_TEST_OUT, te.getMessage());
    }

    @Test
    public void nonRecoverableTest()
    {
        TransactionException te = new TransactionException(new ArrayList<Throwable>() {{
            add(new Exception("something bad happened."));
            add(new TransactionException("This means there is db corruption",
                    new ArrayList<Throwable>() {{
                        add(new Error("Corruption!!"));
                    }}, true));
            add(new Error("SEGFAULT!"));
        }});
        Assert.assertTrue(te.isNonRecoverable());
        Assert.assertEquals("Wrong number of exceptions reported", 3, te.exceptionCount());
        Assert.assertEquals("The wrong exception message was given",
            NONRECOVERABLE_TEST_OUT, te.getMessage());
    }

    private TransactionException getException()
    {
        return new TransactionException(new ArrayList<Throwable>() {{
            add(new Exception("One exception."));
            add(new RuntimeException("Number 2"));
            add(new OutOfMemoryError("Ut oh"));
        }});
    }
}
