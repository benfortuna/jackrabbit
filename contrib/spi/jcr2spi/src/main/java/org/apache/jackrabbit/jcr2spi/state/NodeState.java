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
package org.apache.jackrabbit.jcr2spi.state;

import org.apache.commons.collections.MapIterator;
import org.apache.commons.collections.OrderedMapIterator;
import org.apache.commons.collections.map.LinkedMap;
import org.apache.jackrabbit.util.WeakIdentityCollection;
import org.apache.jackrabbit.spi.IdFactory;
import org.apache.jackrabbit.spi.QNodeDefinition;
import org.apache.jackrabbit.name.Path;
import org.apache.jackrabbit.name.QName;
import org.apache.jackrabbit.spi.NodeId;
import org.apache.jackrabbit.spi.ItemId;
import org.apache.jackrabbit.spi.PropertyId;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * <code>NodeState</code> represents the state of a <code>Node</code>.
 */
public class NodeState extends ItemState {

    private static Logger log = LoggerFactory.getLogger(NodeState.class);

    /**
     * the name of this node's primary type
     */
    private QName nodeTypeName;

    /**
     * the names of this node's mixin types
     */
    private QName[] mixinTypeNames = new QName[0];

    /**
     * The id of this node state.
     */
    private NodeId id;

    /**
     * The id of the parent <code>NodeState</code> or <code>null</code> if this
     * instance represents the root node.
     */
    private NodeId parentId;
    
    /**
     * this node's definition
     */
    private QNodeDefinition def;

    /**
     * insertion-ordered collection of ChildNodeEntry objects
     */
    private ChildNodeEntries childNodeEntries = new ChildNodeEntries();

    /**
     * Set to <code>true</code> if {@link #childNodeEntries} are shared between
     * different <code>NodeState</code> instance.
     */
    private boolean sharedChildNodeEntries = false;

    /**
     * set of property names (QName objects)
     */
    private HashSet propertyNames = new HashSet();

    /**
     * Set to <code>true</code> if {@link #propertyNames} is shared between
     * different <code>NodeState</code> instances.
     */
    private boolean sharedPropertyNames = false;

    /**
     * Listeners (weak references)
     */
    private final transient Collection listeners = new WeakIdentityCollection(3);

    // DIFF JR: limit creation of property-ids to the nodeState
    // TODO: check again....
    private final IdFactory idFactory;

    /**
     * Constructs a new <code>NodeState</code> that is initially connected to
     * an overlayed state.
     *
     * @param overlayedState the backing node state being overlayed
     * @param initialStatus  the initial status of the node state object
     * @param isTransient    flag indicating whether this state is transient or not
     */
    public NodeState(NodeState overlayedState, int initialStatus,
                     boolean isTransient) {
        super(overlayedState, initialStatus, isTransient);
        pull();
        idFactory = overlayedState.idFactory;
    }

    /**
     * Constructs a new node state that is not connected.
     *
     * @param id            id of this node
     * @param nodeTypeName  node type of this node
     * @param parentId      id of the parent node
     * @param initialStatus the initial status of the node state object
     * @param isTransient   flag indicating whether this state is transient or not
     */
    public NodeState(NodeId id, QName nodeTypeName, NodeId parentId,
                     int initialStatus, boolean isTransient, IdFactory idFactory) {
        super(initialStatus, isTransient);
        this.id = id;
        this.parentId = parentId;
        this.nodeTypeName = nodeTypeName;
        this.idFactory = idFactory;
    }

    /**
     * {@inheritDoc}
     */
    protected synchronized void copy(ItemState state) {
        synchronized (state) {
            NodeState nodeState = (NodeState) state;
            id = nodeState.id;
            parentId = nodeState.parentId;
            nodeTypeName = nodeState.nodeTypeName;
            mixinTypeNames = nodeState.mixinTypeNames;
            def = nodeState.getDefinition();
            propertyNames = nodeState.propertyNames;
            sharedPropertyNames = true;
            nodeState.sharedPropertyNames = true;
            childNodeEntries = nodeState.childNodeEntries;
            sharedChildNodeEntries = true;
            nodeState.sharedChildNodeEntries = true;
        }
    }

    //-----------------------------------------------------< public methods >---
    /**
     * Determines if this item state represents a node.
     *
     * @return always true
     * @see ItemState#isNode
     */
    public final boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public NodeId getParentId() {
        return parentId;
    }

