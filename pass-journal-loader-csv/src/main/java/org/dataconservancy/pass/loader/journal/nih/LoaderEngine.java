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
import java.util.stream.Stream;

import org.dataconservancy.pass.client.PassClient;
import org.dataconservancy.pass.model.Journal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads journal records into the repository
 * <p>
 * TODO: once we have a search service, check to see of the journal already exists before depositing it
 * </p>
 *
 * @author apb@jhu.edu
 */
public class LoaderEngine {

    private final PassClient client;

    Logger LOG = LoggerFactory.getLogger(LoaderEngine.class);

    public LoaderEngine(PassClient client) {
        this.client = client;
    }

    public void load(Stream<JournalRecord> journals) {
        journals
                .filter(JournalRecord::isActive)
                .map(JournalRecord::journal)
                .forEach(this::load);
    }

    private void load(Journal j) {
        final URI uri;
        try {
            uri = client.createResource(j);
            LOG.debug("Loaded journal {} at {}", j.getNlmta(), uri);
        } catch (final Exception e) {
            LOG.warn("Could not load journal " + j.getName(), e);
        }
    }
}
