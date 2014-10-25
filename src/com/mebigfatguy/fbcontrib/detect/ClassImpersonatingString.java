/*
 * fb-contrib - Auxiliary detectors for Java programs
 * Copyright (C) 2005-2014 Dave Brosius
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.mebigfatguy.fbcontrib.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.Type;

import com.mebigfatguy.fbcontrib.utils.BugType;
import com.mebigfatguy.fbcontrib.utils.Values;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.CustomUserValue;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * looks for string fields that appear to be built with parsing or calling
 * toString() on another object, or from objects that are fields.
 */
@CustomUserValue
public class ClassImpersonatingString extends BytecodeScanningDetector {

	private static Map<CollectionMethod, int[]> COLLECTION_PARMS = new HashMap<CollectionMethod, int[]>();
	static {
		int[] parm0 = new int[] { Values.ZERO };
		int[] parm0N1 = new int[] { Values.NEGATIVE_ONE, Values.ZERO };
		int[] parm01N1 = new int[] { Values.NEGATIVE_ONE, Values.ZERO, Values.ONE };
		
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "contains", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "add", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "remove", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "set", "(ILjava/lang/Object;)Ljava/lang/Object;"), parm0N1);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "add", "(ILjava/lang/Object;)V"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "indexOf", "(Ljava/lang/Object;)I"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/List", "lastIndexOf", "(Ljava/lang/Object;)I"), parm0);

		COLLECTION_PARMS.put(new CollectionMethod("java/util/Set", "contains", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Set", "add", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Set", "remove", "(Ljava/lang/Object;)Z"), parm0);
		
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "containsKey", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "containsValue", "(Ljava/lang/Object;)Z"), parm0);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;"), parm0N1);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), parm01N1);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), parm01N1);
		COLLECTION_PARMS.put(new CollectionMethod("java/util/Map", "remove", "(Ljava/lang/Object;)Ljava/lang/Object;"), parm0N1);
	}
	
	private static final Set<String> STRING_PARSE_METHODS = new HashSet<String>();
	static {
		STRING_PARSE_METHODS.add("indexOf");
		STRING_PARSE_METHODS.add("lastIndexOf");
		STRING_PARSE_METHODS.add("substring");
		STRING_PARSE_METHODS.add("split");
		STRING_PARSE_METHODS.add("startsWith");
		STRING_PARSE_METHODS.add("endsWith");
	}
	
	private static final String TO_STRING = "toString";
	private static final String FROM_FIELD = "FROM_FIELD";
	
	private BugReporter bugReporter;
	private OpcodeStack stack;
	
	public ClassImpersonatingString(BugReporter reporter) {
		bugReporter = reporter;
	}
	
	@Override
	public void visitClassContext(ClassContext classContext) {
		try {
			stack = new OpcodeStack();
			super.visitClassContext(classContext);
		} finally {
			stack = null;
		}
	}
	
	@Override
	public void visitCode(Code obj) {
		stack.resetForMethodEntry(this);
		super.visitCode(obj);
	}
	
	@Override
	public void sawOpcode(int seen) {
		String userValue = null;
		int[] checkParms = null;
		try {
			stack.precomputation(this);
			switch (seen) {
				case INVOKEVIRTUAL: {
					String clsName = getClassConstantOperand();
					String methodName = getNameConstantOperand();
					String sig = getSigConstantOperand();
					boolean isStringBuilder = "java/lang/StringBuilder".equals(clsName) || "java/lang/StringBuffer".equals(clsName);
					
					if (TO_STRING.equals(methodName) && "()Ljava/lang/String;".equals(sig)) {
						if (isStringBuilder) {
							if (stack.getStackDepth() > 0) {
								OpcodeStack.Item item = stack.getStackItem(0);
								userValue = (String) item.getUserValue();
							}
						} else {
							userValue = TO_STRING;
						}
					} else if (isStringBuilder && "append".equals(methodName)) {
						if (stack.getStackDepth() > 0) {
							OpcodeStack.Item item = stack.getStackItem(0);
							userValue = (String) item.getUserValue();
							if (userValue == null) {
								if (!"Ljava/lang/String;".equals(item.getSignature())) {
									userValue = TO_STRING;
								}
							}
						}
					} else if ("java/lang/String".equals(clsName) && STRING_PARSE_METHODS.contains(methodName)) {
						Type[] parmTypes = Type.getArgumentTypes(sig);
						if (stack.getStackDepth() > parmTypes.length) {
							OpcodeStack.Item item = stack.getStackItem(parmTypes.length);
							if ((item.getXField() != null) || FROM_FIELD.equals(item.getUserValue())) {
								bugReporter.reportBug(new BugInstance(this, BugType.CIS_STRING_PARSING_A_FIELD.name(), NORMAL_PRIORITY)
											.addClass(this)
											.addMethod(this)
											.addSourceLine(this));
							}
						}
					}
				}
				break;
					
				case INVOKEINTERFACE: {
					String clsName = getClassConstantOperand();
					String methodName = getNameConstantOperand();
					String sig = getSigConstantOperand();
					
					Type[] parmTypes = Type.getArgumentTypes(sig);
					if (stack.getStackDepth() > parmTypes.length) {
						CollectionMethod cm = new CollectionMethod(clsName, methodName, sig);
						checkParms = COLLECTION_PARMS.get(cm);
						if (checkParms != null) {
							OpcodeStack.Item item = stack.getStackItem(parmTypes.length);
							if (item.getXField() != null) {
								for (int parm : checkParms) {
									if (parm >= 0) {
										item = stack.getStackItem(parm);
										if (TO_STRING.equals(item.getUserValue())) {
											bugReporter.reportBug(new BugInstance(this, BugType.CIS_TOSTRING_STORED_IN_FIELD.name(), NORMAL_PRIORITY)
														.addClass(this)
														.addMethod(this)
														.addSourceLine(this));
											break;
										}
									}
								}
							} else {
								checkParms = null;
							}
						} else {
							checkParms = null;
						}
					}
				}
				break;
				
				case PUTFIELD:
					if (stack.getStackDepth() > 0) {
						OpcodeStack.Item item = stack.getStackItem(0);
						if ("toString".equals(item.getUserValue())) {
							bugReporter.reportBug(new BugInstance(this, BugType.CIS_TOSTRING_STORED_IN_FIELD.name(), NORMAL_PRIORITY)
										.addClass(this)
										.addMethod(this)
										.addSourceLine(this));
						}
					}
					
			}
		} finally {
			stack.sawOpcode(this, seen);
			if (userValue != null) {
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					item.setUserValue(userValue);
				}
			}
			if ((checkParms != null) && (checkParms[0] == -1)) {
				if (stack.getStackDepth() > 0) {
					OpcodeStack.Item item = stack.getStackItem(0);
					item.setUserValue(FROM_FIELD);
				}
			}
		}
	}
	
	static class CollectionMethod {
		private String clsName;
		private String methodName;
		private String signature;
		
		public CollectionMethod(String clsN, String methodN, String sig) {
			clsName = clsN;
			methodName = methodN;
			signature = sig;
		}
		
		@Override
		public int hashCode() {
			return clsName.hashCode() ^ methodName.hashCode() ^ signature.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof CollectionMethod)) {
				return false;
			}
			
			CollectionMethod that = (CollectionMethod) o;
			return clsName.equals(that.clsName) && methodName.equals(that.methodName) && signature.equals(that.signature); 
		}
		
		@Override
		public String toString() {
			return "CollectionMethod[clsName=" + clsName + ", methodName=" + methodName + ", signature=" + signature + "]";
		}
	}
}