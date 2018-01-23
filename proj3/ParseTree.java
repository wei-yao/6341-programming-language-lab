import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ParseTree {
//    public static void main(String[] args) throws IOException {
//        new ParseTree().ParseStart();
//    }

    private ArrayList<TokenInfo> tokenInfos = new ArrayList<>();
    int pos = 0;
    int topExpStart = 0;

    public void parseStart() throws IOException {
        WordParser wordParser = new WordParser();
        while (true) {
            TokenInfo tokenInfo = wordParser.getNextToken();
            tokenInfos.add(tokenInfo);
            if (tokenInfo.token == Token.EOF) break;
            if (tokenInfo.token == Token.ERROR) {
                println("ERROR: Invalid token " + tokenInfo.literal_value);
                return;
            }
        }

        while (tokenInfos.get(pos).token != Token.EOF) {
            topExpStart = pos;
            try {
                Node root = ParseExp();
                Node evalTree = eval(root);
                printNode(evalTree);
                println("");
            }catch (ParseException e){
                System.exit(0);
//                e.printStackTrace();

            }
        }


    }

    private Node eval(Node root) throws ParseException {
        if (!root.isInner()) {
            if (isNil(root) || (root.tokenInfo.equals(T_TOKEN)) || root.tokenInfo.isNumeric()) {
                return root;
            } else {
                error("invalid atom " + root.tokenInfo);
                return ERROR_NODE;
            }
        } else {
            //list
            TokenInfo op = root.leftChild.tokenInfo;
            if (op.isLiteral) {
                ArrayList<Node> args = getArgs(root.rightChild);
//                int argNum = height(root.rightChild);

                if (BIN_NUM_OP.contains(op.literal_value)) {
                    if (args.size() != 2) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
//                    Node arg1 = eval(root.rightChild.leftChild);
//                    Node arg2 = eval(root.rightChild.rightChild.leftChild);
                    Node arg1 = eval(args.get(0));
                    Node arg2 = eval(args.get(1));
                    if (arg1.tokenInfo != null && arg1.tokenInfo.isNumeric() && arg2.tokenInfo != null &&
                            arg2.tokenInfo.isNumeric()) {
                        int argN1 = arg1.tokenInfo.numValue;
                        int argN2 = arg2.tokenInfo.numValue;
                        if (op.literal_value.equals("PLUS")) {
                            return new Node(new TokenInfo(Token.ATOM, argN1 + argN2));
                        } else if ("MINUS".equals(op.literal_value)) {
                            return new Node(new TokenInfo(Token.ATOM, argN1 - argN2));
                        } else if ("TIMES".equals(op.literal_value)) {
                            return new Node(new TokenInfo(Token.ATOM, argN1 * argN2));

                        } else if ("LESS".equals(op.literal_value)) {
                            if (argN1 < argN2) return T_NODE;
                            else return NIL_NODE;
                        } else if ("GREATER".equals(op.literal_value)) {
                            if (argN1 > argN2) return T_NODE;
                            else return NIL_NODE;
                        } else {
                            return ERROR_NODE;
                        }


                    } else {
                        error(op.literal_value + " wrong type of args");
                        return ERROR_NODE;
                    }
                } else if ("EQ".equals(op.literal_value)) {
                    if (args.size() != 2) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node arg1 = eval(args.get(0));
                    Node arg2 = eval(args.get(1));
                    if (arg1.tokenInfo!=null&&arg2.tokenInfo!=null) {
                        if (arg1.tokenInfo.equals(arg2.tokenInfo)) return T_NODE;
                        else return NIL_NODE;

                    } else {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                } else if ("ATOM".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }

                    if (!eval(args.get(0)).isInner()) return T_NODE;
                    else return NIL_NODE;
                } else if ("NULL".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node argNode = eval(args.get(0));
                    if (isNil(argNode)) return T_NODE;
                    else return NIL_NODE;
                } else if ("INT".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node argNode = eval(args.get(0));
                    if (argNode.tokenInfo != null && argNode.tokenInfo.isNumeric()) return T_NODE;
                    else return NIL_NODE;
                } else if ("CAR".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node argNode = eval(args.get(0));
                    //argument should be a list
                    if (!argNode.isInner()) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    } else {
                        return argNode.leftChild;
                    }

                } else if ("CDR".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node argNode = eval(args.get(0));
                    if (!argNode.isInner()) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    } else {
                        return argNode.rightChild;
                    }
                } else if ("CONS".equals(op.literal_value)) {
                    if (args.size() != 2) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    Node argNode0 = eval(args.get(0));
                    Node argNode1 = eval(args.get(1));
                    Node rootNode = new Node(argNode0, argNode1);
                    return  rootNode;
                } else if ("QUOTE".equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(op.literal_value + ":invalid arguements");
                        return ERROR_NODE;
                    }
                    return args.get(0);
                } else if ("COND".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
                    ArrayList<ArrayList<Node>> condargs = new ArrayList<>(args.size());
                    for (Node arg : args) {
                        if (!arg.isInner()) {
                            error(op.literal_value + ":invalid arguements");
                            return ERROR_NODE;
                        }
                        ArrayList<Node> temp = getArgs(arg);
                        if (temp.size() != 2) {
                            error(op.literal_value + ":invalid arguements");
                            return ERROR_NODE;
                        } else {
                            condargs.add(getArgs(arg));
                        }

                    }
                    for (ArrayList<Node> be : condargs) {
                        Node cond = be.get(0);
                        if (!isNil(eval(cond))) {
                            return eval(be.get(1));
                        }
                    }
                    error(op.literal_value + "all conditions in COND statement holds false");
                    return ERROR_NODE;

//                    Node argNode = eval(args.get(0));
                } else {
                    error(op.literal_value + "invalid function");
                    return ERROR_NODE;
                }

            }
            error("invalid list start");
            return ERROR_NODE;

        }
    }

    private boolean isNil(Node node){
        return (node.tokenInfo!=null&&node.tokenInfo.equals(NIL_TOKEN));
    }
    //test if token is NIL,T, or numeric
    private boolean isValidAtom(TokenInfo tokenInfo) {
        boolean ret = false;
        if (tokenInfo != null) {
            if (NIL_TOKEN.equals(tokenInfo) || T_TOKEN.equals(tokenInfo) || tokenInfo.isNumeric()) {
                ret = true;
            }
        }
        return ret;
    }

    //    private Node car(Node root){
