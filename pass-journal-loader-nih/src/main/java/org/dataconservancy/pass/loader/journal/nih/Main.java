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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.dataconservancy.pass.client.util.ConfigUtil.getSystemProperty;

import java.io.FileInputStream;
import java.io.InputStream;

import org.dataconservancy.pass.client.PassClientFactory;
import org.dataconservancy.pass.client.fedora.FedoraConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main method for nih loader executable.
 *
 * @author apb@jhu.edu
 */
public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {

        LogUtil.adjustLogLevels();

        final JournalFinder finder = new BatchJournalFinder(FedoraConfig.getBaseUrl() + "journals");

        try (final LoaderEngine loader = new LoaderEngine(PassClientFactory.getPassClient(), finder)) {

            if (getSystemProperty("dryRun", null) != null) {
                loader.setDryRun(true);
            }

            final String pmcFile = getSystemProperty("pmc", null);
            final String medlineFile = getSystemProperty("medline", null);

            if (pmcFile != null) {
                final NihTypeAReader reader = new NihTypeAReader();
                try (InputStream file = new FileInputStream(pmcFile)) {
                    loader.load(reader.readJournals(file, UTF_8), reader.hasPmcParticipation());
                }
            }

            if (medlineFile != null) {
                final MedlineReader reader = new MedlineReader();
                try (InputStream file = new FileInputStream(medlineFile)) {
                    loader.load(reader.readJournals(file, UTF_8), reader.hasPmcParticipation());
                }
            }
        }
        LOG.info("done!");
    }
}
