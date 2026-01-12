package imagingbook.jaruco.cornerdata.DICT_5x5_CharucoBoard_12x8_A4L;

import imagingbook.jaruco.cornerdata.CornerSet;

/**
 * DICT_5x5_CharucoBoard_12x8_A4L image corner data.
 * TODO: merge with associated sample images in resources!
 * Usage:
 * {@code double[][] icorners = DICT_5x5_CharucoBoard_12x8_A4L.DSC_2691.getCorners();}
 */
@Deprecated
public enum ImageCornerSet {
    DSC_2691(new DSC_2691()),
    DSC_2692(new DSC_2692()),
    DSC_2693(new DSC_2693()),
    DSC_2694(new DSC_2694()),
    DSC_2696(new DSC_2696()),
    DSC_2698(new DSC_2698()),
    DSC_2699(new DSC_2699()),
    DSC_2700(new DSC_2700()),
    DSC_2702(new DSC_2702()),
    DSC_2704(new DSC_2704()),
    DSC_2705(new DSC_2705()),
    DSC_2706(new DSC_2706()),
    DSC_2707(new DSC_2707()),
    DSC_2708(new DSC_2708()),
    DSC_2709(new DSC_2709()),
    DSC_2710(new DSC_2710()),
    DSC_2711(new DSC_2711()),
    DSC_2712(new DSC_2712()),
    DSC_2713(new DSC_2713()),
    DSC_2715(new DSC_2715());

    ImageCornerSet(CornerSet data){
        this.instance = data;
    }
    private final CornerSet instance;

    public CornerSet getInstance(){
        return instance;
    }

    // public double[][] getCorners() {
    //     return instance.getCorners();
    // }

    // public static void main(String[] args) {
    //     // for (int i = 2693; i < 2716; i++) {
    //     //     System.out.format("  DSC_%d(new DSC_%d()),\n", i, i);
    //     // }
    //
    //     // double[][] icorners = DSC_2691.getCorners();
    //     for (ImageCornerSet cornerSet : ImageCornerSet.values()) {
    //         double[][] icorners = cornerSet.getInstance().getCorners();
    //         System.out.println(cornerSet + " corners: " + icorners.length);
    //     }
    //
    // }
}

