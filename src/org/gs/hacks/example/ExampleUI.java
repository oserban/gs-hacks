/*
 * Copyright (c) 2016. This is a file, part of the GS Hacks project
 *  Everything is provided as it is, without any licence and guarantee
 */

package org.gs.hacks.example;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swingViewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.gs.hacks.ControllableView;
import org.gs.hacks.CustomMouseObserver;
import org.gs.hacks.springbox.SpringBox;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Ovidiu Serban, ovidiu@roboslang.org
 * @version 1, 1/10/13
 */
public class ExampleUI extends JFrame implements ActionListener {
    private JPanel viewPanel;

    private Graph graph;
    private ViewPanel view;

    public ExampleUI() {
        super("Example graph");
        initComponents();
        initLayout();
        initActions();
        initGraph();
    }

    private void initComponents() {
        System.setProperty("org.graphstream.ui.renderer", "org.graphstream.ui.j2dviewer.J2DGraphRenderer");

        setSize(new Dimension(800, 600));
        setPreferredSize(new Dimension(800, 600));

        viewPanel = new JPanel();
        graph = new SingleGraph("noID");
        graph.setAutoCreate(true);

        Viewer viewer = new Viewer(graph, Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
        System.err.println("Cannot activate DOT Layout, using auto layouts instead !");
        viewer.enableAutoLayout(new SpringBox());
        view = new ControllableView(viewer);
    }

    private void initLayout() {
        setLayout(new BorderLayout());
        add(viewPanel, BorderLayout.CENTER);
        viewPanel.setLayout(new BorderLayout());
        viewPanel.add(view, BorderLayout.CENTER);
    }

    private void initActions() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        new CustomMouseObserver(view);
    }

    private void initGraph() {
        graph.clear();

        graph.addAttribute("ui.stylesheet",
                           "graph { padding: 50px; } " +
                                   "node { size-mode: fit; shape: rounded-box; fill-color: lightgray; stroke-mode: plain; padding: 10px; text-size: 20;}" +
                                   "edge { shape: angle; size: 15px; arrow-shape: arrow; arrow-size: 20px, 6px; fill-color: #36454F; }");
    }

    public void actionPerformed(ActionEvent e) {
        // implement this if needed
    }

    private void createRandomGraph() {
        graph.addNode("node1");
        graph.addNode("node2");
        graph.addNode("node3");

        graph.addEdge("node1-node2", "node1", "node2");
        graph.addEdge("node2-node3", "node1", "node3");
    }

    public static void main(String[] args) {
        ExampleUI exampleUI = new ExampleUI();
        exampleUI.createRandomGraph();
        exampleUI.setVisible(true);
    }
}