/*
 * Copyright (c) 2016. This is a file, part of the GS Hacks project
 *  Everything is provided as it is, without any licence and guarantee
 */
package org.gs.hacks.springbox;

import org.graphstream.stream.SourceBase;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.layout.Layout;
import org.miv.pherd.ParticleBox;
import org.miv.pherd.ParticleBoxListener;
import org.miv.pherd.ntree.*;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Random;

/**
 * An implementation of spring algorithms to layout a graph.
 *
 * <p>
 * This spring method use repulsive forces (electric field) between each nodes
 * and attractive forces (springs) between each node connected by an edge. To
 * speed up the algorithm, a n-tree is used to divide space. A Barnes-Hut like
 * algorithm is used to speed up repulsion force influence when nodes are far
 * away.
 * </p>
 *
 * <p>
 * This algorithm can be configured using several attributes put on the graph :
 * <ul>
 * <li>layout.force : a floating point number (default 0.5f), that allows to
 * define the importance of movement of each node at each computation step. The
 * larger the value the quicker nodes move to their position of lowest energy.
 * However too high values can generate non stable layouts and oscillations.</li>
 * <li>layout.quality : an integer between 0 and 4. With value 0 the layout is
 * faster but it also can be farther from equilibrium. With value 4 the
 * algorithm tries to be as close as possible from equilibrium (the n-tree and
 * Barnes-Hut algorithms are disabled), but the computation can take a lot of
 * time (the algorithm becomes O(n^2)).</li>
 * </ul>
 * You can also put the following attributes on nodes :
 * <ul>
 * <li>layout.weight : The force of repulsion of a node. The larger the value,
 * the more the node repulses its neighbours.</li>
 * </ul>
 * And on edges :
 * <ul>
 * <li>layout.weight : the multiplier for the desired edge length. By default
 * the algorithm tries to make each edge of length one. This is the position of
 * lowest energy for a spring. This coefficient allows to modify this target
 * spring length. Value larger than one will make the edge longer. Values
 * between 0 and 1 will make the edge smaller.</li>
 * <li>layout.stabilization-limit : the stabilisation of a layout is a
 * number between 0 and 1. 1 means fully stable, but this value is rare.
 * Therefore one can consider the layout stable at a lower value. The
 * default is 0.9. You can fix it with this attribute.</li>
 * </ul>
 */
public class SpringBox extends SourceBase implements Layout, ParticleBoxListener {
    // Attributes -- Data

    /**
     * The nodes representation and the n-tree. The particle-box is an
     * implementation of a recursive space decomposition method that is used
     * here to break the O(n^2) complexity into something that is closer to O(n
     * log n).
     */
    protected ParticleBox nodes;

    /**
     * The set of edges.
     */
    protected HashMap<String, EdgeSpring> edges = new HashMap<String, EdgeSpring>();

    /**
     * Used to avoid stabilising if an event occurred.
     */
    protected int lastElementCount = 0;

    /**
     * Random number generator.
     */
    protected Random random;

    /**
     * The lowest node position.
     */
    protected Point3 lo = new Point3(0, 0, 0);

    /**
     * The highest node position.
     */
    protected Point3 hi = new Point3(1, 1, 1);
    /**
     * Output stream for statistics if in debug mode.
     */
    protected PrintStream statsOut;

    /**
     * Energy, and the history of energies.
     */
    protected Energies energies = new Energies();

    // Attributes -- Parameters

    /**
     * The optimal distance between nodes.
     */
    protected double k = 1f;

    /**
     * Default attraction.
     */
    protected double K1 = 0.06f; // 0.3 ??

    /**
     * Default repulsion.
     */
    protected double K2 = 0.024f; // 0.12 ??

    /**
     * Global force strength. This is a factor in [0..1] that is used to scale
     * all computed displacements.
     */
    protected double force = 1f;

    /**
     * The view distance at which the cells of the n-tree are explored
     * exhaustively, after this the poles are used. This is a multiple of k.
     */
    protected double viewZone = 5f;

    /**
     * The Barnes/Hut theta threshold to know if we use a pole or not.
     */
    protected double theta = .7f;

    /**
     * The quality level.
     */
    protected int quality = 1;

    /**
     * Number of nodes per space-cell.
     */
    protected int nodesPerCell = 10;

    // Attributes -- Statistics

    /**
     * Current step.
     */
    protected int time;

    /**
     * The duration of the last step in milliseconds.
     */
    protected long lastStepTime;

    /**
     * The diagonal of the graph area at the current step.
     */
    protected double area = 1;

    /**
     * The maximum length of a node displacement at the current step.
     */
    protected double maxMoveLength;

    /**
     * Average move length.
     */
    protected double avgLength;

    /**
     * Number of nodes that moved during last step.
     */
    protected int nodeMoveCount;

