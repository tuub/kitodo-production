package org.kitodo.production.plugin.step.archive;

import de.sub.goobi.beans.Prozess;
import de.sub.goobi.export.download.ExportMets;
import de.sub.goobi.helper.exceptions.DAOException;
import de.sub.goobi.helper.exceptions.SwapException;
import java.io.IOException;
import ugh.dl.Fileformat;
import ugh.exceptions.DocStructHasNoTypeException;
import ugh.exceptions.PreferencesException;
import ugh.exceptions.ReadException;
import ugh.exceptions.TypeNotAllowedForParentException;
import ugh.exceptions.WriteException;

/**
 * Simple METS file exporter.
 */
public class ArchiveMetsExporter extends ExportMets {

    /**
     * Export default METS file to specific location.
     *
     * @param process the process to be exported
     * @param destinationFile the path to save the METS file to
     * @throws IOException
     * @throws InterruptedException
     * @throws PreferencesException
     * @throws WriteException
     * @throws DocStructHasNoTypeException
     * @throws ReadException
     * @throws SwapException
     * @throws DAOException
     * @throws TypeNotAllowedForParentException
     */
    public void export(Prozess process, String destinationFile)
        throws IOException, InterruptedException, PreferencesException,
            WriteException, DocStructHasNoTypeException, ReadException,
            SwapException, DAOException, TypeNotAllowedForParentException {

        this.myPrefs = process.getRegelsatz().getPreferences();

        // Metadata of the process
        Fileformat metadata = process.readMetadataFile();

        // Export METS/MODS file in default export format (including filegroups)
        if (!writeMetsFile(process, destinationFile, metadata, false)) {
            throw new WriteException("Could not create default export METS file '" + destinationFile + "'.");
        }
    }
}
