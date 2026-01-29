// https://gemini.google.com/share/964b80af968d

package Aruco_Plugins;

import ij.IJ;
import ij.gui.DialogListener;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import java.awt.AWTEvent;
import java.awt.TextField;
import java.util.Vector;

public class Dynamic_Dialog_Example2 implements PlugIn, DialogListener {

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
    public boolean dialogItemChanged(GenericDialog gd, java.awt.AWTEvent e) {
        Vector<?> numFields = gd.getNumericFields();
        TextField fieldA = (TextField) numFields.get(0);
        TextField fieldB = (TextField) numFields.get(1);

        // Check which field triggered the event
        if (e != null && e.getSource() == fieldA) {
            double valA = gd.getNextNumber();
            fieldB.setText(String.valueOf(valA * 2));
        }
        else if (e != null && e.getSource() == fieldB) {
            double valB = gd.getNextNumber();
            fieldA.setText(String.valueOf(valB / 2));
        }

        return !gd.invalidNumber();
    }
}
