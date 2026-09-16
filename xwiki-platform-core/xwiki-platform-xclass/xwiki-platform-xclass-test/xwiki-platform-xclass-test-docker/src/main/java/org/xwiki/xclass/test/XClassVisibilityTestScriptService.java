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
package org.xwiki.xclass.test;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.function.FailableRunnable;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.index.TaskManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.search.solr.internal.api.SolrIndexer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Test-only script service that checks that an XClass saved while a document using it is being loaded stays visible to
 * that load, and is not left in the document cache as non-existing or outdated.
 * <p>
 * Loading a document resolves the XClass of each of its objects <em>inside</em> the transaction opened for that load:
 * {@code XWikiHibernateStore#loadXWikiDoc} calls {@code BaseClass#newCustomClassInstance} per object, and the
 * {@code getDocument} this ends up in joins the already open transaction because
 * {@code HibernateStore#beginTransaction} returns {@code false} when the execution context already holds a session. On
 * a database whose transaction isolation is {@code REPEATABLE READ} - the InnoDB default, so MySQL and MariaDB - that
 * nested read is answered from the snapshot taken when the load started, so an XClass committed after that point is
 * invisible to it, and {@code XWikiCacheStore} then remembers that verdict with no expiry.
 * <p>
 * Rather than racing two threads and hoping one of them lands in that window, this test opens the transaction itself
 * and uses latches to order the three steps that matter, so it either reproduces the problem or does not, on every
 * machine: the loading thread takes a database snapshot, the main thread commits the XClass, and only then does the
 * loading thread load the document that uses it.
 * <p>
 * This component is only ever deployed by the functional tests of this module, never in a real distribution.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Singleton
@Named("xclassVisibilityTest")
public class XClassVisibilityTestScriptService implements ScriptService
{
    /**
     * Space holding every document created by this test.
     */
    private static final String SPACE = "XClassCacheVisibility";

    /**
     * The XClass that is recreated, respectively extended, while the probe document is being loaded.
     */
    private static final String VICTIM_CLASS = "VictimClass";

    /**
     * The document carrying an object of the victim XClass, whose load has to resolve that XClass.
     */
    private static final String PROBE = "Probe";

    /**
     * The field the victim XClass is created with.
     */
    private static final String FIELD = "field";

    /**
     * The field added to the victim XClass while the probe document is being loaded, in the second scenario.
     */
    private static final String ADDED_FIELD = "addedField";

    private static final String NEW_LINE = "\n";

    /**
     * How long to wait for the asynchronous work triggered by saving the XClass before giving up on it.
     */
    private static final long QUIESCENCE_TIMEOUT_SECONDS = 60;

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TaskManager taskManager;

    @Inject
    private SolrIndexer solrIndexer;

    /**
     * Run the test.
     *
     * @return an empty string if the victim XClass stayed visible in both scenarios, a description of the failures
     *     otherwise
     * @throws Exception if the test could not be run at all
     */
    public String perform() throws Exception
    {
        StringBuilder errors = new StringBuilder();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            checkRecreatedClassIsVisible(executor, errors);
            checkExtendedClassIsVisible(executor, errors);
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                errors.append("Timed out waiting for the loading thread to finish.").append(NEW_LINE);
            }
        }

        return errors.toString();
    }

    /**
     * The effective transaction isolation level of the store's JDBC connections.
     * <p>
     * Returned as one of the {@link Connection} {@code TRANSACTION_*} constants so that the test can assert on it
     * without knowing the database in use.
     *
     * @return the isolation level of a store connection
     * @throws XWikiException if the isolation level could not be read
     */
    public int getTransactionIsolation() throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();

        return context.getWiki().getHibernateStore().executeRead(context,
            session -> session.doReturningWork(Connection::getTransactionIsolation));
    }

    /**
     * The XClass does not exist when the probe document starts loading, and is created before its objects are
     * resolved. This is what an extension installation or a subwiki creation does while the wiki serves requests.
     */
    private void checkRecreatedClassIsVisible(ExecutorService executor, StringBuilder errors) throws Exception
    {
        XWikiContext context = this.contextProvider.get();

        saveVictimClass(FIELD);
        saveProbe();
        deleteVictimClass();
        context.getWiki().flushCache(context);

        ProbeLoad load = loadProbeWhile(executor, () -> saveVictimClass(FIELD));

        report("recreated", load, FIELD,
            "The XClass [%s] was created and committed before the probe document was loaded, but the load did not "
                + "see it: the class of the probe's object has no properties.",
            errors);
    }

    /**
     * The XClass exists but gains a property before the probe document's objects are resolved. This is what an
     * administrator extending a class on a live wiki does, and it poisons the cache with the previous version of the
     * class rather than with a non-existing one.
     */
    private void checkExtendedClassIsVisible(ExecutorService executor, StringBuilder errors) throws Exception
    {
        XWikiContext context = this.contextProvider.get();

        // Start from a class that only has the original field, whatever the previous scenario left behind.
        deleteVictimClass();
        saveVictimClass(FIELD);
        saveProbe();
        context.getWiki().flushCache(context);

        ProbeLoad load = loadProbeWhile(executor, () -> saveVictimClass(ADDED_FIELD));

        report("extended", load, ADDED_FIELD,
            "The property [" + ADDED_FIELD + "] was added to the XClass [%s] and committed before the probe document "
                + "was loaded, but the load did not see it: the class of the probe's object still has only its "
                + "original properties.",
            errors);
    }

    /**
     * Load the probe document in another thread, in a transaction that took its database snapshot before the given
     * action committed.
     *
     * @param executor the executor running the loading thread
     * @param commit the action committing the change to the victim XClass, run in the calling thread once the loading
     *     thread has taken its snapshot and before it loads the probe document
     * @return what the loading thread saw
     */
    private ProbeLoad loadProbeWhile(ExecutorService executor, FailableRunnable<Exception> commit) throws Exception
    {
        CountDownLatch snapshotTaken = new CountDownLatch(1);
        CountDownLatch committed = new CountDownLatch(1);

        Future<ProbeLoad> reader = executor.submit((Callable<ProbeLoad>) () -> {
            pushFreshContext();
            try {
                return loadProbeInOpenTransaction(snapshotTaken, committed);
            } finally {
                this.execution.popContext();
            }
        });

        if (!snapshotTaken.await(1, TimeUnit.MINUTES)) {
            // Release the loading thread in case it is still waiting, then let its own failure surface rather than
            // reporting a timeout with no cause.
            committed.countDown();
            reader.get(1, TimeUnit.MINUTES);
            throw new IllegalStateException("The loading thread did not take a database snapshot within a minute.");
        }

        try {
            commit.run();
        } finally {
            committed.countDown();
        }

        return reader.get(1, TimeUnit.MINUTES);
    }

    private ProbeLoad loadProbeInOpenTransaction(CountDownLatch snapshotTaken, CountDownLatch committed)
        throws Exception
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        XWikiHibernateStore store = wiki.getHibernateStore();

        boolean opened = store.beginTransaction(context);
        try {
            // Read inside the transaction before letting the other thread commit: InnoDB takes the snapshot a
            // REPEATABLE READ transaction sees at its first consistent read, not when the transaction begins.
            store.getSession(context).createQuery("select count(*) from XWikiDocument", Long.class).uniqueResult();
            Session sessionBeforeLoad = store.getSession(context);

            snapshotTaken.countDown();
            if (!committed.await(1, TimeUnit.MINUTES)) {
                throw new IllegalStateException(
                    "The XClass was not saved within a minute of the database snapshot being taken.");
            }

            // Make the load read the XClass from the database rather than from the document cache. Saving the
            // class invalidated its cache entry, but something else may have re-cached it in the meantime - a
            // listener reacting to the save reads the document in its own, newer transaction - and racing that is
            // what made an earlier version of this test unreliable. In production this state needs no help: the save
            // invalidates the entry and whichever load comes first has to go to the database.
            awaitQuiescence();
            flushDocumentCache(wiki);

            // This load joins the transaction opened above, the way the XClass resolution of each of a document's
            // objects joins the transaction opened by the document load itself in production.
            XWikiDocument probe = wiki.getDocument(probeReference(context), context);
            BaseObject victimObject = probe.getXObject(victimClassReference(context));
            BaseClass victimClass = victimObject == null ? null : victimObject.getXClass(context);

            return new ProbeLoad(opened, sessionBeforeLoad == store.getSession(context), victimObject != null,
                victimClass == null ? List.of() : List.copyOf(victimClass.getPropertyList()));
        } finally {
            if (opened) {
                store.endTransaction(context, false);
            }
        }
    }

    /**
     * Turn what the loading thread saw, and what the wiki serves afterwards, into error messages.
     *
     * @param scenario the name of the scenario, for the error messages
     * @param load what the loading thread saw
     * @param expectedProperty the property the victim XClass must have
     * @param staleMessage the message for a load that did not see the committed XClass, with the class reference as
     *     its single argument
     */
    private void report(String scenario, ProbeLoad load, String expectedProperty, String staleMessage,
        StringBuilder errors) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        DocumentReference victimReference = victimClassReference(context);

        if (!load.transactionOpened()) {
            errors.append(String.format("Scenario %s: the test could not open its own store transaction, so the "
                + "probe document was not loaded in a transaction older than the change to the XClass. This test "
                + "did not check anything.%n", scenario));
            return;
        }

        if (!load.sameSession()) {
            errors.append(String.format("Scenario %s: loading a document no longer runs in the store transaction the "
                + "caller had already opened, so this test no longer reproduces the transaction nesting it was "
                + "written for. It must be reworked rather than deleted.%n", scenario));
            return;
        }

        if (!load.probeHasVictimObject()) {
            errors.append(String.format("Scenario %s: the probe document has no object of the XClass [%s], so the "
                + "load had no reason to resolve that class. The test setup no longer produces the document this "
                + "test relies on.%n", scenario, victimReference));
            return;
        }

        if (!load.victimClassProperties().contains(expectedProperty)) {
            errors.append("Scenario ").append(scenario).append(": ")
                .append(String.format(staleMessage, victimReference)).append(NEW_LINE);
        }

        checkVisibleAfterLoad(scenario, expectedProperty, errors);
    }

    /**
     * Check what the wiki serves once the loading thread's transaction is over: a stale read is only a bug worth
     * reporting because the cache keeps it, and this distinguishes a poisoned cache entry from a change that never
     * made it to the database.
     */
    private void checkVisibleAfterLoad(String scenario, String expectedProperty, StringBuilder errors)
        throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        DocumentReference victimReference = victimClassReference(context);

        if (hasProperty(wiki.getDocument(victimReference, context), expectedProperty)) {
            return;
        }

        wiki.flushCache(context);

        if (hasProperty(wiki.getDocument(victimReference, context), expectedProperty)) {
            errors.append(String.format(
                "Scenario %s: the XClass [%s] has no property [%s] although its document in the database has one - it "
                    + "reappeared after flushing the cache. The document cache kept the verdict produced by the load "
                    + "that ran while the class was being saved.%n",
                scenario, victimReference, expectedProperty));
        } else {
            errors.append(String.format(
                "Scenario %s: the XClass [%s] still has no property [%s] after flushing the cache, so saving it did "
                    + "not work. This is not the cache problem this test looks for.%n",
                scenario, victimReference, expectedProperty));
        }
    }

    /**
     * Wait for the asynchronous work that saving the XClass triggered.
     * <p>
     * A document save queues work for the tasks consumer and for the Solr indexer, both of which load the document
     * again, in their own and therefore newer transactions, and leave it in the document cache. Letting that work
     * finish before emptying the cache is what leaves the load below as the only reader of the XClass, and so what
     * makes this test decide the same way on every machine instead of racing whatever the save set in motion.
     *
     * @throws Exception if the work did not finish in time
     */
    private void awaitQuiescence() throws Exception
    {
        awaitEmptyTaskQueue();
        // The indicator completes once everything submitted before this call has been processed.
        this.solrIndexer.waitReady().get(QUIESCENCE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        // Indexing itself queues tasks, so drain the queue once more now that the indexer is done.
        awaitEmptyTaskQueue();
    }

    private void awaitEmptyTaskQueue() throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(QUIESCENCE_TIMEOUT_SECONDS);
        while (this.taskManager.getQueueSize() > 0) {
            if (System.currentTimeMillis() > deadline) {
                throw new IllegalStateException(String.format(
                    "The task queue still held %d entries after %d seconds.", this.taskManager.getQueueSize(),
                    QUIESCENCE_TIMEOUT_SECONDS));
            }
            Thread.sleep(50);
        }
    }

    /**
     * Empty the document cache, so that the next load of a document has to read it from the database.
     *
     * @param wiki the wiki whose store to flush
     * @throws IllegalStateException if the store is not a caching one, in which case this test cannot check anything
     */
    private void flushDocumentCache(XWiki wiki)
    {
        if (!(wiki.getStore() instanceof XWikiCacheStoreInterface cacheStore)) {
            throw new IllegalStateException(
                "The store is not a caching store, so there is no cache for this test to check.");
        }

        cacheStore.flushCache();
    }

    private boolean hasProperty(XWikiDocument document, String property)
    {
        return !document.isNew() && document.getXClass().getPropertyList().contains(property);
    }

    /**
     * Recreate the probe document from scratch, so that repeated runs cannot accumulate objects on it.
     */
    private void saveProbe() throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();
        DocumentReference probeReference = probeReference(context);

        XWikiDocument existing = wiki.getDocument(probeReference, context);
        if (!existing.isNew()) {
            wiki.deleteDocument(existing, context);
        }

        XWikiDocument probe = wiki.getDocument(probeReference, context).clone();
        probe.setContent("Probe document for the XClass visibility test.");
        BaseObject victimObject = probe.newXObject(victimClassReference(context), context);
        victimObject.setStringValue(FIELD, "victim");
        wiki.saveDocument(probe, "Probe document for the XClass visibility test", context);
    }

    /**
     * Save the victim XClass with the given field, creating the document if needed.
     * <p>
     * The field is always (re)declared rather than only added when missing: the document this loads may itself be a
     * poisoned one, and saving that as-is would replace a perfectly good XClass with an empty one.
     */
    private void saveVictimClass(String field) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();

        XWikiDocument document = wiki.getDocument(victimClassReference(context), context).clone();
        document.getXClass().addTextField(field, field, 30);
        wiki.saveDocument(document, "Victim XClass for the XClass visibility test", context);
    }

    private void deleteVictimClass() throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWikiDocument victim = context.getWiki().getDocument(victimClassReference(context), context);
        if (!victim.isNew()) {
            context.getWiki().deleteDocument(victim, context);
        }
    }

    private DocumentReference victimClassReference(XWikiContext context)
    {
        return new DocumentReference(VICTIM_CLASS, testSpace(context));
    }

    private DocumentReference probeReference(XWikiContext context)
    {
        return new DocumentReference(PROBE, testSpace(context));
    }

    private SpaceReference testSpace(XWikiContext context)
    {
        return new SpaceReference(SPACE, context.getWikiReference());
    }

    /**
     * Give the calling thread a brand new execution context, and therefore its own store session and transaction,
     * since {@code HibernateStore} keeps both in the execution context.
     * <p>
     * It also matters that the context is new rather than reused: {@code XWikiContext} carries a class cache that
     * {@code XWikiHibernateStore#loadXWikiDoc} fills and {@code XWiki#getXClass} consults before the store, so a
     * thread reusing a context could answer from that map instead of reading the database.
     * <p>
     * The caller must {@link Execution#popContext()} when done.
     */
    private void pushFreshContext()
    {
        ExecutionContext context = new ExecutionContext();

        try {
            this.executionContextManager.initialize(context);
        } catch (Exception e) {
            this.logger.error("Error while initializing execution context", e);
        }

        XWikiContext xwikiContext = this.stubContextProvider.createStubContext();
        xwikiContext.declareInExecutionContext(context);
        this.execution.pushContext(context);
    }

    /**
     * What the thread loading the probe document saw.
     *
     * @param transactionOpened whether the loading thread opened the store transaction itself, rather than finding
     *     one already open
     * @param sameSession whether loading the document used the store session the loading thread had already opened
     * @param probeHasVictimObject whether the loaded probe document still holds an object of the victim XClass
     * @param victimClassProperties the properties of that object's XClass, as the load resolved it
     */
    private record ProbeLoad(boolean transactionOpened, boolean sameSession, boolean probeHasVictimObject,
        List<String> victimClassProperties)
    {
    }
}
