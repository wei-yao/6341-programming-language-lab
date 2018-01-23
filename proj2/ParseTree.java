
import java.io.IOException;
import java.util.ArrayList;

public class ParseTree {
//    public static void main(String[] args) throws IOException {
//        new ParseTree().ParseStart();
//    }

    private ArrayList<TokenInfo> tokenInfos = new ArrayList<>();
    int pos = 0;
    int topExpStart=0;
    public void parseStart() throws IOException {
        WordParser wordParser = new WordParser();
        while (true) {
            TokenInfo tokenInfo = wordParser.getNextToken();
            tokenInfos.add(tokenInfo);
            if (tokenInfo.token == Token.EOF)
                break;
            if (tokenInfo.token == Token.ERROR) {
                println("ERROR: Invalid token " + tokenInfo.literal_value);
                return;
            }
        }

        while (tokenInfos.get(pos).token != Token.EOF) {
            topExpStart=pos;
            ParseExp();
            println("");
        }


    }

    void println(String s) {
        System.out.println(s);
    }

    void print(String s) {
        System.out.print(s);
    }

    private void ParseExp() {
        TokenInfo nextToken = tokenInfos.get(pos);
        pos++;
        if (nextToken.token == Token.ATOM) {
            print(nextToken.toString());
        } else if (nextToken.token ==Token.OPEN_PARENTHESES){
          //  print("(");
           // pos++;
          //  int start=pos;
            ParseList();
            nextToken = tokenInfos.get(pos);
            pos++;
            if(nextToken.token!=Token.CLOSING_PARENTHESES){
                println("\nERROR:INVALID EXPRESSION");
                for(int i=topExpStart;i<pos;i++)
                {
                    print(tokenInfos.get(i).toString()+" ");
                }
                System.exit(0);

            }
        }else{
//            println("ERROR:INVALID EXPRESSION START :\""+nextToken.toString()+"\"");
//            System.exit(0);
            println("\nERROR:INVALID EXPRESSION");
            for(int i=topExpStart;i<pos;i++)
            {
                print(tokenInfos.get(i).toString()+" ");
            }
            System.exit(0);
        }

}
    public static final String NIL="NIL";
    private void ParseList() {
        TokenInfo nextToken = tokenInfos.get(pos);
        if(nextToken.token==Token.CLOSING_PARENTHESES){
            print(NIL);

        }else{
            print("(");
            //left child
            ParseExp();
            print(".");
            //right child
            ParseList();
            print(")");
        }
    }

}
