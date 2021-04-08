

package apryraz.eworld;

import java.util.ArrayList;


public class EnvelopeWorldEnv {
    /**
     * world dimension
     **/
    int WorldDim;


    /**
     * ArrayList with the positions that have envelopes
     */
    ArrayList<Position> envelopePositions;

    /**
     * Class constructor
     *
     * @param dim          dimension of the world
     * @param envelopeFile File with list of envelopes locations
     **/
    public EnvelopeWorldEnv(int dim, String envelopeFile) {
        WorldDim = dim;
        loadEnvelopeLocations(envelopeFile);
    }

    /**
     * Load the list of pirates locations
     *
     * @param envelopeFile: name of the file that should contain a
     *                      set of envelope locations in a single line.
     **/
    public void loadEnvelopeLocations(String envelopeFile) {
        // TODO: Carregar les localitzacions dels sobres al arrayList
    }


    /**
     * Process a message received by the EFinder agent,
     * by returning an appropriate answer
     * It should answer to moveto and detectsat messages
     *
     * @param msg message sent by the Agent
     * @return a msg with the answer to return to the agent
     **/
    public AMessage acceptMessage(AMessage msg) {
        AMessage ans = new AMessage("voidmsg", "", "", "");

        msg.showMessage();
        if (msg.getComp(0).equals("moveto")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));

            if (withinLimits(nx, ny)) {
                ans = new AMessage("movedto", msg.getComp(1), msg.getComp(2), "");
            } else {
                ans = new AMessage("notmovedto", msg.getComp(1), msg.getComp(2), "");
            }
        } else if (msg.getComp(0).equals("detectsat")) {
            int nx = Integer.parseInt(msg.getComp(1));
            int ny = Integer.parseInt(msg.getComp(2));

            String solutions = "";

            if (hasEnvelopeAt(nx + 1, ny - 1) || hasEnvelopeAt(nx + 1, ny) || hasEnvelopeAt(nx + 1, ny + 1)) {
                solutions += "1";
            }
            if (hasEnvelopeAt(nx + 1, ny + 1) || hasEnvelopeAt(nx, ny + 1) || hasEnvelopeAt(nx - 1, ny + 1)) {
                solutions += "2";
            }
            if (hasEnvelopeAt(nx - 1, ny - 1) || hasEnvelopeAt(nx - 1, ny) || hasEnvelopeAt(nx - 1, ny + 1)) {
                solutions += "3";
            }
            if (hasEnvelopeAt(nx + 1, ny - 1) || hasEnvelopeAt(nx, ny - 1) || hasEnvelopeAt(nx - 1, ny - 1)) {
                solutions += "4";
            }
            if (hasEnvelopeAt(nx, ny)) {
                solutions += "5";
            }
            ans = new AMessage("detectedat", solutions, String.valueOf(nx), String.valueOf(ny));
        }
        return ans;
    }

    private boolean hasEnvelopeAt(int x, int y) {
        for (Position pos : this.envelopePositions) {
            if (pos.x == x && pos.y == y) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if position x,y is within the limits of the
     * WorldDim x WorldDim   world
     *
     * @param x x coordinate of agent position
     * @param y y coordinate of agent position
     * @return true if (x,y) is within the limits of the world
     **/
    public boolean withinLimits(int x, int y) {
        return (x >= 1 && x <= WorldDim && y >= 1 && y <= WorldDim);
    }

}
