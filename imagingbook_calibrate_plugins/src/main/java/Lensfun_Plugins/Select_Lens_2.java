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

    // private static final String placeHolder = "\u00A0".repeat(60);
    private static final String placeHolder = "#".repeat(60);

    String camMaker;
    String camModel;
    String lensMaker;
    String lensModel;
    LensfunDatabase db = LensfunDatabase.getInstance();

    public void run(String arg) {
        // GenericDialog gd = new GenericDialog("Select Lens");
        GenericDialog gd = new GenericDialog("Select Lens") {
            @Override
            protected void setup() {    // called by showDialog() after pack()
                for (Object c : getChoices()) {
                    Choice choice = (Choice) c;
                    // Lock the width that the placeholders created
                    Dimension d = choice.getPreferredSize();
                    choice.setPreferredSize(new Dimension(d.width, d.height));
                    choice.setMinimumSize(new Dimension(d.width, d.height));
                }
                // populate choices 1-3:
                updateCameraModels(this); // Now clear and update
            }
        };

        String[] camMakers = db.getCameraMakers().toArray(new String[0]);
        gd.addChoice("Camera Maker:", camMakers, camMakers[0]);         // Choice 0
        gd.addChoice("Camera Model:", new String[]{placeHolder}, "");   // Choice 1
        gd.addChoice("Lens Maker:",   new String[]{placeHolder}, "");   // Choice 2
        gd.addChoice("Lens Model:",   new String[]{placeHolder}, "");   // Choice 3

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

        Camera theCam = db.findCamera(camMaker, camModel);
        IJ.log("Selected camera: " + theCam);
        Lens theLens = db.findLens(lensMaker, lensModel);
        IJ.log("Selected lens: " + theLens);
    }

    private void updateCameraModels(GenericDialog gd) {
        String maker = ((Choice)gd.getChoices().get(0)).getSelectedItem();
        List<Camera> cams = db.getCamerasByMaker(maker);
        Choice modelChoice = (Choice)gd.getChoices().get(1);

        modelChoice.removeAll();
        for (Camera c : cams) {
            modelChoice.add(c.getModel());
        }
        updateLensMakers(gd); // Chain down
    }

    private void updateLensMakers(GenericDialog gd) {
        Camera cam = db.findCamera(
                ((Choice)gd.getChoices().get(0)).getSelectedItem(), // camera maker
                ((Choice)gd.getChoices().get(1)).getSelectedItem()  // camera model
        );
        if (cam != null) {
            List<String> lensMakers = db.getLensMakersForCamera(cam);
            Choice lensMakerChoice = (Choice) gd.getChoices().get(2);

            lensMakerChoice.removeAll();
            for (String m : lensMakers) {
                lensMakerChoice.add(m);
            }
        }
        updateLensList(gd);
    }

    private void updateLensList(GenericDialog gd) {
        Choice camMakerChoice = (Choice) gd.getChoices().get(0);
        Choice camModelChoice = (Choice) gd.getChoices().get(1);
        Choice lensMakerChoice = (Choice) gd.getChoices().get(2);
        Choice lensChoice = (Choice) gd.getChoices().get(3);

        String selectedCamMaker  = camMakerChoice.getSelectedItem();
        String selectedCamModel = camModelChoice.getSelectedItem();
        String selectedLensMaker = lensMakerChoice.getSelectedItem();

        // 1. Get the current camera to find its mount
        Camera cam = db.findCamera(selectedCamMaker, selectedCamModel);

        lensChoice.removeAll();
        if (cam != null && selectedLensMaker != null) {
            // 2. Filter lenses by Maker AND Compatibility
            // List<Lens> compatibleLenses = db.getMasterLensList().stream()
            //         .filter(lens -> lens.getMaker().equals(selectedLensMaker))
            //         .filter(lens -> db.isCompatible(mount, lens.getMounts()))
            //         .toList();
            // for (Lens l : compatibleLenses) {
            //     lensChoice.add(l.getModel());
            // }

            String mount = cam.mount();
            List<String> lensModels = db.getLensModels(selectedLensMaker, mount);
            for (String lm : lensModels) {
                lensChoice.add(lm);
            }
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

        return true; // Keep dialog open
    }

}