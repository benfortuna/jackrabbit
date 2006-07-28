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

import org.apache.jackrabbit.name.QName;

import java.lang.ref.WeakReference;

/**
 * <code>ChildItemReference</code> implements base functionality for child node
 * and property references.
 * @see ChildNodeReference
 * @see PropertyReference
 */
abstract class ChildItemReference {

    /**
     * Cached weak reference to the target NodeState.
     */
    private WeakReference target;

    /**
     * The parent that owns this <code>ChildItemReference</code>.
     */
    protected final NodeState parent;

    /**
     * The name of the target item state.
     */
    protected final QName name;

    /**
     * Creates a new <code>ChildItemReference</code> with the given parent
     * <code>NodeState</code>.
     *
     * @param parent the <code>NodeState</code> that owns this child node
     *               reference.
     * @param name      the name of the child item.
     */
    public ChildItemReference(NodeState parent, QName name) {
        this.parent = parent;
        this.name = name;
    }

    /**
     * Resolves this <code>ChildItemReference</code> and returns the target
     * <code>ItemState</code> of this reference.
     *
     * @param isf the item state factory responsible for creating node states.
     * @param ism the item state manager to access already created / known
     *            <code>ItemState</code>s.
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    public ItemState resolve(ItemStateFactory isf, ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException {
        // check if cached
        if (target != null) {
            ItemState state = (ItemState) target.get();
            if (state != null) {
                return state;
            }
        }
        // not cached. retrieve and keep weak reference to state
        ItemState state = doResolve(isf, ism);
        target = new WeakReference(state);
        return state;
    }

    /**
     * Returns the parent <code>NodeState</code>. This is the source of this
     * <code>ChildItemReference</code>.
     *
     * @return the parent <code>NodeState</code>.
     */
    public NodeState getParent() {
        return parent;
    }

    /**
     * Resolves this <code>ChildItemReference</code> and returns the target
     * <code>ItemState</code> of this reference.
     *
     * @param isf the item state factory responsible for creating node states.
     * @param ism the item state manager to access already created / known
     *            <code>ItemState</code>s.
     * @return the <code>ItemState</code> where this reference points to.
     * @throws NoSuchItemStateException if the referenced <code>ItemState</code>
     *                                  does not exist.
     * @throws ItemStateException       if an error occurs.
     */
    protected abstract ItemState doResolve(ItemStateFactory isf,
                                           ItemStateManager ism)
            throws NoSuchItemStateException, ItemStateException;
}