    // Attributes -- Settings

    /**
     * Compute the third coordinate ?.
     */
    protected boolean is3D = false;

    /**
     * Send node informations?.
     */
    protected boolean sendNodeInfos = false;

    /**
     * If true a file is created to output the statistics of the elastic box
     * algorithm.
     */
    protected boolean outputStats = false;

    /**
     * If true a file is created for each node (!!!) and its movement statistics
     * are logged.
     */
    protected boolean outputNodeStats = false;

    /**
     * If greater than one, move events are sent only every N steps.
     */
    protected int sendMoveEventsEvery = 1;

    /**
     * The stabilisation limit of this algorithm.
     */
    protected double stabilizationLimit = 0.9;

    // Constructors

    public SpringBox() {
        this(false);
    }

    public SpringBox(boolean is3D) {
        this(is3D, new Random(System.currentTimeMillis()));
    }

    public SpringBox(boolean is3D, Random randomNumberGenerator) {
        CellSpace space;

        this.is3D = is3D;
        this.random = randomNumberGenerator;

        // checkEnvironment();

        if (is3D)
            space = new OctreeCellSpace(new Anchor(-1, -1, -1), new Anchor(1,
                    1, 1));
        else
            space = new QuadtreeCellSpace(new Anchor(-1, -1, -0.01f),
                    new Anchor(1, 1, 0.01f));

        this.nodes = new ParticleBox(nodesPerCell, space,
                new BarycenterCellData());

        nodes.addParticleBoxListener(this);
        setQuality(quality);

        // System.err.printf(
        // "You are using the SpringBox (sur le retour) layout algorithm !%n" );
    }

    // protected void checkEnvironment()
    // {
    // Environment env = Environment.getGlobalEnvironment();
    //
    // if( env.hasParameter( "Layout.3d" ) )
    // this.is3D = env.getBooleanParameter( "Layout.3d" );
    // }

    // Access

    public Point3 getLowPoint() {
        org.miv.pherd.geom.Point3 p = nodes.getNTree().getLowestPoint();
        lo.set(p.x, p.y, p.z);
        return lo;
    }

    public Point3 getHiPoint() {
        org.miv.pherd.geom.Point3 p = nodes.getNTree().getHighestPoint();
        hi.set(p.x, p.y, p.z);
        return hi;
    }

    public long getLastStepTime() {
        return lastStepTime;
    }

    public String getLayoutAlgorithmName() {
        return "SpringBox's back";
    }

    public int getNodeMovedCount() {
        return nodeMoveCount;
    }

    public double getStabilization() {
        if (lastElementCount == nodes.getParticleCount() + edges.size()) {
            if (time > energies.getBufferSize())
                return energies.getStabilization();
        }

        lastElementCount = nodes.getParticleCount() + edges.size();

        return 0;
    }

    public double getStabilizationLimit() {
        return stabilizationLimit;
    }

    public int getSteps() {
        return time;
    }

    public double getQuality() {
        return quality;
    }

    public double getForce() {
        return force;
    }

    // Commands

    public void setSendNodeInfos(boolean on) {
        sendNodeInfos = on;
    }

    public void setForce(double value) {
        this.force = value;
    }

    public void setStabilizationLimit(double value) {
        this.stabilizationLimit = value;
    }

    public void setQuality(double qualityLevel) {
        quality = (int) qualityLevel;

        switch (quality) {
            case 0:
                viewZone = k;
                break;
            case 1:
                viewZone = 2 * k;
                break;
            case 2:
                viewZone = 5 * k;
                break;
            case 3:
                viewZone = 10 * k;
                break;
            case 4:
                System.err.printf("viewZone = -1%n");
                viewZone = -1;
                break;
            default:
                System.err.printf("invalid quality level %d%n", (int) qualityLevel);
                break;
        }
    }

    public void clear() {
        energies.clearEnergies();
        nodes.removeAllParticles();
        edges.clear();
        nodeMoveCount = 0;
        lastStepTime = 0;
    }

    public void compute() {
        long t1;

        computeArea();

        maxMoveLength = Double.MIN_VALUE;
        k = 1f;
        t1 = System.currentTimeMillis();
        nodeMoveCount = 0;
        avgLength = 0;
        //for( Edge edge : edges.values() ) edge.attraction();
        nodes.step();

        if (nodeMoveCount > 0)
            avgLength /= nodeMoveCount;

        // Ready for the next step.

        getLowPoint();
        getHiPoint();
        energies.storeEnergy();
        printStats();
        time++;
        lastStepTime = System.currentTimeMillis() - t1;
    }

