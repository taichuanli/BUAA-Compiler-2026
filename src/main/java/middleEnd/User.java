package middleEnd;

import java.util.LinkedList;

public class User extends Value {
    protected LinkedList<Use> useList = new LinkedList<>();

    public void useValue(Value value) {
        Use use = new Use(this, value);
        useList.add(use);
        value.addUser(use);
    }
}
