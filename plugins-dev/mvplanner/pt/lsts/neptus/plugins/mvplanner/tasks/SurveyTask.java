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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: tsm
 * 24 Jan 2017
 */
package pt.lsts.neptus.plugins.mvplanner.tasks;

import pt.lsts.neptus.plugins.mvplanner.PlanTask;
import pt.lsts.neptus.plugins.mvplanner.mapdecomposition.GridArea;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.PolygonType;

import java.awt.Graphics2D;

public class SurveyTask extends PlanTask {
    private GridArea area;

    public SurveyTask(PolygonType object) {
        super(object);
        area = new GridArea(object, 60);
    }

    @Override
    public void paintTask(Graphics2D g, StateRenderer2D source) {
        if(area == null)
            return;

        area.paint(g, source, objectColor, false, true);
    }

    @Override
    public TaskTypeEnum getTaskType() {
        return TaskTypeEnum.Survey;
    }
}