import java.io.IOException;
import java.util.HashSet;

public class Main {
    public static  boolean DEBUG=false;
    public static boolean USE_FILE=false;
    public static void main(String[] args) throws IOException {
        for(String arg:args){
            if("-d".equals(arg)){
                DEBUG=true;
            }else if("-f".equals(arg)){
                USE_FILE=true;
            }

        }

        new ParseTree().parseStart();
    }
}
