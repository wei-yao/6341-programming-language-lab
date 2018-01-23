import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;

/**
 * author: yao wei
 * date: 08/28/2017
 */
public class WordParser {
    private  InputStream ParseInputStream;

    public WordParser() throws FileNotFoundException {
        if(!Main.USE_FILE){
            ParseInputStream=System.in;
        }else{
            ParseInputStream= new FileInputStream("src/in");
        }
    }
//    public static void main(String[] args) throws IOException {
//        new WordParser().ParseStart();
//    }


    public void parseStart() throws IOException {


        TokenInfo nextToken;
        LinkedList<String> literalAtoms = new LinkedList<>();
        int sumOfNumAtom = 0;
        int numOfNumAtom = 0;
        /**
         * op:number of open parentheses
         * cp: number of closed parentheses
         */
        int op = 0, cp = 0;
        try {
            while ((nextToken = getNextToken()).token != Token.EOF) {
                switch (nextToken.token) {
                    case OPEN_PARENTHESES:
                        op++;
                        break;
                    case CLOSING_PARENTHESES:
                        cp++;
                        break;
                    case ATOM:
                        if(nextToken.isLiteral) {
                            literalAtoms.add(nextToken.literal_value);
                        }else{
                            sumOfNumAtom += nextToken.numValue;
                            numOfNumAtom++;
                        }
                        break;
                    case ERROR:
                        System.out.println("ERROR: Invalid token " + nextToken.literal_value);
                        return;


                }
            }
            System.out.print("LITERAL ATOMS: " + literalAtoms.size());
            for (String atom : literalAtoms) {
                System.out.print(", " + atom);
            }
            System.out.println();
            System.out.println("NUMERIC ATOMS: " + numOfNumAtom + ", " + sumOfNumAtom);
            System.out.println("OPEN PARENTHESES: " + op);
            System.out.println("CLOSING PARENTHESES: " + cp);
        } finally {
            ParseInputStream.close();

        }
    }



    private boolean delimiter(char c) {
        return (c == ' ' || c == '\r' || c == '\n');
    }

    /**
     *  used to store the character that getNextToken method read for later process
     */
    private char nextChar = (char) -1;
    private boolean hasNextChar = false;
//    private final InputStream ParseInputStream=new FileInputStream("src/in");

    public TokenInfo getNextToken() throws IOException {
        TokenInfo tokenInfo;
        // scanner.hasNext();
        int tmp = 0;
        if (!hasNextChar) {
            //skip delimiter（space,\r,\n）
            while ((tmp = ParseInputStream.read()) != -1 && delimiter((char) tmp)) ;
        } else {
            tmp = nextChar;
            hasNextChar = false;
        }
        if (tmp != -1) {

            char first = (char) tmp;
            if (first == '(') {
                tokenInfo = new TokenInfo(Token.OPEN_PARENTHESES);
            } else if (first == ')') {
                tokenInfo = new TokenInfo(Token.CLOSING_PARENTHESES);
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append(first);
                //add char to buffer until delimiter or parentheses
                while ((tmp = ParseInputStream.read()) != -1 && !delimiter((char) tmp)) {
                    if (tmp != '(' && tmp != ')') {
                        sb.append((char) tmp);
                    } else {
                        //if encounter parentheses, parentheses must be stored for next token
                        nextChar = (char) tmp;
                        hasNextChar = true;
                        break;
                    }
                }

                String word = sb.toString();
                //if it's numeric
                if (word.matches("\\d+")) {
                    tokenInfo = new TokenInfo(Token.ATOM, Integer.valueOf(word));

                } else if (word.matches("[A-Z][A-Z0-9]*")) {
                    tokenInfo = new TokenInfo(Token.ATOM, word);
                } else {
                    tokenInfo = new TokenInfo(Token.ERROR, word);
                }
            }

        } else {
            tokenInfo = new TokenInfo(Token.EOF);
        }
        return tokenInfo;

    }
}

enum Token {
    EOF, OPEN_PARENTHESES, CLOSING_PARENTHESES, ERROR,ATOM
//    LITERAL_ATOM, NUMERIC_ATOM,
}


class TokenInfo {
    Token token;
    String literal_value;
    int numValue = -1;
    boolean isLiteral=false;
    public TokenInfo(Token token) {
        this.token = token;
    }
    public boolean isNumeric(){
        return token==Token.ATOM&&!isLiteral;
    }
    public TokenInfo(Token token, String literal_value) {
        this.token = token;
        this.literal_value = literal_value;
        isLiteral=true;
    }

    public TokenInfo(Token token, int numValue) {
        this.token = token;
        this.numValue = numValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TokenInfo tokenInfo = (TokenInfo) o;

        if (numValue != tokenInfo.numValue) return false;
        if (isLiteral != tokenInfo.isLiteral) return false;
        if (token != tokenInfo.token) return false;
        return literal_value != null ? literal_value.equals(tokenInfo.literal_value) : tokenInfo.literal_value == null;
    }

    @Override
    public int hashCode() {
        int result = token != null ? token.hashCode() : 0;
        result = 31 * result + (literal_value != null ? literal_value.hashCode() : 0);
        result = 31 * result + numValue;
        result = 31 * result + (isLiteral ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        if(token==Token.OPEN_PARENTHESES)
            return"(";
        else if(token== Token.CLOSING_PARENTHESES){
            return ")";
        }else if(token==Token.ATOM){
            if(isLiteral)
                return literal_value;
            else
                return numValue+"";
        }else if(token==Token.EOF){
            return "EOF";
        }else  if(token==Token.ERROR){
            return "ERROR";
        }
        return super.toString();

    }
}
