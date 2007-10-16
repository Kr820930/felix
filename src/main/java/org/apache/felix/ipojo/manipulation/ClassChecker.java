/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.manipulation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Check that a POJO is already manipulated or not.
 * Moreover it allows to get manipulation data about this class. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ClassChecker extends EmptyVisitor implements ClassVisitor, Opcodes {

    /**
     * True if the class is already manipulated.
     */
    private boolean m_isAlreadyManipulated = false;

    /**
     * Interfaces implemented by the component.
     */
    private List m_itfs = new ArrayList();

    /**
     * Field map [field name, type] discovered in the component class.
     */
    private Map m_fields = new HashMap();

    /**
     * Method List of method descriptor discovered in the component class.
     */
    private List m_methods = new ArrayList()/* <MethodDesciptor> */;
    
    /**
     * Super class if not java.lang.Object.
     */
    private String m_superClass;

    /**
     * Check if the _cm field already exists.
     * Update the field list.
     * @param access : access of the field
     * @param name : name of the field
     * @param desc : description of the field
     * @param signature : signature of the field
     * @param value : value of the field (for static field only)
     * @return the field visitor
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    public FieldVisitor visitField(int access, String name, String desc,
            String signature, Object value) {

        if (access == ACC_PRIVATE && name.equals("_cm")
                && desc.equals("Lorg/apache/felix/ipojo/InstanceManager;")) {
            m_isAlreadyManipulated = true;
        }

        Type type = Type.getType(desc);
        if (type.getSort() == Type.ARRAY) {
            if (type.getInternalName().startsWith("L")) {
                String internalType = type.getInternalName().substring(1);
                String nameType = internalType.replace('/', '.');
                m_fields.put(name, nameType + "[]");
            } else {
                String nameType = type.getClassName().substring(0,
                        type.getClassName().length() - 2);
                m_fields.put(name, nameType + "[]");
            }
        } else {
            m_fields.put(name, type.getClassName());
        }

        return null;
    }

    /**
     * Check if the class was already manipulated.
     * @return true if the class is already manipulated.
     */
    public boolean isalreadyManipulated() {
        return m_isAlreadyManipulated;
    }

    /**
     * Visit the class.
     * Update the implemented interface list.
     * @param version : version of the class
     * @param access : access of the class
     * @param name : name of the class
     * @param signature : signature of the class
     * @param superName : super class of the class
     * @param interfaces : implemented interfaces.
     * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        
        if (! superName.equals("java/lang/Object")) {
            m_superClass = superName.replace('/', '.');
        }
        
        for (int i = 0; i < interfaces.length; i++) {
            if (! interfaces[i].equals("org/apache/felix/ipojo/Pojo")) {
                m_itfs.add(interfaces[i].replace('/', '.'));
            }
        }
    }

    /**
     * Visit a method.
     * Update the method list (except if it init or clinit.
     * @param  access - the method's access flags (see Opcodes). This parameter also indicates if the method is synthetic and/or deprecated.
     * @param name - the method's name.
     * @param desc - the method's descriptor (see Type).
     * @param signature - the method's signature. May be null if the method parameters, return type and exceptions do not use generic types.
     * @param exceptions - the internal names of the method's exception classes (see getInternalName). May be null.
     * @return nothing.
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc,
            String signature, String[] exceptions) {
        if (!name.equals("<clinit>")) { 
            
            if (name.equals("<init>")) {
                m_methods.add(new MethodDescriptor("$init", desc));
            } else {
                // Avoid constructors.
                if (!(name.startsWith("_get") || // Avoid getter method
                        name.startsWith("_set") || // Avoid setter method
                        name.equals("_setComponentManager") || // Avoid the set method
                        name.equals("getComponentInstance"))) { // Avoid the getComponentInstance method
                    m_methods.add(new MethodDescriptor(name, desc));
                }
            }
            
        }
        return null;
    }
    
    /**
     * Get collected interfaces.
     * @return the interfaces implemented by the component class.
     */
    public List getInterfaces() {
        return m_itfs;
    }

    /**
     * Get collected fields.
     * @return the field map [field_name, type].
     */
    public Map getFields() {
        return m_fields;
    }

    /**
     * Get collected methods.
     * @return the method list of [method, signature].
     */
    public List getMethods() {
        return m_methods;
    }
    
    public String getSuperClass() {
        return m_superClass;
    }

}
