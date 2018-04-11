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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;

import org.dataconservancy.pass.model.Journal;
import org.dataconservancy.pass.model.PmcParticipation;

import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class BatchJournalFinderTest {

    @Test
    public void journalFindTest() throws Exception {

        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNotNull(toTest.byIssn("0000-0001"));
            assertNotNull(toTest.byIssn("0000-0002"));
            assertNotNull(toTest.byIssn("0000-0002X"));

        }
    }

    @Test
    public void journalNotFoundTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            assertNull(toTest.byIssn("0000-000"));

        }
    }

    @Test
    public void pmcParticipationTest() throws Exception {

        final BatchJournalFinder toTest = new BatchJournalFinder();

        try (final InputStream in = this.getClass().getResourceAsStream("/journals.nt")) {

            toTest.load(in);

            final Journal j1 = toTest.byIssn("0000-0001");
            final Journal j2 = toTest.byIssn("0000-0002");

            assertNull(j1.getPmcParticipation());
            assertEquals(PmcParticipation.A, j2.getPmcParticipation());
        }
    }

    @Test
    public void manualAddTest() throws Exception {
        final BatchJournalFinder toTest = new BatchJournalFinder();

        final URI ID = URI.create("test/uri");
        final String ISSN1 = "000-001";
        final String ISSN2 = "000-002";

        final Journal toAdd = new Journal();
        toAdd.setId(ID);
        toAdd.setIssns(Arrays.asList(ISSN1, ISSN2));
        toAdd.setPmcParticipation(PmcParticipation.A);

        toTest.add(toAdd);

        final Journal found = toTest.byIssn("000-001");
        assertNotNull(found);
        assertNotNull(toTest.byIssn("000-002"));

        assertEquals(PmcParticipation.A, found.getPmcParticipation());

    }
}
