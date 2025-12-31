package imagingbook.jaruco.boards;

import static imagingbook.jaruco.dict.ArucoDictionaryPredefined.DICT_5X5_100;
import static imagingbook.jaruco.dict.ArucoDictionaryPredefined.DICT_5X5_1000;

public enum GridBoardPredefined {
    DICT_5X5_GridBoard_8x5_A4L {
        @Override
        GridBoard makeInstance() {
            return new GridBoard(8, 5, 35.0, 25.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
        }
    },
    DICT_5X5_GridBoard_12x8_A4L {
        @Override
        GridBoard makeInstance() {
            return new GridBoard(12, 8, 21.0, 15.0, DICT_5X5_100.getInstance(), 1, PageFmt.A4_Landscape);
        }
    },
    DICT_5X5_GridBoard_18x12_A3L {
        @Override
        GridBoard makeInstance() {
            return new GridBoard(18, 12, 21.0, 15.0, DICT_5X5_1000.getInstance(), 1, PageFmt.A3_Landscape);
        }
    };

    // TODO: hide makeInstance()
    abstract GridBoard makeInstance();

    public GridBoard getInstance() {
        GridBoard gb = makeInstance();
        gb.setName(this.name());
        return gb;
    }


}
