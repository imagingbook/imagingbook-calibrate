// https://gemini.google.com/share/964b80af968d

package Lensfun_Plugins;

import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import imagingbook.lensfun.Lens;
import imagingbook.lensfun.LensfunDatabase;

import java.awt.AWTEvent;
import java.awt.Choice;
import java.util.List;

public class Select_Lens_1 implements PlugIn, DialogListener {

    String lensMaker;
    String lensModel;
    LensfunDatabase db = LensfunDatabase.getInstance();

    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Select Lens");

        // 1. Get the initial list of makers
        String[] lensMakers = db.getAllLensMakers().toArray(new String[0]);
        gd.addChoice("Lens Maker", lensMakers, lensMakers[0]);

        // 2. Define dummy field for lens model
        gd.addChoice("Lens Model:", new String[]{"---------------------------------------------------------"}, "");


        updateLensList(gd); // manual refresh to current maker's lenses

        gd.addDialogListener(this);
        gd.showDialog();
        if (gd.wasCanceled())
            return;

        lensMaker = gd.getNextChoice();
        IJ.log("Lens maker: " + lensMaker);
        lensModel = gd.getNextChoice();
        IJ.log("Lens model: " + lensModel);

        Lens theLens = db.findLens(lensMaker, lensModel);
        IJ.log("Selected lens: " + theLens);
    }

    private void updateLensList(GenericDialog gd) {
        Choice makerChoice = (Choice) gd.getChoices().get(0);
        Choice lensChoice = (Choice) gd.getChoices().get(1);

        String selectedMaker = makerChoice.getSelectedItem();
        List<Lens> lenses = db.getLensesByMaker(selectedMaker);

        // Clear and repopulate
        lensChoice.removeAll();
        for (Lens lens : lenses) {
            lensChoice.add(lens.getModel());
        }
    }
    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        // Only update if the Maker choice (index 0) was the source of the event
        if (e != null && e.getSource() == gd.getChoices().get(0)) {
            updateLensList(gd);
        }
        gd.repaint();   // optional
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