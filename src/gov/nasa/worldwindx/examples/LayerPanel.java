
package gov.nasa.worldwindx.examples;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Dimension;
import java.util.Vector;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.BorderFactory;
import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import javax.swing.tree.TreePath;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.StarsLayer;
import gov.nasa.worldwind.layers.SkyGradientLayer;
import gov.nasa.worldwind.layers.SkyColorLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;

import gov.nasa.worldwindx.examples.util.CheckBoxNode;
import gov.nasa.worldwindx.examples.util.CheckBoxNodeEditor;
import gov.nasa.worldwindx.examples.util.CheckBoxNodeRenderer;
import gov.nasa.worldwindx.examples.util.TreeAction;
import gov.nasa.worldwindx.examples.util.NamedVector;

public class LayerPanel extends JPanel
{
	private JPanel layersPanel = null ;
	private JPanel treePanel_ = null ;
	private JTree tree_ = null ;
	private JScrollPane scrollPane = null ;
	private Font defaultFont = null ;
	private WorldWindow wwd = null ;
	private Dimension size_ = null ;

	// Font problem : too thin, use the textfield one
	private Font font_ = UIManager.getFont("Textfield.font");

	public LayerPanel( WorldWindow wwd )
	{
		this.wwd = wwd ;

		makePanel();
	}

	public LayerPanel( WorldWindow wwd, Dimension size )
	{
		this.wwd = wwd ;
		this.size_ = size ;

		makePanel();
	}

	protected void makePanel()
	{
		this.setLayout( new BorderLayout() );

		// Make and fill the panel holding the layer titles.
		this.layersPanel = new JPanel(new GridLayout(0, 1, 0, 4));
		this.layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		tree_ = this.fill(wwd);

		// Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
		JPanel dummyPanel = new JPanel(new BorderLayout());
		dummyPanel.add(this.layersPanel, BorderLayout.NORTH);

		// Put the tree in a scroll bar.
		this.scrollPane = new JScrollPane( tree_ );
		this.scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		if (size_ == null)
			size_= new Dimension( 200, 400 );

		this.scrollPane.setPreferredSize(size_);

		// Add the scroll bar and name panel to a titled panel that will resize with the main window.
		treePanel_ = new JPanel(new GridLayout(0, 1, 0, 10));
		treePanel_.setBorder(
			new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Layers")));
		treePanel_.setToolTipText("Layers to Show");
		treePanel_.add(scrollPane);
		this.add(treePanel_, BorderLayout.CENTER);
	}

	 /**
	 * Update the panel to match the layer list active in a WorldWindow.
	 *
	 * @param wwd WorldWindow that will supply the new layer list.
	 */
	public void update(WorldWindow wwd)
	{
		// Replace all the layer names in the layers panel with the names of the current layers.
		this.removeAll();

		makePanel();
	} 

	protected JTree fill(WorldWindow wwd)
	{
		JPopupMenu popup = new JPopupMenu();

		final TreeAction action = new TreeAction( wwd, this, "Properties" );

		popup.add(action); 

		Vector nasaOptions = new Vector();
		Vector microsoftOptions = new Vector();
		Vector usgsOptions = new Vector();
		Vector miscOptions = new Vector();

		// Fill the layers panel with the titles of all layers in the world window's current model.
		for (Layer layer : wwd.getModel().getLayers())
		{
			String name = layer.getName() ;

			if ( name.startsWith("NASA") )
				nasaOptions.add( new CheckBoxNode( "Group1", shorten("NASA",name), layer.isEnabled() ) );
			else if ( name.startsWith("USGS") )
				usgsOptions.add( new CheckBoxNode( "USGS", shorten("USGS",name), layer.isEnabled() ) );
			else
				miscOptions.add( new CheckBoxNode( "Misc", name, layer.isEnabled() ) );
			/*
			LayerAction action = new LayerAction(layer, wwd, layer.isEnabled());
			JCheckBox jcb = new JCheckBox(action);
			jcb.setSelected(action.selected);
			this.layersPanel.add(jcb);
			*/
		}

		Object rootNodes[] = new Object[3];

		rootNodes[0] = new NamedVector("Group1", nasaOptions);
		rootNodes[1] = new NamedVector("USGS", usgsOptions);
		rootNodes[2] = new NamedVector("Misc", miscOptions);

		Vector rootVector = new NamedVector("Root", rootNodes);

		JTree tree = new JTree(rootVector){
			/** 
			* @inherited <p>
			*/
			@Override
			public Point getPopupLocation(MouseEvent e)
			{
				if (e != null)
				{
			 		// here do your custom config, like f.i add/remove menu items based on context
			 		// this example simply changes the action name 
			 		TreePath path = getClosestPathForLocation(e.getX(), e.getY());

					TreePath parentPath = path.getParentPath();

					String layerName = rebuildLayerName( parentPath, path );

					if ( hasProperties( layerName ) )
					{
						action.putValue(Action.NAME, "Properties"); 
			 			action.setObjectName( layerName );
					}
					else
					{
						action.putValue(Action.NAME, "No Properties yet!"); 
			 			action.setObjectName( null );
					}

			 		return e.getPoint();
				}

				action.putValue(Action.NAME, "No Properties yet!"); 
			 	action.setObjectName( null );

				return null;
			}
		};

		tree.setComponentPopupMenu(popup);

		tree.setCellRenderer( new CheckBoxNodeRenderer( wwd, font_ ));
		tree.setCellEditor(new CheckBoxNodeEditor( wwd, tree, font_ ));

		tree.setEditable(true);
	
		tree.expandRow( 3 ); // Misc one
		tree.expandRow( 0 ); // Nasa one

		return tree ;
	}

	private String shorten( String parent, String name )
	{
		return name.substring( parent.length()+1 );
	}

	private String rebuildLayerName( TreePath parentPath, TreePath path )
	{
		String parentName = String.valueOf( parentPath.getLastPathComponent() );
		String name = String.valueOf(path.getLastPathComponent());

		if ( parentName.equals("Misc") )
			return name ;

		return parentName + " " + name ;
	}

	private boolean hasProperties( String layerName )
	{
		Layer layer = wwd.getModel().getLayers().getLayerByName( layerName );

		if ( layer == null || 
			layer instanceof StarsLayer ||
			layer instanceof SkyGradientLayer ||
			layer instanceof SkyColorLayer ||
			layer instanceof PlaceNameLayer ||
			layer instanceof WorldMapLayer ||
			layer instanceof CompassLayer )
		{
			return false ;
		}

		return true ;
	}
}
