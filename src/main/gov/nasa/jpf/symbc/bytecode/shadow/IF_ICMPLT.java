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

//Copyright (C) 2007 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.

//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.

//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.symbc.bytecode.shadow;

import gov.nasa.jpf.symbc.SymbolicInstructionFactory;
import gov.nasa.jpf.symbc.numeric.*;
import gov.nasa.jpf.symbc.numeric.PathCondition.Diff;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.ThreadInfo.Execute;

public class IF_ICMPLT extends gov.nasa.jpf.jvm.bytecode.IF_ICMPLT {
    public IF_ICMPLT(int targetPosition) {
        super(targetPosition);
    }

    @Override
    public Instruction execute(ThreadInfo ti) {
        StackFrame sf = ti.getModifiableTopFrame();

        Object op_v1 = sf.getOperandAttr(1);
        Object op_v2 = sf.getOperandAttr(0);

        if ((op_v1 == null) && (op_v2 == null)) { // both conditions are concrete
            return super.execute(ti);
        } else { // at least one condition is symbolic
            if (!ti.isFirstStepInsn()) {
                PCChoiceGenerator curPcCg;
                ChoiceGenerator<?> curCg = ti.getVM().getSystemState().getChoiceGenerator();
                if (curCg instanceof PCChoiceGenerator) {
                    curPcCg = (PCChoiceGenerator) curCg;
                } else {
                    curPcCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);
                }

                PathCondition pc;
                if (curPcCg != null) {
                    pc = curPcCg.getCurrentPC();
                } else {
                    pc = new PathCondition();
                }

                PCChoiceGenerator nextCg;
                if (SymbolicInstructionFactory.collect_constraints) {
                    nextCg = new PCChoiceGenerator(1);
                } else {
                    boolean diffPossible = ShadowBytecodeUtils.checkOperandsForPotentialDiff(op_v1, op_v2);
                    if (diffPossible && ti.getExecutionMode() == Execute.BOTH && !pc.isDiffPC()
                            && !ShadowBytecodeUtils.isChangeBoolean(this, ti)) {
                        nextCg = new PCChoiceGenerator(4);
                    } else {
                        nextCg = new PCChoiceGenerator(2);
                    }
                }
                nextCg.setOffset(this.position);
                nextCg.setMethodName(this.getMethodInfo().getFullName());
                nextCg.setExecutionMode(ti.getExecutionMode());
                ti.getVM().getSystemState().setNextChoiceGenerator(nextCg);
                return this;
            } else {
                // This actually returns the next instruction
                PCChoiceGenerator curCg = (PCChoiceGenerator) ti.getVM().getSystemState().getChoiceGenerator();

                PathCondition pc;
                PCChoiceGenerator prevCg = curCg.getPreviousChoiceGeneratorOfType(PCChoiceGenerator.class);

                if (prevCg == null) {
                    pc = new PathCondition();
                } else {
                    pc = prevCg.getCurrentPC();
                }

                assert (pc != null);

                int v2 = sf.pop();
                int v1 = sf.pop();

                IntegerExpression oldSym_v1 = ShadowBytecodeUtils.getOldSymbolicExpr(op_v1, v1);
                IntegerExpression oldSym_v2 = ShadowBytecodeUtils.getOldSymbolicExpr(op_v2, v2);
                IntegerExpression oldCon_v1 = ShadowBytecodeUtils.getOldConcreteExpr(op_v1, v1);
                IntegerExpression oldCon_v2 = ShadowBytecodeUtils.getOldConcreteExpr(op_v2, v2);

                IntegerExpression newSym_v1 = ShadowBytecodeUtils.getNewSymbolicExpr(op_v1, v1);
                IntegerExpression newSym_v2 = ShadowBytecodeUtils.getNewSymbolicExpr(op_v2, v2);
                IntegerExpression newCon_v1 = ShadowBytecodeUtils.getNewConcreteExpr(op_v1, v1);
                IntegerExpression newCon_v2 = ShadowBytecodeUtils.getNewConcreteExpr(op_v2, v2);

                ti.setExecutionMode(curCg.getExecutionMode());

                if (SymbolicInstructionFactory.collect_constraints) {

                    /* Follow concrete input. */
                    boolean oldConcrete_result = oldCon_v1.solutionInt() < oldCon_v2.solutionInt();
                    boolean newConcrete_result = newCon_v1.solutionInt() < newCon_v2.solutionInt();

                    if (pc.isDiffPC()) {
                        // Only execute new version as soon as we are in a diff path
                        pc._addDet(newConcrete_result ? Comparator.LT : Comparator.GE, newSym_v1, newSym_v2);
                        curCg.select(newConcrete_result ? 1 : 0);
                        curCg.setCurrentPC(pc);
                        return newConcrete_result ? this.getTarget() : this.getNext(ti);
                    } else if (!pc.containsDiffExpr()) {
                        if (!oldConcrete_result) {
                            // Both versions follow the true path in src
                            curCg.select(0);
                            if (ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH) {
                                pc._addDet(Comparator.GE, newSym_v1, newSym_v2);
                            }
                            if (ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH) {
                                pc._addDet(Comparator.GE, oldSym_v1, oldSym_v2);
                            }
                            curCg.setCurrentPC(pc);
                            return this.getNext(ti);
                        } else {
                            // Both versions follow the false path in src
                            curCg.select(1);
                            if (ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH) {
                                pc._addDet(Comparator.LT, newSym_v1, newSym_v2);
                            }
                            if (ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH) {
                                pc._addDet(Comparator.LT, oldSym_v1, oldSym_v2);
                            }
                            curCg.setCurrentPC(pc);
                            return this.getTarget();
                        }
                    } else if (ti.getExecutionMode() == Execute.BOTH) {
                        if (!oldConcrete_result && !newConcrete_result) {
                            // Both versions follow the true path in src
                            curCg.select(0);
                            pc._addDet(Comparator.GE, newSym_v1, newSym_v2);
                            pc._addDet(Comparator.GE, oldSym_v1, oldSym_v2);
                            curCg.setCurrentPC(pc);
                            return this.getNext(ti);
                        } else if (oldConcrete_result && newConcrete_result) {
                            // Both versions follow the false path in src
                            curCg.select(1);
                            pc._addDet(Comparator.LT, newSym_v1, newSym_v2);
                            pc._addDet(Comparator.LT, oldSym_v1, oldSym_v2);
                            curCg.setCurrentPC(pc);
                            return this.getTarget();
                        } else if (oldConcrete_result && !newConcrete_result) {
                            // Diff true path in src (new true, old false)
                            curCg.select(2);
                            pc._addDet(Comparator.GE, newSym_v1, newSym_v2);
                            pc._addDet(Comparator.LT, oldSym_v1, oldSym_v2);
                            pc.markAsDiffPC(this.getLineNumber(), Diff.diffTrue);
                            curCg.setCurrentPC(pc);
                            return this.getNext(ti); // continue with new version
                        } else {
                            // Diff false path in src (new false, old true)
                            curCg.select(3);
                            pc._addDet(Comparator.LT, newSym_v1, newSym_v2);
                            pc._addDet(Comparator.GE, oldSym_v1, oldSym_v2);
                            pc.markAsDiffPC(this.getLineNumber(), Diff.diffFalse);
                            curCg.setCurrentPC(pc);
                            return this.getTarget(); // continue with new version
                        }
                    } else {
                        // Evaluating the old or new expression in an if(change(boolean,boolean)) stmt
                        if (ti.getExecutionMode() == Execute.OLD) {
                            pc._addDet(oldConcrete_result ? Comparator.LT : Comparator.GE, oldSym_v1, oldSym_v2);
                            curCg.select(oldConcrete_result ? 1 : 0);
                            curCg.setCurrentPC(pc);
                            return oldConcrete_result ? this.getTarget() : this.getNext(ti);
                        } else {
                            pc._addDet(newConcrete_result ? Comparator.LT : Comparator.GE, newSym_v1, newSym_v2);
                            curCg.select(newConcrete_result ? 1 : 0);
                            curCg.setCurrentPC(pc);
                            return newConcrete_result ? this.getTarget() : this.getNext(ti);
                        }
                    }
                } else {
                    /* Full four-way symbolic forking. */
                    int choice = curCg.getNextChoice();
                    switch (choice) {

                    case 0: // True path in src
                        if (ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH) {
                            pc._addDet(Comparator.GE, newSym_v1, newSym_v2);
                        }
                        if (ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH) {
                            if (!pc.isDiffPC()) { // ignore old for diff path
                                pc._addDet(Comparator.GE, oldSym_v1, oldSym_v2);
                            }
                        }
                        if (!pc.simplify()) {
                            // path not feasible
                            ti.getVM().getSystemState().setIgnored(true);
                        } else {
                            curCg.setCurrentPC(pc);
                        }
                        return this.getNext(ti);

                    case 1: // False path in src
                        if (ti.getExecutionMode() == Execute.NEW || ti.getExecutionMode() == Execute.BOTH) {
                            pc._addDet(Comparator.LT, newSym_v1, newSym_v2);
                        }
                        if (ti.getExecutionMode() == Execute.OLD || ti.getExecutionMode() == Execute.BOTH) {
                            if (!pc.isDiffPC()) { // ignore old for diff path
                                pc._addDet(Comparator.LT, oldSym_v1, oldSym_v2);
                            }
                        }
                        if (!pc.simplify()) {
                            // path not feasible
                            ti.getVM().getSystemState().setIgnored(true);
                        } else {
                            curCg.setCurrentPC(pc);
                        }
                        return this.getTarget();

                    case 2: // Diff true path in src (new true, old false)
                        pc._addDet(Comparator.GE, newSym_v1, newSym_v2);
                        pc._addDet(Comparator.LT, oldSym_v1, oldSym_v2);
                        if (!pc.simplify()) {
                            ti.getVM().getSystemState().setIgnored(true);
                        } else {
                            pc.markAsDiffPC(this.getLineNumber(), Diff.diffTrue);
                            curCg.setCurrentPC(pc);
                        }
                        return this.getNext(ti); // continue with new version

                    case 3: // Diff false path in src (new false, old true)
                        pc._addDet(Comparator.LT, newSym_v1, newSym_v2);
                        pc._addDet(Comparator.GE, oldSym_v1, oldSym_v2);
                        if (!pc.simplify()) {
                            ti.getVM().getSystemState().setIgnored(true);
                        } else {
                            pc.markAsDiffPC(this.getLineNumber(), Diff.diffFalse);
                            curCg.setCurrentPC(pc);
                        }
                        return this.getTarget(); // continue with new version

                    default:
                        throw new RuntimeException("Unhandled choice in IF_ICMPLT: " + choice);
                    }
                }
            }
        }
    }
}