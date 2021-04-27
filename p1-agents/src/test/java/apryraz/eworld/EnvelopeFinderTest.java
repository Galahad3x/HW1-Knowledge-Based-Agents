/**
 * @author Joel Aumedes Serrano (48051307Y)
 * @author Joel Farré Cortés (78103400T)
 **/

package apryraz.eworld;

import org.junit.Test;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.System.exit;
import static org.junit.Assert.assertEquals;

/**
 * Class for testing the TreasureFinder agent
 **/
public class EnvelopeFinderTest {

    /**
     * eAgent   Search agent
     **/
    EnvelopeFinder eAgent;

    /**
     * env  Envelope world that simulates the positions of the envelopes
     **/
    EnvelopeWorldEnv env;

    /**
     * states   List of the results that the agent sould return to check it correctly
     **/
    ArrayList<EFState> states;

    public void setUp(String envelopesFile, String stepsFile, String statesFile, int wDim, int numStates) throws ContradictionException {
        eAgent = new EnvelopeFinder(wDim);
        env = new EnvelopeWorldEnv(wDim, envelopesFile);
        eAgent.setEnvironment(env);
        eAgent.loadListOfSteps(numStates, stepsFile);
        states = loadListOfTargetStates(wDim, numStates, statesFile);
    }

    /**
     * This function should execute the next step of the agent, and the assert
     * whether the resulting state is equal to the targetState
     *
     * @param eAgent      EnvelopeFinder agent
     * @param targetState the state that should be equal to the resulting state of
     *                    the agent after performing the next step
     **/
    public void testMakeSimpleStep(EnvelopeFinder eAgent,
                                   EFState targetState) throws
            ContradictionException, TimeoutException {
        // Check (assert) whether the resulting state is equal to
        //  the targetState after performing action runNextStep with bAgent
        eAgent.runNextStep();
        assertEquals(targetState, eAgent.getState());
    }

    /**
     * Read an state from the current position of the file trough the
     * BufferedReader object
     *
     * @param br   BufferedReader object interface to the opened file of states
     * @param wDim dimension of the world
     **/
    public EFState readTargetStateFromFile(BufferedReader br, int wDim) throws
            IOException {
        EFState efstate = new EFState(wDim);
        String row;
        String[] rowvalues;

        for (int i = wDim; i >= 1; i--) {
            row = br.readLine();
            rowvalues = row.split(" ");
            for (int j = 1; j <= wDim; j++) {
                efstate.set(i, j, rowvalues[j - 1]);
            }
        }
        return efstate;
    }

    /**
     * Load a sequence of states from a file, and return the list
     *
     * @param wDim       dimension of the world
     * @param numStates  num of states to read from the file
     * @param statesFile file name with sequence of target states, that should
     *                   be the resulting states after each movement in fileSteps
     * @return returns an ArrayList of TFState with the resulting list of states
     **/
    ArrayList<EFState> loadListOfTargetStates(int wDim, int numStates, String statesFile) {

        ArrayList<EFState> listOfStates = new ArrayList<>(numStates);

        try {
            BufferedReader br = new BufferedReader(new FileReader(statesFile));
            // steps = br.readLine();
            for (int s = 0; s < numStates; s++) {
                listOfStates.add(readTargetStateFromFile(br, wDim));
                // Read a blank line between states
                br.readLine();
            }
            br.close();
        } catch (FileNotFoundException ex) {
            System.out.println("MSG.   => States file not found");
            exit(1);
        } catch (IOException ex) {
            Logger.getLogger(EnvelopeFinderTest.class.getName()).log(Level.SEVERE, null, ex);
            exit(2);
        }
        return listOfStates;
    }

    /**
     * This function should run the sequence of steps stored in the file fileSteps,
     * but only up to numSteps steps.
     **/
    public void testMakeSeqOfSteps()
            throws ContradictionException, TimeoutException {
        // You should make TreasureFinder and TreasureWorldEnv objects to test.
        // Then load sequence of target states, load sequence of steps into the eAgent
        // and then test the sequence calling testMakeSimpleStep once for each step.
        //EnvelopeFinder eAgent = null;
        // load information about the World into the EnvAgent
        //EnvelopeWorldEnv envAgent = null;
        // Load list of states
        //ArrayList<EFState> seqOfStates;


        // Set environment agent and load list of steps into the finder agent
        //eAgent.loadListOfSteps(numSteps, fileSteps);
        //eAgent.setEnvironment(env);

        // Test here the sequence of steps and check the resulting states with the
        // ones in seqOfStates
        for (EFState state : states) {
            eAgent.runNextStep();
            assertEquals(state, eAgent.getState());
        }
    }

    /**
     * Test for every file set of all the steps
     **/
    @Test
    public void SeqOfStepsTests() throws
            Exception {
        for (int i = 1; i < 5; i++){
            String statesFile = "tests/states" + i + ".txt";
            String envsFile = "tests/envelopes" + i + ".txt";
            String stepsFile = "tests/steps" + i + ".txt";
            int wDim, numStates;
            if (i < 3){
                wDim = 5;
                numStates = 5;
                if(i == 2){
                    numStates = 7;
                }
            }else{
                wDim = 7;
                if(i == 3){
                    numStates = 6;
                }else{
                    numStates = 12;
                }
            }
            setUp(envsFile, stepsFile, statesFile, wDim, numStates);
            testMakeSeqOfSteps();
            System.out.println("TEST " + i + " DONE");
        }
    }

    /**
     * Test for every file set of a single step
     **/
    @Test
    public void SimpleStepTests() throws
            Exception {
        for (int i = 1; i < 5; i++){
            String statesFile = "tests/states" + i + ".txt";
            String envsFile = "tests/envelopes" + i + ".txt";
            String stepsFile = "tests/steps" + i + ".txt";
            int wDim, numStates;
            if (i < 3){
                wDim = 5;
                numStates = 5;
            }else{
                wDim = 7;
                if(i == 3){
                    numStates = 6;
                }else{
                    numStates = 12;
                }
            }
            setUp(envsFile, stepsFile, statesFile, wDim, numStates);
            testMakeSimpleStep(eAgent, states.get(0));
            System.out.println("TEST " + i + " DONE");
        }
    }
}