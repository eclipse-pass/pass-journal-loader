# PASS Journal Loader

Parses the PMC type A journal `.csv` file, and/or the medline database `.txt` file, and syncs with the repository:

    * Adds journals if they do not already exist
    * Updates PMC method A participation if it differs from the corresponding resource in the repository

## Usage

Using java sustem properties to launch the journal:

    java -DPMC=/path/to/pmc.csv -DMEDLINE=/path/to/medline.txt -DPASS_FEDORA_BASEURL=http://localhost:8080/fcrepo/rest/ -jar pass-journal-loader-nih-0.1.0-SNAPSHOT-exe.jar

### Properties or Environment Variables

The following may be provided as system properties on the command line `-Dprop-value`, or as environment variables `PROP=value`:

`pass.fedora.url`
BaseURL of Fedora.  Must end in trailing slash e.g. `http://localhost:8080/fcrepo/rest/`

`DryRun`
Do not add or update resources in the repository, just give statistics of resources that would be added or updated

`pmc`
Location of the PMC "type A" journal .csv file, as retrieved from [http://www.ncbi.nlm.nih.gov/pmc/front-page/NIH_PA_journal_list.csv]( http://www.ncbi.nlm.nih.gov/pmc/front-page/NIH_PA_journal_list.csv)

`medline`
Location of the Medline journal file, as retrieved from [ftp://ftp.ncbi.nih.gov/pubmed/J_Medline.txt](ftp://ftp.ncbi.nih.gov/pubmed/J_Medline.txt)

`LOG.*`
Adjust the logging level of a particular component, e.g. `LOG.org.dataconservancy.pass.client=WARN`