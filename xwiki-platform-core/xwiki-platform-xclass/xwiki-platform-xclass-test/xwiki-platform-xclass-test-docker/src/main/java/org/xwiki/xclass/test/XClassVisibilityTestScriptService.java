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
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * Test-only script service that checks that an XClass document which is (re)created while another thread is loading a
 * document using it does not end up cached as non-existing.
 * <p>
 * The scenario it reproduces is the following. Loading a document opens one store transaction and resolves the XClass
 * of each of its objects <em>inside</em> that transaction (see {@code XWikiHibernateStore#loadXWikiDoc}, which calls
 * {@code BaseClass#newCustomClassInstance} per object while {@code HibernateStore#beginTransaction} joins the already
 * open session). On a database whose transaction isolation is {@code REPEATABLE READ}, that nested read is served from
 * the snapshot taken when the outer load started, so an XClass committed by another thread after that point is
 * invisible — and {@code XWikiCacheStore} then remembers that verdict indefinitely.
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
     * The XClass that gets deleted and recreated while the probe document is being loaded.
     */
    private static final String VICTIM_CLASS = "VictimClass";

    /**
     * The document carrying one object of every filler class plus one object of the victim class.
     */
    private static final String PROBE = "Probe";

    private static final String FIELD = "field";

    private static final String NEW_LINE = "\n";

    /**
     * The number given to the probe's victim object so that it sorts after every filler object, which all carry
     * number 0 because object numbers are per class. Any number greater than 0 would do.
     */
    private static final int VICTIM_OBJECT_NUMBER = 1;

    /**
     * Each step of the delay sweep must be comfortably longer than thread scheduling jitter, otherwise consecutive
     * rounds all aim at the same instant and the sweep stops covering the window.
     */
    private static final long MIN_SWEEP_STEP_MILLIS = 5;

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

    /**
     * Run the test.
     * <p>
     * Each round deletes the victim XClass, flushes the document cache, then loads the probe document in one thread
     * while a second thread recreates the victim XClass part-way through that load. The delay before recreating is
     * swept across the measured load window so that, over the whole run, the recreation is guaranteed to land both
     * before and after the moment the loading thread resolves the victim XClass — rather than relying on the order in
     * which the store happens to return the probe's objects.
     *
     * @param fillerClassCount the number of distinct filler XClasses the probe document holds an object of; they are
     *     what makes the probe's load long enough to aim at, and using distinct classes rather than many objects of
     *     one class keeps the window even if the store later batches object property loading
     * @param rounds the number of rounds, which is also the resolution of the delay sweep
     * @return an empty string if the victim XClass stayed visible in every round, a description of the failures
     *     otherwise
     * @throws Exception if the test could not be run at all
     */
    public String perform(int fillerClassCount, int rounds) throws Exception
    {
        StringBuilder errors = new StringBuilder();

        setUp(fillerClassCount);

        // Measure how long loading the probe takes with a cold cache. This is the window the recreation has to hit,
        // and asserting on it is what keeps this test honest: if a future change makes loading a document resolve its
        // XClasses in fewer round trips, the window collapses and the test would silently stop exercising anything.
        long windowMillis = measureProbeLoad();
        this.logger.warn("Probe document loads in [{}] ms with a cold cache.", windowMillis);

        long minWindowMillis = rounds * MIN_SWEEP_STEP_MILLIS;
        if (windowMillis < minWindowMillis) {
            errors.append(String.format(
                "Reproduction window collapsed: loading the probe document with a cold cache took %d ms, expected at "
                    + "least %d ms (%d rounds x %d ms per sweep step). Loading a document no longer resolves its "
                    + "XClasses in enough separate reads for this test to aim at, so it is no longer exercising the "
                    + "race it was written for. Raise the number of filler XClasses, or rework the test - do not "
                    + "simply delete it.%n",
                windowMillis, minWindowMillis, rounds, MIN_SWEEP_STEP_MILLIS));
            return errors.toString();
        }

        checkProbeShape(fillerClassCount, errors);
        if (!errors.isEmpty()) {
            return errors.toString();
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 1; round <= rounds; round++) {
                // Sweep the delay across the window so some round lands after the loading thread took its snapshot
                // and before it resolves the victim XClass.
                long delayMillis = (windowMillis * (round - 1)) / rounds;
                runRound(round, delayMillis, windowMillis, executor, errors);
            }
        } finally {
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                errors.append("Timed out waiting for the test threads to finish.").append(NEW_LINE);
            }
        }

        // Re-check the probe now that the rounds have deleted and recreated the victim XClass many times: if deleting
        // an XClass ever starts removing the objects using it, the rounds would have quietly emptied the probe and
        // stopped testing anything.
        this.contextProvider.get().getWiki().flushCache(this.contextProvider.get());
        checkProbeShape(fillerClassCount, errors);

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

    private void runRound(int round, long delayMillis, long windowMillis, ExecutorService executor,
        StringBuilder errors) throws Exception
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();

        // Start from a state where the victim XClass genuinely does not exist and nothing is cached, so that the
        // loading thread's snapshot cannot already contain it.
        deleteVictimClass();
        wiki.flushCache(context);

        CountDownLatch loadingStarted = new CountDownLatch(1);

        Future<Long> reader = executor.submit((Callable<Long>) () -> {
            pushFreshContext();
            try {
                loadingStarted.countDown();
                long start = System.currentTimeMillis();
                XWikiContext threadContext = this.contextProvider.get();
                threadContext.getWiki().getDocument(probeReference(threadContext), threadContext);
                return System.currentTimeMillis() - start;
            } finally {
                this.execution.popContext();
            }
        });

        Future<Void> writer = executor.submit((Callable<Void>) () -> {
            pushFreshContext();
            try {
                loadingStarted.await();
                Thread.sleep(delayMillis);
                createVictimClass();
                return null;
            } finally {
                this.execution.popContext();
            }
        });

        long loadMillis = reader.get(1, TimeUnit.MINUTES);
        writer.get(1, TimeUnit.MINUTES);

        // The victim XClass exists again at this point, so anything other than a fully defined XClass is a bug.
        XWikiDocument victim = wiki.getDocument(victimClassReference(context), context);
        boolean missing = victim.isNew();
        boolean emptyClass = victim.getXClass().getPropertyList().isEmpty();

        if (missing || emptyClass) {
            // Distinguish a poisoned cache entry from a creation that genuinely failed: if the document reappears
            // once the cache is flushed, the row was in the database all along.
            wiki.flushCache(context);
            XWikiDocument afterFlush = wiki.getDocument(victimClassReference(context), context);

            if (!afterFlush.isNew() && !afterFlush.getXClass().getPropertyList().isEmpty()) {
                errors.append(String.format(
                    "Round %d: the XClass [%s] was reported as %s although its row exists in the database — it "
                        + "reappeared after flushing the cache. The document cache kept a stale \"does not exist\" "
                        + "verdict produced while it was being recreated (recreated %d ms into a %d ms load, the "
                        + "load itself took %d ms).%n",
                    round, victimClassReference(context), missing ? "non-existing" : "having no properties",
                    delayMillis, windowMillis, loadMillis));
            } else {
                errors.append(String.format(
                    "Round %d: the XClass [%s] is still %s after flushing the cache, so recreating it did not work. "
                        + "This is not the cache problem this test looks for.%n",
                    round, victimClassReference(context), missing ? "missing" : "empty"));
            }
        }
    }

    private long measureProbeLoad() throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();

        // Two passes, keeping the longer one: the first pass may pay for lazily initialised state that has nothing to
        // do with the number of reads we are trying to measure.
        long longest = 0;
        for (int i = 0; i < 2; i++) {
            wiki.flushCache(context);
            long start = System.currentTimeMillis();
            wiki.getDocument(probeReference(context), context);
            longest = Math.max(longest, System.currentTimeMillis() - start);
        }

        return longest;
    }

    private void checkProbeShape(int fillerClassCount, StringBuilder errors) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWikiDocument probe = context.getWiki().getDocument(probeReference(context), context);

        int distinctClasses = probe.getXObjects().keySet().size();
        int expected = fillerClassCount + 1;

        if (distinctClasses != expected) {
            errors.append(String.format(
                "The probe document holds objects of %d distinct XClasses, expected %d. The test setup no longer "
                    + "produces the document it relies on.%n",
                distinctClasses, expected));
        }

        BaseObject victimObject = probe.getXObject(victimClassReference(context));
        if (victimObject == null) {
            errors.append(String.format(
                "The probe document has no object of the victim XClass [%s]. Deleting the XClass most likely removes "
                    + "the objects using it now, which this test did not expect.%n",
                victimClassReference(context)));
            return;
        }

        int highestOtherNumber = probe.getXObjects().entrySet().stream()
            .filter(entry -> !entry.getKey().equals(victimClassReference(context)))
            .flatMap(entry -> entry.getValue().stream())
            .filter(Objects::nonNull)
            .mapToInt(BaseObject::getNumber)
            .max()
            .orElse(-1);

        if (victimObject.getNumber() <= highestOtherNumber) {
            errors.append(String.format(
                "The victim object is number %d while another object has number %d, so the victim XClass is no longer "
                    + "the last one resolved while the probe loads and the delay sweep has nothing to aim before. The "
                    + "setup no longer produces the document this test relies on.%n",
                victimObject.getNumber(), highestOtherNumber));
        }
    }

    private void setUp(int fillerClassCount) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();

        for (int i = 0; i < fillerClassCount; i++) {
            saveSingleFieldClass(fillerClassReference(i, context), "Filler XClass for the XClass visibility test");
        }

        createVictimClass();

        // Recreate the probe from scratch so that repeated runs cannot accumulate objects on it.
        DocumentReference probeReference = probeReference(context);
        XWikiDocument existing = wiki.getDocument(probeReference, context);
        if (!existing.isNew()) {
            wiki.deleteDocument(existing, context);
        }

        XWikiDocument probe = wiki.getDocument(probeReference, context).clone();
        probe.setContent("Probe document for the XClass visibility test.");
        for (int i = 0; i < fillerClassCount; i++) {
            BaseObject object = probe.newXObject(fillerClassReference(i, context), context);
            object.setStringValue(FIELD, "filler " + i);
        }
        // Give the victim object the highest object number in the document. The store returns a document's objects
        // "order by bobject.number" and resolves each object's XClass in that order, so this makes the victim XClass
        // the LAST one resolved - near the end of the load window rather than at an arbitrary point in it, which is
        // what the delay sweep needs to be able to aim before it. Object numbers are per class, so every filler
        // object above is number 0 and the number set here only has to beat that.
        BaseObject victimObject = BaseClass.newCustomClassInstance(victimClassReference(context), context);
        probe.setXObject(VICTIM_OBJECT_NUMBER, victimObject);
        victimObject.setStringValue(FIELD, "victim");
        wiki.saveDocument(probe, "Probe document for the XClass visibility test", context);
    }

    private void createVictimClass() throws XWikiException
    {
        saveSingleFieldClass(victimClassReference(this.contextProvider.get()),
            "Victim XClass for the XClass visibility test");
    }

    private void deleteVictimClass() throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWikiDocument victim = context.getWiki().getDocument(victimClassReference(context), context);
        if (!victim.isNew()) {
            context.getWiki().deleteDocument(victim, context);
        }
    }

    private void saveSingleFieldClass(DocumentReference reference, String comment) throws XWikiException
    {
        XWikiContext context = this.contextProvider.get();
        XWiki wiki = context.getWiki();

        XWikiDocument document = wiki.getDocument(reference, context).clone();
        BaseClass xclass = document.getXClass();
        // Always (re)declare the field: the document we just loaded may be an empty one, and saving it as-is would
        // replace a perfectly good XClass with an empty one.
        xclass.addTextField(FIELD, "Field", 30);
        wiki.saveDocument(document, comment, context);
    }

    private DocumentReference fillerClassReference(int index, XWikiContext context)
    {
        return new DocumentReference("FillerClass" + index, testSpace(context));
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
     * A <em>new</em> one per round, not one per thread: {@code XWikiContext} also carries a class cache that
     * {@code XWikiHibernateStore#loadXWikiDoc} fills and {@code XWiki#getXClass} consults first, so a thread that
     * reused its context would serve the victim XClass from that map from the second round on and never read the
     * database again — every round after the first would silently test nothing.
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
}
