/*
 * Copyright (C) 2014 David Barry <david.barry at cancer.org.uk>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.calm.adapt.adapt;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.PointRoi;
import ij.plugin.PlugIn;
import net.calm.adapt.ui.GUI;
import net.calm.iaclasslibrary.Cell.CellData;
import net.calm.iaclasslibrary.UtilClasses.Utilities;

import java.io.File;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Runs the ADAPT analysis pipeline on a movie whose cells have already been
 * segmented and tracked in TrackMate. Instead of segmenting the cytosol
 * channel itself, ADAPT imports the per-cell, per-frame boundaries recorded in
 * a TrackMate session XML and uses them to drive the same downstream
 * velocity/curvature/signal and protrusion analysis as
 * {@link Analyse_Movie}.
 */
public class Analyse_TrackMate extends Analyse_Movie implements PlugIn {

    private File trackMateFile;

    public Analyse_TrackMate() {
        super();
        batchMode = true;
    }

    public Analyse_TrackMate(File trackMateFile) {
        super();
        batchMode = true;
        this.trackMateFile = trackMateFile;
    }

    @Override
    public void run(String arg) {
        String version = "unknown";
        try {
            final Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));
            version = properties.getProperty("version");
        } catch (Exception e) {
            IJ.log("Failed to read version from project.properties.");
        }
        TITLE = TITLE + "_v" + version;
        if (WindowManager.getIDList() == null) {
            IJ.error("No images open. Open the movie corresponding to the TrackMate file first.");
            return;
        }
        if (trackMateFile == null) {
            try {
                File selected = Utilities.getFile(directory, "Select TrackMate XML file...", false);
                if (selected == null) {
                    return;
                }
                trackMateFile = selected;
            } catch (Exception e) {
                IJ.log("TrackMate file selection failed: " + e.getMessage());
                return;
            }
        }
        if (!trackMateFile.exists()) {
            IJ.error("TrackMate file not found: " + trackMateFile.getAbsolutePath());
            return;
        }
        try {
            directory = Utilities.getFolder(new File(IJ.getDirectory("current")), "Specify directory for output files...", true);
        } catch (Exception e) {
            IJ.log("Failed to select output directory: " + e.getMessage());
        }
        if (directory == null) {
            return;
        }
        IJ.log(String.format("Using %d parallel processes.\n", Runtime.getRuntime().availableProcessors()));
        analyse(trackMateFile.getName());
    }

    @Override
    public boolean analyse(String imageName) {
        if (!checkDependencies()) {
            return false;
        }
        ImagePlus cytoImp = WindowManager.getCurrentImage();
        if (cytoImp == null) {
            IJ.error("No active image.");
            return false;
        }
        if (!cytoImp.isHyperStack() || cytoImp.getNSlices() != 1 || cytoImp.getNFrames() < 2) {
            IJ.error("The active image must be a hyperstack with a single z-slice and multiple frames (T > 1).");
            return false;
        }
        roi = (PointRoi) cytoImp.getRoi();
        inputImage = cytoImp;
        cytoImp.setTitle(cytoImp.getTitle().replace(" ", "_"));
        ArrayList<CellData> imported;
        try {
            imported = new ArrayList<>(TrackMateImporter.importTracks(trackMateFile, cytoImp.getWidth(), cytoImp.getHeight()));
        } catch (Exception e) {
            IJ.error("Failed to read TrackMate file: " + e.getMessage());
            return false;
        }
        if (imported.isEmpty()) {
            IJ.error("No tracks were found in the TrackMate file.");
            return false;
        }
        IJ.log(String.format("%d tracks imported from %s.", imported.size(), trackMateFile.getName()));
        if (!createOutputDirectories(cytoImp, imageName)) {
            return false;
        }
        setImportedCells(imported);
        GUI gui = new GUI(null, false, TITLE, cytoImp, roi, true);
        gui.setOnRun(() -> {
            stacks = gui.getSelectedStacks();
            uv = gui.getUv();
            props = gui.getProperties();
            new Thread(this::finishAnalysis).start();
        });
        gui.setVisible(true);
        return true;
    }
}
