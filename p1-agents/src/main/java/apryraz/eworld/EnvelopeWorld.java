/**
 * @author Joel Aumedes Serrano (48051307Y)
 * @author Joel Farré Cortés (78103400T)
 **/

package apryraz.eworld;

import org.sat4j.specs.*;

/**
 * The class for the main program of the Barcenas World
 **/
public class EnvelopeWorld {

    /**
     * This function should execute the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps. Each step must be executed with function
     * runNextStep() of the BarcenasFinder agent.
     *
     * @param wDim          the dimension of world
     * @param numSteps      num of steps to perform
     * @param fileSteps     file name with sequence of steps to perform
     * @param fileEnvelopes file name with sequence of steps to perform
     **/
    public static void runStepsSequence(int wDim,
                                        int numSteps, String fileSteps, String fileEnvelopes) throws
            ContradictionException, TimeoutException {
        // Make instances of TreasureFinder agent and environment object classes
        EnvelopeFinder EAgent = new EnvelopeFinder(wDim);
        EnvelopeWorldEnv EnvAgent = new EnvelopeWorldEnv(wDim, fileEnvelopes);

        // save environment object into EAgent
        EAgent.setEnvironment(EnvAgent);

        // load list of steps into the Finder Agent
        EAgent.loadListOfSteps(numSteps, fileSteps);

        // Execute sequence of steps with the Agent
        for (int i = 0; i < numSteps; i++) {
            EAgent.runNextStep();
        }
    }

    /**
     * This function should load five arguments from the command line:
     * arg[0] = dimension of the word
     * arg[3] = num of steps to perform
     * arg[4] = file name with sequence of steps to perform
     * arg[5] = file name with list of envelopes positions
     **/
    public static void main(String[] args) throws ContradictionException, TimeoutException {

        int wDim = Integer.parseInt(args[0]);
        int numSteps = Integer.parseInt(args[1]);
        String fileSteps = args[2];
        String fileEnvelopes = args[3];

        runStepsSequence(wDim, numSteps, fileSteps, fileEnvelopes);
    }
}