    /**
     * Output some statistics on the layout process. This method is active only
     * if {@link #outputStats} is true.
     */
    protected void printStats() {
        if (outputStats) {
            if (statsOut == null) {
                try {
                    statsOut = new PrintStream("springBox.dat");
                    statsOut.printf("# stabilization nodeMoveCount energy energyDiff maxMoveLength avgLength area%n");
                    statsOut.flush();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            if (statsOut != null) {
                double energyDiff = energies.getEnergy()
                        - energies.getPreviousEnergyValue(30);

                statsOut.printf(Locale.US, "%f %d %f %f %f %f %f%n",
                        getStabilization(), nodeMoveCount,
                        energies.getEnergy(), energyDiff, maxMoveLength,
                        avgLength, area);
                statsOut.flush();
            }
        }
    }

    protected void computeArea() {
        area = getHiPoint().distance(getLowPoint());
    }

    public void shake() {
        energies.clearEnergies();
    }

    // Graph representation

    protected void addNode(String id) {
        nodes.addParticle(new NodeParticle(this, id));
    }

    public void moveNode(String id, double dx, double dy, double dz) {
        NodeParticle node = (NodeParticle) nodes.getParticle(id);

        if (node != null) {
            node.move(dx, dy, dz);
            energies.clearEnergies();
        }
    }

    public void freezeNode(String id, boolean on) {
        NodeParticle node = (NodeParticle) nodes.getParticle(id);

        if (node != null) {
            node.frozen = on;
        }
    }

    protected void setNodeWeight(String id, double weight) {
        NodeParticle node = (NodeParticle) nodes.getParticle(id);

        if (node != null)
            node.setWeight(weight);
    }

    protected void removeNode(String id) {
        NodeParticle node = (NodeParticle) nodes.removeParticle(id);

        if (node != null) {
            node.removeNeighborEdges();
        }
    }

    protected void addEdge(String id, String from, String to) {
        NodeParticle n0 = (NodeParticle) nodes.getParticle(from);
        NodeParticle n1 = (NodeParticle) nodes.getParticle(to);

        if (n0 != null && n1 != null) {
            EdgeSpring e = new EdgeSpring(id, n0, n1);
            EdgeSpring o = edges.put(id, e);

            if (o != null) {
                // throw new SingletonException( "edge '"+id+"' already exists");
                System.err.printf("edge '%s' already exists%n", id);
            } else {
                n0.registerEdge(e);
                n1.registerEdge(e);
            }

            chooseNodePosition(n0, n1);
        }
    }

    protected void chooseNodePosition(NodeParticle n0, NodeParticle n1) {
        if (n0.getEdges().size() == 1 && n1.getEdges().size() > 1) {
            org.miv.pherd.geom.Point3 pos = n1.getPosition();
            n0.move(pos.x, pos.y, pos.z);
        } else if (n1.getEdges().size() == 1 && n0.getEdges().size() > 1) {
            org.miv.pherd.geom.Point3 pos = n0.getPosition();
            n1.move(pos.x, pos.y, pos.z);
        }
    }

    protected void ignoreEdge(String edgeId, boolean on) {
        EdgeSpring edge = edges.get(edgeId);

        if (edge != null) {
            edge.ignored = on;
        }
    }

    protected void setEdgeWeight(String id, double weight) {
        EdgeSpring edge = edges.get(id);

        if (edge != null)
            edge.weight = weight;
    }

    protected void removeEdge(String id) {
        EdgeSpring e = edges.remove(id);

        if (e != null) {
            e.node0.unregisterEdge(e);
            e.node1.unregisterEdge(e);
        }
    }

    public void particleMoved(Object id, double x, double y, double z) {
        if ((time % sendMoveEventsEvery) == 0) {
            Object xyz[] = new Object[3];
            xyz[0] = x;
            xyz[1] = y;
            xyz[2] = z;

            sendNodeAttributeChanged(getLayoutAlgorithmName(), (String) id, "xyz", xyz, xyz);
        }
    }

    public void particleRemoved(Object id) {
    }

    public void stepFinished(int time) {
    }

    public void particleAdded(Object id, double x, double y, double z) {
    }

    public void particleAttributeChanged(Object id, String attribute,
                                         Object newValue, boolean removed) {
    }

    // Output interface

    public void edgeAdded(String graphId, long time, String edgeId,
                          String fromNodeId, String toNodeId, boolean directed) {
        addEdge(edgeId, fromNodeId, toNodeId);
        sendEdgeAdded(graphId, time, edgeId, fromNodeId, toNodeId, directed);
    }

    public void nodeAdded(String graphId, long time, String nodeId) {
        addNode(nodeId);
        sendNodeAdded(graphId, time, nodeId);
    }

    public void edgeRemoved(String graphId, long time, String edgeId) {
        removeEdge(edgeId);
        sendEdgeRemoved(graphId, time, edgeId);
    }

    public void nodeRemoved(String graphId, long time, String nodeId) {
        removeNode(nodeId);
        sendNodeRemoved(graphId, time, nodeId);
    }

    public void graphCleared(String graphId, long time) {
        clear();
        sendGraphCleared(graphId, time);
    }

    public void stepBegins(String graphId, long time, double step) {
        sendStepBegins(graphId, time, step);
    }

    public void graphAttributeAdded(String graphId, long time,
                                    String attribute, Object value) {
        graphAttributeChanged_(attribute, value);
        sendGraphAttributeAdded(graphId, time, attribute, value);
    }

    public void graphAttributeChanged(String graphId, long time,
                                      String attribute, Object oldValue, Object newValue) {
        graphAttributeChanged_(attribute, newValue);
        sendGraphAttributeChanged(graphId, time, attribute, oldValue, newValue);
    }

    protected void graphAttributeChanged_(String attribute, Object newValue) {
        if (attribute.equals("layout.force")) {
            if (newValue instanceof Number)
                setForce(((Number) newValue).doubleValue());

            energies.clearEnergies();
        } else if (attribute.equals("layout.quality")) {
            if (newValue instanceof Number) {
                int q = ((Number) newValue).intValue();

                q = q > 4 ? 4 : q;
                q = q < 0 ? 0 : q;

                setQuality(q);
                System.err.printf("layout.elasticBox.quality: %d%n", q);
            }

            energies.clearEnergies();
        } else if (attribute.equals("layout.exact-zone")) {
            if (newValue instanceof Number) {
                double factor = ((Number) newValue).doubleValue();

                factor = factor > 1 ? 1 : factor;
                factor = factor < 0 ? 0 : factor;

                viewZone = factor;
                System.err.printf("layout.elasticBox.exact-zone: %f of [0..1]%n", viewZone);
                energies.clearEnergies();
            }
        } else if (attribute.equals("layout.output-stats")) {
            outputStats = newValue != null;

            System.err.printf("layout.elasticBox.output-stats: %b%n", outputStats);
        } else if (attribute.equals("layout.stabilization-limit")) {
            if (newValue instanceof Number) {
                stabilizationLimit = ((Number) newValue).doubleValue();
                if (stabilizationLimit > 1) stabilizationLimit = 1;
                else if (stabilizationLimit < 0) stabilizationLimit = 0;

                energies.clearEnergies();
            }
        }
    }

    public void graphAttributeRemoved(String graphId, long time, String attribute) {
        sendGraphAttributeRemoved(graphId, time, attribute);
    }

    public void nodeAttributeAdded(String graphId, long time, String nodeId, String attribute, Object value) {
        nodeAttributeChanged_(nodeId, attribute, value);
        sendNodeAttributeAdded(graphId, time, nodeId, attribute, value);
    }

    public void nodeAttributeChanged(String graphId, long time, String nodeId, String attribute, Object oldValue, Object newValue) {
        nodeAttributeChanged_(nodeId, attribute, newValue);
        sendNodeAttributeChanged(graphId, time, nodeId, attribute, oldValue,
                newValue);
    }

    protected void nodeAttributeChanged_(String nodeId, String attribute, Object newValue) {
        if (attribute.equals("layout.weight")) {
            if (newValue instanceof Number)
                setNodeWeight(nodeId, ((Number) newValue).doubleValue());
            else if (newValue == null)
                setNodeWeight(nodeId, 1);

            energies.clearEnergies();
        }
    }

    public void nodeAttributeRemoved(String graphId, long time, String nodeId, String attribute) {
        sendNodeAttributeRemoved(graphId, time, nodeId, attribute);
    }

    public void edgeAttributeAdded(String graphId, long time, String edgeId, String attribute, Object value) {
        edgeAttributeChanged_(edgeId, attribute, value);
        sendEdgeAttributeAdded(graphId, time, edgeId, attribute, value);
    }

    public void edgeAttributeChanged(String graphId, long time, String edgeId, String attribute, Object oldValue, Object newValue) {
        edgeAttributeChanged_(edgeId, attribute, newValue);
        sendEdgeAttributeChanged(graphId, time, edgeId, attribute, oldValue, newValue);
    }

    protected void edgeAttributeChanged_(String edgeId, String attribute, Object newValue) {
        if (attribute.equals("layout.weight")) {
            if (newValue instanceof Number)
                setEdgeWeight(edgeId, ((Number) newValue).doubleValue());
            else if (newValue == null)
                setEdgeWeight(edgeId, 1);

            energies.clearEnergies();
        } else if (attribute.equals("layout.ignored")) {
            if (newValue instanceof Boolean)
                ignoreEdge(edgeId, (Boolean) newValue);
            energies.clearEnergies();
        }
    }

    public void edgeAttributeRemoved(String graphId, long time, String edgeId, String attribute) {
        sendEdgeRemoved(attribute, time, edgeId);
    }
}