    /**
     * {@inheritDoc}
     */
    public ItemId getId() {
        return id;
    }

    /**
     * Returns the id of this node state.
     * @return the id of this node state.
     */
    public NodeId getNodeId() {
        return id;
    }

    /**
     * Sets the Id of the parent <code>NodeState</code>.
     *
     * @param parentId the parent <code>NodeState</code>'s Id or <code>null</code>
     * if either this node state should represent the root node or this node
     * state should be 'free floating', i.e. detached from the repository's
     * hierarchy.
     */
    public void setParentId(NodeId parentId) {
        this.parentId = parentId;
    }

    /**
     * Returns the name of this node's node type.
     *
     * @return the name of this node's node type.
     */
    public QName getNodeTypeName() {
        return nodeTypeName;
    }

    /**
     * Returns the names of this node's mixin types.
     *
     * @return a set of the names of this node's mixin types.
     */
    public synchronized QName[] getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * Sets the names of this node's mixin types.
     *
     * @param mixinTypeNames set of names of mixin types
     */
    public synchronized void setMixinTypeNames(QName[] mixinTypeNames) {
        if (mixinTypeNames != null) {
            this.mixinTypeNames = mixinTypeNames;
        } else {
            this.mixinTypeNames = new QName[0];
        }
    }

    /**
     * Return all nodetype names that apply to this <code>NodeState</code>
     * including the primary nodetype and the mixins.
     *
     * @return
     */
    public synchronized QName[] getNodeTypeNames() {
        // mixin types
        QName[] types = new QName[mixinTypeNames.length + 1];
        System.arraycopy(mixinTypeNames, 0, types, 0, mixinTypeNames.length);
        // primary type
        types[types.length - 1] = getNodeTypeName();
        return types;
    }

    /**
     * Returns the id of the definition applicable to this node state.
     *
     * @return the id of the definition
     */
    public QNodeDefinition getDefinition() {
        return def;
    }

    /**
     * Sets the id of the definition applicable to this node state.
     *
     * @param def the definition
     */
    public void setDefinition(QNodeDefinition def) {
        this.def = def;
    }

