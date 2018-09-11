# Marc Plugin for Kitodo.Production

## Usage

The plugin is build as a separate jar file in dist/plugins/opac. Put this jar in your plugin folder defined in the main configuration
file goobi_config.properties. You also need to put the configuration file kitodo_marc_opac.xml in your config folder.

## Configuration

The configuration file kitodo_marc_opac.xml defines clients, urls, document types and mappings from marc to Kitodo metadata fields.
An example file can be found in Goobi/config.
Adapt this file to your needs and place it in your config folder defined in the main configuration file goobi_config.properties.

## Implementation

The main class of the plugin is MarcPlugin. This class gets loaded by the PluginLoader and is used by the class ProzesskopieForm.
It thus implements all methods requiered by these two classes.

To implement a new catalogue client, please extend the AbstractCatalogueClient or implement the interface IMarcCatalogueClient directly.
Which implementation is used by which client is set in the configuration file.

The reading of the response is done using marc4j. In the current implementation, only marcXml is supported. The mapping of the marc fields
to Kitodo metadata is done in the MarcKitodoMapper using the configuration in the config file.
