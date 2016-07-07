/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: tsmarques
 * 14 Mar 2016
 */
package pt.lsts.neptus.plugins.mvplanning.interfaces;

import java.util.List;

import pt.lsts.neptus.types.coord.LocationType;

/**
 * @author tsmarques
 *
 */
public abstract class MapCell {
    private boolean hasObstacle;
    private String id;

    public MapCell(boolean hasObstacle) {
        this.hasObstacle = hasObstacle;
    }

    /**
     * Add a neighbour of this cell
     * */
    public abstract void addNeighbour(MapCell neighCell);

    /**
     * Get a list of all the neighbour cells that have
     * no obstacles.
     * */
    public abstract List<MapCell> getNeighbours();

    /**
     * List this cell's neighbours, that have no obstacles,
     * in an anti-clockwise manner, starting at the given cell
     * */
    public abstract List<MapCell> getNeighboursAntiClockwise(MapCell firstNeighbour);


    /**
     * Verifies if a given cell is neighbour of this one
     * */
    public abstract boolean isNeighbour(MapCell cell);

    /**
     * Returns the center location of this cell
     * */
    public abstract LocationType getLocation();

    /**
     * Rotates this cell by 'yaw' radians around a
     * pivot location
     * */
    public abstract void rotate(double yaw, LocationType pivot);


    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setHasObstacle(boolean value) {
        hasObstacle = value;
    }

    public boolean hasObstacle() {
       return hasObstacle;
    }
}