package frontEnd;

public class MyError {
    // package-private 写法
    final int lineNum;
    final ErrorCode code;

    public MyError(int lineNum, ErrorCode code) {
        this.lineNum = lineNum;
        this.code = code;
    }

    @Override
    public String toString() {
        return lineNum + " " + code.value();
    }
}