    /**
     * Determines if there are any child node entries.
     *
     * @return <code>true</code> if there are child node entries,
     *         <code>false</code> otherwise.
     */
    public boolean hasChildNodeEntries() {
        return !childNodeEntries.isEmpty();
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code>.
     *
     * @param name <code>QName</code> object specifying a node name
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name) {
        return !childNodeEntries.get(name).isEmpty();
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>NodeId</code>.
     *
     * @param id the id of the child node
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code>.
     */
    public synchronized boolean hasChildNodeEntry(NodeId id) {
        return childNodeEntries.get(id) != null;
    }

    /**
     * Determines if there is a <code>ChildNodeEntry</code> with the
     * specified <code>name</code> and <code>index</code>.
     *
     * @param name  <code>QName</code> object specifying a node name
     * @param index 1-based index if there are same-name child node entries
     * @return <code>true</code> if there is a <code>ChildNodeEntry</code> with
     *         the specified <code>name</code> and <code>index</code>.
     */
    public synchronized boolean hasChildNodeEntry(QName name, int index) {
        return childNodeEntries.get(name, index) != null;
    }

    /**
     * Determines if there is a property entry with the specified
     * <code>QName</code>.
     *
     * @param propName <code>QName</code> object specifying a property name
     * @return <code>true</code> if there is a property entry with the specified
     *         <code>QName</code>.
     */
    public synchronized boolean hasPropertyName(QName propName) {
        return propertyNames.contains(propName);
    }

    /**
     * Return the id of the Property with the specified name or <code>null</code>
     * if no property exists with the given name.
     * 
     * @param propName
     * @return
     */
    public synchronized PropertyId getPropertyId(QName propName) {
        if (hasPropertyName(propName)) {
            return idFactory.createPropertyId(id, propName);
        } else {
            return null;
        }
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified name and index
     * or <code>null</code> if there's no matching entry.
     *
     * @param nodeName <code>QName</code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return the <code>ChildNodeEntry</code> with the specified name and index
     *         or <code>null</code> if there's no matching entry.
     */
    public synchronized ChildNodeEntry getChildNodeEntry(QName nodeName, int index) {
        return childNodeEntries.get(nodeName, index);
    }

    /**
     * Returns the <code>ChildNodeEntry</code> with the specified <code>NodeId</code> or
     * <code>null</code> if there's no matching entry.
     *
     * @param id the id of the child node
     * @return the <code>ChildNodeEntry</code> with the specified <code>NodeId</code> or
     *         <code>null</code> if there's no matching entry.
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized ChildNodeEntry getChildNodeEntry(NodeId id) {
        return childNodeEntries.get(id);
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code> objects denoting the
     * child nodes of this node.
     *
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized List getChildNodeEntries() {
        return childNodeEntries;
    }

    /**
     * Returns a list of <code>ChildNodeEntry</code>s with the specified name.
     *
     * @param nodeName name of the child node entries that should be returned
     * @return list of <code>ChildNodeEntry</code> objects
     * @see #addChildNodeEntry
     * @see #removeChildNodeEntry
     */
    public synchronized List getChildNodeEntries(QName nodeName) {
        return childNodeEntries.get(nodeName);
    }

    /**
     * Adds a new <code>ChildNodeEntry</code>.
     *
     * @param nodeName <code>QName</code> object specifying the name of the new entry.
     * @param id the id the new entry is refering to.
     * @return the newly added <code>ChildNodeEntry</code>
     */
    public synchronized ChildNodeEntry addChildNodeEntry(QName nodeName,
                                                         NodeId id) {
        if (sharedChildNodeEntries) {
            childNodeEntries = (ChildNodeEntries) childNodeEntries.clone();
            sharedChildNodeEntries = false;
        }
        ChildNodeEntry entry = childNodeEntries.add(nodeName, id);
        notifyNodeAdded(entry);
        return entry;
    }

    /**
     * Renames a new <code>ChildNodeEntry</code>.
     *
     * @param oldName <code>QName</code> object specifying the entry's old name
     * @param index 1-based index if there are same-name child node entries
     * @param newName <code>QName</code> object specifying the entry's new name
     * @return <code>true</code> if the entry was sucessfully renamed;
     *         otherwise <code>false</code>
     */
    public synchronized boolean renameChildNodeEntry(QName oldName, int index,
                                                     QName newName) {
        if (sharedChildNodeEntries) {
            childNodeEntries = (ChildNodeEntries) childNodeEntries.clone();
            sharedChildNodeEntries = false;
        }
        ChildNodeEntry oldEntry = childNodeEntries.remove(oldName, index);
        if (oldEntry != null) {
            ChildNodeEntry newEntry =
                    childNodeEntries.add(newName, oldEntry.getId());
            notifyNodeAdded(newEntry);
            notifyNodeRemoved(oldEntry);
            return true;
        }
        return false;
    }

    /**
     * Removes a <code>ChildNodeEntry</code>.
     *
     * @param nodeName <code>ChildNodeEntry</code> object specifying a node name
     * @param index    1-based index if there are same-name child node entries
     * @return <code>true</code> if the specified child node entry was found
     *         in the list of child node entries and could be removed.
     */
    public synchronized boolean removeChildNodeEntry(QName nodeName, int index) {
        if (sharedChildNodeEntries) {
            childNodeEntries = (ChildNodeEntries) childNodeEntries.clone();
            sharedChildNodeEntries = false;
        }
        ChildNodeEntry entry = childNodeEntries.remove(nodeName, index);
        if (entry != null) {
            notifyNodeRemoved(entry);
        }
        return entry != null;
    }

    /**
     * Removes a <code>ChildNodeEntry</code>.
     *
     * @param id the id of the entry to be removed
     * @return <code>true</code> if the specified child node entry was found
     *         in the list of child node entries and could be removed.
     */
    public synchronized boolean removeChildNodeEntry(NodeId id) {
        if (sharedChildNodeEntries) {
            childNodeEntries = (ChildNodeEntries) childNodeEntries.clone();
            sharedChildNodeEntries = false;
        }
        ChildNodeEntry entry = childNodeEntries.remove(id);
        if (entry != null) {
            notifyNodeRemoved(entry);
        }
        return entry != null;
    }

    /**
     * Removes all <code>ChildNodeEntry</code>s.
     */
    public synchronized void removeAllChildNodeEntries() {
        if (sharedChildNodeEntries) {
            childNodeEntries = (ChildNodeEntries) childNodeEntries.clone();
            sharedChildNodeEntries = false;
        }
        childNodeEntries.removeAll();
    }

    /**
     * Sets the list of <code>ChildNodeEntry</code> objects denoting the
     * child nodes of this node.
     */
    public synchronized void setChildNodeEntries(List nodeEntries) {
        if (nodeEntries instanceof ChildNodeEntries) {
            // optimization
            ChildNodeEntries entries = (ChildNodeEntries) nodeEntries;
            childNodeEntries = (ChildNodeEntries) entries.clone();
            sharedChildNodeEntries = false;
        } else {
            if (sharedChildNodeEntries) {
                childNodeEntries = new ChildNodeEntries();
                sharedChildNodeEntries = false;
            } else {
                childNodeEntries.removeAll();
            }
            childNodeEntries.addAll(nodeEntries);

        }
        notifyNodesReplaced();
    }

    /**
     * Returns the names of this node's properties as a set of
     * <code>QNames</code> objects.
     *
     * @return set of <code>QNames</code> objects
     * @see #addPropertyName
     * @see #removePropertyName
     */
    public synchronized Set getPropertyNames() {
        return Collections.unmodifiableSet(propertyNames);
    }

    /**
     * Adds a property name entry.
     *
     * @param propName <code>QName</code> object specifying the property name
     */
    public synchronized void addPropertyName(QName propName) {
        if (sharedPropertyNames) {
            propertyNames = (HashSet) propertyNames.clone();
            sharedPropertyNames = false;
        }
        propertyNames.add(propName);
    }

    /**
     * Removes a property name entry.
     *
     * @param propName <code>QName</code> object specifying the property name
     * @return <code>true</code> if the specified property name was found
     *         in the list of property name entries and could be removed.
     */
    public synchronized boolean removePropertyName(QName propName) {
        if (sharedPropertyNames) {
            propertyNames = (HashSet) propertyNames.clone();
            sharedPropertyNames = false;
        }
        return propertyNames.remove(propName);
    }

    /**
     * Removes all property name entries.
     */
    public synchronized void removeAllPropertyNames() {
        if (sharedPropertyNames) {
            propertyNames = new HashSet();
            sharedPropertyNames = false;
        } else {
            propertyNames.clear();
        }
    }

    /**
     * Sets the set of <code>QName</code> objects denoting the
     * properties of this node.
     */
    public synchronized void setPropertyNames(Set propNames) {
        if (propNames instanceof HashSet) {
            HashSet names = (HashSet) propNames;
            propertyNames = (HashSet) names.clone();
            sharedPropertyNames = false;
        } else {
            if (sharedPropertyNames) {
                propertyNames = new HashSet();
                sharedPropertyNames = false;
            } else {
                propertyNames.clear();
            }
            propertyNames.addAll(propNames);
        }
    }

    /**
     * Set the node type name. Needed for deserialization and should therefore
     * not change the internal status.
     *
     * @param nodeTypeName node type name
     */
    public synchronized void setNodeTypeName(QName nodeTypeName) {
        this.nodeTypeName = nodeTypeName;
    }

    //---------------------------------------------------------< diff methods >
    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * do not exist in the overlayed node state but have been added to
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been added.
     */
    public synchronized Set getAddedPropertyNames() {
        if (!hasOverlayedState()) {
            return Collections.unmodifiableSet(propertyNames);
        }

        NodeState other = (NodeState) getOverlayedState();
        HashSet set = new HashSet(propertyNames);
        set.removeAll(other.propertyNames);
        return set;
    }

    /**
     * Returns a list of child node entries that do not exist in the overlayed
     * node state but have been added to <i>this</i> node state.
     *
     * @return list of added child node entries
     */
    public synchronized List getAddedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return childNodeEntries;
        }

        NodeState other = (NodeState) getOverlayedState();
        return childNodeEntries.removeAll(other.childNodeEntries);
    }

