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
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * 22/08/2016
 */
package pt.lsts.neptus.types.coord;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import info.necsave.proto.parser.WGS84Utilities;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.ws.Location;

/**
 * @author zp
 *
 */
@XmlType
public class PolygonType implements Renderer2DPainter {

    @XmlElement
    protected ArrayList<Vertex> vertices = new ArrayList<>();
    private String id;

    protected PathElement elem = null;
    protected boolean filled = true;

    private Color color = Color.RED.darker();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Add a polygon vertex
     * @param latDegs the latitude of the vertex to add
     * @param lonDegs the longitude of the vertex to add
     */
    public void addVertex(double latDegs, double lonDegs) {
        synchronized (vertices) {
            vertices.add(new Vertex(latDegs, lonDegs));
        }
        recomputePath();
    }

    /**
     * Add new vertex given its LocationType
     * */
    public void addVertex(LocationType loc) {
        synchronized (vertices) {
            vertices.add(new Vertex(loc));
        }
    }

    /**
     * Remove all polygon vertices
     */
    public void clearVertices() {
        synchronized (vertices) {
            vertices.clear();
        }
        recomputePath();

    }

    /**
     * Retrieve an <strong>unmodifiable</strong> list of vertices
     * @return list of the vertices in this polygon
     */
    public List<Vertex> getVertices() {
        return Collections.unmodifiableList(vertices);
    }


    public void removeVertex(Vertex v) {
        if (v == null)
            return;
        synchronized (vertices) {
            vertices.remove(v);
        }
        recomputePath();
    }

    public void setColor(Color c) {
        color = c;
    }

    /**
     * @param filled the filled to set
     */
    public final void setFilled(boolean filled) {
        this.filled = filled;
    }

    /**
     * @see Renderer2DPainter
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (elem != null) {
            elem.setMyColor(color);
            elem.paint(g, renderer, renderer.getRotation());
        }
    }

    public void recomputePath() {
        synchronized (vertices) {
            if (vertices.isEmpty()) {
                elem = null;
                return;
            }

            elem = new PathElement();
            elem.setFilled(filled);
            elem.setMyColor(Color.yellow);

            elem.setCenterLocation(new LocationType(vertices.get(0).getLocation()));
            for (Vertex v : vertices)
                elem.addPoint(new LocationType(v.getLocation()));
            elem.setFinished(true);
        }
    }

    @Override
    public String toString() {
        return "Polygon ("+vertices.size()+" vertices)";
    }

    public PolygonType clone() {
        StringWriter writer = new StringWriter();
        JAXB.marshal(this, writer);
        return JAXB.unmarshal(new StringReader(writer.toString()), getClass());
    }

    /**
     * Given a list of locations, calculates and returns its centroid's
     * location
     * */
    public LocationType getCentroid() {
        List<LocationType> locations = new ArrayList<>();
        vertices.forEach(v -> locations.add(v.lt.getNewAbsoluteLatLonDepth()));

        return CoordinateUtil.computeLocationsCentroid(locations);
    }

    @XmlType
    public static class Vertex {
        private LocationType lt;

        public Vertex(LocationType lt) {
            this.lt = new LocationType(lt);
        }

        public Vertex(double latDegs, double lonDegs) {
            lt = new LocationType(latDegs, lonDegs);
        }

        @Override
        public int hashCode() {
            return (""+lt.getLatitudeDegs()+","+lt.getLongitudeDegs()).hashCode();
        }

        public void setLocation(LocationType newLt) {
            lt = new LocationType(newLt);
        }

        public LocationType getLocation() {
            return lt;
        }

        public double getLatitudeDegs() {
         return lt.getNewAbsoluteLatLonDepth().getLatitudeDegs();
        }

        public double getLongitudeDegs() {
            return lt.getNewAbsoluteLatLonDepth().getLongitudeDegs();
        }
    }

    public void translate(double offsetNorth, double offsetEast) {
        synchronized (vertices) {
            vertices.forEach(v -> v.getLocation().translatePosition(offsetNorth, offsetEast, 0));
        }
        recomputePath();
    }

    /**
     * Rotate this polygon by yaw rads
     * */
    public void rotate(double yawRads) {
        synchronized (vertices) {
            LocationType pivot = elem.getCenterPoint().getNewAbsoluteLatLonDepth();
            for (PolygonType.Vertex v : vertices) {
                v.lt.convertToAbsoluteLatLonDepth();

                double pivotLat = pivot.getLatitudeRads();
                double pivotLon = pivot.getLongitudeRads();
                double shiftLat = v.lt.getLatitudeRads() - pivotLat;
                double shiftLon = v.lt.getLongitudeRads() - pivotLon;

                double newLon = pivotLon + Math.cos(yawRads) * shiftLon - Math.sin(yawRads) * shiftLat;
                double newLat = pivotLat + Math.sin(yawRads) * shiftLon + Math.cos(yawRads) * shiftLat;

                v.setLocation(new LocationType(Math.toDegrees(newLat), Math.toDegrees(newLon)));
            }
        }
        recomputePath();
    }

    public static void main(String[] args) {
        PolygonType pt = new PolygonType();
        pt.addVertex(41, -8);
        pt.addVertex(42, -8);
        pt.addVertex(42, -7);
        pt.addVertex(41, -8);

        StringWriter writer = new StringWriter();
        JAXB.marshal(pt, writer);
        String xml1 = writer.toString();
        System.out.println(xml1);

        writer = new StringWriter();
        PolygonType other = JAXB.unmarshal(new StringReader(xml1), PolygonType.class);
        JAXB.marshal(other, writer);
        String xml2 = writer.toString();
        System.out.println(xml2);
        System.out.println(xml1.equals(xml2));
        System.out.println(other.clone());
    }
}