# CLIforce plugin to import data into the Music Library Sample App

This code needs a bit more polishing. It lets you install a plugin into cliforce that can pull data from freebase and import into the [Music Library Sample App](https://github.com/jesperfj/vmforce-musiclib) so you have some data to play with.

The following should work:

    $ git clone git://github.com/jesperfj/musiclib-dataimporter.git
    [...]
    $ cd musiclib-dataimporter
    $ mvn install -DupdateReleaseInfo
    $ cliforce plugin sampledata
    $ cliforce sampledata:getmusic
    $ cliforce sampledata:import
