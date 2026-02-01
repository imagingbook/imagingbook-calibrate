// https://gemini.google.com/share/964b80af968d

package Lensfun_Plugins;

import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagingbook.lensfun.Camera;
import imagingbook.lensfun.Lens;
import imagingbook.lensfun.LensfunDatabase;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.awt.Dimension;
import java.util.List;
import java.util.Vector;

public class Select_Lens_2 implements PlugIn, DialogListener {

    private static final String placeHolder = "-".repeat(40);

    String camMaker;
    String camModel;
    String lensMaker;
    String lensModel;
    LensfunDatabase db = LensfunDatabase.getInstance();

    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Select Lens");

        String[] camMakers = db.getCameraMakers().toArray(new String[0]);

        gd.addChoice("Camera Maker:", camMakers, "Canon");
        gd.addChoice("Camera Model:", new String[]{placeHolder}, ""); // Index 1
        gd.addChoice("Lens Maker:", new String[]{placeHolder}, "");   // Index 2
        gd.addChoice("Lens Model:", new String[]{placeHolder}, "");   // Index 3

        // Vector<?> choices = gd.getChoices();
        // for (Object c : choices) {
        //     Choice choice = (Choice) c;
        //     // Get the size the placeholder created
        //     Dimension d = choice.getPreferredSize();
        //     // Force this to be the minimum size so it never shrinks
        //     choice.setMinimumSize(d);
        // }

        gd.pack();
        // -------------------------------

        // This will chain down and fill all boxes
        updateCameraModels(gd);
        gd.addDialogListener(this);
        gd.showDialog();
        if (gd.wasCanceled()) {
            return;
        }

        // -------------------------------

        camMaker = gd.getNextChoice();
        camModel = gd.getNextChoice();
        lensMaker = gd.getNextChoice();
        lensModel = gd.getNextChoice();

        IJ.log("Cam maker: " + camMaker);
        IJ.log("Cam model: " + camModel);
        IJ.log("Lens maker: " + lensMaker);
        IJ.log("Lens model: " + lensModel);

        Lens theLens = db.findLens(lensMaker, lensModel);
        IJ.log("Selected lens: " + theLens);
    }

    public void updateCameraModels(GenericDialog gd) {
        String maker = ((Choice)gd.getChoices().get(0)).getSelectedItem();
        List<Camera> cams = db.getCamerasByMaker(maker);
        Choice modelChoice = (Choice)gd.getChoices().get(1);

        modelChoice.removeAll();
        for (Camera c : cams) {
            modelChoice.add(c.getModel());
        }
        // modelChoice.add(placeHolder);
        updateLensMakers(gd); // Chain down
    }

    public void updateLensMakers(GenericDialog gd) {
        Camera cam = db.findCamera(
                ((Choice)gd.getChoices().get(0)).getSelectedItem(), // camera maker
                ((Choice)gd.getChoices().get(1)).getSelectedItem()  // camera model
        );

        String mount = cam.mount();
        // Filter Lens Makers who have lenses for cam.getMount()
        List<String> validMakers = db.getLensMakersForMount(mount);
        Choice lensMakerChoice = (Choice)gd.getChoices().get(2);

        lensMakerChoice.removeAll();
        for (String m : validMakers) {
            lensMakerChoice.add(m);
        }
        // lensMakerChoice.add(placeHolder);

        updateLensList(gd);
    }

    private void updateLensList(GenericDialog gd) {
        Choice camMakerChoice = (Choice) gd.getChoices().get(0);
        Choice camModelChoice = (Choice) gd.getChoices().get(1);
        Choice lensMakerChoice = (Choice) gd.getChoices().get(2);
        Choice lensChoice = (Choice) gd.getChoices().get(3);

        // 1. Get the current camera to find its mount
        Camera cam = db.findCamera(camMakerChoice.getSelectedItem(), camModelChoice.getSelectedItem());
        String selectedLensMaker = lensMakerChoice.getSelectedItem();

        lensChoice.removeAll();
        if (cam != null && selectedLensMaker != null) {
            // 2. Filter lenses by Maker AND Compatibility
            List<Lens> compatibleLenses = db.getMasterLensList().stream()
                    .filter(lens -> lens.getMaker().equals(selectedLensMaker))
                    .filter(lens -> db.isCompatible(cam.mount(), lens.getMounts()))
                    .toList();

            for (Lens l : compatibleLenses) {
                lensChoice.add(l.getModel());
            }
            // lensChoice.add(placeHolder);
        }
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        Vector<?> choices = gd.getChoices();
        if (e == null || choices.size() < 4)
            return true;

        Object source = e.getSource();

        if (source == choices.get(0)) {
            // Start at the top: Camera Maker -> Model -> Lens Maker -> Lens
            updateCameraModels(gd);
        }
        else if (source == choices.get(1)) {
            // Skip Camera Maker: Camera Model -> Lens Maker -> Lens
            updateLensMakers(gd);
        }
        else if (source == choices.get(2)) {
            // Skip Cameras: Lens Maker -> Lens
            updateLensList(gd);
        }

        return true;
    }


    // @Override
    // public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
    //     // Get the Choice components (Maker is at index 0, Lens is at index 1)
    //     Choice makerChoice = (Choice) gd.getChoices().get(0);
    //     Choice lensChoice = (Choice) gd.getChoices().get(1);
    //
    //     String selectedMaker = makerChoice.getSelectedItem();
    //     IJ.log("selectedMaker: " + selectedMaker);
    //
    //     // Only update if the event came from the Maker choice or if the Lens list is empty
    //     if (e != null && e.getSource() == makerChoice) {
    //         IJ.log("updating lensChoice");
    //
    //         // 1. Get filtered lenses from your manager
    //         List<Lens> filteredLenses = db.getLensesByMaker(selectedMaker);
    //
    //         IJ.log("adding lenses: " + filteredLenses.size());
    //
    //         // 2. Update the AWT Choice component
    //         lensChoice.removeAll();
    //         for (Lens lens : filteredLenses) {
    //             lensChoice.add(lens.getModel());
    //         }
    //
    //         // 3. Optional: Trigger a UI refresh
    //         gd.repaint();
    //     }
    //
    //     return true; // Keep dialog open
    // }
}