    /**
     * Returns a set of <code>QName</code>s denoting those properties that
     * exist in the overlayed node state but have been removed from
     * <i>this</i> node state.
     *
     * @return set of <code>QName</code>s denoting the properties that have
     *         been removed.
     */
    public synchronized Set getRemovedPropertyNames() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_SET;
        }

        NodeState other = (NodeState) getOverlayedState();
        HashSet set = new HashSet(other.propertyNames);
        set.removeAll(propertyNames);
        return set;
    }

    /**
     * Returns a list of child node entries, that exist in the overlayed node state
     * but have been removed from <i>this</i> node state.
     *
     * @return list of removed child node entries
     */
    public synchronized List getRemovedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        NodeState other = (NodeState) getOverlayedState();
        return other.childNodeEntries.removeAll(childNodeEntries);
    }

    /**
     * Returns a list of child node entries that exist both in <i>this</i> node
     * state and in the overlayed node state but have been reordered.
     * <p/>
     * The list may include only the minimal set of nodes that have been
     * reordered. That is, even though a certain number of nodes have changed
     * their absolute position the list may include less that this number of
     * nodes.
     * <p/>
     * Example:<br/>
     * Initial state:
     * <pre>
     *  + node1
     *  + node2
     *  + node3
     * </pre>
     * After reorder:
     * <pre>
     *  + node2
     *  + node3
     *  + node1
     * </pre>
     * All nodes have changed their absolute position. The returned list however
     * may only return that <code>node1</code> has been reordered (from the
     * first position to the end).
     *
     * @return list of reordered child node enties.
     */
    public synchronized List getReorderedChildNodeEntries() {
        if (!hasOverlayedState()) {
            return Collections.EMPTY_LIST;
        }

        ChildNodeEntries otherChildNodeEntries =
                ((NodeState) overlayedState).childNodeEntries;

        if (childNodeEntries.isEmpty()
                || otherChildNodeEntries.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        // build intersections of both collections,
        // each preserving their relative order
        List ours = childNodeEntries.retainAll(otherChildNodeEntries);
        List others = otherChildNodeEntries.retainAll(childNodeEntries);

        // do a lazy init
        List reordered = null;
        // both entry lists now contain the set of nodes that have not
        // been removed or added, but they may have changed their position.
        for (int i = 0; i < ours.size();) {
            ChildNodeEntry entry = (ChildNodeEntry) ours.get(i);
            ChildNodeEntry other = (ChildNodeEntry) others.get(i);
            if (entry == other || entry.getId().equals(other.getId())) {
                // no reorder, move to next child entry
                i++;
            } else {
                // reordered entry detected
                if (reordered == null) {
                    reordered = new ArrayList();
                }
                // Note that this check will not necessarily find the
                // minimal reorder operations required to convert the overlayed
                // child node entries into the current.

                // is there a next entry?
                if (i + 1 < ours.size()) {
                    // if entry is the next in the other list then probably
                    // the other entry at position <code>i</code> was reordered
                    if (entry.getId().equals(((ChildNodeEntry) others.get(i + 1)).getId())) {
                        // scan for the uuid of the other entry in our list
                        for (int j = i; j < ours.size(); j++) {
                            if (((ChildNodeEntry) ours.get(j)).getId().equals(other.getId())) {
                                // found it
                                entry = (ChildNodeEntry) ours.get(j);
                                break;
                            }
                        }
                    }
                }

                reordered.add(entry);
                // remove the entry from both lists
                // entries > i are already cleaned
                for (int j = i; j < ours.size(); j++) {
                    if (((ChildNodeEntry) ours.get(j)).getId().equals(entry.getId())) {
                        ours.remove(j);
                    }
                }
                for (int j = i; j < ours.size(); j++) {
                    if (((ChildNodeEntry) others.get(j)).getId().equals(entry.getId())) {
                        others.remove(j);
                    }
                }
                // if a reorder has been detected index <code>i</code> is not
                // incremented because entries will be shifted when the
                // reordered entry is removed.
            }
        }
        if (reordered == null) {
            return Collections.EMPTY_LIST;
        } else {
            return reordered;
        }
    }

    //--------------------------------------------------< ItemState overrides >
    /**
     * {@inheritDoc}
     * <p/>
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we add it to our list of specialized listeners.
     */
    public void addListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                if (listeners.contains(listener)) {
                    log.debug("listener already registered: " + listener);
                    // no need to add to call ItemState.addListener()
                    return;
                } else {
                    listeners.add(listener);
                }
            }
        }
        super.addListener(listener);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * If the listener passed is at the same time a <code>NodeStateListener</code>
     * we remove it from our list of specialized listeners.
     */
    public void removeListener(ItemStateListener listener) {
        if (listener instanceof NodeStateListener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
        super.removeListener(listener);
    }

    /**
     * TODO: find a better way to provide the index of a child node entry
     * Returns the index of the given <code>ChildNodeEntry</code> and with
     * <code>name</code>.
     *
     * @param name the name of the child node.
     * @param cne  the <code>ChildNodeEntry</code> instance.
     * @return the index of the child node entry or <code>0</code> if it is not
     *         found in this <code>NodeState</code>.
     */
    int getChildNodeIndex(QName name, ChildNodeEntry cne) {
        List sns = childNodeEntries.get(name);
        return sns.indexOf(cne) + 1;
    }

    //-------------------------------------------------< misc. helper methods >
    /**
     * Notify the listeners that a child node entry has been added
     */
    protected void notifyNodeAdded(ChildNodeEntry added) {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeAdded(this, added.getName(), added.getIndex(), added.getId());
                }
            }
        }
    }

    /**
     * Notify the listeners that the child node entries have been replaced
     */
    protected void notifyNodesReplaced() {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodesReplaced(this);
                }
            }
        }
    }

    /**
     * Notify the listeners that a child node entry has been removed
     */
    protected void notifyNodeRemoved(ChildNodeEntry removed) {
        synchronized (listeners) {
            Iterator iter = listeners.iterator();
            while (iter.hasNext()) {
                NodeStateListener l = (NodeStateListener) iter.next();
                if (l != null) {
                    l.nodeRemoved(this, removed.getName(),
                            removed.getIndex(), removed.getId());
                }
            }
        }
    }

    //--------------------------------------------------------< inner classes >
    /**
     * <code>ChildNodeEntries</code> represents an insertion-ordered
     * collection of <code>ChildNodeEntry</code>s that also maintains
     * the index values of same-name siblings on insertion and removal.
     * <p/>
     * <code>ChildNodeEntries</code> also provides an unmodifiable
     * <code>List</code> view.
     */
    private class ChildNodeEntries implements List, Cloneable {

        // insertion-ordered map of entries (key=NodeId, value=entry)
        private LinkedMap entries;
        // map used for lookup by name
        // (key=name, value=either a single entry or a list of sns entries)
        private HashMap nameMap;

        ChildNodeEntries() {
            entries = new LinkedMap();
            nameMap = new HashMap();
        }

        ChildNodeEntry get(NodeId id) {
            return (ChildNodeEntry) entries.get(id);
        }

        List get(QName nodeName) {
            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return Collections.EMPTY_LIST;
            }
            if (obj instanceof ArrayList) {
                // map entry is a list of siblings
                return Collections.unmodifiableList((ArrayList) obj);
            } else {
                // map entry is a single child node entry
                return Collections.singletonList(obj);
            }
        }

        ChildNodeEntry get(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }
            if (obj instanceof ArrayList) {
                // map entry is a list of siblings
                ArrayList siblings = (ArrayList) obj;
                if (index <= siblings.size()) {
                    return (ChildNodeEntry) siblings.get(index - 1);
                }
            } else {
                // map entry is a single child node entry
                if (index == Path.INDEX_DEFAULT) {
                    return (ChildNodeEntry) obj;
                }
            }
            return null;
        }

        ChildNodeEntry add(QName nodeName, NodeId id) {
            List siblings = null;
            Object obj = nameMap.get(nodeName);
            if (obj != null) {
                if (obj instanceof ArrayList) {
                    // map entry is a list of siblings
                    siblings = (ArrayList) obj;
                } else {
                    // map entry is a single child node entry,
                    // convert to siblings list
                    siblings = new ArrayList();
                    siblings.add(obj);
                    nameMap.put(nodeName, siblings);
                }
            }

            ChildNodeEntry entry = createChildNodeEntry(nodeName, id);
            if (siblings != null) {
                siblings.add(entry);
            } else {
                nameMap.put(nodeName, entry);
            }
            entries.put(id, entry);

            return entry;
        }

        void addAll(List entriesList) {
            Iterator iter = entriesList.iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                // delegate to add(QName, String) to maintain consistency
                add(entry.getName(), entry.getId());
            }
        }

        public ChildNodeEntry remove(QName nodeName, int index) {
            if (index < Path.INDEX_DEFAULT) {
                throw new IllegalArgumentException("index is 1-based");
            }

            Object obj = nameMap.get(nodeName);
            if (obj == null) {
                return null;
            }

            if (obj instanceof ChildNodeEntry) {
                // map entry is a single child node entry
                if (index != Path.INDEX_DEFAULT) {
                    return null;
                }
                ChildNodeEntry removedEntry = (ChildNodeEntry) obj;
                nameMap.remove(nodeName);
                entries.remove(removedEntry.getId());
                return removedEntry;
            }

            // map entry is a list of siblings
            List siblings = (ArrayList) obj;
            if (index > siblings.size()) {
                return null;
            }

            // remove from siblings list
            ChildNodeEntry removedEntry = (ChildNodeEntry) siblings.remove(index - 1);
            // remove from ordered entries map
            entries.remove(removedEntry.getId());

            // clean up name lookup map if necessary
            if (siblings.size() == 0) {
                // no more entries with that name left:
                // remove from name lookup map as well
                nameMap.remove(nodeName);
            } else if (siblings.size() == 1) {
                // just one entry with that name left:
                // discard siblings list and update name lookup map accordingly
                nameMap.put(nodeName, siblings.get(0));
            }

            // we're done
            return removedEntry;
        }

        /**
         * Removes the child node entry refering to the node with the given id.
         *
         * @param id id of node whose entry is to be removed.
         * @return the removed entry or <code>null</code> if there is no such entry.
         */
        ChildNodeEntry remove(NodeId id) {
            ChildNodeEntry entry = (ChildNodeEntry) entries.get(id);
            if (entry != null) {
                return remove(entry.getName(), entry.getIndex());
            }
            return entry;
        }

        /**
         * Removes the given child node entry.
         *
         * @param entry entry to be removed.
         * @return the removed entry or <code>null</code> if there is no such entry.
         */
        public ChildNodeEntry remove(ChildNodeEntry entry) {
            return remove(entry.getName(), entry.getIndex());
        }

        /**
         * Removes all child node entries
         */
        public void removeAll() {
            nameMap.clear();
            entries.clear();
        }

        /**
         * Returns a list of <code>ChildNodeEntry</code>s who do only exist in
         * <code>this</code> but not in <code>other</code>.
         * <p/>
         * Note that two entries are considered identical in this context if
         * they have the same name and uuid, i.e. the index is disregarded
         * whereas <code>ChildNodeEntry.equals(Object)</code> also compares
         * the index.
         *
         * @param other entries to be removed
         * @return a new list of those entries that do only exist in
         *         <code>this</code> but not in <code>other</code>
         */
        List removeAll(ChildNodeEntries other) {
            if (entries.isEmpty()) {
                return Collections.EMPTY_LIST;
            }
            if (other.isEmpty()) {
                return this;
            }

            List result = new ArrayList();
            Iterator iter = iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                ChildNodeEntry otherEntry = other.get(entry.getId());
                if (entry == otherEntry) {
                    continue;
                }
                if (otherEntry == null || !entry.getName().equals(otherEntry.getName())) {
                    result.add(entry);
                }
            }

            return result;
        }

        /**
         * Returns a list of <code>ChildNodeEntry</code>s who do exist in
         * <code>this</code> <i>and</i> in <code>other</code>.
         * <p/>
         * Note that two entries are considered identical in this context if
         * they have the same name and uuid, i.e. the index is disregarded
         * whereas <code>ChildNodeEntry.equals(Object)</code> also compares
         * the index.
         *
         * @param other entries to be retained
         * @return a new list of those entries that do exist in
         *         <code>this</code> <i>and</i> in <code>other</code>
         */
        List retainAll(ChildNodeEntries other) {
            if (entries.isEmpty()
                    || other.isEmpty()) {
                return Collections.EMPTY_LIST;
            }

            List result = new ArrayList();
            Iterator iter = iterator();
            while (iter.hasNext()) {
                ChildNodeEntry entry = (ChildNodeEntry) iter.next();
                ChildNodeEntry otherEntry = other.get(entry.getId());
                if (entry == otherEntry) {
                    result.add(entry);
                } else if (otherEntry != null
                        && entry.getName().equals(otherEntry.getName())) {
                    result.add(entry);
                }
            }

            return result;
        }

        /**
         * Creates a <code>ChildNodeEntry</code> instance based on
         * <code>nodeName</code>, <code>id</code> and <code>index</code>.
         *
         * @param nodeName the name of the child node.
         * @param id the id of the child node.
         * @return
         */
        private ChildNodeEntry createChildNodeEntry(QName nodeName, NodeId id) {
            if (id.getRelativePath() != null) {
                return new PathElementReference(NodeState.this, nodeName, NodeState.this.idFactory);
            } else {
                return new UUIDReference(NodeState.this, id, nodeName);
            }
        }

        //-------------------------------------------< unmodifiable List view >
        public boolean contains(Object o) {
            if (o instanceof ChildNodeEntry) {
                return entries.containsKey(((ChildNodeEntry) o).getId());
            } else {
                return false;
            }
        }

        public boolean containsAll(Collection c) {
            Iterator iter = c.iterator();
            while (iter.hasNext()) {
                if (!contains(iter.next())) {
                    return false;
                }
            }
            return true;
        }

        public Object get(int index) {
            return entries.getValue(index);
        }

        public int indexOf(Object o) {
            if (o instanceof ChildNodeEntry) {
                return entries.indexOf(((ChildNodeEntry) o).getId());
            } else {
                return -1;
            }
        }

        public boolean isEmpty() {
            return entries.isEmpty();
        }

        public int lastIndexOf(Object o) {
            // entries are unique
            return indexOf(o);
        }

        public Iterator iterator() {
            return new EntriesIterator();
        }

        public ListIterator listIterator() {
            return new EntriesIterator();
        }

        public ListIterator listIterator(int index) {
            if (index < 0 || index >= entries.size()) {
                throw new IndexOutOfBoundsException();
            }
            ListIterator iter = new EntriesIterator();
            while (index-- > 0) {
                iter.next();
            }
            return iter;
        }

        public int size() {
            return entries.size();
        }

        public List subList(int fromIndex, int toIndex) {
            // @todo FIXME does not fulfil the contract of List.subList(int,int)
            return Collections.unmodifiableList(new ArrayList(this).subList(fromIndex, toIndex));
        }

        public Object[] toArray() {
            ChildNodeEntry[] array = new ChildNodeEntry[size()];
            return toArray(array);
        }

        public Object[] toArray(Object[] a) {
            if (!a.getClass().getComponentType().isAssignableFrom(ChildNodeEntry.class)) {
                throw new ArrayStoreException();
            }
            if (a.length < size()) {
                a = new ChildNodeEntry[size()];
            }
            MapIterator iter = entries.mapIterator();
            int i = 0;
            while (iter.hasNext()) {
                iter.next();
                a[i] = entries.getValue(i);
                i++;
            }
            while (i < a.length) {
                a[i++] = null;
            }
            return a;
        }

        public void add(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        public boolean add(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(int index, Collection c) {
            throw new UnsupportedOperationException();
        }

        public void clear() {
            throw new UnsupportedOperationException();
        }

        public Object remove(int index) {
            throw new UnsupportedOperationException();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public boolean retainAll(Collection c) {
            throw new UnsupportedOperationException();
        }

        public Object set(int index, Object element) {
            throw new UnsupportedOperationException();
        }

        //------------------------------------------------< Cloneable support >
        /**
         * Returns a shallow copy of this <code>ChildNodeEntries</code> instance;
         * the entries themselves are not cloned.
         *
         * @return a shallow copy of this instance.
         */
        protected Object clone() {
            ChildNodeEntries clone = new ChildNodeEntries();
            clone.entries = (LinkedMap) entries.clone();
            clone.nameMap = new HashMap(nameMap.size());
            for (Iterator it = nameMap.keySet().iterator(); it.hasNext();) {
                Object key = it.next();
                Object obj = nameMap.get(key);
                if (obj instanceof ArrayList) {
                    // clone List
                    obj = ((ArrayList) obj).clone();
                }
                clone.nameMap.put(key, obj);
            }
            return clone;
        }

        //----------------------------------------------------< inner classes >
        class EntriesIterator implements ListIterator {

            private final OrderedMapIterator mapIter;

            EntriesIterator() {
                mapIter = entries.orderedMapIterator();
            }

            public boolean hasNext() {
                return mapIter.hasNext();
            }

            public Object next() {
                mapIter.next();
                return mapIter.getValue();
            }

            public boolean hasPrevious() {
                return mapIter.hasPrevious();
            }

            public int nextIndex() {
                return entries.indexOf(mapIter.getKey()) + 1;
            }

            public Object previous() {
                mapIter.previous();
                return mapIter.getValue();
            }

            public int previousIndex() {
                return entries.indexOf(mapIter.getKey()) - 1;
            }

            public void add(Object o) {
                throw new UnsupportedOperationException();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

            public void set(Object o) {
                throw new UnsupportedOperationException();
            }
        }
    }

}
