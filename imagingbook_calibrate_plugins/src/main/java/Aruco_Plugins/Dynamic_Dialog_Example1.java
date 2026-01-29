// https://gemini.google.com/share/964b80af968d

package Aruco_Plugins;

import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.Vector;

public class Dynamic_Dialog_Example1 implements PlugIn, DialogListener {

    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Dynamic Update");

        gd.addNumericField("Value_A", 10, 0);
        gd.addNumericField("Value_B (A x 2)", 20, 0);

        // This is the crucial step:
        gd.addDialogListener(this);
        gd.showDialog();
        if (gd.wasCanceled())
            return;

        double A = gd.getNextNumber();
        double B = gd.getNextNumber();

        IJ.log("A = " + A);
        IJ.log("B = " + B);
    }

    @Override
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        // 1. Grab the current components
        Vector<?> numericFields = gd.getNumericFields();
        TextField fieldA = (TextField) numericFields.get(0);
        TextField fieldB = (TextField) numericFields.get(1);

        try {
            // 2. Perform the logic
            double valA = Double.parseDouble(fieldA.getText());
            double newValB = valA * 2;

            // 3. Update the other field automatically
            // We use setText() directly on the AWT component
            fieldB.setText(String.valueOf(newValB));
        } catch (NumberFormatException nfe) {
            // Handle empty fields or non-numeric input gracefully
        }

        return true; // Return true to keep the OK button enabled
    }
}
