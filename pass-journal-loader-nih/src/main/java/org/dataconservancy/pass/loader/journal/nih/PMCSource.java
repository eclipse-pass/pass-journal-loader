/*
 * Copyright 2018 Johns Hopkins University
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

import org.dataconservancy.pass.model.Journal;

/**
 * Marker class used when parsing data that provides PMC participation data.
 * <p>
 * When using this class, pmcParticipation == null means "does not have any pmc participation", rather than "unknown".
 * </p>
 *
 * @author apb@jhu.edu
 */
public class PMCSource extends Journal {

    public PMCSource() {
        super();
    }

    public PMCSource(Journal initial) {
        super(initial);
    }
}
