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
import java.util.List;
import java.util.ListIterator;

/**
 * A construct for altering storage in a safe way.
 * TransactionRunnable represents a storage transaction or a unit of work which may be done inside
 * of a transaction. It provides hooks to acquire locks, do work, commit and or roll the work back,
 * and release locks and cleanup temporary storage. Unless a TransactionRunnable extends
 * StartableTransactionRunnable, it must be run inside of another transaction and it may by the generic
 * {@code <T>} specify that it can only be run inside of a certain type of TransactionRunnable.
 *
 * @param <T> The type of TransactionRunnable which this TransactionRunnable must be run inside of.
 * A TransactionRunnable which alters a database through Hibernate would fail if it was
 * started outside of a TransactionRunnable which began and committed a transaction around
 * it. A class which extends {@code TransactionRunnable<DatabaseTransactionRunnable>} can only be
 * run inside of a DatabaseTransactionRunnable or a subclass of it. Breaking that rule will
 * be a compile time error.
 * @version $Id$
 * @since 3.0M2
 */
public class TransactionRunnable<T>
{
    /**
     * All of the runnables to be run then committed.
     */
    private final List<TransactionRunnable> allRunnables = new ArrayList<TransactionRunnable>();

    /**
     * The runnable which this runnable is being run inside of.
     * Used to check for loops and double assignment.
     */
    private TransactionRunnable<T> parent;

    /**
     * If true then this runnable has already started and nothing else may be runIn it.
     */
    private boolean hasPreRun;

    /**
     * Run this TransactionRunnable inside of a "parent" runnable.
     * This runnable will not be pre-run until after the parent is pre-run and this will not be
     * run until after the parent runnable is run. This runnable will be committed before the parent
     * and if something goes wrong, this runnable will be rolled back before it's parent.
     * This runnable will have onComplete called after it's parent though.
     * It is safe to have a parent runnable start a transaction inside of it's onRun() function and
     * commit it inside of it's onCommit() function and allow that transaction to be used by the
     * children of that runnable.
     *
     * If multiple runnables are run in a parent, they will be prerun, run, and onComplete'd in the
     * order they were added and they will be committed and/or rolled back in reverse the order they
     * were added.
     *
     * By using the return value of this function, it is possible to sandwich a TransactionRunnable which
     * with few requirements between 2 runnables with many requirements. Normally you cannot run a
     * {@code TransactionRunnable<DatabaseTransaction>} of a TransactionRunnable which does not offer database
     * access. However, when you add a runnable which does not need or offer database access to one which
     * does, this function returns that runnable casted to a type which does offer database access
     * (since it is running in one which does).
     * {@code
     * StartableTransactionRunnable<DbTransaction> transaction = new DbTransactionRunnable();
     * StartableTransactionRunnable<Standalone> standalone = new StandaloneTransactionRunnable();
     * TransactionRunnable<DbTransaction> runnableRequiringDb = new DbRequiringTransactionRunnable();
     *
     * // This will not compile:
     * runnableRequiringDb.runIn(standalone);
     * // Because if it did, it would allow you to do this:
     * standalone.start();
     * // Ut oh, using the database outside of a transaction!
     *
     * // This will work:
     * TransactionRunnable<DbTransaction> castedStandalone = standalone.runIn(transaction);
     * runnableRequiringDb.runIn(castedStandalone);
     * transaction.start();
     * }
     *
     * @param <U> The type of capabilities provided by the parent runnable.
     * This defines the state which the state which the storage engine is guaranteed to be in
     * when this runnable starts. It must extend the type of capabilities required by this
     * runnable.
     * @param parentRunnable the TransactionRunnable to run this runnable inside of.
     * @return this runnable casted to a TransactionRunnable with the capabilities of it's parent.
     * @throws IllegalStateException if this function has already been called on this runnable because a
     * TransactionRunnable may only be run once.
     * @throws IllegalArgumentException if this runnable is an ancestor of the parentRunnable as it would
     * create an unresolvable loop.
     */
    public <U extends T> TransactionRunnable<U> runIn(final TransactionRunnable<U> parentRunnable)
    {
        if (this.parent != null) {
            throw new IllegalStateException("This TransactionRunnable is already scheduled to run inside "
                + this.parent.toString() + " and cannot be run in "
                + parentRunnable.toString() + " too.");
        }
        if (parentRunnable.hasPreRun) {
            throw new IllegalStateException("This TransactionRunnable cannot be runIn() " + parentRunnable
                + " because it has already started 'the ship has set sail'");
        }
        parentRunnable.assertNoLoop(this);

        // U extends T so this is safe.
        this.parent = (TransactionRunnable<T>) parentRunnable;

        parentRunnable.allRunnables.add(this);

        // Since this runnable runs inside of parentRunnable this cast is safe because all of the
        // pre-run and post-run operations will be executed by the parent.
        return (TransactionRunnable<U>) this;
    }

