package middleEnd;

import java.util.LinkedList;

public class Value {
    protected LinkedList<Use> usedBy = new LinkedList<>();
    protected void addUser(Use use) {
        usedBy.add(use);
    }
}
