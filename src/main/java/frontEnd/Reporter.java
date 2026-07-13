package frontEnd;

import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeSet;

public class Reporter {
    // 报错用，写入到 error.txt
    public static final Reporter REPORTER = new Reporter();
    // 先根据行号比，然后如果相同根据 message 字典序排序
    private final TreeSet<MyError> errors = new TreeSet<>((o1, o2) -> {
        if (o1.lineNum == o2.lineNum) {
            return o1.message.compareTo(o2.message);
        } else {
            return Integer.compare(o1.lineNum, o2.lineNum);
        }
    });

    private Reporter() {
    }

    public void add(MyError e) {
        errors.add(e);
    }

    public void report() {
        String forError = "error.txt";
        try (FileWriter writer = new FileWriter(forError)) {
            for (MyError error : errors) {
                writer.write(error.toString() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace(System.out);
        }
    }

    public boolean hasError() {
        return !errors.isEmpty();
    }

}
