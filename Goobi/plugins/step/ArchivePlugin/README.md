# Kitodo Archive Plugin

A step plugin for Kitodo.Production 2.2 to create an archive of the process' master files and metadata to be used by long term preservation.

The plugin is built as separate JAR file. Place it in your *plugins/step* directory.

## Configuration

Place the [configuration file](kitodo_archive_plugin.xml) in your *config* directory. Edit it to your needs.

Add a step to the process or process template in Kitodo.Production and enable *automatic task* and *script step* and add "Kitodo Archive Plugin" as *step plugin*. The script name and path fields stay empty.
