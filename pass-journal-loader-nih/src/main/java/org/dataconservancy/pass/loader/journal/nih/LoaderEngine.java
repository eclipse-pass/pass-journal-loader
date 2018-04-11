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
public class LoaderEngine {

    private final PassClient client;

    private final JournalFinder finder;

    Logger LOG = LoggerFactory.getLogger(LoaderEngine.class);

    public LoaderEngine(PassClient client, JournalFinder finder) {
        this.client = client;
        this.finder = finder;
    }

    public void load(Stream<Journal> journals, boolean doUpdates) {
        journals
                .forEach(j -> load(j, doUpdates));
    }

    private void load(Journal j, boolean doUpdates) {
        final URI uri;

        if (j.getIssns().isEmpty()) {
            LOG.warn("Journal has no ISSNs: {}", j.getName());
            return;
        }

        final Journal found = find(j.getIssns());

        if (found == null) {
            // If journal not found, deposit as new
            try {
                uri = client.createResource(j);
                j.setId(uri);
                finder.add(j);
                LOG.debug("Loaded journal {} at {}", j.getNlmta(), uri);
            } catch (final Exception e) {
                LOG.warn("Could not load journal " + j.getName(), e);
            }
        } else if (doUpdates && found.getPmcParticipation() != j.getPmcParticipation()) {

            // IF PMC participation has changed, update PMC participation in the repository.
            try {
                final Journal toUpdate = client.readResource(found.getId(), Journal.class);
                toUpdate.setPmcParticipation(j.getPmcParticipation());
                client.updateResource(toUpdate);
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
            }
        }

        return null;
    }
}
