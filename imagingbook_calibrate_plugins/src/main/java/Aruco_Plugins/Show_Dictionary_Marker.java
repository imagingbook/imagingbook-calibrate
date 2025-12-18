package Aruco_Plugins;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import imagingbook.core.jdoc.JavaDocHelp;
import imagingbook.jaruco.ArucoDictionary;
import imagingbook.jaruco.ArucoDictionaryPredefined;
import imagingbook.jaruco.gui.ZoomableImagePlus;

/**
 * This ImageJ plugin demonstrates the basic use of {@link GenericDialog} to create a new byte image.
 *
 * @author WB
 */
public class Show_Dictionary_Marker implements PlugIn {

    static ArucoDictionary dict = ArucoDictionaryPredefined.DICT_5X5_1000.getInstance();
    // TODO: make dictionary selectable

    static int DISPLAY_SIZE = 200;

    int idx = 0;
    int rot = 0;

    public void run(String arg) {
        int dictionarySize = dict.getNumberOfCodes();
        String[] idxItems = new String[dictionarySize];
        for (int i = 0; i < dictionarySize; i++) {
            idxItems[i] = String.valueOf(i);
        }
        String[] rotItems = {"0", "1", "2", "3"};

        GenericDialog gd = new GenericDialog("Select Dict Entry");
              gd.addChoice("Marker idx: ", idxItems, idxItems[idx]);
        gd.addChoice("Rotation: ", rotItems, rotItems[rot]);

        gd.showDialog();

        idx = gd.getNextChoiceIndex();
        rot = gd.getNextChoiceIndex();

        IJ.log("idx = " + idx);
        IJ.log("rot = " + rot);
        IJ.log("bits = " + dict.getBits(idx, rot));

        ByteProcessor bp = (ByteProcessor) dict.getMarkerImage(idx, rot).resize(DISPLAY_SIZE);
        new ImagePlus("Marker " + idx + "/" + rot, bp).show();

        // ImagePlus imp = NewImage.createByteImage(Title, Width, Height, 1, NewImage.FILL_WHITE);
        // imp.show();
    }
}
