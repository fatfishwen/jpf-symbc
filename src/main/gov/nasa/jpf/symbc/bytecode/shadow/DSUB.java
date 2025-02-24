/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.symbc.bytecode.shadow;

import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;
import gov.nasa.jpf.symbc.numeric.*;

/**
 * Subtract double ..., value1, value2 => ..., result
 */
public class DSUB extends gov.nasa.jpf.jvm.bytecode.DSUB {

    @Override
    public Instruction execute(ThreadInfo th) {
        StackFrame sf = th.getModifiableTopFrame();

        Object op_v1 = sf.getLongOperandAttr();
        double v1 = sf.popDouble();
        Object op_v2 = sf.getLongOperandAttr();
        double v2 = sf.popDouble();
        sf.pushDouble(v2 - v1);

        if (!(op_v1 == null && op_v2 == null && th.getExecutionMode() == Execute.BOTH)) {

            // Get symbolic and shadow expressions from the operands
            RealExpression oldSym_v1 = ShadowBytecodeUtils.getOldSymbolicExpr(op_v1, v1);
            RealExpression oldSym_v2 = ShadowBytecodeUtils.getOldSymbolicExpr(op_v2, v2);
            RealExpression oldCon_v1 = ShadowBytecodeUtils.getOldConcreteExpr(op_v1, v1);
            RealExpression oldCon_v2 = ShadowBytecodeUtils.getOldConcreteExpr(op_v2, v2);

            RealExpression newSym_v1 = ShadowBytecodeUtils.getNewSymbolicExpr(op_v1, v1);
            RealExpression newSym_v2 = ShadowBytecodeUtils.getNewSymbolicExpr(op_v2, v2);
            RealExpression newCon_v1 = ShadowBytecodeUtils.getNewConcreteExpr(op_v1, v1);
            RealExpression newCon_v2 = ShadowBytecodeUtils.getNewConcreteExpr(op_v2, v2);

            RealExpression oldSym_result = null;
            RealExpression newSym_result = null;
            RealExpression oldCon_result = null;
            RealExpression newCon_result = null;

            if (th.getExecutionMode() == Execute.OLD || th.getExecutionMode() == Execute.BOTH) {
                if (th.getExecutionMode() == Execute.OLD) {
                    newSym_result = new RealConstant(0);
                    newCon_result = new RealConstant(0);
                }
                if (oldSym_v1 instanceof RealConstant && oldSym_v2 instanceof RealConstant) {
                    oldSym_result = new RealConstant(oldSym_v2.solution() - oldSym_v1.solution());
                } else {
                    oldSym_result = oldSym_v2._minus(oldSym_v1);
                }
                oldCon_result = new RealConstant(oldCon_v2.solution() - oldCon_v1.solution());
            }

            if (th.getExecutionMode() == Execute.NEW || th.getExecutionMode() == Execute.BOTH) {
                if (th.getExecutionMode() == Execute.NEW) {
                    oldSym_result = new RealConstant(0);
                    oldCon_result = new RealConstant(0);
                }
                if (newSym_v2 instanceof RealConstant && newSym_v1 instanceof RealConstant) {
                    newSym_result = new RealConstant(newSym_v2.solution() - newSym_v1.solution());
                } else {
                    newSym_result = newSym_v2._minus(newSym_v1);
                }
                newCon_result = new RealConstant(newCon_v2.solution() - newCon_v1.solution());
            }

            if (op_v1 instanceof DiffExpression || op_v2 instanceof DiffExpression
                    || th.getExecutionMode() != Execute.BOTH) {
                DiffExpression result = new DiffExpression(oldSym_result, newSym_result, oldCon_result, newCon_result);
                sf.setLongOperandAttr(result);
            } else {
                // oldSym_result and newSym_result are equal, so we just store one of them
                RealExpression result = (RealExpression) newSym_result;
                sf.setLongOperandAttr(result);
            }
        }

        return getNext(th);
    }

}
