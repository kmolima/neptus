/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: hfq
 * Apr 3, 2013
 */
package pt.up.fe.dceg.neptus.plugins.vtk;

import java.awt.Component;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.mra.MRAPanel;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.mra3d.Marker3d;
import pt.up.fe.dceg.neptus.plugins.vtk.filters.DownsamplePointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.MultibeamToPointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointcloud.PointCloud;
import pt.up.fe.dceg.neptus.plugins.vtk.pointtypes.PointXYZ;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.AxesWidget;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.MultibeamToolBar;
import pt.up.fe.dceg.neptus.plugins.vtk.visualization.Window;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import vtk.vtkActor;
import vtk.vtkCanvas;
import vtk.vtkLinearExtrusionFilter;
import vtk.vtkNativeLibrary;
import vtk.vtkPolyDataMapper;
import vtk.vtkVectorText;

/**
 * @author hfq
 *
 */
@PluginDescription(author = "hfq", name = "Vtk")
public class Vtk extends JPanel implements MRAVisualization, PropertiesProvider {
    private static final long serialVersionUID = 1L;
    
    @NeptusProperty(name = "Points to ignore on Multibeam 3D", description="Fixed step of number of points to jump on multibeam Pointcloud stored for render purposes.")
    public int ptsToIgnore = 40;
    
    @NeptusProperty(name = "Approach to ignore points on Multibeam 3D", description="Type of approach to ignore points on multibeam either by a fixed step (false) or by a probability (true).")
    public boolean approachToIgnorePts = true; 
    
    @NeptusProperty(name = "Depth exaggeration multiplier", description="Multiplier value for depth exaggeration.")
    public int zExaggeration = 10;
    
    @NeptusProperty(name = "Timestamp increment", description="Timestamp increment for the 83P parser (in miliseconds).")
    public long timestampMultibeamIncrement = 0;
    
    @NeptusProperty(name = "Yaw Increment", description="180 Yaw (psi) increment for the 83P parser, set true to increment + 180º.")
    public boolean yawMultibeamIncrement = false;
    
    // there are 2 types of rendering objects on VTK - vtkPanel and vtkCanvas. vtkCanvas seems to have a better behaviour and performance.
    //public vtkPanel vtkPanel;
    public vtkCanvas vtkCanvas;
    
    public Window winCanvas;

    private static final String FILE_83P_EXT = ".83P";
    
    public LinkedHashMap<String, PointCloud<PointXYZ>> linkedHashMapCloud = new LinkedHashMap<>();       
    public PointCloud<PointXYZ> pointCloud;
 
    private Vector<Marker3d> markers = new Vector<>();
    
    public IMraLogGroup mraVtkLogGroup;
    public File file;
    
    private Boolean componentEnabled = false;

    private DownsamplePointCloud performDownsample;
    private Boolean isDownsampleDone = false;
    
    static {
        try {
            System.loadLibrary("jawt");
        }
        catch (Throwable e) {
            NeptusLog.pub().info("<###> cannot load jawt lib!");
        } 
            // for simple visualizations
        try {
            vtkNativeLibrary.COMMON.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkCommon, skipping...");
        }
        try {
            vtkNativeLibrary.FILTERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkFiltering, skipping...");
        }
        try {
            vtkNativeLibrary.IO.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkImaging, skipping...");
        }
        try {
            vtkNativeLibrary.GRAPHICS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkGrahics, skipping...");
        }
        try {
            vtkNativeLibrary.RENDERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkRendering, skipping...");
        }
                
        // Other
        try {
            vtkNativeLibrary.INFOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkInfoVis, skipping...");
        }
        try {
            vtkNativeLibrary.VIEWS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkViews, skipping...");
        }
        try {
            vtkNativeLibrary.WIDGETS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkWidgets, skipping...");
        }
        try {
            vtkNativeLibrary.GEOVIS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkGeoVis, skipping...");
        }
        try {
            vtkNativeLibrary.CHARTS.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkCharts, skipping...");
        }
        // FIXME not loading vtkHybrid ?!
        try {
            vtkNativeLibrary.HYBRID.LoadLibrary();
        }
        catch (Throwable e) {
            //NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().warn("cannot load vtkHybrid, skipping...");
        }
        try {
            vtkNativeLibrary.VOLUME_RENDERING.LoadLibrary();
        }
        catch (Throwable e) {
            NeptusMRA.vtkEnabled = false;
            NeptusLog.pub().info("<###> cannot load vtkVolumeRendering, skipping...");
        }
    }
    
    /**
     * @param panel
     */
    public Vtk(MRAPanel panel) {
        super(new MigLayout());
    }

