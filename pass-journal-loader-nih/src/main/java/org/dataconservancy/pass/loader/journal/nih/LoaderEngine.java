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
import java.util.Collection;
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
 * Uses ISSN to correlate parsed journals with journals in the repository, updates the repository only if PMC
 * participation has changed.
 * </p>
 *
 * @author apb@jhu.edu
 */
public class LoaderEngine implements AutoCloseable {

    Executor exe = r -> r.run();

    private final PassClient client;

    private final JournalFinder finder;

    Logger LOG = LoggerFactory.getLogger(LoaderEngine.class);

    private boolean dryRun = false;

    private final AtomicInteger numCreated = new AtomicInteger(0);

    private final AtomicInteger numUpdated = new AtomicInteger(0);

    private final AtomicInteger numSkipped = new AtomicInteger(0);

    public LoaderEngine(PassClient client, JournalFinder finder) {
        this.client = client;
        this.finder = finder;
    }

    int numThreads = 1;

    public void setNumThreads(int threads) {
        exe = Executors.newFixedThreadPool(threads);
    }

    public void load(Stream<Journal> journals, boolean doUpdates) {

        journals
                .forEach(j -> load(j, doUpdates));

    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    @Override
    public void close() {
        if (dryRun) {
            LOG.info("Dry run: would have created {} new journals", numCreated);
            LOG.info("Dry run: would have updated {} journals", numUpdated);
            LOG.info("Dry run: Skipped {} journals due to lack of ISSN", numSkipped);
        } else {
            LOG.info("Created {} new journals", numCreated);
            LOG.info("Updated {} journals", numUpdated);
            LOG.info("Skipped {} journals due to lack of ISSN", numSkipped);
        }
    }

    private void load(Journal j, boolean doUpdates) {

        if (j.getIssns().isEmpty()) {
            LOG.debug("Journal has no ISSNs: {}", j.getName());
            numSkipped.incrementAndGet();
            return;
        }

        final Journal found = find(j.getIssns());

        if (found == null) {
            // If journal not found, deposit as new
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
                    numCreated.incrementAndGet();
                }
            } catch (final Exception e) {
                LOG.warn("Could not load journal " + j.getName(), e);
            }
        } else if (doUpdates &&
                ((found.getPmcParticipation() != j.getPmcParticipation()) || !found.getIssns().containsAll(j
                        .getIssns()))) {

            // IF PMC participation or ISSNs has changed, update the journal in the repository.
            try {
                final Journal toUpdate = client.readResource(found.getId(), Journal.class);
                toUpdate.setIssns(j.getIssns());

                if (j instanceof PMCSource) {
                    toUpdate.setPmcParticipation(j.getPmcParticipation());
                }

                if (j.getNlmta() != null) {
                    toUpdate.setNlmta(j.getNlmta());
                }

                if (!dryRun) {
                    exe.execute(() -> {
                        client.updateResource(toUpdate);
                        numUpdated.incrementAndGet();
                        LOG.debug("Updated journal {} at {}", j.getName(), j.getId());
                    });
                } else {
                    numUpdated.incrementAndGet();
                }
            } catch (final Exception e) {
                LOG.warn("Could not update journal");
            }
        }
    }

    private Journal find(Collection<String> issns) {
        for (final String issn : issns) {
            final Journal j = finder.byIssn(issn);
            if (j != null) {
                return j;
            } else {
                LOG.debug("No hits for issn {}", issn);
            }
        }

        return null;
    }
}
