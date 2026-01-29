// https://gemini.google.com/share/964b80af968d

package Aruco_Plugins;

import ij.IJ;
import ij.gui.*;
import ij.plugin.PlugIn;
import java.awt.*;
import java.util.Vector;

public class Dynamic_Dialog_Example3 implements PlugIn, DialogListener {

    private TextField fieldA, fieldB;
    private Checkbox linkCheck;

    public void run(String arg) {
        GenericDialog gd = new GenericDialog("Linked Fields Logic");

        gd.addNumericField("Radius:", 10, 2);
        gd.addNumericField("Diameter:", 20, 2);
        gd.addCheckbox("Link_Diameter_to_Radius", true);

        // Fetch components immediately after adding them
        Vector<?> numFields = gd.getNumericFields();
        fieldA = (TextField) numFields.get(0);
        fieldB = (TextField) numFields.get(1);

        Vector<?> checkboxes = gd.getCheckboxes();
        linkCheck = (Checkbox) checkboxes.get(0);

        // Initial state check
        fieldB.setEnabled(!linkCheck.getState());

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
        boolean isLinked = linkCheck.getState();

        // Toggle the 'Enabled' state of Field B based on the checkbox
        fieldB.setEnabled(!isLinked);

        if (isLinked) {
            try {
                double valA = Double.parseDouble(fieldA.getText());
                // Automatically update B
                fieldB.setText(String.valueOf(valA * 2));
            } catch (NumberFormatException nfe) {
                // Ignore parsing errors during typing
            }
        }

        return true;
    }
}