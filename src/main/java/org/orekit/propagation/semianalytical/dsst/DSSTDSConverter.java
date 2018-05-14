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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.FieldAttitude;
import org.orekit.errors.OrekitException;
import org.orekit.orbits.FieldEquinoctialOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.integration.AbstractDSConverter;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

/** Converter for states and parameters arrays.
 * @author Luc Maisonobe
 * @since 9.0
 */
class DSSTDSConverter extends AbstractDSConverter {

    /** Dimension of the state. */
    private final int freeStateParameters;

    /** States with various number of additional parameters for force models. */
    private final List<FieldSpacecraftState<DerivativeStructure>> dsStates;

    /** Simple constructor.
     * @param state
     * @param state regular state
     * @param freeStateParameters number of free parameters, either
     * 6 (6 equinoctial parameters (EP)) or 7 (EP and mass)
     * @param provider provider to use if attitude needs to be recomputed
     * @exception OrekitException if attitude cannot be computed
     */
    DSSTDSConverter(final SpacecraftState state, final int freeStateParameters,
                    final AttitudeProvider provider)
        throws OrekitException {

        super(freeStateParameters);

        this.freeStateParameters = freeStateParameters;

        // prepare derivation variables, position, optionally mass
        final DSFactory factory = new DSFactory(freeStateParameters, 1);

        final DerivativeStructure sma = factory.variable(0, state.getA());
        final DerivativeStructure ex = factory.variable(1, state.getEquinoctialEx());
        final DerivativeStructure ey = factory.variable(2, state.getEquinoctialEy());
        final DerivativeStructure hx = factory.variable(3, state.getHx());
        final DerivativeStructure hy = factory.variable(4, state.getHy());
        final DerivativeStructure l = factory.variable(5, state.getLv());

        // date
        final AbsoluteDate date = state.getDate();
        final FieldAbsoluteDate<DerivativeStructure> dateField = new FieldAbsoluteDate<>(factory.getDerivativeField(), date);

        // mass may have derivatives or not
        final DerivativeStructure dsM = (freeStateParameters > 6) ?
                                                                   factory.variable(6, state.getMass()) :
                                                                       factory.constant(state.getMass());

        final FieldOrbit<DerivativeStructure> dsOrbit =
                        new FieldEquinoctialOrbit<>(sma, ex, ey, hx, hy, l, PositionAngle.TRUE,
                                        state.getFrame(), dateField, state.getMu());

        final FieldAttitude<DerivativeStructure> dsAttitude;
        // compute attitude partial derivatives with respect to position/velocity
        dsAttitude = provider.getAttitude(dsOrbit, dsOrbit.getDate(), dsOrbit.getFrame());

        // initialize the list with the state having 0 force model parameters
        dsStates = new ArrayList<>();
        dsStates.add(new FieldSpacecraftState<>(dsOrbit, dsAttitude, dsM));

    }

    /** Get the state with the number of parameters consistent with force model.
     * @param forceModel force model
     * @return state with the number of parameters consistent with force model
     */
    public FieldSpacecraftState<DerivativeStructure> getState(final DSSTForceModel forceModel) {

        // count the required number of parameters
        int nbParams = 0;
        for (final ParameterDriver driver : forceModel.getParametersDrivers()) {
            if (driver.isSelected()) {
                ++nbParams;
            }
        }

        // fill in intermediate slots
        while (dsStates.size() < nbParams + 1) {
            dsStates.add(null);
        }

        if (dsStates.get(nbParams) == null) {
            // it is the first time we need this number of parameters
            // we need to create the state
            final DSFactory factory = new DSFactory(freeStateParameters + nbParams, 1);
            final FieldSpacecraftState<DerivativeStructure> s0 = dsStates.get(0);

            // orbit
            final FieldPVCoordinates<DerivativeStructure> pv0 = s0.getPVCoordinates();
            final FieldOrbit<DerivativeStructure> dsOrbit =
                            new FieldEquinoctialOrbit<>(new TimeStampedFieldPVCoordinates<>(s0.getDate().toAbsoluteDate(),
                                                                                          extend(pv0.getPosition(),     factory),
                                                                                          extend(pv0.getVelocity(),     factory),
                                                                                          extend(pv0.getAcceleration(), factory)),
                                                      s0.getFrame(), s0.getMu());

            // attitude
            final FieldAngularCoordinates<DerivativeStructure> ac0 = s0.getAttitude().getOrientation();
            final FieldAttitude<DerivativeStructure> dsAttitude =
                            new FieldAttitude<>(s0.getAttitude().getReferenceFrame(),
                                                new TimeStampedFieldAngularCoordinates<>(dsOrbit.getDate(),
                                                                                         extend(ac0.getRotation(), factory),
                                                                                         extend(ac0.getRotationRate(), factory),
                                                                                         extend(ac0.getRotationAcceleration(), factory)));

            // mass
            final DerivativeStructure dsM = extend(s0.getMass(), factory);

            dsStates.set(nbParams, new FieldSpacecraftState<>(dsOrbit, dsAttitude, dsM));

        }

        return dsStates.get(nbParams);

    }

    /** Get the force model parameters.
     * @param state state as returned by {@link #getState(DSSTForceModel)}
     * @param forceModel force model associated with the parameters
     * @return force model parameters
     * @since 9.0
     */
    public DerivativeStructure[] getParameters(final FieldSpacecraftState<DerivativeStructure> state,
                                               final DSSTForceModel forceModel) {
        final DSFactory factory = state.getMass().getFactory();
        final ParameterDriver[] drivers = forceModel.getParametersDrivers();
        final DerivativeStructure[] parameters = new DerivativeStructure[drivers.length];
        int index = freeStateParameters;
        for (int i = 0; i < drivers.length; ++i) {
            parameters[i] = drivers[i].isSelected() ?
                            factory.variable(index++, drivers[i].getValue()) :
                            factory.constant(drivers[i].getValue());
        }
        return parameters;
    }

}