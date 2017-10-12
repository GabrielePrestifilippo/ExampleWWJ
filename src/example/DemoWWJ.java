/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package example;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWAbsentRequirementException;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.*;
import gov.nasa.worldwind.layers.*;

import gov.nasa.worldwind.terrain.*;
import gov.nasa.worldwind.util.*;

import gov.nasa.worldwindx.examples.util.*;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URISyntaxException;

import gov.nasa.worldwindx.examples.*;

/**
 * Provides a base application framework for simple WorldWind examples. Examine other examples in this package to see
 * how it's used.
 *
 * @version $Id: ApplicationTemplate.java 2115 2014-07-01 17:58:16Z tgaskins $
 */
public class DemoWWJ
{
    public static class AppPanel extends JPanel
    {
        protected WorldWindow wwd;
        protected StatusBar statusBar;
        protected ToolTipController toolTipController;
        protected HighlightController highlightController;

        public AppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            super(new BorderLayout());

            this.wwd = this.createWorldWindow();
            ((Component) this.wwd).setPreferredSize(canvasSize);

            // Create the default model as described in the current worldwind properties.
            Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
            this.wwd.setModel(m);

            // Setup a select listener for the worldmap click-and-go feature
            this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

            this.add((Component) this.wwd, BorderLayout.CENTER);
            if (includeStatusBar)
            {
                this.statusBar = new StatusBar();
                this.add(statusBar, BorderLayout.PAGE_END);
                this.statusBar.setEventSource(wwd);
            }

            // Add controllers to manage highlighting and tool tips.
            this.toolTipController = new ToolTipController(this.getWwd(), AVKey.DISPLAY_NAME, null);
            this.highlightController = new HighlightController(this.getWwd(), SelectEvent.ROLLOVER);
        }

        protected WorldWindow createWorldWindow()
        {
            return new WorldWindowGLCanvas();
        }

        public WorldWindow getWwd()
        {
            return wwd;
        }

