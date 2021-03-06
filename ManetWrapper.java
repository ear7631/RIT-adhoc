import java.util.HashSet;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Random;
import java.awt.geom.Point2D;


/**
 * ManetWrapper - abstract class that provides the basic functionality 
 * 	needed for simulating a routing protocol.
 * 
 * Uses Manet to represent network of nodes
 * 	
 * 
 *
 */
public abstract class ManetWrapper implements ManetListener{

    private Random selector;
    protected Manet network;


    /**
     * Constructor ManetWrapper(network, selector_seed)
     * 
     * @param network - Manet containing network of nodes
     * @param selector_seed - Long number for seeding random
     */
    public ManetWrapper(Manet network, long selector_seed) {
        this.network = network;
        this.network.addListener(this);
        selector = new Random(selector_seed);
    }

    
    /**
     * getRandomNode()
     * 	Generates a random node in the graph that is guaranteed to be 
     * 	connected to another node already in the graph.
     *  
     * @return
     */
    public Node getRandomNode() {
        double x = selector.nextDouble();
        double y = selector.nextDouble();
        x = (x * network.WORLD_LIMIT) - (network.WORLD_LIMIT / 2);
        y = (y * network.WORLD_LIMIT) - (network.WORLD_LIMIT / 2);

        double minimum_distance = Double.MAX_VALUE;
        Node closest_node = null;

        for (Node node : network.getGraph()) {
            double distance = Point2D.distance(x, y, node.getX(), node.getY());
            
            if (distance < minimum_distance) {
                minimum_distance = distance;
                closest_node = node;
            }
        }

        return closest_node;
    }


    /**
     * floodBFS()
     * 	Helper function, used to test that each Node in the network 
     * 	is linked to another node. Also provides how many layers the network
     * 	contains.
     * 
     * @return Integer - number of layers in the BFS
     */
    public int floodBFS() { return floodBFS(false); }
    public int floodBFS(boolean verbose) {

        if (network.getGraph().isEmpty() ) {
            return 0;
        }
        
        Node closest_node = getRandomNode();

        //Internal variables used in BFS
        LinkedList<Node> queue = new LinkedList<Node>();
        HashSet<Node> marked = new HashSet<Node>();
        HashSet<Node> current_layer = new HashSet<Node>();
        HashSet<Node> next_layer = new HashSet<Node>();
        int num_layers = 1;

        if(verbose) {
            System.out.println("Layer: " + num_layers + " --> " + current_layer.size());
        }

        //Pick first node
        queue.offer(closest_node);
        marked.add(closest_node);

        current_layer.add(closest_node);

        //Iterate through all nodes in the network
        while (!queue.isEmpty()) {
            Node visit = queue.poll();

            boolean add_to_next = false;
            if (current_layer.contains(visit)) {
                current_layer.remove(visit);
                add_to_next = true;
            }

            for (Node neighbor : visit.getNeighbors()) {
                if (!marked.contains(neighbor)) {
                    marked.add(neighbor);
                    queue.offer(neighbor);

                    if(add_to_next) {
                        next_layer.add(neighbor);
                    }
                }
            }

            if (current_layer.isEmpty()) {
                current_layer = next_layer;
                next_layer = new HashSet<Node>();

                if (!current_layer.isEmpty()) {
                    num_layers ++;
                    if(verbose) {
                        System.out.println("Layer: " + num_layers + " --> " + current_layer.size());
                    }
                }
            }
        }

        if(verbose) { System.out.println("Number of BFS Layers: " + num_layers); }

        // Sanity check that the BFS covers all nodes
        if( marked.size() != network.getGraph().size() ) {
            System.err.println("Logical Error in Flood BFS: Incorrect Visited.");
            System.err.println("  The number of nodes visited in the BFS is not the size of the graph.");
            System.exit(1);
        }
        return num_layers;
    }

    
    public void show() { this.network.show(); }
    public Iterator<Node> iterator() { return this.network.iterator(); }

    /*
     * Abstract functions that are all implemented by protocol 
     * 	specific wrappers.
     */
    public abstract LinkedList<Node> ping(Node source, Node destination);
    public abstract void addNodeCallback(Node node);
    public abstract void removeNodeCallback(Node node);
    public abstract void clearMetrics();
}