    /**
     * Get whatever is required by this TransactionRunnable.
     * In the case of a {@link ProvidingTransactionRunnable} this will get what is <i>required</i>
     * by that runnable, not what is <i>provided</i>. To get the provided context from a
     * {@link ProvidingTransactionRunnable} use {@link ProvidingTransactionRunnable#getProvidedContext()}
     *
     * @return an implementation of T, the context which is required by this TransactionRunnable.
     */
    protected T getContext()
    {
        if (this.parent != null) {
            if (this.parent instanceof ProvidingTransactionRunnable) {
                // Casting this.parent to ProvidingTransactionRunnable is safe because instanceof says so.
                // Casting the provided context to T is safe because T be what it provides and to add it
                // as a ProvidingTR, what it provides (P) must extend T, and adding it as a plain
                // TransactionRunnable, what it requires must extend T and what it provides must extend
                // what it requires so this.parent.getProvidedContext() is guarenteed to extend T.
                return (T) ((ProvidingTransactionRunnable) this.parent).getProvidedContext();
            }
            return this.parent.getContext();
        }
        return null;
    }

    /**
     * This will be run first.
     * This MUST NOT alter the state of storage engine.
     * This is intended for acquiring locks and putting temporary storage in order prior to calling run().
     *
     * @throws Exception which will cause the execution of
     * onRollback then onComplete before being wrapped in a TransactionException and rethrown.
     */
    protected void onPreRun() throws Exception
    {
        // By default this will do nothing.
    }

    /**
     * This will be run after preRun and before onCommit.
     * This SHOULD NOT alter the state of storage engine except for it's own temporary storage.
     * Whatever it does MUST be able to be reverted by calling onRollback().
     *
     * @throws Exception which will cause a rollback of the transaction and then execution of
     * onRollback then onComplete before being wrapped in a TransactionException and rethrown.
     */
    protected void onRun() throws Exception
    {
        // By default this will do nothing.
    }

    /**
     * This will be run after onRun is complete.
     * After this function has completed successfully the storage engine MUST be in it's completed state.
     * No work may be done in onComplete() except for cleanup of temporary files.
     *
     * @throws Exception which will stop the committing process and cause onRollback to be invoked on each
     * of the runnables in the chain before being wrapped in a TransactionException and
     * rethrown.
     */
    protected void onCommit() throws Exception
    {
        // By default this will do nothing.
    }

    /**
     * This will run if the transaction fails.
     * This function is guaranteed to be run no matter what happens before in this transaction.
     * onRollback() will NOT run for a given TransactionRunnable unless onRun() has been called.
     * onRollback() will be called for each transaction in the reverse order as they are run, children
     * first, siblings in opposite order as registered.
     *
     * This MUST roll the storage engine back to it's prior state even after onCommit has been invoked.
     * TransactionRunnables which cannot rollback after a successful onCommit must extend
     * {@link RootTransactionRunnable} which it cannot be run inside of another runnable.
     *
     * @throws Exception which will be reported as the cause of possible storage corruption.
     * before being wrapped in a TransactionException and rethrown.
     */
    protected void onRollback() throws Exception
    {
        // By default this will do nothing.
    }

    /**
     * This will be run when after onCommit or onRollback no matter the outcome.
     * This function is guaranteed to be run no matter what happens before in this transaction.
     * This is intended to do post commit cleanup and it MUST NOT be used to finalize a commit.
     * onComplete() will NOT run for a given TransactionRunnable unless onPreRun() has been called.
     * onComplete() will be called for each transaction in the reverse order as they are run, children
     * first, siblings in opposite order as registered.
     *
     * @throws Exception which will be reported as a failure which cannot cause database corruption.
     */
    protected void onComplete() throws Exception
    {
        // By default this will do nothing.
    }

    /* -------------------- Internals -------------------- */

