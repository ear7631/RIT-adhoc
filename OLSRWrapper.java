import java.util.HashSet;
import java.util.HashMap;
import java.util.Random;
import java.awt.BasicStroke;
import edu.rit.numeric.plot.Plot;
import edu.rit.numeric.ListXYSeries;


public class OLSRWrapper extends ManetWrapper {

    // seed specifically for the MPR generation
    final long MPR_PRNG_SEED = 1122334455;

    HashMap<Node, Double> tp_timer;

    HashMap<Node, Integer> hello_sent_counter;
    HashMap<Node, Integer> hello_recv_counter;
    HashMap<Node, Integer> tc_sent_counter;
    HashMap<Node, Integer> tc_recv_counter;

    HashSet<Node> mpr_set;

    Random mpr_prng;


    public OLSRWrapper(Manet network) {
        super(network);
        this.tp_timer = new HashMap<Node, Double>();
        this.hello_sent_counter = new HashMap<Node, Integer>();
        this.hello_recv_counter = new HashMap<Node, Integer>();
        this.tc_sent_counter = new HashMap<Node, Integer>();
        this.tc_recv_counter = new HashMap<Node, Integer>();
    
        this.mpr_prng = new Random(this.MPR_PRNG_SEED);
        this.mpr_set = findMPRs(getRandomNode(mpr_prng));

        // Initialize all the counters for metrics
        for (Node node : this.network.getGraph()) {
            hello_sent_counter.put(node, 0);
            hello_recv_counter.put(node, 0);
            tc_sent_counter.put(node, 0);
            tc_recv_counter.put(node, 0);
        }
    }


    public void floodPing() {

    }



    public void floodTopology(Node source) { 
        HashSet<Node> visited = floodTopology(source, new HashSet<Node>());
        System.out.println("Number of nodes visited: " + visited.size());
        return;
    }


    public HashSet<Node> floodTopology(Node source, HashSet<Node> visited) {
        if(visited.contains(source)) {
            return visited;
        }
        visited.add(source);

        // If this is not an MPR, just return and don't propegate.
        if (!this.mpr_set.contains(source)) {
            return visited;
        }

        // N1 layer is just neighbors of source
        HashSet<Node> n_one = source.getNeighbors();
        // N2 layer is layer of N1's neighbors
        HashSet<Node> n_two = new HashSet<Node>();
        
        HashSet<Node> coverage = new HashSet<Node>(visited);

        // If there are no "unseen" nodes from this source, return
        HashSet<Node> unseen = new HashSet<Node>(n_one);
        unseen.removeAll(coverage);
        if (unseen.isEmpty()) {
            return visited;
        }

        HashSet<Node> next_mpr = new HashSet<Node>(this.mpr_set);
        next_mpr.removeAll(visited);
        if (next_mpr.isEmpty()) {
            return visited;
        }

        for (Node mpr : next_mpr) {
            visited = floodTopology(mpr, visited);
        }

        return visited; 
    }


    public HashSet<Node> findMPRs(Node source) {
        return findMPRs(source, new HashSet<Node>());
    }


    // TODO: use counters
    public HashSet<Node> findMPRs(Node source, HashSet<Node> coverage) {

        // N1 layer is just neighbors of source
        HashSet<Node> n_one = source.getNeighbors();
        // N2 layer is the layer of neighbors from the source
        HashSet<Node> n_two = new HashSet<Node>();
        // List of selected nodes for the MPRs on this layer
        HashSet<Node> selectedMPRs = new HashSet<Node>();

        // Add the initial node to the MPR set.
        if(coverage.isEmpty()) {
            selectedMPRs.add(source);
            coverage.add(source);
        }

        // Generate N2
        for (Node n : n_one ) {
            for (Node neighbor : n.getNeighbors()) {
                if (neighbor != source && !n_one.contains(neighbor)) {
                    n_two.add(neighbor);
                }
            }
        }

        // Base Case
        // If there are no "unseen" nodes from this source, return an empty list
        HashSet<Node> unseen = new HashSet<Node>(n_one);
        unseen.addAll(n_two);
        
        unseen.removeAll(coverage);
        if (unseen.isEmpty()) {
            return selectedMPRs;
        }


        coverage.add(source);
        coverage.addAll(n_one);

        // Single-neighbor nodes in N2
        for (Node n : n_one) {
            for (Node neighbor : n.getNeighbors()) {
                if (neighbor == source || n_one.contains(neighbor)) {
                    continue;
                }
                if (neighbor.numNeighbors() == 1) {
                    selectedMPRs.add(n);
                }
            }

            if (selectedMPRs.contains(n)) {
                coverage.addAll(n.getNeighbors());
            }
        }

        while (!coverage.containsAll(n_two)) {

            // Go through all remaining in the N1 layer, find the one with the
            // most amount of uncovered neighbors
            HashSet<Node> remaining = new HashSet<Node>(n_one);
            remaining.removeAll(selectedMPRs);

            Node maximum = null;
            int max_num = 0;

            for (Node n : remaining) {
                if (maximum == null) { maximum = n; }

                HashSet<Node> neighbors_clone = new HashSet<Node>(n.getNeighbors());
                neighbors_clone.removeAll(coverage);

                if (neighbors_clone.size() > max_num) {
                    maximum = n;
                    max_num = neighbors_clone.size();
                }
            }

            selectedMPRs.add(maximum);
            coverage.addAll(maximum.getNeighbors());
        }


        HashSet<Node> retSet = new HashSet<Node>();
        retSet.addAll(selectedMPRs);
        for (Node mpr : selectedMPRs) {
            if (mpr != source) {
                // Add the MPRs that come back from the recursion
                retSet.addAll(findMPRs(mpr, coverage));
            }
        }

        return retSet;
    }


    public void addNodeCallback(Node node) {}
    public void removeNodeCallback(Node node) {}

    public void showMPRs() {
        Plot csclPlot = new Plot();
        csclPlot.plotTitle("Graphical Representation of this MANET's MBRs");
        csclPlot.xAxisStart(-1 * (network.WORLD_LIMIT / 2.0));
        csclPlot.xAxisEnd(1 * (network.WORLD_LIMIT / 2.0));
        csclPlot.yAxisStart(-1 * (network.WORLD_LIMIT / 2.0));
        csclPlot.yAxisEnd(1 * (network.WORLD_LIMIT / 2.0));
        
        ListXYSeries series = new ListXYSeries();

        for (Node mpr : this.mpr_set) {
            series.add(mpr.getX(), mpr.getY());
        }

        csclPlot.seriesStroke(null);
        csclPlot.xySeries(series);
        csclPlot.getFrame().setVisible(true);
    }


    public HashSet<Node> getMPRSet() { return this.mpr_set; }
    public int getManetSize() { return this.network.getGraph().size(); }

}