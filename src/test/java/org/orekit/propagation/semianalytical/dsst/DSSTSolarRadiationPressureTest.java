/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.semianalytical.dsst;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedAngularCoordinates;

public class DSSTSolarRadiationPressureTest {
    
    @Test
    public void testGetMeanElementRate() throws IllegalArgumentException, OrekitException {
        
        final Frame earthFrame = FramesFactory.getGCRF();
        final AbsoluteDate initDate = new AbsoluteDate(2003, 9, 16, 0, 0, 0, TimeScalesFactory.getUTC());
        
        // a  = 42166258 m
        // ex = 6.532127416888538E-6
        // ey = 9.978642849310487E-5
        // hx = -5.69711879850274E-6
        // hy = 6.61038518895005E-6
        // lM = 8.56084687583949 rad
        final Orbit orbit = new EquinoctialOrbit(4.2166258E7,
                                                 6.532127416888538E-6,
                                                 9.978642849310487E-5,
                                                 -5.69711879850274E-6,
                                                 6.61038518895005E-6,
                                                 8.56084687583949,
                                                 PositionAngle.TRUE,
                                                 earthFrame,
                                                 initDate,
                                                 3.986004415E14);

        // SRP Force Model
        DSSTForceModel srp = new DSSTSolarRadiationPressure(1.2, 100., CelestialBodyFactory.getSun(),
                                                            Constants.WGS84_EARTH_EQUATORIAL_RADIUS);
        // Attitude of the satellite
        Rotation rotation =  new Rotation(0.9999999999999984,
                                          1.653020584550675E-8,
                                          -4.028108631990782E-8,
                                          -3.539139805514139E-8,
                                          false);
        Vector3D rotationRate = new Vector3D(0., 0., 0.);
        Vector3D rotationAcceleration = new Vector3D(0., 0., 0.);
        TimeStampedAngularCoordinates orientation = new TimeStampedAngularCoordinates(initDate,
                                                                                      rotation,
                                                                                      rotationRate,
                                                                                      rotationAcceleration);
        final Attitude att = new Attitude(earthFrame, orientation);
        
        // Spacecraft state
        final SpacecraftState state = new SpacecraftState(orbit, att, 1000.0);
        final AuxiliaryElements auxiliaryElements = new AuxiliaryElements(state.getOrbit(), 1);
        
        // Register the attitude provider to the force model
        AttitudeProvider attitudeProvider = new InertialProvider(rotation);
        srp.registerAttitudeProvider(attitudeProvider );
        
        // Compute the mean element rate
        final double[] elements = new double[7];
        Arrays.fill(elements, 0.0);
        final double[] daidt = srp.getMeanElementRate(state, auxiliaryElements);
        for (int i = 0; i < daidt.length; i++) {
            elements[i] = daidt[i];
        }
        
        Assert.assertEquals(6.843966348263062E-8, elements[0], 1.e-23);
        Assert.assertEquals(-2.990913371084091E-11, elements[1], 1.-26);
        Assert.assertEquals(-2.538374405334012E-10, elements[2], 1.e-25);
        Assert.assertEquals(2.0384702426501394E-13, elements[3], 1.e-28);
        Assert.assertEquals(-2.3346333406116967E-14, elements[4], 1.e-29);
        Assert.assertEquals(1.6087485237156322E-11, elements[5], 1.e-26);

    }
    
    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }
}