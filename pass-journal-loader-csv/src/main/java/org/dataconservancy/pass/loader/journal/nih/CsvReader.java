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

import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.stream.Stream;

import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author apb@jhu.edu
 */
public class CsvReader {

    static final Logger LOG = LoggerFactory.getLogger(CsvReader.class);

    public static Stream<JournalRecord> readJournals(Reader csv) throws IOException {

        return stream(CSVFormat.RFC4180.parse(csv).spliterator(), false)
                .map(CsvReader::toJournal)
                .filter(Objects::nonNull);
    }

    static JournalRecord toJournal(final CSVRecord record) {

        final Journal j = new Journal();

        try {

            j.setPmcParticipation(PmcParticipation.A);
            j.setName(record.get(0));
            j.setNlmta(record.get(1));

            // columns 2, 3 are issns
            addIssnIfPresent(j, record.get(2));
            addIssnIfPresent(j, record.get(3));

            // 4 is start date (we don't cate)
            // 5 is end date (if ended, then it's not active
            final String endDate = record.get(5);
            final boolean isActive = (endDate == null || endDate.trim().equals(""));

            return new JournalRecord() {

                @Override
                public boolean isActive() {
                    return isActive;
                }

                @Override
                public Journal journal() {
                    return j;
                }
            };
        } catch (final Exception e) {
            LOG.warn("Could not create journal record for {}", j.getName(), e);
            return null;
        }

    }

    static void addIssnIfPresent(Journal journal, String issn) {
        if (issn != null && !issn.trim().equals("")) {
            journal.getIssns().add(issn);
        }
    }

}
