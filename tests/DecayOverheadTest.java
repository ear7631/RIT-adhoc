import java.util.Random;
import java.util.ArrayList;
import edu.rit.numeric.ListSeries;
import edu.rit.numeric.ListXYSeries;
import edu.rit.numeric.plot.Plot;
import edu.rit.numeric.plot.Strokes;
import edu.rit.numeric.Series;
import edu.rit.numeric.Statistics;
import java.awt.Color;


/* TEST 2 - Overhead of a decaying network */
public class DecayOverheadTest {

    public static void main(String args[]) {

        int NL = 0;
        int NU = 0;
        int num_tests = 0;
        long seed = 0;

        if (args.length != 4) {
            System.out.println("Usage: java DecayOverheadTest <NU> <NL> <tests> <seed>");
            System.exit(1);
        }

        try {
            NU = Integer.parseInt(args[0]);
            NL = Integer.parseInt(args[1]);
            num_tests = Integer.parseInt(args[2]);
            seed = Long.parseLong(args[3]);
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

        ArrayList< ArrayList<Integer> > tora_results = new ArrayList< ArrayList<Integer> >(num_tests);
        ArrayList< ArrayList<Integer> > olsr_results = new ArrayList< ArrayList<Integer> >(num_tests);


        for (int test = 0; test < num_tests; test++) {
            System.out.println("Test " + (test + 1) + "...");

            tora_results.add(test, new ArrayList<Integer>(NU - NL));
            olsr_results.add(test, new ArrayList<Integer>(NU - NL));

            ArrayList<Integer> tora_test_results = tora_results.get(test);
            ArrayList<Integer> olsr_test_results = olsr_results.get(test);


            // make the MANET
            Manet network = new UniformManet(seed_generator.nextLong());
            network.generateNode();

            for (int i = NL; i <= NU; i++) {
                network.generateNode();
            }

            // Wrap it with the protocols
            TORAWrapper tora = new TORAWrapper(network, seed_generator.nextLong());
            OLSRWrapper olsr = new OLSRWrapper(network, seed_generator.nextLong());

            for (int i = NU; i >= NL; i--) {
                network.removeLastNode();

                // GET OVERHEAD HERE
                int tora_overhead = tora.getTotalPacketsRecieved();
                int olsr_overhead = olsr.getTotalPacketsRecieved();

                tora_test_results.add(tora_overhead);
                olsr_test_results.add(olsr_overhead);
            }
        }

        // Now that we ran through the tests, time to do some stats
        ListXYSeries tora_averages = new ListXYSeries();
        ListXYSeries olsr_averages = new ListXYSeries();

        System.out.println("      Overhead Averages         ");
        System.out.println("N Nodes     TORA        OLSR    ");


        int i = 0;
        for (int n = NU; n >= NL; n--) {
            double n_tora_average = 0.0;
            double n_olsr_average = 0.0;

            for (int j = 0; j < num_tests; j++) {
                n_tora_average = n_tora_average + tora_results.get(j).get(i);
                n_olsr_average = n_olsr_average + olsr_results.get(j).get(i);
            }
            n_tora_average = n_tora_average / num_tests;
            n_olsr_average = n_olsr_average / num_tests;

            tora_averages.add(n, n_tora_average);
            olsr_averages.add(n, n_olsr_average);

            System.out.printf ("%3d       %7.2f    %7.2f %n", n, n_tora_average, n_olsr_average);
            i++;
        }

        System.out.println("-----------------------------------------");
        double[] ttest = Statistics.tTestUnequalVariance(tora_averages.ySeries(), olsr_averages.ySeries());
        System.out.printf ("T Value: %.3f    P Value: %.3f %n", ttest[0], ttest[1]);

        new Plot()
         .plotTitle ("Message Overhead during Network Decay")
         .xAxisTitle ("Nodes N in Network")
         .yAxisTitle ("Number of Messages Recieved")
         .xAxisStart (NU)
         .xAxisEnd (NL)
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