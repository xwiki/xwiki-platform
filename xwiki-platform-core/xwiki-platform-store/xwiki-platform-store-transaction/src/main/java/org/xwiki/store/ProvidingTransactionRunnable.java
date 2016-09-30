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

/**
 * A special type of TransactionRunnable which guarentees to provide state to the TransactionRunnables
 * which are run inside of it. It can also provide them with data.
 * Suppose you have a unit of work which loads data and that data must be saved in a later
 * TransactionRunnable in the same transaction.
 *
 * IE:
 * TR1 fetch data A1
 * TR2 update another data A2
 * TR3 perform some operation involving A1
 *
 * A TR3 can only run inside of a TR1.
 * To make things interesting we will assume that TR1, TR2, and TR3 all must run inside of a database
 * transaction.
 * TR1 is interesting because it <i>requires</i> a database transaction and it <i>provides</i> some data
 * (data A1).
 *
 * To implement this safely, TR1 should be a ProvidingTransactionRunnable.
 * We will assume that there already exists a TransactionRunnable for handling the begin and commit of the
 * database transaction, this will be called: DBStartableTransactionRunnable and we will assume that it
 * <i>provides</i> an interface called DBTransaction so TR1, TR2, and TR3 will all <i>require</i>
 * DBTransaction.
 *
 * <pre><code>
 * // MyInterfaceWithDataA1 must extend DBTransaction because all
 * // ProvidingTransactionRunnables must provide more than they require.
 * public interface MyInterfaceWithDataA1 extends DBTransaction { Data getA1(); }
 *
 * public class ImplementationWithDataA1 implements MyInterfaceWithDataA1 { ... }
 *
 * public class TR1 extends ProvidingTransactionRunnable&lt;DBTransaction, MyInterfaceWithDataA1&gt;
 * {
 *     private Data dataA1;
 *
 *     protected void onRun() { this.dataA1 = ..... }
 *
 *     {@literal @}Override
 *     protected MyInterfaceWithDataA1 getProvidedContext()
 *     {
 *         // this.getContext() will return a DBTransaction which our implementation can wrap.
 *         return new ImplementationWithDataA1(this.getContext(), this.dataA1);
 *     }
 * }
 *
 * public class TR2 extends TransactionRunnable&lt;DBTransaction&gt; { ... }
 *
 * public class TR3 extends TransactionRunnable&lt;MyInterfaceWithDataA1&gt;
 * {
 *     protected void onRun()
 *     {
 *         final Data A1 = this.getContext();
 *         ...
 *     }
 * }
 *
 * To use these classes:
 *
 * DBStartableTransactionRunnable run = new DBStartableTransactionRunnable(dataSourceName);
 * TransactionRunnable&lt;MyInterfaceWithDataA1&gt; tr1 = new TR1();
 * tr1.runIn(run);
 * TransactionRunnable&lt;DBTransaction&gt; tr2 = new TR2();
 * TransactionRunnable&lt;MyInterfaceWithDataA1&gt; tr2WithNewCapabilities = tr2.runIn(tr1.asProvider());
 * new TR3().runIn(tr2WithNewCapabilities);
 * // This would fail at compile time: new TR3().runIn(tr2);
 * run.start();
 * </code></pre>
 *
 * @param <R> The type of transaction or transaction state which this runnable requires.
 *            A TransactionRunnable which alters a database through Hibernate would fail if it was
 *            started outside of a TransactionRunnable which began and committed a transaction around
 *            it. A class which extends {@code TransactionRunnable<DatabaseTransaction>} can only be
 *            run inside of another {@code TransactionRunnable<DatabaseTransaction>} or inside of a
 *            {@code ProvidingTransactionRunnable<?, DatabaseTransaction>}
 *            Breaking that rule will be a compile time error.
 *
 * @param <P> the transaction state which this TransactionRunnable provides. Any
 *            {@code TransactionRunnable<P>} may be {@link TransactionRunnable#runIn(TransactionRunnable)}
 *            This runnable.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class ProvidingTransactionRunnable<R, P extends R> extends TransactionRunnable<R>
{
    /**
     * Get whatever is provided by this ProvidingTransactionRunnable.
     *
     * @return an implementation of P, the transaction type which this runnable runs in.
     *         null by default.
     * @see TransactionRunnable#getContext()
     */
    protected P getProvidedContext()
    {
        return null;
    }

    /**
     * View this as what it provides.
     * Cast this ProvidingTransactionRunnable to the type of
     * TransactionRunnable which can run inside of it.
     *
     * @return this ProvidingTransactionRunnable cast to a TransactionRunnable<P>
     */
    public TransactionRunnable<P> asProvider()
    {
        // This cast is ok because every PTR<R, P> promises to provide P.
        // calling runIn on the output from this cast is also safe because P extends R.
        return (TransactionRunnable<P>) this;
    }
}