    /**
     * Make sure that the given runnable is not this object, not any one of it's parents.
     *
     * @param runnable to check against.
     * @throws IllegalArgumentException if the runnable is the same as this one.
     */
    private void assertNoLoop(TransactionRunnable runnable)
    {
        if (this == runnable) {
            throw new IllegalArgumentException("A TransactionRunnable cannot be run inside of itself as "
                + "it would create a loop which would never resolve.");
        }
        if (this.parent != null) {
            this.parent.assertNoLoop(runnable);
        }
    }

    /**
     * PreRun this and all of the chained runnables.
     * Run in the order as they were registered in a deep first tree walk.
     * If an exception occures, call onComplete on each runnable in the reverse order preRun was called.
     *
     * @throws TransactionException if an exception is thrown by this or one of the chained runnables'
     * onPreRun() functions, also may contain exceptions thrown by one or more
     * of the chained runnables' onComplete() functions.
     */
    protected final void preRun() throws TransactionException
    {
        final ListIterator<TransactionRunnable> runPathIterator = this.getRunPath().listIterator();
        try {
            while (runPathIterator.hasNext()) {
                final TransactionRunnable tr = runPathIterator.next();
                tr.hasPreRun = true;
                tr.onPreRun();
            }
        } catch (Throwable t) {
            final List<Throwable> errors = new ArrayList<Throwable>();
            errors.add(t);
            try {
                completeAll(runPathIterator);
            } catch (TransactionException e) {
                errors.add(e);
            }
            throw new TransactionException("Failure in onPreRun()", errors, false);
        }
    }

    /**
     * @return all TransactionRunnables under and including this one in the order they need to be run.
     */
    private List<TransactionRunnable> getRunPath()
    {
        List<TransactionRunnable> runPath = new ArrayList<TransactionRunnable>();
        this.addAllToRunPath(runPath);
        return runPath;
    }

    /**
     * Get all TransactionRunnables under and including this one in the order they need to be run.
     *
     * @param runPath a list of TransactionRunnable, every transactionRunnable in the chain
     * under this will be added in the order they should be run.
     */
    private void addAllToRunPath(final List<TransactionRunnable> runPath)
    {
        runPath.add(this);
        for (TransactionRunnable run : this.allRunnables) {
            run.addAllToRunPath(runPath);
        }
    }

    /**
     * Run this and all of the chained runnables.
     * Run in the same order as they were registered, deep first tree walking.
     * If an exception is thrown, all runnables so far run will be rollback'd and all runnables will be
     * completed. Rollback and complete will happen in the reverse order as run.
     *
     * @throws TransactionException made by grouping together whatever is thrown by this or one of the
     * chained runnables' onRun() functions and whatever might be thrown by
     * onRollback() or onComplete() which are called if somethign goes wrong.
     */
    protected final void run() throws TransactionException
    {
        final ListIterator<TransactionRunnable> runPathIterator = this.getRunPath().listIterator();
        try {
            while (runPathIterator.hasNext()) {
                runPathIterator.next().onRun();
            }
        } catch (Throwable t) {
            final List<Throwable> errors = new ArrayList<Throwable>();
            errors.add(t);
            try {
                rollbackAll(runPathIterator);
            } catch (TransactionException e) {
                errors.add(e);
            }
            try {
                this.complete();
            } catch (TransactionException e) {
                errors.add(e);
            }
            throw new TransactionException("Failure in onRun()", errors, false);
        }
    }

    /**
     * Commit this and all of the chained runnables.
     * Committed in the reverse order as they were run().
     * If any of the runnables throws an exception while committing, all will be rollback'd in reverse the
     * order they were run, starting at the last one, not starting at the one which failed.
     * After all are rolled back, onComplete() will be called on each, also in reverse order.
     *
     * @throws TransactionException made from the exception thrown by this or one of the child runnables'
     * onCommit function or rollback or complete.
     */
    protected final void commit() throws TransactionException
    {
        final List<TransactionRunnable> runPath = this.getRunPath();
        final ListIterator<TransactionRunnable> runPathReverseIterator =
            runPath.listIterator(runPath.size());

        try {
            while (runPathReverseIterator.hasPrevious()) {
                runPathReverseIterator.previous().onCommit();
            }
        } catch (Throwable t) {
            final List<Throwable> errors = new ArrayList<Throwable>();
            errors.add(t);
            try {
                this.rollback();
            } catch (TransactionException e) {
                errors.add(e);
            }
            try {
                this.complete();
            } catch (TransactionException e) {
                errors.add(e);
            }
            throw new TransactionException("Failure in onCommit()", errors, false);
        }
    }

