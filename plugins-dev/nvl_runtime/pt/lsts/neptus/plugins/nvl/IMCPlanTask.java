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
 * Author: lsts
 * 09/03/2017
 */
package pt.lsts.neptus.plugins.nvl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.types.mission.plan.PlanCompatibility;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.nvl.imc.AbstractIMCPlanTask;
import pt.lsts.nvl.runtime.NodeFilter;
import pt.lsts.nvl.runtime.Payload;
import pt.lsts.nvl.runtime.PayloadComponent;


public final class IMCPlanTask extends AbstractIMCPlanTask implements Cloneable {
    
    private List<PayloadComponent> requirements = Collections.synchronizedList(new ArrayList<>());;
    
    public IMCPlanTask(PlanType plan) {
        super(plan.getId(), (PlanSpecification) plan.asIMCPlan(true));
        //PlanType only allows definitions of sensor payload other payloads must be defined through setRequirements 
        for(String p: PlanCompatibility.payloadsRequired(plan))
            requirements.add(new PayloadComponent(p));
    }
    
    public IMCPlanTask(PlanSpecification plan){
        super(plan.getPlanId(), plan);
    }
    

    @Override
    public IMCPlanExecutor getExecutor() {
        return new IMCPlanExecutor(this);
    }
    
    public NodeFilter  getRequirements( ){
             
        return new NodeFilter().payload(new Payload(requirements));
    }
    
    public void setRequirements(NodeFilter requirements ){
        this.requirements = requirements.getRequiredPayload().getComponents();
    }
    
   public void addRequirement(String name){
       requirements.add(new PayloadComponent(name));
   }
   
   @Override
   public IMCPlanTask clone() {
       return new IMCPlanTask(getPlanSpecification());
   }
}
