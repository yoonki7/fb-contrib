import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SLS_Sample {
    private String name;
    private int age;
    private int height;

    SLS_Sample hasIt(List<SLS_Sample> l, String n) {

        SLS_Sample found = null;
        for (SLS_Sample s : l) {
            if (s.name.equals(n)) {
                found = s;
            }
        }

        return found;
    }

    SLS_Sample hasAge(List<SLS_Sample> l, int age) {

        SLS_Sample found = null;
        for (SLS_Sample s : l) {
            if (s.age == age) {
                found = s;
            }
        }

        return found;
    }

    SLS_Sample fpBreak(List<SLS_Sample> l, String n) {

        SLS_Sample found = null;
        for (SLS_Sample s : l) {
            if (s.name.equals(n)) {
                found = s;
                break;
            }
        }

        return found;
    }

    SLS_Sample fpReturn(List<SLS_Sample> l, String n) {

        SLS_Sample found = null;
        for (SLS_Sample s : l) {
            if (s.name.equals(n)) {
                found = s;
                return found;
            }
        }

        return found;
    }

    boolean fpSetFlag(Map<String, SLS_Sample> m, String name) {
        boolean found = false;
        for (SLS_Sample s : m.values()) {
            if ((s == null) || s.name.equals(name)) {
                found = true;
                s.age = 1;
            }
        }

        return found;
    }

    int fpCalcTotal(List<SLS_Sample> l, String name) {
        int total = 0;
        for (SLS_Sample s : l) {
            if (s.name.equals(name)) {
                total += s.age;
            }
        }

        return total;
    }

    void fpTwoSets(List<SLS_Sample> l, String name, int age, int height) {
        String n = null;
        int a = 0;
        int h = 0;

        for (SLS_Sample s : l) {
            if (s.name.equals(name)) {
                n = s.name;
            } else if (s.age == age) {
                a = s.age;
            } else if (s.height == height) {
                h = s.height;
            }
        }

        System.out.println("Found: " + n + " " + a + " " + h);
    }

    enum State {
        A, B, C
    }

    void fpStateMachine(char[] args, String val) {
        State state = State.A;

        for (char c : args) {
            switch (state) {
                case A:
                    if (val.equals(String.valueOf(c))) {
                        state = State.B;
                    } else {
                        return;
                    }
                break;
                case B:
                    if (val.equals(String.valueOf(c))) {
                        state = State.C;
                    }
                break;

                default:
                    if (val.equals(String.valueOf(c))) {
                        state = State.A;
                    }
                break;
            }
        }
    }

    public int fpContinue(BufferedReader reader) throws IOException {
        String line;
        int mode = 0;
        while ((line = reader.readLine()) != null) {

            if (line.equals("foo")) {
                mode = 1;
                continue;
            }
            if (line.equals("bar")) {
                mode = 2;
                continue;
            }
        }

        return mode;
    }

}
