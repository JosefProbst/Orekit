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
package org.orekit.models.earth;

import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class EstimatedTroposphericModelTest {

    @BeforeClass
    public static void setUpGlobal() {
        Utils.setDataRoot("atmosphere");
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data:potential/shm-format");
    }

    @Test
    public void testMendesPavlis() {

        // Site:   McDonald Observatory
        //         latitude:  30.67166667°
        //         height:    2075 m
        //
        // Meteo:  pressure:            798.4188 hPa
        //         water vapor presure: 14.322 hPa
        //         temperature:         300.15 K
        //         humidity:            40 %
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)

        final AbsoluteDate date = new AbsoluteDate(2009, 8, 12, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(30.67166667);
        final double height      = 2075;
        final double pressure    = 798.4188;
        final double temperature = 300.15;
        final double humidity    = 0.4;
        final double lambda      = 0.532;
        
        final double elevation        = FastMath.toRadians(15.0);
        // Expected mapping factor: 3.80024367 (Ref)
        final double[] expectedMapping = new double[] {
            3.80024367,
            3.80024367
        };
        
        // Test for the second constructor
        final MendesPavlisModel model = new MendesPavlisModel(temperature, pressure,
                                                               humidity, latitude, lambda);

        doTestMappingFactor(expectedMapping, model, height, elevation, date, 5.0e-8);
    }

    @Test
    public void testGMF() {

        // Site (NRAO, Green Bank, WV): latitude:  0.6708665767 radians
        //                              longitude: -1.393397187 radians
        //                              height:    844.715 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //         IERS Technical Note No. 36, BKG (2010)
        //
        // Expected mapping factors : hydrostatic -> 3.425246 (Ref)
        //                                    wet -> 3.449589 (Ref)

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());
        
        final double latitude    = 0.6708665767;
        final double longitude   = -1.393397187;
        final double height      = 844.715;

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425246;
        final double expectedWet   = 3.449589;

        final double[] expectedMappingFactors = new double[] {
            expectedHydro,
            expectedWet
        };

        final MappingFunction model = new GlobalMappingFunctionModel(latitude, longitude);

        doTestMappingFactor(expectedMappingFactors, model, height, elevation, date, 1.0e-6);

    }
    
    @Test
    public void testVMF1() {

        // Site (NRAO, Green Bank, WV): latitude:  38°
        //                              longitude: 280°
        //                              height:    824.17 m
        //
        // Date: MJD 55055 -> 12 August 2009 at 0h UT
        //
        // Ref for the inputs:    Petit, G. and Luzum, B. (eds.), IERS Conventions (2010),
        //                        IERS Technical Note No. 36, BKG (2010)
        //
        // Values: ah  = 0.00127683
        //         aw  = 0.00060955
        //         zhd = 2.0966 m
        //         zwd = 0.2140 m
        //
        // Values taken from: http://vmf.geo.tuwien.ac.at/trop_products/GRID/2.5x2/VMF1/VMF1_OP/2009/VMFG_20090812.H00
        //
        // Expected mapping factors : hydrostatic -> 3.425088
        //                                    wet -> 3.448300
        //
        // Expected outputs are obtained by performing the Matlab script vmf1_ht.m provided by TU WIEN:
        // http://vmf.geo.tuwien.ac.at/codes/
        //

        final AbsoluteDate date = AbsoluteDate.createMJDDate(55055, 0, TimeScalesFactory.getUTC());
        
        final double latitude    = FastMath.toRadians(38.0);
        final double height      = 824.17;

        final double elevation     = 0.5 * FastMath.PI - 1.278564131;
        final double expectedHydro = 3.425088;
        final double expectedWet   = 3.448300;
        
        final double[] expectedMappingFactors = new double[] {
            expectedHydro,
            expectedWet
        };
        
        final double[] a = { 0.00127683, 0.00060955 };
        final double[] z = {2.0966, 0.2140};
        
        final ViennaOneModel model = new ViennaOneModel(a, z, latitude);

        doTestMappingFactor(expectedMappingFactors, model, height, elevation, date, 4.1e-6);

    }

    private void doTestMappingFactor(final double[] expectedMappingFactors, final MappingFunction model,
                                     final double height, final double elevation, final AbsoluteDate date,
                                     final double precision) {

        final double[] computedMappingFactors = model.mappingFactors(height, elevation, date);

        Assert.assertEquals(expectedMappingFactors[0],   computedMappingFactors[0], precision);
        Assert.assertEquals(expectedMappingFactors[1],   computedMappingFactors[1], precision);
    }
    
}