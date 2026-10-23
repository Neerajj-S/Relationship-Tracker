package org.relativesdb.connection.model;

import java.util.List;

public class NetworkData {

    private List<Node> nodes;
    private List<Edge> edges;

    public NetworkData(List<Node> nodes, List<Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<Node> getNodes() { return nodes; }
    public List<Edge> getEdges() { return edges; }

    public static class Node {
        private String id;
        private String label;
        private String group;

        public Node(String id, String label, String group) {
            this.id = id;
            this.label = label;
            this.group = group;
        }

        public String getId() { return id; }
        public String getLabel() { return label; }
        public String getGroup() { return group; }
    }

    public static class Edge {
        private String from;
        private String to;
        private String label;

        public Edge(String from, String to, String label) {
            this.from = from;
            this.to = to;
            this.label = label;
        }

        public String getFrom() { return from; }
        public String getTo() { return to; }
        public String getLabel() { return label; }
    }
}