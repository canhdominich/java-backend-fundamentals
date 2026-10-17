package dev.canhdominich.spring.mvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserStore {

    private final List<String> names = Collections.synchronizedList(new ArrayList<>());

    public void add(User user) {
        names.add(user.getName());
    }

    public int size() {
        return names.size();
    }

    public void clear() {
        names.clear();
    }
}
