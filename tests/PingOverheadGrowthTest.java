import java.util.Random;
import java.util.ArrayList;
import edu.rit.numeric.ListSeries;
import edu.rit.numeric.ListXYSeries;
import edu.rit.numeric.plot.Plot;
import edu.rit.numeric.plot.Strokes;
import edu.rit.numeric.Series;
import edu.rit.numeric.Statistics;
import java.awt.Color;

/* TEST 3 - Overhead of a ping in a growing network */
public class PingOverheadGrowthTest {

    public static void main(String args[]) {

        int NU = 0;
        int NL = 0;
        int num_tests = 0;
        int num_trials = 0;
        long seed = 0;

        // Argument Validation
        if (args.length != 5) {
            System.out.println("Usage: java PingOverheadGrowthTest <NL> <NU> <tests> <trials> <seed>");
            System.exit(1);
        }

        try {
            NL = Integer.parseInt(args[0]);
            NU = Integer.parseInt(args[1]);
            num_tests = Integer.parseInt(args[2]);
            num_trials = Integer.parseInt(args[3]);
            
            seed = Long.parseLong(args[4]);
        }
        catch (Exception e) {
            System.out.println("Error - All arguments must be numerical.");
            System.exit(1);
        }

        if (NL < 1) {
            System.out.println("Error - NL must be a positive number.");
            System.exit(1);
        }

        if (NU <= NL) {
            System.out.println("Error - NU must be greater than NL.");
            System.exit(1);
        }


        Random seed_generator = new Random(seed);

        ArrayList< ArrayList<Double> > tora_results = new ArrayList< ArrayList<Double> >(num_tests);
        ArrayList< ArrayList<Double> > olsr_results = new ArrayList< ArrayList<Double> >(num_tests);


        for (int test = 0; test < num_tests; test++) {
            System.out.println("Test " + (test + 1) + "...");

            tora_results.add(test, new ArrayList<Double>());
            olsr_results.add(test, new ArrayList<Double>());

            // make the MANET
            Manet network = new UniformManet(seed_generator.nextLong());
            network.generateNode();

            for (int i = 1; i < NL; i++) {
                network.generateNode();
            }

            // Wrap it with the protocols
            TORAWrapper tora = new TORAWrapper(network, seed_generator.nextLong());
            OLSRWrapper olsr = new OLSRWrapper(network, seed_generator.nextLong());

            for (int i = NL; i <= NU; i++) {
                network.generateNode();

                // GET OVERHEAD HERE
                ListSeries toraBSeries = new ListSeries();
                ListSeries olsrBSeries = new ListSeries();

                for (int trial = 0; trial < num_trials; trial++) {
                    olsr.clearMetrics();
                    tora.clearMetrics();
                    
                    Node source = olsr.getRandomNode();
                    Node destination = olsr.getRandomNode();

                    olsr.ping(source, destination);
                    tora.ping(source, destination);
                    
                    int tora_overhead = tora.getTotalPacketsRecieved();
                    int olsr_overhead = olsr.getTotalPacketsRecieved();

                    toraBSeries.add(tora_overhead);
                    olsrBSeries.add(olsr_overhead);
                }

                Series.Stats toraTrialsStats = toraBSeries.stats();
                Series.Stats olsrTrialsStats = olsrBSeries.stats();

                //System.out.println(toraTrialsStats.mean + " VS " + olsrTrialsStats.mean);

                tora_results.get(test).add(toraTrialsStats.mean);
                olsr_results.get(test).add(olsrTrialsStats.mean);
            }
        }

        ListXYSeries tora_averages = new ListXYSeries();
        ListXYSeries olsr_averages = new ListXYSeries();

        System.out.println("      Overhead Averages         ");
        System.out.println("N Nodes     TORA        OLSR    ");
        

        for (int n = NL; n <= NU; n++ ){
            if(n == 0) {
                tora_averages.add(n, 0);
                olsr_averages.add(n, 0);
                continue;
            }

            ListSeries toraBSeries = new ListSeries();
            ListSeries olsrBSeries = new ListSeries();

            for (int t = 0; t < num_tests; t++) {
                toraBSeries.add(tora_results.get(t).get(n - NL));
                olsrBSeries.add(olsr_results.get(t).get(n - NL));
            }

            Series.Stats toraTestStats = toraBSeries.stats();
            Series.Stats olsrTestStats = olsrBSeries.stats();

            tora_averages.add(n, toraTestStats.mean);
            olsr_averages.add(n, olsrTestStats.mean);

            System.out.printf ("%3d       %7.2f    %7.2f %n", n, toraTestStats.mean, olsrTestStats.mean);
        }

        System.out.println("-----------------------------------------");
        double[] ttest = Statistics.tTestUnequalVariance(tora_averages.ySeries(), olsr_averages.ySeries());
        System.out.printf ("T Value: %.3f    P Value: %.3f %n", ttest[0], ttest[1]);
        
        // Now that we ran through the tests, time to do some stats
        new Plot()
         .plotTitle ("Overhead during Pings (Growth)")
         .xAxisTitle ("Nodes N in Network")
         .yAxisTitle ("Packets Recieved")
         .seriesStroke (Strokes.solid (1))
         .seriesDots (null)
         .seriesColor (Color.RED)
         .xySeries (tora_averages)
         .seriesStroke (Strokes.solid (1))
         .seriesColor (Color.BLUE)
         .xySeries (olsr_averages)
         .getFrame()
         .setVisible (true);
         
    }

}