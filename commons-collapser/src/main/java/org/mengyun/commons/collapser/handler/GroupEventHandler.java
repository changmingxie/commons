/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.mengyun.commons.collapser.handler;

import com.lmax.disruptor.EventHandler;
import org.mengyun.commons.collapser.CollapserConfig;
import org.mengyun.commons.collapser.context.ThreadContextSynchronizationManager;
import org.mengyun.commons.collapser.queue.GroupEvent;
import org.mengyun.commons.collapser.queue.RequestPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This event handler gets passed messages from the RingBuffer as they become
 * available. Processing of these messages is done in a separate thread,
 * controlled by the {@code Executor} passed to the {@code Disruptor}
 * constructor.
 */
public class GroupEventHandler implements
        EventHandler<GroupEvent> {
    static final Logger logger = LoggerFactory.getLogger(GroupEventHandler.class);

    private final long ordinal;
    private final long workPoolSize;

    private CollapserConfig collapserConfig;

    private List<GroupEvent> groupEvents = new ArrayList<GroupEvent>();

    public GroupEventHandler(long ordinal, long workPoolSize, CollapserConfig collapserConfig) {
        this.ordinal = ordinal;
        this.workPoolSize = workPoolSize;
        this.collapserConfig = collapserConfig;
    }

    @Override
    public void onEvent(final GroupEvent event, final long sequence,
                        final boolean endOfBatch) throws Exception {

//        if ((sequence % workPoolSize) == ordinal) {
        if ((event.getRequestPromise().getGroup().hashCode() % workPoolSize) == ordinal) {
            doInvoke(event, sequence, endOfBatch);
        }
    }


    protected void doInvoke(GroupEvent event, long sequence, boolean endOfBatch) {

        groupEvents.add(event);

        int maxBatchSize = this.collapserConfig.getMaxBatchSize();

        if (groupEvents.size() >= maxBatchSize || endOfBatch) {

            try {

                logger.debug("batch done, seq:" + sequence + ", total events:" + groupEvents.size());

                //group by thread context
                Map<String, List<GroupEvent>> threadContextGroupedEventsMap = new HashMap<>();

                for (GroupEvent groupEvent : groupEvents) {

                    if (!threadContextGroupedEventsMap.containsKey(groupEvent.getThreadContext())) {
                        threadContextGroupedEventsMap.put(groupEvent.getThreadContext(), new ArrayList<>());
                    }

                    threadContextGroupedEventsMap.get(groupEvent.getThreadContext()).add(groupEvent);
                }

                for (Map.Entry<String, List<GroupEvent>> groupEventsEntry : threadContextGroupedEventsMap.entrySet()) {

                    List<GroupEvent> groupedEvents = groupEventsEntry.getValue();

                    //group by request group
                    Map<String, List<RequestPromise>> requestPromiseMap = new HashMap<>();

                    for (GroupEvent groupEvent : groupedEvents) {

                        if (!requestPromiseMap.containsKey(groupEvent.getRequestPromise().getGroup())) {
                            requestPromiseMap.put(groupEvent.getRequestPromise().getGroup(), new ArrayList<>());
                        }
                        requestPromiseMap.get(groupEvent.getRequestPromise().getGroup()).add(groupEvent.getRequestPromise());
                    }


                    ThreadContextSynchronizationManager threadContextSynchronizationManager = new ThreadContextSynchronizationManager(groupEventsEntry.getKey());

                    threadContextSynchronizationManager.executeWithBindThreadContext(new Runnable() {
                        @Override
                        public void run() {

                            for (Map.Entry<String, List<RequestPromise>> requestPromiseEntry : requestPromiseMap.entrySet()) {

                                List requests = new ArrayList();

                                for (RequestPromise requestPromise : requestPromiseEntry.getValue()) {
                                    requests.add(requestPromise.getRequest());
                                }

                                if (requestPromiseEntry.getValue().size() > 0) {

                                    try {
                                        Map map = requestPromiseEntry.getValue().get(0).getCallable().call(requests);

                                        for (RequestPromise requestPromise : requestPromiseEntry.getValue()) {
                                            requestPromise.setResponse(map.get(requestPromise.getRequest()));
                                            requestPromise.setSuccess(true);
                                        }

                                    } catch (Throwable throwable) {
                                        for (RequestPromise requestPromise : requestPromiseEntry.getValue()) {
                                            requestPromise.setSuccess(false);
                                            requestPromise.setThrowable(throwable);
                                        }
                                    } finally {
                                        for (RequestPromise requestPromise : requestPromiseEntry.getValue()) {
                                            synchronized (requestPromise) {
                                                requestPromise.setComplete(true);
                                                requestPromise.notify();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    });
                }
            } finally {

                try {
                    //try to notify
                    for (GroupEvent groupEvent : groupEvents) {
                        RequestPromise requestPromise = groupEvent.getRequestPromise();
                        if (!requestPromise.isComplete()) {
                            synchronized (requestPromise) {
                                requestPromise.setComplete(true);
                                requestPromise.notify();
                            }
                        }
                    }
                } finally {
                    for (GroupEvent groupEvent : groupEvents) {
                        groupEvent.clear();
                    }
                    groupEvents.clear();
                }
            }
        } else {
            logger.debug("batch doing, seq:" + sequence + ", total events:" + groupEvents.size());
        }
    }
}