    /**
     * Rollback this and all of the chained runnables.
     * Run in the reverse order as they were run so that each runnable
     * will be rolling back a storage engine which is in as close as possible a state to what it was
     * when onRun() was called for that runnable.
     * onRollback() will NOT run for a given TransactionRunnable unless onRun() has been called.
     *
     * @throws TransactionException made from gathering whatever exceptions were thrown
     * running onRollback() on this and the child runnables.
     */
    protected final void rollback() throws TransactionException
    {
        final List<TransactionRunnable> runPath = this.getRunPath();
        final ListIterator<TransactionRunnable> runPathReverseIterator =
            runPath.listIterator(runPath.size());
        rollbackAll(runPathReverseIterator);
    }

    /**
     * Run onComplete() on this and each of the chained runnables.
     * Run in the opposite order as they were registered, child before parent.
     *
     * @throws TransactionException if one or more of the runnables throw any exception.
     */
    protected final void complete() throws TransactionException
    {
        final List<TransactionRunnable> runPath = this.getRunPath();
        final ListIterator<TransactionRunnable> runPathReverseIterator =
            runPath.listIterator(runPath.size());
        completeAll(runPathReverseIterator);
    }

    /*--------------------Stateless functions--------------------*/

    /**
     * Call onComplete() on each TransactionRunnable in the iterator.
     * Call them in reverse order until hasPrevious() returns false starting at whatever point the
     * the iterator is left at.
     *
     * @param iterator the iterator of TransactionRunnables to complete.
     * @throws TransactionException made up any exceptions throws by any of the onComplete calls.
     * this function does not stop for exceptions.
     */
    private static void completeAll(final ListIterator<TransactionRunnable> iterator)
        throws TransactionException
    {
        final List<ExceptionThrowingRunnable> list = new ArrayList<ExceptionThrowingRunnable>();

        while (iterator.hasPrevious()) {
            final TransactionRunnable runnable = iterator.previous();
            list.add(new ExceptionThrowingRunnable()
            {
                public void run() throws Exception
                {
                    runnable.onComplete();
                }
            });
        }
        doAllAndCollectThrowables(list, "Failure in onComplete() the storage engine should be "
            + "consistant although it may contain uncollected garbage.",
            false);
    }

    /**
     * Call onRollback() on each TransactionRunnable in the iterator.
     *
     * @param iterator the iterator of TransactionRunnables to rollback.
     * @throws TransactionException made up any exceptions throws by any of the onRollback calls.
     * this function does not stop for exceptions.
     */
    private static void rollbackAll(final ListIterator<TransactionRunnable> iterator)
        throws TransactionException
    {
        final List<ExceptionThrowingRunnable> list = new ArrayList<ExceptionThrowingRunnable>();

        while (iterator.hasPrevious()) {
            final TransactionRunnable runnable = iterator.previous();
            list.add(new ExceptionThrowingRunnable()
            {
                public void run() throws Exception
                {
                    runnable.onRollback();
                }
            });
        }

        doAllAndCollectThrowables(list, "Failure in onRollback() the storage engine might be "
            + "in an inconsistent state", true);
    }

    /**
     * Execute a list of ExceptionThrowingRunnables and group any error messages together.
     * This is run when despite an exception thrown by one runnable, the show must go on.
     *
     * @param runnables the things to run.
     * @param message the error message to add to the TransactionException if it is thrown.
     * @param isNonRecoverable true if this operation is critical and an exception cannot be recovered
     * from eg: storage corruption.
     * @throws TransactionException made by grouping all of the exceptions from the runnables together.
     */
    private static void doAllAndCollectThrowables(final List<ExceptionThrowingRunnable> runnables,
        final String message,
        final boolean isNonRecoverable)
        throws TransactionException
    {
        List<Throwable> causes = null;
        for (ExceptionThrowingRunnable run : runnables) {
            try {
                run.run();
            } catch (Throwable t) {
                if (causes == null) {
                    causes = new ArrayList<Throwable>();
                }
                causes.add(t);
            }
        }
        if (causes != null) {
            throw new TransactionException(message, causes, isNonRecoverable);
        }
    }

    /**
     * A closure which is capable of throwing an exception.
     */
    private interface ExceptionThrowingRunnable
    {
        /**
         * Do something.
         *
         * @throws Exception if something goes wrong while doing it.
         */
        void run() throws Exception;
    }
}
