package org.tester.selector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class PersonaLoadConfig {

    private static int totalUsers = 0;

    public final LoadInputMode mode;
    public final Map<String, Integer> valuesPerPersona;

    public PersonaLoadConfig(LoadInputMode mode, Map<String, Integer> valuesPerPersona) {
        this.mode = mode;
        this.valuesPerPersona = Collections.unmodifiableMap(new LinkedHashMap<>(valuesPerPersona));
    }

    public static void setTotalUsers(int count) {
        totalUsers = count;
    }

    public static int getTotalUsers() {
        return totalUsers;
    }
}
