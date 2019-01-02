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

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * An exception made up of a group of exceptions.
 * This class satisfies the use case when multiple things which may throw exceptions
 * must be done, each may throw an exception but the others must run all the same.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class TransactionException extends Exception
{
    /**
     * The platform dependent newline string.
     */
    private static final String NEWLINE = System.getProperty("line.separator");

    /**
     * A tab character which will be used to tab in the messages from nested exceptions.
     */
    private static final String TAB = "\t";

    /**
     * The list of exceptions which caused this to be thrown.
     */
    private final List<Throwable> causes;

    /**
     * Does this failure mean that the storage engine may be corrupt?
     */
    private final boolean isNonRecoverable;

    /**
     * Total number of exceptions under this one.
     */
    private final int exceptionCount;

    /**
     * Constructor with message specified.
     *
     * @param causes the list of Throwables which caused this exception.
     */
    public TransactionException(final List<Throwable> causes)
    {
        this(null, causes);
    }

    /**
     * Constructor with message specified.
     *
     * @param message the message to give with the exception.
     * @param causes the list of Throwables which caused this exception.
     */
    public TransactionException(final String message, final List<Throwable> causes)
    {
        this(message, causes, false);
    }

    /**
     * Constructor with message specified.
     *
     * @param message the message to give with the exception.
     * @param causes the list of Throwables which caused this exception.
     * @param isNonRecoverable true if the storage engine could not recover from this
     * exception and corruption might have resulted.
     */
    public TransactionException(final String message,
        final List<Throwable> causes,
        final boolean isNonRecoverable)
    {
        super(message);
        this.causes = new ArrayList<Throwable>(causes);
        boolean nonRecoverable = isNonRecoverable;

        int total = 0;
        for (Throwable cause : this.causes) {
            if (cause instanceof TransactionException) {
                final TransactionException teCause = (TransactionException) cause;

                total += teCause.exceptionCount();
                if (teCause.isNonRecoverable()) {
                    nonRecoverable = true;
                }
            } else {
                total++;
            }
        }
        this.exceptionCount = total;
        this.isNonRecoverable = nonRecoverable;
    }

    @Override
    public synchronized Throwable getCause()
    {
        return this.causes.isEmpty() ? super.getCause() : this.causes.get(0);
    }

    /**
     * @return all of the exceptions which caused this exception to be thrown.
     */
    public List<Throwable> getCauses()
    {
        return new ArrayList<Throwable>(this.causes);
    }

    /**
     * @return the total number of exceptions which caused this exception to be thrown.
     */
    public int exceptionCount()
    {
        return this.exceptionCount;
    }

    /**
     * @return true if the storage engine could not recover from an exception in this group and
     *         corruption of the storage engine might have resulted.
     */
    public boolean isNonRecoverable()
    {
        return this.isNonRecoverable;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In this implementation the message is also included in the
     * stack trace so calling both is redundant.
     * </p>
     *
     * @see java.lang.Throwable#getMessage()
     */
    public String getMessage()
    {
        final Writer writer = new StringWriter();
        final PrintWriter printer = new PrintWriter(writer);
        printInfo(printer, false);
        return writer.toString();
    }

    /**
     * Utility method to get the stack trace for this exception as a string.
     *
     * @return what {@link #printStackTrace()} would have printed.
     */
    public String getStackTraceString()
    {
        final Writer writer = new StringWriter();
        final PrintWriter printer = new PrintWriter(writer);
        this.printStackTrace(printer);
        return writer.toString();
    }

    @Override
    public void printStackTrace()
    {
        this.printStackTrace(System.err);
    }

    @Override
    public void printStackTrace(final PrintStream writeTo)
    {
        this.printStackTrace(new PrintWriter(writeTo));
    }

    @Override
    public void printStackTrace(final PrintWriter writeTo)
    {
        this.printInfo(writeTo, true);
    }

    /**
     * Get the exception message or stack trace.
     *
     * @param writeTo the PrintWriter to write the output to.
     * @param includeStackTrace if true then an integrated message and stack trace is produced.
     */
    private void printInfo(final PrintWriter writeTo, final boolean includeStackTrace)
    {
        if (super.getMessage() != null) {
            writeTo.println(super.getMessage());
        }
        writeTo.println("Caused by:");

        for (Throwable cause : causes) {

            writeTo.println(cause.getClass().getName());
            writeTo.print(TAB);
            writeTo.print(("" + cause.getMessage()).replaceAll(NEWLINE, NEWLINE + TAB));
            writeTo.print(NEWLINE);

            if (includeStackTrace) {
                // Include the stack trace tabbed in for each so they are recognizable as different.
                // TODO End the stack trace at the frame which caused the TransactionException to throw.
                final Writer stw = new StringWriter();
                final PrintWriter stpw = new PrintWriter(stw);
                cause.printStackTrace(stpw);
                writeTo.print(stw.toString().replaceAll(NEWLINE, NEWLINE + TAB));
            }
        }
        if (includeStackTrace) {
            super.printStackTrace(writeTo);
        }
    }
}
