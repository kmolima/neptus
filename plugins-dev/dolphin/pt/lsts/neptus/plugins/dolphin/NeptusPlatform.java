/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: edrdo
 * May 16, 2017
 */
package pt.lsts.neptus.plugins.dolphin;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.VehicleState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.dolphin.dsl.Engine;
import pt.lsts.dolphin.runtime.EnvironmentException;
import pt.lsts.dolphin.runtime.NodeSet;
import pt.lsts.dolphin.runtime.Platform;

public enum NeptusPlatform implements Platform {
    INSTANCE;

    private final Map<String,IMCPlanTask> imcPlanTasks = new ConcurrentHashMap<>();


    public static final NeptusPlatform getInstance() {
        return INSTANCE;
    }

    private DolphinConsolePanel consolePanel;

    private NeptusPlatform() {
        pt.lsts.dolphin.util.Debug.enable();
        Engine.create(this);
        consolePanel = null;
        d("initialized");
    }

    public void associateTo(DolphinConsolePanel cp) {
        detach();
        d("attached to console");
        consolePanel = cp;
        refreshConsolePlans(cp); 
    }


    private void refreshConsolePlans(DolphinConsolePanel cp) {
        imcPlanTasks.clear();
        for(PlanType plan: cp.getConsole().getMission().getIndividualPlansList().values()){
            //displayMessage("IMC plan available: %s", plan.getId());
            imcPlanTasks.put(plan.getId(), new IMCPlanTask((PlanSpecification) plan.asIMCPlan(true)));
        }
    }

    public void detach() {
        imcPlanTasks.clear();
        if (consolePanel != null) {
            consolePanel = null;
            d("detach from console");
        }
    }

    @Override
    public NodeSet getConnectedNodes() {
        NodeSet set = new NodeSet();
        for(ImcSystem vec: ImcSystemsHolder.lookupActiveSystemVehicles()){
            VehicleState state  = ImcMsgManager.getManager().getState(vec.getName()).last(VehicleState.class);
            if (state != null && state.getOpMode() == VehicleState.OP_MODE.SERVICE) {
                displayMessage("%s is available!", vec.getName());
                set.add(new NodeAdapter(vec));
            }
        }
        return set;
    }

//    public void onPlanChanged(ConsoleEventPlanChange changedPlan) {
//        PlanType oldPlan = changedPlan.getOld();
//        PlanType newPlan = changedPlan.getCurrent();
//
//        if (newPlan == null){
//            if(! consolePanel.getConsole().getMission().getIndividualPlansList().containsKey(oldPlan.getId())){
//                displayMessage("removing IMC plan %s", oldPlan.getId());
//                imcPlanTasks.remove(oldPlan.getId());
//            }
//        }
//        else{
//            displayMessage("replacing IMC plan %s", oldPlan.getId());
//            imcPlanTasks.put(newPlan.getId(), new IMCPlanTask((PlanSpecification) oldPlan.asIMCPlan(true)));
//        }
//
//    }
    
    public void storeInConsole(PlanSpecification ps){
        
        if(consolePanel!=null){
            PlanType plan = IMCUtils.parsePlanSpecification(consolePanel.getConsole().getMission(),ps);
            consolePanel.getConsole().getMission().addPlan(plan);
            consolePanel.getConsole().getMission().save(true);
            consolePanel.getConsole().updateMissionListeners();
        }
        else{
            displayMessage("Unable to add plan to console because it does not exits.\n",(Object[])null);
            NeptusLog.pub().error("Unable to add plan to console because it does not exits.\n");
        }
           
    }
    
    @Override
    public IMCPlanTask getPlatformTask(String id) {
        refreshConsolePlans(consolePanel);
        IMCPlanTask task = imcPlanTasks.get(id);
        if (task == null) {
            displayMessage("No such IMC plan: '%s'", id);
            throw new EnvironmentException("No such IMC plan: " + id);
        }
        return task.clone();
    }

    @Override
    public void displayMessage(String fmt, Object... args) {
      //d(fmt, args);
      String debug = String.format(fmt, args);
      NeptusLog.pub().debug(debug);
      if (consolePanel != null) {
          consolePanel.displayMessage(fmt, args); 
      }
    }

    public void run(File scriptFile) {
        if (scriptFile.exists()) {
            if(consolePanel!=null)
                refreshConsolePlans(consolePanel); //unable to dispatch OnPlanChangedEvent sometimes!
            displayMessage("will run %s", scriptFile.getAbsolutePath());
            Engine.getInstance().run(scriptFile);
        }
    }
    
    public void stopExecution() {
       Engine.getInstance().stopExecution(); 
       //TODO invoke IMCPlanExecutor.teardown() somehow 
    }  

    @Override
    public void customizeGroovyCompilation(CompilerConfiguration cc) {
        displayMessage("Customizing compilation for Neptus runtime ...");
        ImportCustomizer ic = new ImportCustomizer();
        ic.addStaticStars("pt.lsts.neptus.plugins.dolphin.dsl.Instructions");
        ic.addStarImports("pt.lsts.imc.groovy.dsl");
        for (String msg : IMCDefinition.getInstance().getConcreteMessages()) {
          ic.addImports("pt.lsts.imc." + msg);
        }
        cc.addCompilationCustomizers(ic);
    }

    @Override 
    public List<File> getExtensionFiles() {
        displayMessage("Configuring extension files ...");
        LinkedList<File> list = new LinkedList<>();
        File dir = new File("conf/dolphin/extensions");
        if (dir.isDirectory()) {
          for (String fileName : dir.list()) {
             if (fileName.endsWith(".groovy")) {
                 File f = new File(dir,fileName);
                 displayMessage("- Extension file found: %s", f.getAbsolutePath());
                 list.add(new File(dir,fileName));
             }
          }
        }
        return list;
    }


    @Override
    public String askForInput(String prompt) {
        return consolePanel.askForInput(prompt);
    }


}
