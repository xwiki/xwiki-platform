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
package org.xwiki.observation.remote.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.ClusterMemberDepartedEvent;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannels;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkMember;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultClusterMemberDepartureManager}. The tasks of the manager are run synchronously and the
 * time is controlled by the test. The channel is a mocked {@link JGroupsNetworkChannel} whose network view and
 * identified members are built like the real channel builds them.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultClusterMemberDepartureManagerTest
{
    private static final String LOCAL = "local";

    private static final String OTHER = "other";

    private static final String GONE = "gone";

    private static final long DELAY = 900;

    @InjectMockComponents
    private DefaultClusterMemberDepartureManager manager;

    @MockComponent
    private RemoteObservationManagerConfiguration configuration;

    @MockComponent
    private JGroupsNetworkChannels channels;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ExecutionContextManager executionContextManager;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private final TestClock clock = new TestClock();

    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    private JGroupsNetworkChannel channel;

    private JGroupsNetworkMember localMember;

    /**
     * A clock whose time is moved by the test.
     */
    private static final class TestClock extends Clock
    {
        private Instant instant = Instant.parse("2026-10-12T10:00:00Z");

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return this.instant;
        }

        void advance(long seconds)
        {
            this.instant = this.instant.plusSeconds(seconds);
        }
    }

    private static final class ScheduledTask
    {
        private final Runnable runnable;

        private final Instant time;

        private boolean cancelled;

        ScheduledTask(Runnable runnable, Instant time)
        {
            this.runnable = runnable;
            this.time = time;
        }
    }

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.configuration.isEnabled()).thenReturn(true);
        when(this.configuration.getId()).thenReturn(LOCAL);
        when(this.configuration.getReclaimDelay()).thenReturn(DELAY);
        when(this.configuration.getReclaimMaxAge()).thenReturn(86400L);

        this.localMember = mockMember(LOCAL, true);
        this.channel = mock(JGroupsNetworkChannel.class);
        setMembers(OTHER);
        when(this.channels.getChannels()).thenReturn(Map.of("tcp", this.channel));

        // Run the tasks synchronously, the scheduled ones when the test moves the time
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(executor).execute(any());
        when(executor.schedule(any(Runnable.class), anyLong(), any())).then(invocation -> {
            long delay = invocation.<TimeUnit>getArgument(2).toMillis(invocation.getArgument(1));
            ScheduledTask task = new ScheduledTask(invocation.getArgument(0), this.clock.instant().plusMillis(delay));
            this.scheduledTasks.add(task);
            ScheduledFuture<?> future = mock(ScheduledFuture.class);
            when(future.cancel(false)).then(cancelInvocation -> {
                task.cancelled = true;
                return true;
            });
            return future;
        });

        ReflectionUtils.setFieldValue(this.manager, "executor", executor);
        ReflectionUtils.setFieldValue(this.manager, "clock", this.clock);
    }

    private JGroupsNetworkMember mockMember(String id, boolean local)
    {
        JGroupsNetworkMember member = mock(JGroupsNetworkMember.class);
        when(member.getId()).thenReturn(id);
        when(member.isLocal()).thenReturn(local);

        return member;
    }

    /**
     * Set the network view of the channel, the identified members being derived from it as
     * {@link JGroupsNetworkChannel} does: indexed by identifier, the last member of the view with a given identifier
     * winning.
     */
    private void setView(JGroupsNetworkMember... viewMembers)
    {
        Map<String, JGroupsNetworkMember> members = new LinkedHashMap<>();
        for (JGroupsNetworkMember member : viewMembers) {
            if (member.getId() != null) {
                members.put(member.getId(), member);
            }
        }
        setView(List.of(viewMembers), List.copyOf(members.values()));
    }

    private void setView(List<JGroupsNetworkMember> viewMembers, List<NetworkMember> members)
    {
        when(this.channel.getViewMembers()).thenReturn(viewMembers);
        when(this.channel.getMembers()).thenReturn(members);
    }

    /**
     * Set the members of the channel: the current instance and the members with the passed identifiers.
     */
    private void setMembers(String... ids)
    {
        List<JGroupsNetworkMember> members = new ArrayList<>();
        members.add(this.localMember);
        for (String id : ids) {
            members.add(mockMember(id, false));
        }
        setView(members.toArray(JGroupsNetworkMember[]::new));
    }

    private void setNotRunning()
    {
        setView(List.of(), List.of());
    }

    /**
     * Move the time and run the scheduled tasks which are due, in the order of their time.
     */
    private void advance(long seconds)
    {
        this.clock.advance(seconds);

        while (true) {
            ScheduledTask next = this.scheduledTasks.stream()
                .filter(task -> !task.cancelled && !task.time.isAfter(this.clock.instant()))
                .min(Comparator.comparing(task -> task.time)).orElse(null);
            if (next == null) {
                break;
            }
            this.scheduledTasks.remove(next);
            next.runnable.run();
        }
    }

    private long pendingTasks()
    {
        return this.scheduledTasks.stream().filter(task -> !task.cancelled).count();
    }

    private void verifyDepartedNotified(String memberId, int times)
    {
        verify(this.observationManager, times(times)).notify(any(ClusterMemberDepartedEvent.class),
            same(this.manager), eq(memberId));
    }

    private void verifyNothingNotified()
    {
        verify(this.observationManager, never()).notify(any(), any(), any());
    }

    /**
     * The other member leaves at the current time.
     */
    private Instant leave()
    {
        this.manager.updateAsync(null);
        setMembers();
        // The member carried by the event is not trusted (it's the previous leader without XWIKI-25245)
        this.manager.updateAsync(LOCAL);

        return this.clock.instant();
    }

    private static String left(String memberId)
    {
        return "Cluster member [" + memberId + "] left, its pending work will be taken over in [900] seconds if it"
            + " does not come back";
    }

    private static String watched(String memberId)
    {
        return "Cluster member [" + memberId + "] is not part of the cluster, its pending work will be taken over in"
            + " [900] seconds if it does not join";
    }

    private static String departed(String memberId, Instant since)
    {
        return "Cluster member [" + memberId + "] is considered gone for good (absent since [" + Date.from(since)
            + "])";
    }

    private static String back(String memberId)
    {
        return "Cluster member [" + memberId + "] came back";
    }

    private static String postponed(String memberId)
    {
        return "Cluster member [" + memberId + "] has been absent for more than [900] seconds but is not considered"
            + " gone for good yet because some members of the cluster are not identified";
    }

    private void assertLogs(String... messages)
    {
        assertEquals(messages.length, this.logCapture.size());
        for (int i = 0; i < messages.length; i++) {
            assertEquals(messages[i], this.logCapture.getMessage(i));
        }
    }

    @Test
    void leftMemberIsDepartedAfterTheDelay()
    {
        Instant leftTime = leave();

        assertEquals(Date.from(leftTime), this.manager.getDepartureDate(OTHER));
        assertFalse(this.manager.isDeparted(OTHER));
        assertEquals(1, pendingTasks());

        advance(DELAY - 1);

        assertFalse(this.manager.isDeparted(OTHER));
        verifyNothingNotified();

        advance(1);

        assertTrue(this.manager.isDeparted(OTHER));
        assertEquals(Date.from(leftTime), this.manager.getDepartureDate(OTHER));
        verifyDepartedNotified(OTHER, 1);

        // Notified only once, and nothing is scheduled anymore
        assertEquals(0, pendingTasks());
        advance(DELAY * 2);
        this.manager.updateAsync(null);
        verifyDepartedNotified(OTHER, 1);
        assertEquals(0, pendingTasks());

        assertLogs(left(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void memberComingBackBeforeTheDelayIsNotDeparted()
    {
        leave();
        advance(DELAY / 2);

        setMembers(OTHER);
        this.manager.updateAsync(null);

        assertNull(this.manager.getDepartureDate(OTHER));

        advance(DELAY);

        assertFalse(this.manager.isDeparted(OTHER));
        verifyNothingNotified();
        assertEquals(0, pendingTasks());

        // Leaving again starts a new clock
        Instant leftAgain = leave();
        assertEquals(Date.from(leftAgain), this.manager.getDepartureDate(OTHER));
        advance(DELAY);
        verifyDepartedNotified(OTHER, 1);

        assertLogs(left(OTHER), back(OTHER), left(OTHER), departed(OTHER, leftAgain));
    }

    @Test
    void memberComingBackAfterTheDelay()
    {
        Instant leftTime = leave();
        advance(DELAY);
        verifyDepartedNotified(OTHER, 1);

        // The member is back but the sweep did not run yet
        setMembers(OTHER);
        assertFalse(this.manager.isDeparted(OTHER));
        assertNull(this.manager.getDepartureDate(OTHER));

        this.manager.updateAsync(null);

        // Leaving again starts a new clock and a new notification
        Instant leftAgain = leave();
        advance(DELAY);
        verifyDepartedNotified(OTHER, 2);

        assertLogs(left(OTHER), departed(OTHER, leftTime),
            "Cluster member [other] came back after being considered gone for good", left(OTHER),
            departed(OTHER, leftAgain));
    }

    @Test
    void memberSharingItsIdentifierWithAnotherOneLeaves()
    {
        // Two other members share the same identifier: the channel lists the identifier once
        JGroupsNetworkMember otherMember1 = mockMember(OTHER, false);
        JGroupsNetworkMember otherMember2 = mockMember(OTHER, false);
        setView(this.localMember, otherMember1, otherMember2);
        this.manager.updateAsync(null);

        // One of them leaves: the identifier is still present
        setView(this.localMember, otherMember2);
        this.manager.updateAsync(OTHER);

        assertNull(this.manager.getDepartureDate(OTHER));
        assertEquals(0, pendingTasks());
        assertLogs();
    }

    @Test
    void olderMemberSharingTheLocalIdentifier()
    {
        // The current instance is the older of two members sharing its identifier: the channel indexes the identifier
        // with the younger member, so the network view does not know the identifier of the current instance
        JGroupsNetworkMember unidentifiedLocalMember = mockMember(null, true);
        JGroupsNetworkMember youngerMember = mockMember(LOCAL, false);
        setView(List.of(unidentifiedLocalMember, youngerMember), List.of(youngerMember));

        // The channel is running and the departures work
        assertEquals(Set.of(LOCAL), this.manager.getMemberIds());
        Instant watchTime = this.clock.instant();
        this.manager.watch(List.of(GONE));
        advance(DELAY);

        verifyDepartedNotified(GONE, 1);
        assertLogs(watched(GONE), departed(GONE, watchTime));
    }

    @Test
    void youngerMemberSharingTheLocalIdentifier()
    {
        // The current instance is the younger of two members sharing its identifier: the older member is not
        // identified in the network view, and could be any member
        JGroupsNetworkMember olderMember = mockMember(null, false);
        setView(List.of(olderMember, this.localMember), List.of(this.localMember));

        assertEquals(Set.of(LOCAL), this.manager.getMemberIds());
        this.manager.watch(List.of(GONE));
        advance(DELAY);

        // The departure is postponed
        verifyNothingNotified();
        assertFalse(this.manager.isDeparted(GONE));
        assertLogs(watched(GONE), postponed(GONE));
    }

    @Test
    void unidentifiedMemberPostponesTheDeparture()
    {
        Instant leftTime = leave();

        // A member is in the view but its identifier is not known (for example the member which left came back
        // through a merge of the network partitions and the identifiers were not exchanged again)
        JGroupsNetworkMember unidentifiedMember = mockMember(null, false);
        setView(List.of(this.localMember, unidentifiedMember), List.of(this.localMember));
        this.manager.updateAsync(null);

        assertNull(this.manager.getDepartureDate(OTHER));
        assertFalse(this.manager.isDeparted(OTHER));

        advance(DELAY);
        verifyNothingNotified();
        assertEquals(1, pendingTasks());

        // Still postponed, logged once
        advance(60);
        verifyNothingNotified();
        assertEquals(1, pendingTasks());

        // The member is identified: it's not the member which left
        setMembers("third");
        advance(60);

        verifyDepartedNotified(OTHER, 1);
        assertTrue(this.manager.isDeparted(OTHER));

        assertLogs(left(OTHER), postponed(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void unidentifiedMemberIdentifiedAsTheAbsentOne()
    {
        leave();

        JGroupsNetworkMember unidentifiedMember = mockMember(null, false);
        setView(List.of(this.localMember, unidentifiedMember), List.of(this.localMember));
        advance(DELAY);

        // The member is identified as the absent one
        setMembers(OTHER);
        advance(60);

        verifyNothingNotified();
        assertNull(this.manager.getDepartureDate(OTHER));
        assertEquals(0, pendingTasks());

        assertLogs(left(OTHER), postponed(OTHER), back(OTHER));
    }

    @Test
    void leftMemberReportedByTheEventOnly()
    {
        // The member was never seen by this instance, only reported by the event
        setMembers();
        this.manager.updateAsync("unknown");

        Instant leftTime = this.clock.instant();
        assertEquals(Date.from(leftTime), this.manager.getDepartureDate("unknown"));
        advance(DELAY);
        verifyDepartedNotified("unknown", 1);

        assertLogs(left("unknown"), departed("unknown", leftTime));
    }

    @Test
    void watch()
    {
        this.manager.watch(Arrays.asList(null, LOCAL, OTHER, GONE, GONE));

        Instant watchTime = this.clock.instant();
        assertNull(this.manager.getDepartureDate(LOCAL));
        assertNull(this.manager.getDepartureDate(OTHER));
        assertNull(this.manager.getDepartureDate(null));
        assertFalse(this.manager.isDeparted(null));
        assertEquals(Date.from(watchTime), this.manager.getDepartureDate(GONE));

        // Watching again does not restart the clock nor schedule another sweep
        advance(DELAY / 2);
        this.manager.watch(List.of(GONE));
        assertEquals(1, pendingTasks());

        advance(DELAY / 2);
        verifyDepartedNotified(GONE, 1);

        // Watching a departed member notifies again
        this.manager.watch(List.of(GONE));
        verifyDepartedNotified(GONE, 2);

        this.manager.watch(null);
        this.manager.watch(Arrays.asList((String) null));
        verifyDepartedNotified(GONE, 2);

        assertLogs(watched(GONE), departed(GONE, watchTime));
    }

    @Test
    void watchKeepsTheTimeTheMemberLeft()
    {
        Instant leftTime = leave();
        advance(DELAY / 2);

        this.manager.watch(List.of(OTHER));

        assertEquals(Date.from(leftTime), this.manager.getDepartureDate(OTHER));
        advance(DELAY / 2);
        verifyDepartedNotified(OTHER, 1);

        assertLogs(left(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void watchedMemberJoining()
    {
        this.manager.watch(List.of(GONE));

        setMembers(OTHER, GONE);
        this.manager.updateAsync(null);

        advance(DELAY);
        verifyNothingNotified();

        assertLogs(watched(GONE), back(GONE));
    }

    @Test
    void disabled()
    {
        when(this.configuration.isEnabled()).thenReturn(false);

        this.manager.updateAsync(OTHER);
        this.manager.watch(List.of(GONE));

        assertFalse(this.manager.isDeparted(GONE));
        assertNull(this.manager.getDepartureDate(GONE));
        assertEquals(Set.of(LOCAL), this.manager.getMemberIds());
        assertEquals(0, pendingTasks());
        verifyNoInteractions(this.channels);
        verifyNothingNotified();
    }

    @Test
    void negativeDelay()
    {
        when(this.configuration.getReclaimDelay()).thenReturn(-1L);

        leave();
        this.manager.watch(List.of(GONE));
        advance(DELAY);

        assertFalse(this.manager.isDeparted(OTHER));
        assertFalse(this.manager.isDeparted(GONE));
        assertNull(this.manager.getDepartureDate(GONE));
        assertEquals(Set.of(LOCAL), this.manager.getMemberIds());
        assertLogs();
        verifyNothingNotified();
    }

    @Test
    void noRunningChannel()
    {
        this.manager.updateAsync(null);

        // The channel does not include the current instance anymore (for example while it's reconnecting)
        setNotRunning();
        this.manager.updateAsync(OTHER);
        // Nothing is waiting: no retry
        assertEquals(0, pendingTasks());

        this.manager.watch(List.of(GONE));
        this.manager.watch(List.of(GONE, "gone2"));

        assertNull(this.manager.getDepartureDate(OTHER));
        assertNull(this.manager.getDepartureDate(GONE));
        assertEquals(Set.of(LOCAL), this.manager.getMemberIds());

        // A single retry for all the watches
        assertEquals(1, pendingTasks());
        advance(60);
        assertNull(this.manager.getDepartureDate(GONE));
        assertEquals(1, pendingTasks());

        // Once the channel is running again
        setMembers();
        advance(60);

        // The member which left while the channel was not running is noticed too
        Instant noticeTime = this.clock.instant();
        assertEquals(Date.from(noticeTime), this.manager.getDepartureDate(OTHER));
        assertEquals(Date.from(noticeTime), this.manager.getDepartureDate(GONE));
        assertEquals(Date.from(noticeTime), this.manager.getDepartureDate("gone2"));
        assertEquals(1, pendingTasks());
        advance(DELAY);
        verifyDepartedNotified(OTHER, 1);
        verifyDepartedNotified(GONE, 1);
        verifyDepartedNotified("gone2", 1);

        assertLogs(left(OTHER), watched(GONE), watched("gone2"), departed(OTHER, noticeTime),
            departed(GONE, noticeTime), departed("gone2", noticeTime));
    }

    @Test
    void channelStopsRunningDuringTheDelay()
    {
        Instant leftTime = leave();

        setNotRunning();
        advance(DELAY);

        // The departure is postponed
        verifyNothingNotified();
        assertFalse(this.manager.isDeparted(OTHER));
        assertEquals(1, pendingTasks());

        setMembers();
        advance(60);

        verifyDepartedNotified(OTHER, 1);
        assertEquals(Date.from(leftTime), this.manager.getDepartureDate(OTHER));

        assertLogs(left(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void delayChangedDuringTheDelay()
    {
        Instant leftTime = leave();

        when(this.configuration.getReclaimDelay()).thenReturn(DELAY * 2);
        advance(DELAY);

        verifyNothingNotified();
        assertFalse(this.manager.isDeparted(OTHER));

        advance(DELAY);
        verifyDepartedNotified(OTHER, 1);

        assertLogs(left(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void hugeDelays()
    {
        when(this.configuration.getReclaimDelay()).thenReturn(Long.MAX_VALUE);
        when(this.configuration.getReclaimMaxAge()).thenReturn(Long.MAX_VALUE);

        leave();
        advance(DELAY);

        assertFalse(this.manager.isDeparted(OTHER));
        assertEquals(1, pendingTasks());
        assertLogs("Cluster member [other] left, its pending work will be taken over in [3155760000] seconds if it"
            + " does not come back");
    }

    @Test
    void stop()
    {
        leave();
        this.manager.watch(List.of(GONE));

        this.manager.stop();

        assertEquals(0, pendingTasks());
        advance(DELAY);
        this.manager.updateAsync(null);
        this.manager.watch(List.of(GONE));

        assertFalse(this.manager.isDeparted(OTHER));
        assertNull(this.manager.getDepartureDate(GONE));
        verifyNothingNotified();

        assertLogs(left(OTHER), watched(GONE));
    }

    @Test
    void watchDuringStopDoesNotNotify()
    {
        Instant watchTime = this.clock.instant();
        this.manager.watch(List.of(GONE));
        advance(DELAY);

        // The instance stops while a watch of a departed member is being processed
        doAnswer(invocation -> {
            this.manager.stop();
            return Map.of("tcp", this.channel);
        }).when(this.channels).getChannels();
        this.manager.watch(List.of(GONE));

        verifyDepartedNotified(GONE, 1);

        assertLogs(watched(GONE), departed(GONE, watchTime));
    }

    @Test
    void pruning()
    {
        when(this.configuration.getReclaimMaxAge()).thenReturn(100L);

        Instant leftTime = leave();
        advance(DELAY);
        verifyDepartedNotified(OTHER, 1);

        advance(100);
        this.manager.updateAsync(null);
        assertTrue(this.manager.isDeparted(OTHER));

        advance(1);
        this.manager.updateAsync(null);
        assertFalse(this.manager.isDeparted(OTHER));
        assertNull(this.manager.getDepartureDate(OTHER));

        // Found again in some data: a new clock starts
        this.manager.watch(List.of(OTHER));
        assertEquals(Date.from(this.clock.instant()), this.manager.getDepartureDate(OTHER));

        assertLogs(left(OTHER), departed(OTHER, leftTime), watched(OTHER));
    }

    @Test
    void noPruningWithNegativeMaxAge()
    {
        when(this.configuration.getReclaimMaxAge()).thenReturn(-1L);

        Instant leftTime = leave();
        advance(DELAY * 1000);
        this.manager.updateAsync(null);

        assertTrue(this.manager.isDeparted(OTHER));

        assertLogs(left(OTHER), departed(OTHER, leftTime));
    }

    @Test
    void getMemberIds()
    {
        // Another running channel, with a member not identified yet
        JGroupsNetworkMember local = mockMember(LOCAL, true);
        JGroupsNetworkMember third = mockMember("third", false);
        JGroupsNetworkMember unidentified = mockMember(null, false);
        JGroupsNetworkChannel otherChannel = mock(JGroupsNetworkChannel.class);
        when(otherChannel.getViewMembers()).thenReturn(List.of(local, third, unidentified));
        when(otherChannel.getMembers()).thenReturn(List.of(local, third));
        // A channel which is not running (connecting)
        JGroupsNetworkMember fourth = mockMember("fourth", false);
        JGroupsNetworkChannel notRunningChannel = mock(JGroupsNetworkChannel.class);
        when(notRunningChannel.getViewMembers()).thenReturn(List.of(fourth));
        when(notRunningChannel.getMembers()).thenReturn(List.of(fourth));
        Map<String, JGroupsNetworkChannel> allChannels = new LinkedHashMap<>();
        allChannels.put("tcp", this.channel);
        allChannels.put("other", otherChannel);
        allChannels.put("connecting", notRunningChannel);
        when(this.channels.getChannels()).thenReturn(allChannels);

        Set<String> memberIds = this.manager.getMemberIds();
        assertEquals(Set.of(LOCAL, OTHER, "third"), memberIds);
        assertThrows(UnsupportedOperationException.class, () -> memberIds.add("fifth"));
    }

    @Test
    void getMemberIdsWithoutLocalIdentifier()
    {
        when(this.configuration.getId()).thenReturn(null);
        JGroupsNetworkMember local = mockMember(null, true);
        JGroupsNetworkMember other = mockMember(OTHER, false);
        setView(List.of(local, other), List.of(other));

        assertEquals(Set.of(OTHER), this.manager.getMemberIds());

        when(this.configuration.isEnabled()).thenReturn(false);

        assertEquals(Set.of(), this.manager.getMemberIds());
    }

    @Test
    void dispose() throws Exception
    {
        // Use a real executor
        ReflectionUtils.setFieldValue(this.manager, "executor", null);

        this.manager.watch(List.of(OTHER));

        this.manager.dispose();

        // Nothing is done anymore
        this.manager.updateAsync(null);
        this.manager.watch(List.of(GONE));
        assertFalse(this.manager.isDeparted(GONE));
        verifyNothingNotified();
    }
}
