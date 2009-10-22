/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.core.xml;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.core.NodeImpl;
import org.apache.jackrabbit.core.util.ReferenceChangeTracker;
import org.apache.jackrabbit.core.state.NodeState;
import org.apache.jackrabbit.spi.QPropertyDefinition;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.api.JackrabbitSession;

/**
 * <code>ProtectedPropertyImporter</code> is in charge of importing single
 * properties whith a protected <code>QPropertyDefinition</code>.
 *
 * @see ProtectedNodeImporter for an abstract class used to import protected
 * nodes and the subtree below them.
 */
public interface ProtectedPropertyImporter {

    /**
     * 
     * @param session
     * @param resolver
     * @param isWorkspaceImport
     * @param uuidBehavior
     *@param referenceTracker  @return
     */
    boolean init(JackrabbitSession session, NamePathResolver resolver,
                 boolean isWorkspaceImport, int uuidBehavior,
                 ReferenceChangeTracker referenceTracker);

    /**
     * Handles a single protected property.
     *
     * @param parent The affected parent node.
     * @param protectedPropInfo The <code>PropInfo</code> to be imported.
     * @param def The property definition determined by the importer that
     * calls this method.
     * @return <code>true</code> If the property could be successfully imported;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an error occurs.
     */
    boolean handlePropInfo(NodeImpl parent, PropInfo protectedPropInfo,
                           QPropertyDefinition def)
            throws RepositoryException;

    /**
     * Handles a single protected property.
     *
     * @param parent The affected parent node.
     * @param protectedPropInfo The <code>PropInfo</code> to be imported.
     * @param def The property definition determined by the importer that
     * calls this method.
     * @return <code>true</code> If the property could be successfully imported;
     * <code>false</code> otherwise.
     * @throws RepositoryException If an error occurs.
     */
    boolean handlePropInfo(NodeState parent, PropInfo protectedPropInfo,
                           QPropertyDefinition def)
            throws RepositoryException;


    /**
     * Post processing protected reference properties. This method is called
     * from {@link org.apache.jackrabbit.core.xml.Importer#end()}.
     *
     * @throws RepositoryException If an error occurs.
     */
    void processReferences() throws RepositoryException;

}
