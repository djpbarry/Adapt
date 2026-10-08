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
package net.calm.adapt.ui;

import net.calm.adapt.adapt.Analyse_Batch;
import net.calm.adapt.adapt.Analyse_Movie;
import net.calm.adapt.adapt.StaticVariables;
import net.calm.adapt.adapt.TaskListener;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.process.AutoThresholder;
import net.calm.iaclasslibrary.UIClasses.GUIMethods;
import net.calm.iaclasslibrary.UIClasses.PropertyExtractor;
import net.calm.iaclasslibrary.UserVariables.UserVariables;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.Properties;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JToggleButton;

/**
 *
 * @author barry05
 */
public class GUI extends javax.swing.JDialog implements GUIMethods {

    private final ImagePlus cytoOrig, sigOrig;
    private final ImagePlus hyperstack;
    private final ImageStack[] stacks;
    private final String title;
    private boolean wasOKed = false;
    private final UserVariables UV = new UserVariables();
    ArrayList<Thread> previewThreads = new ArrayList<>();
    private final PointRoi roi;
    private final Properties props = new Properties();
    private Runnable onRun;

    /**
     * Creates new form GUI
     */
    public GUI(java.awt.Frame parent, boolean modal, String title, ImageStack[] stacks, PointRoi roi, ImagePlus cytoOrig, ImagePlus sigOrig) {
        super(parent, modal);
        this.stacks = stacks;
        this.title = title;
        this.hyperstack = null;
        this.roi = roi;
        this.cytoOrig = cytoOrig;
        this.sigOrig = sigOrig;
        initComponents();
        setToolTips();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2);
    }

    public GUI(java.awt.Frame parent, boolean modal, String title, ImagePlus hyperstack, PointRoi roi) {
        super(parent, modal);
        this.stacks = null;
        this.title = title;
        this.hyperstack = hyperstack;
        this.cytoOrig = hyperstack;
        this.sigOrig = hyperstack;
        this.roi = roi;
        initComponents();
        setToolTips();
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getWidth() / 2, dim.height / 2 - this.getHeight() / 2);
    }

    private void setToolTips() {
        greyThreshField.setToolTipText("Manual grey-level threshold used for segmentation when Auto Threshold is off.");
        spatResField.setToolTipText("Image pixel size in micrometres (used to convert pixels to physical units).");
        timeResField.setToolTipText("Acquisition rate in frames per minute (used for velocity and time axes).");
        autoThreshToggleButton.setToolTipText("Automatically determine the segmentation threshold using the selected method.");
        genVisToggleButton.setToolTipText("Generate velocity and curvature visualisation stacks.");
        genMorphToggleButton.setToolTipText("Generate per-frame morphology measurements.");
        minTrajTextField.setToolTipText("Minimum number of frames a cell must be tracked to be analysed.");
        genSigDistToggleButton.setToolTipText("Generate the fluorescence (GLCM) distribution measurements.");
        minMorphAreaTextField.setToolTipText("Minimum object area (in micrometres squared) considered a cell.");
        erosionField.setToolTipText("Number of erosion iterations used to refine the segmentation border.");
        spatFiltRadField.setToolTipText("Spatial smoothing radius (in micrometres) applied to the maps.");
        tempFiltRadField.setToolTipText("Temporal smoothing radius (in seconds) applied to the maps.");
        threshComboBox.setToolTipText("Algorithm used to compute the threshold when Auto Threshold is on.");
        gaussRadField.setToolTipText("Gaussian blur radius applied before segmentation.");
        cortexDepthField.setToolTipText("Width (in micrometres) of the cortex band sampled for velocity and signal.");
        visLineWidthTextField.setToolTipText("Line thickness used when drawing overlays and visualisations.");
        minCurveRangeField.setToolTipText("Window size used for curvature calculation.");
        minCurveThreshField.setToolTipText("Curvature threshold used to detect curvature extrema.");
        cutOffField.setToolTipText("Time cut-off (in seconds) for protrusion analysis.");
        sigThreshFactField.setToolTipText("Multiplier of the signal standard deviation used as the detection threshold.");
        anaProtToggleButton.setToolTipText("Run per-protrusion analysis (blebs or filopodia).");
        useSigThreshToggleButton.setToolTipText("Threshold the protrusion signal maps.");
        blebDetectRadioButton.setToolTipText("Detect blebs using the velocity map.");
        filoDetectRadioButton.setToolTipText("Detect filopodia using morphology.");
        filoSizeField.setToolTipText("Maximum filopodia area (in micrometres squared).");
        filoMinSizeTextField.setToolTipText("Minimum filopodia area (in micrometres squared).");
        displayPlotsToggleButton.setToolTipText("Display protrusion analysis plots.");
        previewButton.setToolTipText("Generate a segmentation preview overlay on the source image.");
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel2 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        simpleTab = new javax.swing.JPanel();
        greyThreshLabel = new javax.swing.JLabel();
        greyThreshField = new javax.swing.JTextField();
        spatResLabel = new javax.swing.JLabel();
        spatResField = new javax.swing.JTextField();
        timeResLabel = new javax.swing.JLabel();
        timeResField = new javax.swing.JTextField();
        autoThreshToggleButton = new javax.swing.JToggleButton();
        genVisToggleButton = new javax.swing.JToggleButton();
        genMorphToggleButton = new javax.swing.JToggleButton();
        minTrajLabel = new javax.swing.JLabel();
        minTrajTextField = new javax.swing.JTextField();
        genSigDistToggleButton = new javax.swing.JToggleButton();
        minMorphAreaLabel = new javax.swing.JLabel();
        minMorphAreaTextField = new javax.swing.JTextField();
        advancedTab = new javax.swing.JPanel();
        erosionField = new javax.swing.JTextField();
        erosionLabel = new javax.swing.JLabel();
        spatFiltRadLabel = new javax.swing.JLabel();
        spatFiltRadField = new javax.swing.JTextField();
        tempFiltRadLabel = new javax.swing.JLabel();
        tempFiltRadField = new javax.swing.JTextField();
        threshComboBox = new javax.swing.JComboBox();
        threshLabel = new javax.swing.JLabel();
        gaussRadField = new javax.swing.JTextField();
        gaussRadLabel = new javax.swing.JLabel();
        cortexDepthField = new javax.swing.JTextField();
        cortexDepthLabel = new javax.swing.JLabel();
        visLineWidthLabel = new javax.swing.JLabel();
        visLineWidthTextField = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        minCurveRangeLabel = new javax.swing.JLabel();
        minCurveRangeField = new javax.swing.JTextField();
        minCurveThreshLabel = new javax.swing.JLabel();
        minCurveThreshField = new javax.swing.JTextField();
        cutOffLabel = new javax.swing.JLabel();
        cutOffField = new javax.swing.JTextField();
        sigThreshFactLabel = new javax.swing.JLabel();
        sigThreshFactField = new javax.swing.JTextField();
        sigRecThreshLabel = new javax.swing.JLabel();
        sigRecThreshField = new javax.swing.JTextField();
        anaProtToggleButton = new javax.swing.JToggleButton();
        useSigThreshToggleButton = new javax.swing.JToggleButton();
        blebDetectRadioButton = new javax.swing.JRadioButton();
        filoDetectRadioButton = new javax.swing.JRadioButton();
        filoSizeLabel = new javax.swing.JLabel();
        filoSizeField = new javax.swing.JTextField();
        displayPlotsToggleButton = new javax.swing.JToggleButton();
        filoMinSizeLabel = new javax.swing.JLabel();
        filoMinSizeTextField = new javax.swing.JTextField();
        previewButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        channelPanel = new javax.swing.JPanel();
        runButton = new javax.swing.JButton();
        loadPresetButton = new javax.swing.JButton();
        savePresetButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle(title);
        getContentPane().setLayout(new java.awt.GridBagLayout());

        channelPanel.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        channelPanel.setLayout(new java.awt.GridBagLayout());

        if (hyperstack != null) {
            cytoChannelLabel = new javax.swing.JLabel();
            cytoChannelLabel.setText("Cytosol channel:");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
            channelPanel.add(cytoChannelLabel, gridBagConstraints);

            cytoChannelCombo = new javax.swing.JComboBox<>();
            for (int c = 1; c <= hyperstack.getNChannels(); c++) {
                cytoChannelCombo.addItem(channelLabel(c));
            }
            cytoChannelCombo.setSelectedIndex(0);
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
            channelPanel.add(cytoChannelCombo, gridBagConstraints);

            sigChannelLabel = new javax.swing.JLabel();
            sigChannelLabel.setText("Signal channel:");
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 2;
            gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
            channelPanel.add(sigChannelLabel, gridBagConstraints);

            sigChannelCombo = new javax.swing.JComboBox<>();
            for (int c = 1; c <= hyperstack.getNChannels(); c++) {
                sigChannelCombo.addItem(channelLabel(c));
            }
            sigChannelCombo.setSelectedIndex(Math.min(1, hyperstack.getNChannels() - 1));
            gridBagConstraints = new java.awt.GridBagConstraints();
            gridBagConstraints.gridx = 3;
            gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
            channelPanel.add(sigChannelCombo, gridBagConstraints);
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        getContentPane().add(channelPanel, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jTabbedPane1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        simpleTab.setLayout(new java.awt.GridBagLayout());

        greyThreshLabel.setText(StaticVariables.GREY_SENS);
        greyThreshLabel.setEnabled(!UV.isAutoThreshold());
        greyThreshLabel.setLabelFor(greyThreshField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleTab.add(greyThreshLabel, gridBagConstraints);

        greyThreshField.setText(String.valueOf(UV.getGreyThresh()));
        greyThreshField.setEnabled(!UV.isAutoThreshold());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        simpleTab.add(greyThreshField, gridBagConstraints);

        spatResLabel.setText(StaticVariables.SPAT_RES);
        spatResLabel.setLabelFor(spatResField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleTab.add(spatResLabel, gridBagConstraints);

        spatResField.setText(String.valueOf(UV.getSpatialRes()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        simpleTab.add(spatResField, gridBagConstraints);

        timeResLabel.setText(StaticVariables.TIME_RES);
        timeResLabel.setLabelFor(timeResField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleTab.add(timeResLabel, gridBagConstraints);

        timeResField.setText(String.valueOf(UV.getTimeRes()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        simpleTab.add(timeResField, gridBagConstraints);

        autoThreshToggleButton.setText(StaticVariables.AUTO_THRESH);
        autoThreshToggleButton.setSelected(UV.isAutoThreshold());
        autoThreshToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoThreshToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        simpleTab.add(autoThreshToggleButton, gridBagConstraints);

        genVisToggleButton.setText(StaticVariables.GEN_VIS);
        genVisToggleButton.setSelected(UV.isGenVis());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        simpleTab.add(genVisToggleButton, gridBagConstraints);

        genMorphToggleButton.setText(StaticVariables.GET_MORPH);
        genMorphToggleButton.setSelected(UV.isGetMorph());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        simpleTab.add(genMorphToggleButton, gridBagConstraints);

        minTrajLabel.setText(StaticVariables.MIN_TRAJ_LENGTH);
        minTrajLabel.setLabelFor(minTrajTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleTab.add(minTrajLabel, gridBagConstraints);

        minTrajTextField.setText(String.valueOf(UV.getMinLength()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        simpleTab.add(minTrajTextField, gridBagConstraints);

        genSigDistToggleButton.setText(StaticVariables.GEN_SIG_DIST);
        genSigDistToggleButton.setSelected(hasSignalChannel()&&UV.isGetFluorDist());
        genSigDistToggleButton.setEnabled(hasSignalChannel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        simpleTab.add(genSigDistToggleButton, gridBagConstraints);

        minMorphAreaLabel.setText(StaticVariables.MIN_MORPH_AREA);
        minMorphAreaLabel.setLabelFor(minMorphAreaTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        simpleTab.add(minMorphAreaLabel, gridBagConstraints);

        minMorphAreaTextField.setText(String.valueOf(UV.getMorphSizeMin()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        simpleTab.add(minMorphAreaTextField, gridBagConstraints);

        jTabbedPane1.addTab("Simple", simpleTab);

        advancedTab.setLayout(new java.awt.GridBagLayout());

        erosionField.setText(String.valueOf(UV.getErosion()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(erosionField, gridBagConstraints);

        erosionLabel.setText(StaticVariables.EROSION);
        erosionLabel.setLabelFor(erosionField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(erosionLabel, gridBagConstraints);

        spatFiltRadLabel.setText(StaticVariables.SPAT_FILT_RAD);
        spatFiltRadLabel.setLabelFor(spatFiltRadField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(spatFiltRadLabel, gridBagConstraints);

        spatFiltRadField.setText(String.valueOf(UV.getSpatFiltRad()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(spatFiltRadField, gridBagConstraints);

        tempFiltRadLabel.setText(StaticVariables.TEMP_FILT_RAD);
        tempFiltRadLabel.setLabelFor(tempFiltRadField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(tempFiltRadLabel, gridBagConstraints);

        tempFiltRadField.setText(String.valueOf(UV.getTempFiltRad()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(tempFiltRadField, gridBagConstraints);

        threshComboBox.setModel(new DefaultComboBoxModel(AutoThresholder.Method.values()));
        threshComboBox.setSelectedItem(AutoThresholder.Method.valueOf(UV.getThreshMethod()));
        threshComboBox.setEnabled(UV.isAutoThreshold());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(threshComboBox, gridBagConstraints);

        threshLabel.setText(StaticVariables.THRESH_METHOD);
        threshLabel.setEnabled(UV.isAutoThreshold());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(threshLabel, gridBagConstraints);

        gaussRadField.setText(String.valueOf(UV.getGaussRad()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(gaussRadField, gridBagConstraints);

        gaussRadLabel.setText(StaticVariables.GAUSS_RAD);
        gaussRadLabel.setLabelFor(gaussRadField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(gaussRadLabel, gridBagConstraints);

        cortexDepthField.setText(String.valueOf(UV.getCortexDepth()));
        cortexDepthField.setEnabled(hasSignalChannel());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(cortexDepthField, gridBagConstraints);

        cortexDepthLabel.setText(StaticVariables.CORTEX_DEPTH);
        cortexDepthLabel.setEnabled(hasSignalChannel());
        cortexDepthLabel.setLabelFor(cortexDepthField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(cortexDepthLabel, gridBagConstraints);

        visLineWidthLabel.setText(StaticVariables.VIS_LINE_WIDTH);
        visLineWidthLabel.setLabelFor(visLineWidthTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        advancedTab.add(visLineWidthLabel, gridBagConstraints);

        visLineWidthTextField.setText(String.valueOf(UV.getVisLineWidth()));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        advancedTab.add(visLineWidthTextField, gridBagConstraints);

        jTabbedPane1.addTab("Advanced", advancedTab);

        jPanel4.setLayout(new java.awt.GridBagLayout());

        minCurveRangeLabel.setText(StaticVariables.MIN_CURVE_RANGE);
        minCurveRangeLabel.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        minCurveRangeLabel.setLabelFor(minCurveRangeField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel4.add(minCurveRangeLabel, gridBagConstraints);

        minCurveRangeField.setText(String.valueOf(UV.getCurveRange()));
        minCurveRangeField.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel4.add(minCurveRangeField, gridBagConstraints);

        minCurveThreshLabel.setText(StaticVariables.MIN_CURVE_THRESH);
        minCurveThreshLabel.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        minCurveThreshLabel.setLabelFor(minCurveThreshField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel4.add(minCurveThreshLabel, gridBagConstraints);

        minCurveThreshField.setText(String.valueOf(UV.getMinCurveThresh()));
        minCurveThreshField.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel4.add(minCurveThreshField, gridBagConstraints);

        cutOffLabel.setText(StaticVariables.CUT_OFF);
        cutOffLabel.setEnabled(UV.isAnalyseProtrusions());
        cutOffLabel.setLabelFor(cutOffField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel4.add(cutOffLabel, gridBagConstraints);

        cutOffField.setText(String.valueOf(UV.getCutOffTime()));
        cutOffField.setEnabled(UV.isAnalyseProtrusions());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel4.add(cutOffField, gridBagConstraints);

        sigThreshFactLabel.setText(StaticVariables.SIG_THRESH_FACT);
        sigThreshFactLabel.setEnabled(UV.isAnalyseProtrusions() && UV.isUseSigThresh());
        sigThreshFactLabel.setLabelFor(sigThreshFactField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel4.add(sigThreshFactLabel, gridBagConstraints);

        sigThreshFactField.setText(String.valueOf(UV.getSigThreshFact()));
        sigThreshFactField.setEnabled(UV.isAnalyseProtrusions() && UV.isUseSigThresh());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel4.add(sigThreshFactField, gridBagConstraints);

        sigRecThreshLabel.setText(StaticVariables.SIG_REC_THRESH);
        sigRecThreshLabel.setEnabled(UV.isAnalyseProtrusions() && UV.isUseSigThresh());
        sigRecThreshLabel.setLabelFor(sigRecThreshField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 0);
        jPanel4.add(sigRecThreshLabel, gridBagConstraints);

        sigRecThreshField.setText(String.valueOf(UV.getSigRecoveryThresh()));
        sigRecThreshField.setEnabled(UV.isAnalyseProtrusions() && UV.isUseSigThresh());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 10);
        jPanel4.add(sigRecThreshField, gridBagConstraints);

        anaProtToggleButton.setText(StaticVariables.ANA_PROT);
        anaProtToggleButton.setSelected(UV.isAnalyseProtrusions());
        anaProtToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                anaProtToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel4.add(anaProtToggleButton, gridBagConstraints);

        useSigThreshToggleButton.setText(StaticVariables.USE_SIG_THRESH);
        useSigThreshToggleButton.setSelected(UV.isUseSigThresh());
        useSigThreshToggleButton.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        useSigThreshToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useSigThreshToggleButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel4.add(useSigThreshToggleButton, gridBagConstraints);

        blebDetectRadioButton.setText(StaticVariables.DETECT_BLEB);
        blebDetectRadioButton.setSelected(UV.isBlebDetect());
        blebDetectRadioButton.setEnabled(UV.isAnalyseProtrusions());
        blebDetectRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blebDetectRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 0);
        jPanel4.add(blebDetectRadioButton, gridBagConstraints);

        filoDetectRadioButton.setText(StaticVariables.DETECT_FILO);
        filoDetectRadioButton.setSelected(!UV.isBlebDetect());
        filoDetectRadioButton.setEnabled(UV.isAnalyseProtrusions());
        filoDetectRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filoDetectRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 10);
        jPanel4.add(filoDetectRadioButton, gridBagConstraints);

        filoSizeLabel.setText(StaticVariables.FILO_MAX_SIZE);
        filoSizeLabel.setEnabled(UV.isAnalyseProtrusions() && !UV.isBlebDetect());
        filoSizeLabel.setLabelFor(filoSizeField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 0, 0);
        jPanel4.add(filoSizeLabel, gridBagConstraints);

        filoSizeField.setText(String.valueOf(UV.getFiloSizeMax()));
        filoSizeField.setEnabled(UV.isAnalyseProtrusions() && !UV.isBlebDetect());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 0, 10);
        jPanel4.add(filoSizeField, gridBagConstraints);

        displayPlotsToggleButton.setText(StaticVariables.DISPLAY_PLOTS);
        displayPlotsToggleButton.setSelected(UV.isDisplayPlots());
        displayPlotsToggleButton.setEnabled(UV.isAnalyseProtrusions() && UV.isBlebDetect());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel4.add(displayPlotsToggleButton, gridBagConstraints);

        filoMinSizeLabel.setText(StaticVariables.FILO_MIN_SIZE);
        filoMinSizeLabel.setEnabled(UV.isAnalyseProtrusions() && !UV.isBlebDetect());
        filoMinSizeLabel.setLabelFor(filoMinSizeTextField);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 10, 0);
        jPanel4.add(filoMinSizeLabel, gridBagConstraints);

        filoMinSizeTextField.setText(String.valueOf(UV.getFiloSizeMin()));
        filoMinSizeTextField.setEnabled(UV.isAnalyseProtrusions() && !UV.isBlebDetect());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 10, 10);
        jPanel4.add(filoMinSizeTextField, gridBagConstraints);

        jTabbedPane1.addTab("Protrusion Analysis", jPanel4);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.9;
        jPanel2.add(jTabbedPane1, gridBagConstraints);

        previewButton.setText("Preview");
        previewButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previewButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        jPanel2.add(previewButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.9;
        getContentPane().add(jPanel2, gridBagConstraints);

        jPanel5.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        loadPresetButton.setText("Load Preset");
        loadPresetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadPresetButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel5.add(loadPresetButton, gridBagConstraints);

        savePresetButton.setText("Save Preset");
        savePresetButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                savePresetButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel5.add(savePresetButton, gridBagConstraints);

        runButton.setText("Run");
        runButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel5.add(runButton, gridBagConstraints);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(10, 0, 10, 0);
        jPanel5.add(cancelButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.1;
        getContentPane().add(jPanel5, gridBagConstraints);

        pack();
    }

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
        this.dispose();
        wasOKed = false;
    }

    private void runButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (!setVariables()) {
            return;
        }
        wasOKed = true;
        this.dispose();
        if (onRun != null) {
            onRun.run();
        }
    }

    private void loadPresetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        OpenDialog od = new OpenDialog("Load parameter preset", null, "params.json");
        String dir = od.getDirectory();
        String name = od.getFileName();
        if (dir == null || name == null) {
            return;
        }
        if (!Analyse_Batch.readParams(UV, new File(dir, name))) {
            IJ.error("Failed to load parameter preset.");
            return;
        }
        populateFields();
    }

    private void savePresetButtonActionPerformed(java.awt.event.ActionEvent evt) {
        if (!setVariables()) {
            return;
        }
        SaveDialog sd = new SaveDialog("Save parameter preset", null, "params.json");
        String dir = sd.getDirectory();
        String name = sd.getFileName();
        if (dir == null || name == null) {
            return;
        }
        if (!Analyse_Batch.writeParams(UV, new File(dir, name))) {
            IJ.error("Failed to save parameter preset.");
        }
    }

    private void populateFields() {
        greyThreshField.setText(String.valueOf(UV.getGreyThresh()));
        spatResField.setText(String.valueOf(UV.getSpatialRes()));
        timeResField.setText(String.valueOf(UV.getTimeRes()));
        autoThreshToggleButton.setSelected(UV.isAutoThreshold());
        genVisToggleButton.setSelected(UV.isGenVis());
        genMorphToggleButton.setSelected(UV.isGetMorph());
        minTrajTextField.setText(String.valueOf(UV.getMinLength()));
        genSigDistToggleButton.setSelected(hasSignalChannel() && UV.isGetFluorDist());
        minMorphAreaTextField.setText(String.valueOf(UV.getMorphSizeMin()));
        erosionField.setText(String.valueOf(UV.getErosion()));
        spatFiltRadField.setText(String.valueOf(UV.getSpatFiltRad()));
        tempFiltRadField.setText(String.valueOf(UV.getTempFiltRad()));
        try {
            threshComboBox.setSelectedItem(AutoThresholder.Method.valueOf(UV.getThreshMethod()));
        } catch (IllegalArgumentException e) {
            threshComboBox.setSelectedItem(AutoThresholder.Method.Otsu);
        }
        gaussRadField.setText(String.valueOf(UV.getGaussRad()));
        cortexDepthField.setText(String.valueOf(UV.getCortexDepth()));
        visLineWidthTextField.setText(String.valueOf(UV.getVisLineWidth()));
        minCurveRangeField.setText(String.valueOf(UV.getCurveRange()));
        minCurveThreshField.setText(String.valueOf(UV.getMinCurveThresh()));
        cutOffField.setText(String.valueOf(UV.getCutOffTime()));
        sigThreshFactField.setText(String.valueOf(UV.getSigThreshFact()));
        sigRecThreshField.setText(String.valueOf(UV.getSigRecoveryThresh()));
        anaProtToggleButton.setSelected(UV.isAnalyseProtrusions());
        useSigThreshToggleButton.setSelected(UV.isUseSigThresh());
        blebDetectRadioButton.setSelected(UV.isBlebDetect());
        filoDetectRadioButton.setSelected(!UV.isBlebDetect());
        filoSizeField.setText(String.valueOf(UV.getFiloSizeMax()));
        displayPlotsToggleButton.setSelected(UV.isDisplayPlots());
        filoMinSizeTextField.setText(String.valueOf(UV.getFiloSizeMin()));
        autoThreshToggleButtonActionPerformed(null);
        anaProtToggleButtonActionPerformed(null);
        blebDetectRadioButtonActionPerformed(null);
        useSigThreshToggleButtonActionPerformed(null);
    }

    private void autoThreshToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        disableComponentOnSelect(greyThreshLabel, greyThreshField, autoThreshToggleButton);
        enableComponentOnSelect(threshLabel, threshComboBox, autoThreshToggleButton, autoThreshToggleButton.isEnabled());
    }

    private void previewButtonActionPerformed(java.awt.event.ActionEvent evt) {
        generatePreview();
    }

    private void anaProtToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        blebDetectRadioButton.setEnabled(anaProtToggleButton.isSelected());
        filoDetectRadioButton.setEnabled(anaProtToggleButton.isSelected());
        filoDetectRadioButtonActionPerformed(null);
    }

    private void useSigThreshToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        enableComponentOnSelect(sigThreshFactLabel, sigThreshFactField, useSigThreshToggleButton, anaProtToggleButton.isSelected() && useSigThreshToggleButton.isEnabled());
        enableComponentOnSelect(sigRecThreshLabel, sigRecThreshField, useSigThreshToggleButton, anaProtToggleButton.isSelected() && useSigThreshToggleButton.isEnabled());
    }

    private void generatePreview() {
        if (!setVariables()) {
            return;
        }
        for (int i = 0; i < previewThreads.size(); i++) {
            previewThreads.get(i).interrupt();
        }
        previewThreads = new ArrayList<>();
        int frame = (hyperstack != null) ? hyperstack.getFrame() : 1;
        final Analyse_Movie previewAnalyser = new Analyse_Movie(getSelectedStacks(), false, false, UV, null, roi);
        previewAnalyser.preparePreview(frame, (UserVariables) UV.clone());
        previewAnalyser.addListener(new TaskListener() {
            public void threadComplete(Runnable runner) {
                if (!Thread.interrupted()) {
                    generatePreviewComplete((Analyse_Movie) runner);
                }
            }
        });
        Thread previewThread = new Thread(previewAnalyser);
        previewThread.start();
        previewThreads.add(previewThread);
    }

    private void generatePreviewComplete(Analyse_Movie analyser) {
        final Overlay overlay = analyser.getPreviewOverlay();
        final int slice = analyser.getPreviewSlice();
        java.awt.EventQueue.invokeLater(() -> {
            if (hyperstack != null) {
                int cyto = cytoChannelCombo.getSelectedIndex() + 1;
                for (Roi r : overlay.toArray()) {
                    int pos = r.getPosition();
                    if (pos > 0) {
                        r.setPosition(cyto, 1, pos);
                    }
                }
                hyperstack.setPosition(cyto, 1, slice);
                hyperstack.setOverlay(overlay);
                hyperstack.updateAndDraw();
            } else {
                if (cytoOrig != null) {
                    cytoOrig.setSlice(slice);
                    cytoOrig.setOverlay(overlay);
                    cytoOrig.updateAndDraw();
                }
                if (sigOrig != null) {
                    sigOrig.setSlice(slice);
                    sigOrig.setOverlay(overlay);
                    sigOrig.updateAndDraw();
                }
            }
        });
        IJ.log("Preview complete");
    }

    private void blebDetectRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {
        filoDetectRadioButton.setSelected(!blebDetectRadioButton.isSelected());
        boolean enabledB = anaProtToggleButton.isSelected() && blebDetectRadioButton.isEnabled();
        boolean enabledF = anaProtToggleButton.isSelected() && filoDetectRadioButton.isEnabled();
        enableComponentOnSelect(minCurveRangeLabel, minCurveRangeField, blebDetectRadioButton, enabledB);
        enableComponentOnSelect(minCurveThreshLabel, minCurveThreshField, blebDetectRadioButton, enabledB);
        enableComponentOnSelect(cutOffLabel, cutOffField, blebDetectRadioButton, enabledB);
        enableComponentOnSelect(filoSizeLabel, filoSizeField, filoDetectRadioButton, enabledF);
        enableComponentOnSelect(filoMinSizeLabel, filoMinSizeTextField, filoDetectRadioButton, enabledF);
        enableComponentOnSelect(null, displayPlotsToggleButton, blebDetectRadioButton, enabledB);
        enableComponentOnSelect(null, useSigThreshToggleButton, blebDetectRadioButton, enabledB);
        useSigThreshToggleButton.setEnabled(anaProtToggleButton.isSelected() && blebDetectRadioButton.isSelected());
        displayPlotsToggleButton.setEnabled(anaProtToggleButton.isSelected() && blebDetectRadioButton.isSelected());
        useSigThreshToggleButtonActionPerformed(null);
    }

    private void filoDetectRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {
        blebDetectRadioButton.setSelected(!filoDetectRadioButton.isSelected());
        blebDetectRadioButtonActionPerformed(evt);
    }

    private void disableComponentOnSelect(JComponent c1, JComponent c2, JToggleButton button) {
        c2.setEnabled(!(button.isSelected() && button.isEnabled()));
        if (c1 != null) {
            c1.setEnabled(!(button.isSelected() && button.isEnabled()));
        }
    }

    private void enableComponentOnSelect(JComponent c1, JComponent c2, JToggleButton button, boolean enabled) {
        c2.setEnabled(button.isSelected() && enabled);
        if (c1 != null) {
            c1.setEnabled(button.isSelected() && enabled);
        }
    }

    public boolean setVariables() {
        try {
            UV.setGreyThresh(Double.parseDouble(greyThreshField.getText()));
            UV.setGenVis(genVisToggleButton.isSelected());
            UV.setCurveRange(Integer.parseInt(minCurveRangeField.getText()));
            UV.setUseSigThresh(useSigThreshToggleButton.isSelected());
            UV.setSpatialRes(Double.parseDouble(spatResField.getText()));
            UV.setCutOffTime(Double.parseDouble(cutOffField.getText()));
            UV.setCortexDepth(Double.parseDouble(cortexDepthField.getText()));
            UV.setAutoThreshold(autoThreshToggleButton.isSelected());
            UV.setTempFiltRad(Double.parseDouble(tempFiltRadField.getText()));
            UV.setSigThreshFact(Double.parseDouble(sigThreshFactField.getText()));
            UV.setSpatFiltRad(Double.parseDouble(spatFiltRadField.getText()));
            UV.setErosion(Integer.parseInt(erosionField.getText()));
            UV.setGetMorph(genMorphToggleButton.isSelected());
            UV.setTimeRes(Double.parseDouble(timeResField.getText()));
            UV.setMinCurveThresh(Double.parseDouble(minCurveThreshField.getText()));
            UV.setAnalyseProtrusions(anaProtToggleButton.isSelected());
            UV.setBlebDetect(blebDetectRadioButton.isSelected());
            UV.setSigRecoveryThresh(Double.parseDouble(sigRecThreshField.getText()));
            UV.setGaussRad(Double.parseDouble(gaussRadField.getText()));
            UV.setMinLength((int) Math.round(Double.parseDouble(minTrajTextField.getText())));
            UV.setThreshMethod(String.valueOf(threshComboBox.getSelectedItem()));
            UV.setFiloSizeMax(Double.parseDouble(filoSizeField.getText()));
            UV.setGetFluorDist(genSigDistToggleButton.isSelected());
            UV.setMorphSizeMin(Double.parseDouble(minMorphAreaTextField.getText()));
            UV.setVisLineWidth(Integer.parseInt(visLineWidthTextField.getText()));
            UV.setDisplayPlots(displayPlotsToggleButton.isSelected());
            UV.setFiloSizeMin(Double.parseDouble(filoMinSizeTextField.getText()));
        } catch (NumberFormatException e) {
            IJ.error("Invalid numeric input. Check that every parameter field contains a valid number.");
            return false;
        }
        if (!validateRanges()) {
            return false;
        }
        setProperties(props, this);
        return true;
    }

    private boolean validateRanges() {
        if (UV.getSpatialRes() <= 0) {
            IJ.error("Spatial Resolution must be greater than 0.");
            return false;
        }
        if (UV.getTimeRes() <= 0) {
            IJ.error("Frames per Minute must be greater than 0.");
            return false;
        }
        if (UV.getErosion() < 0) {
            IJ.error("Erosion Iterations must be 0 or more.");
            return false;
        }
        if (UV.getCortexDepth() <= 0) {
            IJ.error("Cortex Depth must be greater than 0.");
            return false;
        }
        if (UV.getMinLength() < 1) {
            IJ.error("Minimum Trajectory Length must be at least 1.");
            return false;
        }
        if (UV.getMorphSizeMin() <= 0) {
            IJ.error("Minimum Object Size must be greater than 0.");
            return false;
        }
        if (UV.getVisLineWidth() < 1) {
            IJ.error("Visualisation Line Thickness must be at least 1.");
            return false;
        }
        if (UV.getCurveRange() < 1) {
            IJ.error("Curvature Window must be at least 1.");
            return false;
        }
        if (UV.getSigRecoveryThresh() < 0 || UV.getSigRecoveryThresh() > 1) {
            IJ.error("Signal Map Threshold must be between 0 and 1.");
            return false;
        }
        if (UV.getFiloSizeMax() <= 0) {
            IJ.error("Max Filopodia Size must be greater than 0.");
            return false;
        }
        if (UV.getFiloSizeMin() <= 0) {
            IJ.error("Min Filopodia Size must be greater than 0.");
            return false;
        }
        if (UV.getFiloSizeMin() > UV.getFiloSizeMax()) {
            IJ.error("Min Filopodia Size must not exceed Max Filopodia Size.");
            return false;
        }
        return true;
    }

    public void setProperties(Properties p, Container c) {
        PropertyExtractor.setProperties(p, c, PropertyExtractor.WRITE);
    }

    public Properties getProperties() {
        return props;
    }

    public UserVariables getUv() {
        return UV;
    }

    public void setOnRun(Runnable onRun) {
        this.onRun = onRun;
    }

    public ImageStack[] getSelectedStacks() {
        if (hyperstack == null) {
            return stacks;
        }
        ImageStack[] selected = new ImageStack[2];
        selected[0] = Analyse_Movie.extractChannel(hyperstack, cytoChannelCombo.getSelectedIndex() + 1);
        selected[1] = Analyse_Movie.extractChannel(hyperstack, sigChannelCombo.getSelectedIndex() + 1);
        return selected;
    }

    private boolean hasSignalChannel() {
        return hyperstack != null || (stacks != null && stacks[1] != null);
    }

    private String channelLabel(int channel) {
        if (hyperstack == null) {
            return "Channel " + channel;
        }
        ImageStack stack = hyperstack.getImageStack();
        int index = hyperstack.getStackIndex(channel, 1, 1);
        if (index >= 1 && index <= stack.getSize()) {
            String label = stack.getSliceLabel(index);
            if (label != null) {
                String name = label.trim();
                if (!name.isEmpty() && !name.matches("\\d+")) {
                    return name;
                }
            }
        }
        return "Channel " + channel;
    }

    public boolean isWasOKed() {
        return wasOKed;
    }

//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(GUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the dialog */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                GUI dialog = new GUI(new javax.swing.JFrame(), true);
//                dialog.addWindowListener(new java.awt.event.WindowAdapter() {
//                    @Override
//                    public void windowClosing(java.awt.event.WindowEvent e) {
//                        System.exit(0);
//                    }
//                });
//                dialog.setVisible(true);
//            }
//        });
//    }

    // Variables declaration
    private javax.swing.JPanel advancedTab;
    private javax.swing.JToggleButton anaProtToggleButton;
    private javax.swing.JToggleButton autoThreshToggleButton;
    private javax.swing.JRadioButton blebDetectRadioButton;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel channelPanel;
    private javax.swing.JTextField cortexDepthField;
    private javax.swing.JLabel cortexDepthLabel;
    private javax.swing.JTextField cutOffField;
    private javax.swing.JLabel cutOffLabel;
    private javax.swing.JComboBox<String> cytoChannelCombo;
    private javax.swing.JLabel cytoChannelLabel;
    private javax.swing.JToggleButton displayPlotsToggleButton;
    private javax.swing.JTextField erosionField;
    private javax.swing.JLabel erosionLabel;
    private javax.swing.JRadioButton filoDetectRadioButton;
    private javax.swing.JLabel filoMinSizeLabel;
    private javax.swing.JTextField filoMinSizeTextField;
    private javax.swing.JTextField filoSizeField;
    private javax.swing.JLabel filoSizeLabel;
    private javax.swing.JTextField gaussRadField;
    private javax.swing.JLabel gaussRadLabel;
    private javax.swing.JToggleButton genMorphToggleButton;
    private javax.swing.JToggleButton genSigDistToggleButton;
    private javax.swing.JToggleButton genVisToggleButton;
    private javax.swing.JTextField greyThreshField;
    private javax.swing.JLabel greyThreshLabel;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField minCurveRangeField;
    private javax.swing.JLabel minCurveRangeLabel;
    private javax.swing.JTextField minCurveThreshField;
    private javax.swing.JLabel minCurveThreshLabel;
    private javax.swing.JLabel minMorphAreaLabel;
    private javax.swing.JTextField minMorphAreaTextField;
    private javax.swing.JLabel minTrajLabel;
    private javax.swing.JTextField minTrajTextField;
    private javax.swing.JButton previewButton;
    private javax.swing.JButton runButton;
    private javax.swing.JButton loadPresetButton;
    private javax.swing.JButton savePresetButton;
    private javax.swing.JComboBox<String> sigChannelCombo;
    private javax.swing.JLabel sigChannelLabel;
    private javax.swing.JTextField sigRecThreshField;
    private javax.swing.JLabel sigRecThreshLabel;
    private javax.swing.JTextField sigThreshFactField;
    private javax.swing.JLabel sigThreshFactLabel;
    private javax.swing.JPanel simpleTab;
    private javax.swing.JTextField spatFiltRadField;
    private javax.swing.JLabel spatFiltRadLabel;
    private javax.swing.JTextField spatResField;
    private javax.swing.JLabel spatResLabel;
    private javax.swing.JTextField tempFiltRadField;
    private javax.swing.JLabel tempFiltRadLabel;
    private javax.swing.JComboBox threshComboBox;
    private javax.swing.JLabel threshLabel;
    private javax.swing.JTextField timeResField;
    private javax.swing.JLabel timeResLabel;
    private javax.swing.JToggleButton useSigThreshToggleButton;
    private javax.swing.JLabel visLineWidthLabel;
    private javax.swing.JTextField visLineWidthTextField;
    // End of variables declaration
}