//        return root.l
//    }
    //the root should not be a atom node
    private ArrayList<Node> getArgs(Node root) {

        int height = 0;
        Node start = root;
        ArrayList<Node> args = new ArrayList<>();

        while (!isNil(start)) {
            args.add(start.leftChild);
//            if (start.rightChild == null) break;
            start = start.rightChild;
        }
        return args;
    }

    public static final Set<String> BIN_NUM_OP = new HashSet<>(Arrays.asList("PLUS", "MINUS", "TIMES", "LESS",
            "GREATER"));

    void println(String s) {
        System.out.println(s);
    }

    void print(String s) {
        System.out.print(s);
    }

    public static final Node ERROR_NODE = new Node(new TokenInfo(Token.ERROR));

    private Node ParseExp() throws ParseException {
        TokenInfo nextToken = tokenInfos.get(pos);
        pos++;
        if (nextToken.token == Token.ATOM) {
//            print(nextToken.toString());
            return new Node(nextToken);
        } else if (nextToken.token == Token.OPEN_PARENTHESES) {
            //  print("(");
            // pos++;
            //  int start=pos;
            Node listNode = ParseList();
            nextToken = tokenInfos.get(pos);
            pos++;
            if (nextToken.token != Token.CLOSING_PARENTHESES) {
                println("\nERROR:INVALID EXPRESSION");
                for (int i = topExpStart; i < pos; i++) {
                    print(tokenInfos.get(i).toString() + " ");
                }
                error();
//                System.exit(0);
                return ERROR_NODE;

            }
            return listNode;
        } else {
            println("\nERROR:INVALID EXPRESSION");
            for (int i = topExpStart; i < pos; i++) {
                print(tokenInfos.get(i).toString() + " ");
            }
            error();
            return ERROR_NODE;
        }

    }

    public static final String NIL = "NIL";

    public void error() throws ParseException {
        throw  new ParseException();
//        System.exit(0);
    }

    public void error(String msg) throws ParseException {
        println(msg);
        throw  new ParseException(msg);
//        error();
    }
//    public void error(String msg) throws ParseException {
//        println(msg);
//        throw  new ParseException(msg);
////        error();
//    }
    public static final TokenInfo T_TOKEN = new TokenInfo(Token.ATOM, "T");
    public static final Node T_NODE = new Node(T_TOKEN);
    public static final TokenInfo NIL_TOKEN = new TokenInfo(Token.ATOM, "NIL");
    public static final Node NIL_NODE = new Node(NIL_TOKEN);

    //  public static final TokenInfo NULL = new TokenInfo(Token.ATOM, "NULL");
    // public static final Node NULL_NODE = new Node(NULL);
//    public static final Node END_NODE = new Node(true);

    private Node ParseList() throws ParseException {
        TokenInfo nextToken = tokenInfos.get(pos);
        if (nextToken.token == Token.CLOSING_PARENTHESES) {
            // print(NIL);
//            error();
            //null list is not permit
            return NIL_NODE;
        } else {
            //  print("(");
            //inner node
            Node root = new Node(ParseExp(), ParseList());
//            root.leftChild = ParseExp();
//            print(".");
            //right child
//            root.rightChild = ParseList();
            return root;
//            print(")");
        }
    }

public void printNode(Node node){
      if(!node.isInner()){
          print(node.tokenInfo.toString());
      }else{
          print("(");
          Node cur=node;
          printNode(cur.leftChild);
          while (cur.rightChild!=null){
              cur=cur.rightChild;
              if(cur.leftChild!=null){
                  print(" ");
                  printNode(cur.leftChild);
              }else if(!isNil(cur)){
                  print(".");
                  printNode(cur);
              }

          }
          print(")");
      }
}

}

class Node {
    Node leftChild = null;
    Node rightChild = null;
    TokenInfo tokenInfo = null;
    private boolean isInner = false;
//    private boolean nil = false;
    //leaf node
    public Node(TokenInfo tokenInfo) {
        this.tokenInfo = tokenInfo;
    }

    public boolean isInner() {
        return isInner;
    }

    //initialize a nil node
//    public Node(boolean nil) {
//        this.nil = nil;
//    }

    public Node(Node left, Node right) {
        isInner = true;
        leftChild = left;
        rightChild = right;
    }

//    public boolean isNil() {
//        return nil;
//    }
}
