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

package org.dataconservancy.pass.loader.nih;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.dataconservancy.pass.loader.journal.nih.NihTypeAReader;
import org.dataconservancy.pass.loader.journal.nih.JournalRecord;

import org.junit.Test;

/**
 * @author apb@jhu.edu
 */
public class NihTypeAReaderTest {

    @Test
    public void fileParseTest() throws Exception {
        try (final InputStream in = this.getClass().getResourceAsStream("/data.csv")) {

            final List<JournalRecord> records = NihTypeAReader.readJournals(new InputStreamReader(in, UTF_8)).collect(
                    Collectors
                            .toList());

            assertEquals(2, records.size());
            assertEquals(true, records.get(0).isActive());
            assertEquals(false, records.get(1).isActive());

            assertTrue(records.get(0).journal().getIssns().containsAll(Arrays.asList("2190-572X", "2190-5738")));
        }
    }
}