        public StatusBar getStatusBar()
        {
            return statusBar;
        }
    }

    protected static class AppFrame extends JFrame
    {
        private Dimension canvasSize = new Dimension(1000, 800);

        protected AppPanel wwjPanel;
        protected JPanel controlPanel;
        protected LayerPanel layerPanel;
        protected StatisticsPanel statsPanel;

        // The elevation to import
        protected static final String ELEVATIONS_PATH
            = "gov/nasa/worldwindx/examples/data/craterlake-elev-16bit-30m.tif";
        // The Imagery to import
        protected static final String IMAGE_PATH = "gov/nasa/worldwindx/examples/data/craterlake-imagery-30m.tif";


        protected void importElevations()
        {
            try
            {
                // Download the data and save it in a temp file.
                File sourceFile = ExampleUtil.saveResourceToTempFile(ELEVATIONS_PATH, ".tif");

                // Create a local elevation model from the data.
                final LocalElevationModel elevationModel = new LocalElevationModel();
                elevationModel.addElevations(sourceFile);


                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        // Get the WorldWindow's current elevation model.
                        Globe globe = DemoWWJ.AppFrame.this.getWwd().getModel().getGlobe();

                        ElevationModel currentElevationModel = globe.getElevationModel();

                        // Add the new elevation model to the globe.
                        if (currentElevationModel instanceof CompoundElevationModel)
                            ((CompoundElevationModel) currentElevationModel).addElevationModel(elevationModel);
                        else
                            globe.setElevationModel(elevationModel);


                        // Set the view to look at the imported elevations, although they might be hard to detect. To
                        // make them easier to detect, replace the globe's CompoundElevationModel with the new elevation
                        // model rather than adding it.
                        Sector modelSector = elevationModel.getSector();
                        ExampleUtil.goTo(getWwd(), modelSector);
                    }

                });
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            layerPanel.updateElevations(this.getWwd());
        }

        protected JTabbedPane tabbedPane;
        protected int previousTabIndex;

        public AppFrame()
        {

            this.tabbedPane = new JTabbedPane();

            this.initialize(true, true, false);

            this.setCursor(new Cursor(Cursor.WAIT_CURSOR));

            // Import the elevations on a thread other than the event-dispatch thread to avoid freezing the UI.
            Thread t = new Thread(new Runnable()
            {
                public void run()
                {
                    importElevations();
                    setCursor(Cursor.getDefaultCursor());
                }
            });

            t.start();
        }

        public AppFrame(Dimension size)
        {

            this.canvasSize = size;
            this.initialize(true, true, false);
        }

        public AppFrame(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            this.initialize(includeStatusBar, includeLayerPanel, includeStatsPanel);
        }

        protected void initialize(boolean includeStatusBar, boolean includeLayerPanel, boolean includeStatsPanel)
        {
            // Create the WorldWindow.
            this.wwjPanel = this.createAppPanel(this.canvasSize, includeStatusBar);
            this.wwjPanel.setPreferredSize(canvasSize);

            // Put the pieces together.
            this.getContentPane().add(wwjPanel, BorderLayout.CENTER);
            if (includeLayerPanel)
            {
                this.controlPanel = new JPanel(new BorderLayout(10, 10));
                this.layerPanel = new LayerPanel(this.getWwd());
                this.controlPanel.add(this.layerPanel, BorderLayout.CENTER);

                /**
                 * ADD SERVERS
                 */
                final String[] servers = new String[]
                    {
                        "http://localhost:8080/geoserver/ows?service=WCS&version=2.0.1&request=GetCapabilities",
                    };

                for (int i = 0; i < servers.length; i++)
                {
                    this.addTab(i + 0, servers[i]); // i+1 to place all server tabs to the right of the Add Server tab
                }

                // Display the first server pane by default.
                this.tabbedPane.setSelectedIndex(0);
                this.previousTabIndex = this.tabbedPane.getSelectedIndex();
                this.controlPanel.add(this.tabbedPane, BorderLayout.WEST);
                this.getContentPane().add(this.controlPanel, BorderLayout.WEST);
            }

            if (includeStatsPanel || System.getProperty("gov.nasa.worldwind.showStatistics") != null)
            {
                this.statsPanel = new StatisticsPanel(this.wwjPanel.getWwd(), new Dimension(250, canvasSize.height));
                this.getContentPane().add(this.statsPanel, BorderLayout.EAST);
            }

            // Create and install the view controls layer and register a controller for it with the World Window.
            ViewControlsLayer viewControlsLayer = new ViewControlsLayer();
            LayerList layers = getWwd().getModel().getLayers();
            layers.add(viewControlsLayer);
            this.getWwd().addSelectListener(new ViewControlsSelectListener(this.getWwd(), viewControlsLayer));

            // Register a rendering exception listener that's notified when exceptions occur during rendering.
            this.wwjPanel.getWwd().addRenderingExceptionListener(new RenderingExceptionListener()
            {
                public void exceptionThrown(Throwable t)
                {
                    if (t instanceof WWAbsentRequirementException)
                    {
                        String message = "Computer does not meet minimum graphics requirements.\n";
                        message += "Please install up-to-date graphics driver and try again.\n";
                        message += "Reason: " + t.getMessage() + "\n";
                        message += "This program will end when you press OK.";

                        JOptionPane.showMessageDialog(AppFrame.this, message, "Unable to Start Program",
                            JOptionPane.ERROR_MESSAGE);
                        System.exit(-1);
                    }
                }
            });

            // Search the layer list for layers that are also select listeners and register them with the World
            // Window. This enables interactive layers to be included without specific knowledge of them here.

            for (Layer layer : this.wwjPanel.getWwd().getModel().getLayers())
            {
                if (layer instanceof SelectListener)
                {
                    this.getWwd().addSelectListener((SelectListener) layer);
                }
            }

            this.pack();

            // Center the application on the screen.
            WWUtil.alignComponent(null, this, AVKey.CENTER);
            this.setResizable(true);
        }

        protected AppPanel createAppPanel(Dimension canvasSize, boolean includeStatusBar)
        {
            return new AppPanel(canvasSize, includeStatusBar);
        }

        public Dimension getCanvasSize()
        {
            return canvasSize;
        }

        public AppPanel getWwjPanel()
        {
            return wwjPanel;
        }

        public WorldWindow getWwd()
        {
            return this.wwjPanel.getWwd();
        }

        public StatusBar getStatusBar()
        {
            return this.wwjPanel.getStatusBar();
        }

        /**
         * @return This application's layer panel.
         *
         * @deprecated Use getControlPanel instead.
         */
        public LayerPanel getLayerPanel()
        {
            return this.layerPanel;
        }

        public JPanel getControlPanel()
        {
            return this.controlPanel;
        }

        public StatisticsPanel getStatsPanel()
        {
            return statsPanel;
        }

        public WCSCoveragePanel addTab(int position, String server)
        {
            final Dimension wcsPanelSize = new Dimension(200, 400);
            // Add a server to the tabbed dialog.
            try
            {
                WCSCoveragePanel coveragePanel = new WCSCoveragePanel(DemoWWJ.AppFrame.this.getWwd(), server,
                    wcsPanelSize);
                this.tabbedPane.add(coveragePanel, BorderLayout.CENTER);
                this.tabbedPane.setTitleAt(position, "Geoserver Elevations");

                return coveragePanel;
            }
            catch (URISyntaxException e)
            {
                JOptionPane.showMessageDialog(null, "Server URL is invalid", "Invalid Server URL",
                    JOptionPane.ERROR_MESSAGE);
                tabbedPane.setSelectedIndex(previousTabIndex);
                return null;
            }
        }
    }

    public static void insertBeforeCompass(WorldWindow wwd, Layer layer)
    {
        // Insert the layer into the layer list just before the compass.
        int compassPosition = 0;
        LayerList layers = wwd.getModel().getLayers();
        for (Layer l : layers)
        {
            if (l instanceof CompassLayer)
                compassPosition = layers.indexOf(l);
        }
        layers.add(compassPosition, layer);
    }

    static
    {

        Configuration.setValue(AVKey.INITIAL_LATITUDE, 42.92);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, -122.10);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 25000);
        System.setProperty("java.net.useSystemProxies", "true");
        if (Configuration.isMacOS())
        {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
            System.setProperty("apple.awt.brushMetalLook", "true");
        }
        else if (Configuration.isWindowsOS())
        {
            System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
        }
    }

    public static AppFrame start(String appName, Class appFrameClass)
    {
        if (Configuration.isMacOS() && appName != null)
        {
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
        }

        try
        {
            final AppFrame frame = (AppFrame) appFrameClass.newInstance();
            frame.setTitle(appName);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            java.awt.EventQueue.invokeLater(new Runnable()
            {
                public void run()
                {
                    frame.setVisible(true);
                }
            });

            return frame;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args)
    {
        // Call the static start method like this from the main method of your derived class.
        // Substitute your application's name for the first argument.
        DemoWWJ.start("World Wind Demo", AppFrame.class);
    }
}
