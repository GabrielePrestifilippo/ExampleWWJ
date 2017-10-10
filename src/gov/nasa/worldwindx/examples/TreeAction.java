
package gov.nasa.worldwindx.examples.util;

import gov.nasa.worldwindx.examples.LayerPanel;
import gov.nasa.worldwindx.examples.MGRSAttributesPanel;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import gov.nasa.worldwind.WorldWindow;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.ScalebarLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.Earth.MGRSGraticuleLayer;

public class TreeAction extends AbstractAction
{
	private LayerPanel layerPanel = null;
	private String objectName = null;
	private WorldWindow wwd = null ;

	public TreeAction( WorldWindow wwd, LayerPanel layerPanel, String name )
	{
		super(name);

		this.wwd = wwd ;

		this.layerPanel = layerPanel;
	}

	public void setObjectName( String objectName )
	{
		this.objectName = objectName ;
	}

	public void actionPerformed(ActionEvent e)
	{
		if ( objectName == null )
		{
			// problem with the name
			return ;
		}

		JFrame actionFrame = new JFrame("Properties");

		Layer layer = wwd.getModel().getLayers().getLayerByName( objectName );

		if ( layer == null )
		{
			// another problem with the name
		}

		else if ( layer instanceof MGRSGraticuleLayer )
		{
			MGRSAttributesPanel panel = new MGRSAttributesPanel( (MGRSGraticuleLayer) layer );

			actionFrame.getContentPane().add( panel, BorderLayout.CENTER );
		}

		actionFrame.setLocationRelativeTo( layerPanel );
		actionFrame.setAlwaysOnTop(true);

		actionFrame.pack();
		actionFrame.setVisible(true);
	}
}
