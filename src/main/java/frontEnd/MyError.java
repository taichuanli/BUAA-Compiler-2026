package frontEnd;

public class MyError {
    // package-private 写法
    final int lineNum;
    final String message;

    public MyError(int lineNum, String message) {
        this.lineNum = lineNum;
        this.message = message;
    }

    @Override
    public String toString() {
        return lineNum + " " + message;
    }
}
