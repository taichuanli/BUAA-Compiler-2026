package middleEnd;

import java.util.ArrayList;

public class ConstInitVal extends InitVal {
    private final ArrayList<Integer> constExpList = new ArrayList<>();
    public boolean isAllZero = true;


    // 无初值
    public ConstInitVal() {
        super();
    }

    // 字符串常量
    public ConstInitVal(String StringConst, int arrayLength) {
        super();
        // 处理转义字符
        boolean isBackslash = false;
        for (char c : StringConst.substring(1, StringConst.length() - 1).toCharArray()) {
            if (c == '\\' && !isBackslash) {
                isBackslash = true;
                continue;
            }
            if (isBackslash) {
                isBackslash = false;
                switch (c) {
                    case 'a' -> addConstExp(7);
                    case 'b' -> addConstExp(8);
                    case 't' -> addConstExp(9);
                    case 'n' -> addConstExp(10);
                    case 'v' -> addConstExp(11);
                    case 'f' -> addConstExp(12);
                    case '\"' -> addConstExp(34);
                    case '\'' -> addConstExp(39);
                    case '\\' -> addConstExp(92);
                    case '0' -> addConstExp(0);
                    default -> throw new RuntimeException("Invalid escape character: \\" + c);
                }
            } else {
                addConstExp(c);
            }
        }
        padToLength(arrayLength);
    }

    public void addConstExp(int i) {
        if (i != 0) {
            isAllZero = false;
        }
        constExpList.add(i);
    }

    public int getConstValue(int i) {
        return constExpList.get(i);
    }

    public void padToLength(int arrayLength) {
        if (arrayLength == 0) {
            arrayLength = 1;
        }

        while (constExpList.size() < arrayLength) {
            constExpList.add(0);
        }
    }
}
