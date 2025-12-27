package imagingbook.jaruco.boards;

import static imagingbook.jaruco.ArucoDictionaryPredefined.DICT_5X5_100;

public enum GridBoardPredefined {
    DICT_5X5_BOARD_8x5_A4 {
        @Override
        GridBoard makeInstance() {
            return new GridBoard(8, 5, 25.0, 10.0, DICT_5X5_100.getInstance(), 1);
        }
    },
    DICT_5X5_BOARD_12x8_A4 {
        @Override
        GridBoard makeInstance() {
            return new GridBoard(12, 8, 15.0, 6.0, DICT_5X5_100.getInstance(), 1);
        }
    };

    abstract GridBoard makeInstance();

    public GridBoard getInstance() {
        GridBoard gb = makeInstance();
        gb.setName(this.name());
        return gb;
    }


}