    @Override
    public String getName() {
        return "Multibeam 3D";
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {    
        if (!componentEnabled)
        {
            vtkCanvas = new vtkCanvas();
           
            pointCloud = new PointCloud<>();
            pointCloud.setCloudName("multibeam");
            linkedHashMapCloud.put(pointCloud.getCloudName(), pointCloud);
            
            winCanvas = new Window(vtkCanvas, linkedHashMapCloud);
            
            vtkCanvas.GetRenderer().ResetCamera();
            
                // add vtkCanvas to Layout
            add(vtkCanvas, "W 100%, H 100%");
            
            vtkCanvas.LightFollowCameraOn();
            vtkCanvas.setEnabled(true);
            
            componentEnabled = true;

            MultibeamToPointCloud multibeamToPointCloud = new MultibeamToPointCloud(getLog(), pointCloud, approachToIgnorePts, ptsToIgnore, timestampMultibeamIncrement, yawMultibeamIncrement);
            //BathymetryInfo batInfo = new BathymetryInfo();
            //batInfo = multibeamToPointCloud.batInfo;
            
            if (pointCloud.getNumberOfPoints() != 0) {  // checks wether there are any points to render!            
                MultibeamToolBar toolbar = new MultibeamToolBar(this);
                toolbar.createToolBar();
                add(toolbar.getToolBar(), "dock south");
                
                pointCloud.createLODActorFromPoints();

                AxesWidget axesWidget = new AxesWidget(winCanvas.getInteractorStyle().GetInteractor());            
                axesWidget.createAxesWidget();
                
                vtkCanvas.GetRenderer().AddActor(pointCloud.getCloudLODActor()); 
                
                    // set Up scalar Bar look up table
                winCanvas.getInteractorStyle().getScalarBar().setUpScalarBarLookupTable(pointCloud.getColorHandler().getLutZ());
                vtkCanvas.GetRenderer().AddActor(winCanvas.getInteractorStyle().getScalarBar().getScalarBarActor());
                
                    // set up camera to +z viewpoint looking down
                vtkCanvas.GetRenderer().GetActiveCamera().SetViewUp(0.0, -1.0, 1.0);
                vtkCanvas.GetRenderer().GetActiveCamera().SetPosition(0.0,0.0,-100);
            }
            else {           
                String msgErrorMultibeam;
                msgErrorMultibeam = "No beams on Log file!";
                JOptionPane.showMessageDialog(null, msgErrorMultibeam);
                
                vtkVectorText vectText = new vtkVectorText();
                vectText.SetText("No beams on Log file!");
                
                vtkLinearExtrusionFilter extrude = new vtkLinearExtrusionFilter();
                extrude.SetInputConnection(vectText.GetOutputPort());
                extrude.SetExtrusionTypeToNormalExtrusion();
                extrude.SetVector(0, 0, 1);
                extrude.SetScaleFactor(0.5);
           
                vtkPolyDataMapper txtMapper = new vtkPolyDataMapper();
                txtMapper.SetInputConnection(extrude.GetOutputPort());
                vtkActor txtActor = new vtkActor();
                txtActor.SetMapper(txtMapper);
                txtActor.SetPosition(2.0, 2.0, 2.0);
                txtActor.SetScale(10.0);
                
                vtkCanvas.GetRenderer().AddActor(txtActor);
                
            }      
            vtkCanvas.GetRenderer().ResetCamera();
        }      
        return this;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        boolean beApplied = false;        
        
        if (NeptusMRA.vtkEnabled) {   // if it could load vtk libraries
                // Checks existance of a *.83P file
            file = source.getFile("Data.lsf").getParentFile();
            try {
                if (file.isDirectory()) {
                    for (File temp : file.listFiles()) {
                        if ((temp.toString()).endsWith(FILE_83P_EXT)) {
                            setLog(source);
                            beApplied = true;
                        }  
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return beApplied;
    }


    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getIcon("images/buttons/model3d.png");
    }

    @Override
    public Double getDefaultTimeStep() {
        return null;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    @Override
    public void onHide() {
    }

    @Override
    public void onShow() {
    }

    @Override
    public void onCleanup() {
//        try {
//            vtkPanel.disable();
//            //vtkPanel.Delete();
//        }
//        catch (Throwable e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
    }
    
    /**
     * @return the mraVtkLogGroup
     */
    public IMraLogGroup getLog() {
        return mraVtkLogGroup;
    }

    /**
     * @param mraVtkLogGroup the mraVtkLogGroup to set
     */
    private void setLog(IMraLogGroup log) {
        this.mraVtkLogGroup = log;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getProperties()
     */
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#setProperties(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return "Multibeam 3D properties";
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.gui.PropertiesProvider#getPropertiesErrors(com.l2fprod.common.propertysheet.Property[])
     */
    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
}
