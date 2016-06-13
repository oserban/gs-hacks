/*
 * Copyright (c) 2016. This is a file, part of the GS Hacks project
 *  Everything is provided as it is, without any licence and guarantee
 */
package org.gs.hacks.springbox;

/**
 * Edge representation.
 */
public class EdgeSpring {
    /**
     * The edge identifier.
     */
    public String id;

    /**
     * Source node.
     */
    public NodeParticle node0;

    /**
     * Target node.
     */
    public NodeParticle node1;

    /**
     * Edge weight.
     */
    public double weight = 1f;

    /**
     * Make this edge ignored by the layout algorithm ?.
     */
    public boolean ignored = false;

    /**
     * New edge between two given nodes.
     *
     * @param id The edge identifier.
     * @param n0 The first node.
     * @param n1 The second node.
     */
    public EdgeSpring(String id, NodeParticle n0, NodeParticle n1) {
        this.id = id;
        this.node0 = n0;
        this.node1 = n1;
    }

    /**
     * Considering the two nodes of the edge, return the one that was not given
     * as argument.
     *
     * @param node One of the nodes of the edge.
     * @return The other node.
     */
    public NodeParticle getOpposite(NodeParticle node) {
        if (node0 == node)
            return node1;

        return node0;
    }
}