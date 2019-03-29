/*
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataconservancy.pass.loader.journal.nih;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads journal or updates records into the repository
 * <p>
 * Uses ISSN, name and NLMTA data to correlate parsed journals with journals in the repository, updates the repository if
 * pmc participation, NLMTA, or ISSNS have changed
 * </p>
 *
 * @author apb@jhu.edu
 */
public class LoaderEngine implements AutoCloseable {

    private Executor exe = r -> r.run();

    private final PassClient client;

    private final JournalFinder finder;

    private Logger LOG = LoggerFactory.getLogger(LoaderEngine.class);

    private boolean dryRun = false;

    private final AtomicInteger numCreated = new AtomicInteger(0);

    private final AtomicInteger numUpdated = new AtomicInteger(0);

    private final AtomicInteger numSkipped = new AtomicInteger(0);

    private final AtomicInteger numOk = new AtomicInteger(0);

    private final AtomicInteger numError = new AtomicInteger(0);

    private final AtomicInteger numDup = new AtomicInteger(0);

    LoaderEngine(PassClient client, JournalFinder finder) {
        this.client = client;
        this.finder = finder;
    }

    int numThreads = 1;

    public void setNumThreads(int threads) {
        exe = Executors.newFixedThreadPool(threads);
    }

    void load(Stream<Journal> journals, boolean hasPmcParticipation) {

        journals
                .forEach(j -> load(j, hasPmcParticipation));

    }

    void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void close() {
        if (dryRun) {
            LOG.info("Dry run: would have created {} new journals", numCreated);
            LOG.info("Dry run: would have updated {} journals", numUpdated);
            LOG.info("Dry run: {} journals did not need updating", numOk);
            LOG.info("Dry run: Skipped {} journals due to lack of ISSN and NLMTA", numSkipped);
            LOG.info("Dry run: Skipped {} journals due to suspected duplication", numDup);
            LOG.info("Dry run: Could not load or update {} journals due to an error", numError);
        } else {
            LOG.info("Created {} new journals", numCreated);
            LOG.info("Updated {} journals", numUpdated);
            LOG.info("{} journals did not need updating", numOk);
            LOG.info("Skipped {} journals due to lack of ISSN and NLMTA", numSkipped);
            LOG.info("Skipped {} journals due to suspected duplication", numDup);
            LOG.info("Could not load or update {} journals due to an error", numError);
        }
    }

    private void load(Journal j, boolean hasPmcParticipation) {

        if (j.getIssns().isEmpty() && (j.getNlmta() == null || j.getNlmta().isEmpty())) {
            LOG.debug("Journal has no ISSNs or NLMTA: {}", j.getName());
            numSkipped.incrementAndGet();
            return;
        }

        String found = finder.find(j.getNlmta(), j.getName(), j.getIssns());

        if (found == null) {//create a new journal
            try {
                if (!dryRun) {
                    exe.execute(() -> {
                        final URI uri = client.createResource(j);

                        j.setId(uri);
                        finder.add(j);
                        LOG.debug("Loaded journal {} at {}", j.getName(), uri);
                        numCreated.incrementAndGet();
                    });
                } else {
                    j.setId(URI.create(UUID.randomUUID().toString()));
                    finder.add(j);
                    numCreated.incrementAndGet();
                }
            } catch (final Exception e) {
                LOG.warn("Could not load journal " + j.getName(), e);
                numError.getAndIncrement();
            }
        } else if (found.equals("SKIP")) {//this matched something that was already processed
            numDup.getAndIncrement();
            LOG.info("We have already processed this journal, skipping: {}" ,j.getName());
        } else { //update this journal

            try {
                URI uri = URI.create(found);
                boolean update = false;
                final Journal toUpdate = client.readResource(uri, Journal.class);

                if (hasPmcParticipation && toUpdate.getPmcParticipation() != j.getPmcParticipation()) {
                    toUpdate.setPmcParticipation(j.getPmcParticipation());
                    update = true;
                }

                if (j.getIssns() != null && (toUpdate.getIssns() == null || !toUpdate.getIssns().containsAll(j.getIssns()))) {
                    toUpdate.setIssns(j.getIssns());
                    update = true;
                }

               if (toUpdate.getNlmta() == null && j.getNlmta() != null) {
                    toUpdate.setNlmta(j.getNlmta());
                    update = true;
                }

                if (!dryRun) {
                    if (update) {
                        exe.execute(() -> {
                            client.updateResource(toUpdate);
                            numUpdated.incrementAndGet();
                            LOG.debug("Updated journal {} at {}", j.getName(), j.getId());
                        });
                    } else {
                        numOk.incrementAndGet();
                    }
                } else {
                    if (update) {
                        numUpdated.incrementAndGet();
                    } else {
                        numOk.getAndIncrement();
                    }
                }
            } catch (final Exception e) {
                LOG.warn("Could not update journal " + j.getName(), e);
                numError.getAndIncrement();
            }
        }
    }
}
