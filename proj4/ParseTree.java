import java.io.IOException;
import java.util.*;

public class ParseTree {
    public static final String INVALID_FUNCTION_NAME = "invalid function name";
    public static final String INVALID_EXPRESSION = "invalid expression";
    static final String ARG_SIZE_ERROR = "too many or too few arguments";
    public static final String ALL_CONDITIONS_FALSE = "all conditions in COND statement holds false";
    public static final String ARGS_TYPE_ERROR = " invalid arguments type ";
    public static final String FUNC_ARGS_MISMATCH = " formal args and actual args size mis match";

    public static final String CAR = "CAR";
    public static final String CDR = "CDR";
    public static final String CONS = "CONS";
    public static final String PLUS = "PLUS";
    public static final String MINUS = "MINUS";
    public static final String TIMES = "TIMES";
    public static final String LESS = "LESS";
    public static final String GREATER = "GREATER";
    public static final String ATOM = "ATOM";
    public static final String EQ = "EQ";
    public static final String NULL = "NULL";
    public static final String INT = "INT";
    public static final String COND = "COND";
    public static final String QUOTE = "QUOTE";
    public static final String DEFUN = "DEFUN";
    public static final String NIL = "NIL";


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
        HashMap<String, Node> dMap = new HashMap<>();
        HashMap<String, Node> aMap = new HashMap<>();
        while (tokenInfos.get(pos).token != Token.EOF) {
            topExpStart = pos;

            Node root = null;
            try {
                root = ParseExp();
//                printNode(root);
//                print(":   ");
                Node evalTree = eval(root, aMap, dMap);
                if (evalTree == null) {
                    println("");
                    continue;
                }
                printNode(evalTree);
                println("");
            } catch (ParseException e) {
//                System.exit(0);
                println(e.getMessage() + "");

                if (e.node != null) {
                    print("error when parse sub expression: ");
                    printNode(e.node);
                    print("\noriginal expression: ");
                    printNode(root);
                    println("");
                }
                System.exit(0);


//                e.printStackTrace();


            }
        }


    }

    /**
     * @param root
     * @param aMap
     * @param dMap: key=funcionName  value=(params.body)
     * @return
     * @throws ParseException
     */
    private Node eval(Node root, HashMap<String, Node> aMap, HashMap<String, Node> dMap) throws ParseException {
        if (isAtom(root)) {
            if (NIL_TOKEN.equals(root.tokenInfo) || (T_TOKEN.equals(root.tokenInfo)) || root.tokenInfo.isNumeric()) {
                return root;
            } else if (aMap.containsKey(root.tokenInfo.literal_value)) {
                //alist contains literal isAtom
                return aMap.get(root.tokenInfo.literal_value);
            } else {
                error("invalid Atom " + root.tokenInfo, root);
                return ERROR_NODE;
            }
        } else { //exp is a list
            TokenInfo op = root.leftChild.tokenInfo;//car(root)
            if (op.isLiteral) {
                ArrayList<Node> args = getArgs(root.rightChild);

                if (QUOTE.equals(op.literal_value)) {
                    if (args.size() != 1) {
                        error(ARG_SIZE_ERROR, root);
                        return ERROR_NODE;
                    }
                    return args.get(0);
                } else if (COND.equals(op.literal_value)) {
//
                    ArrayList<ArrayList<Node>> condargs = new ArrayList<>(args.size());
                    for (Node arg : args) {
                        if (!arg.isInner()) {
                            error(ARG_SIZE_ERROR, root);
                            return ERROR_NODE;
                        }
                        ArrayList<Node> temp = getArgs(arg);
                        if (temp.size() != 2) {
                            error(ARG_SIZE_ERROR, root);
                            return ERROR_NODE;
                        } else {
                            condargs.add(temp);
                        }

                    }
                    for (ArrayList<Node> be : condargs) {
                        Node cond = be.get(0);
                        if (!isNil(eval(cond, aMap, dMap))) {
                            return eval(be.get(1), aMap, dMap);
                        }
                    }
                    error(ALL_CONDITIONS_FALSE, root);
                    return ERROR_NODE;

//                    Node argNode = eval(args.get(0));
                } else if (DEFUN.equals(op.literal_value)) {
                    if (args.size() != 3) {
                        error(ARG_SIZE_ERROR, root);
                        return ERROR_NODE;
                    }

                    if (!isAtom(args.get(0)) || args.get(0).tokenInfo.literal_value == null) {
                        error(INVALID_FUNCTION_NAME, root);
                        return ERROR_NODE;
                    }
                    String funcId = args.get(0).tokenInfo.literal_value;
                    if (KEYWORDS.contains(funcId)) {
                        error("function name can not be " + funcId, root);
                        return ERROR_NODE;
                    }
                    //args should be a list(empty list included), body is arbitrary
                    //todo : (DEFUN TEST NIL BODY) is as valid as (DEFUN TEST () BODY)
                    //the elements of arglist should be different from each other and not use keyword
                    //list， fundId id should be also different from keyword
                    if ((args.get(1).isInner() || isNil(args.get(1)))) {
                        ArrayList<Node> elements = getArgs(args.get(1));
                        HashSet<String> argName = new HashSet<>();
                        for (Node node : elements) {
                            if (isAtom(node) && node.tokenInfo.literal_value != null && !KEYWORDS.contains(node
                                    .tokenInfo.literal_value)) {
                                argName.add(node.tokenInfo.literal_value);
                            }
                        }
                        //only if all the argNames are literal atom, and different from each other and keywords will the
                        //equal hold true
                        if (argName.size() == elements.size()) {
                            dMap.put(funcId, cons(args.get(1), args.get(2)));
                            return new Node(new TokenInfo(Token.ATOM,funcId));
                        }

                    }
                    error(op.literal_value + ":invalid formal argument list ", root);
                    return ERROR_NODE;
                } else {
                    return apply(root.leftChild, evalList(root.rightChild, aMap, dMap), aMap, dMap);
                }

            }
            error(INVALID_EXPRESSION, root);
            return ERROR_NODE;
        }

    }

    //arg already evaluated
    private Node apply(Node root, Node argList, HashMap<String, Node> aMap, HashMap<String, Node> dMap) throws
            ParseException {
        Node fullExp = cons(root, argList);
        if (isAtom(root) && root.tokenInfo.literal_value != null) {
            String funcId = root.tokenInfo.literal_value;
            ArrayList<Node> actualArgs = getArgs(argList);
            if (BIN_NUM_OP.contains(funcId)) {

                if (actualArgs.size() != 2) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node arg1 = actualArgs.get(0);
                Node arg2 = actualArgs.get(1);
                if (arg1.tokenInfo != null && arg1.tokenInfo.isNumeric() && arg2.tokenInfo != null && arg2.tokenInfo
                        .isNumeric()) {
                    int argN1 = arg1.tokenInfo.numValue;
                    int argN2 = arg2.tokenInfo.numValue;
                    if (funcId.equals(PLUS)) {
                        return new Node(new TokenInfo(Token.ATOM, argN1 + argN2));
                    } else if (MINUS.equals(funcId)) {
                        return new Node(new TokenInfo(Token.ATOM, argN1 - argN2));
                    } else if (TIMES.equals(funcId)) {
                        return new Node(new TokenInfo(Token.ATOM, argN1 * argN2));

                    } else if (LESS.equals(funcId)) {
                        if (argN1 < argN2) return T_NODE;
                        else return NIL_NODE;
                    } else if (GREATER.equals(funcId)) {
                        if (argN1 > argN2) return T_NODE;
                        else return NIL_NODE;
                    } else {
                        return ERROR_NODE;
                    }


                } else {
                    error(funcId + ARGS_TYPE_ERROR, fullExp);
                    return ERROR_NODE;
                }
            } else if (EQ.equals(funcId)) {
                if (actualArgs.size() != 2) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node arg1 = actualArgs.get(0);
                Node arg2 = actualArgs.get(1);
                if (arg1.tokenInfo != null && arg2.tokenInfo != null) {
                    //same numeric or same literal atom
                    if (arg1.tokenInfo.equals(arg2.tokenInfo)) return T_NODE;
                    else return NIL_NODE;

                } else {
                    error(ARGS_TYPE_ERROR, fullExp);
                    return ERROR_NODE;
                }
            } else if (ATOM.equals(funcId)) {
                if (actualArgs.size() != 1) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }

                if (isAtom(actualArgs.get(0))) return T_NODE;
                else return NIL_NODE;
            } else if (NULL.equals(funcId)) {
                if (actualArgs.size() != 1) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node argNode = actualArgs.get(0);
                if (isNil(argNode)) return T_NODE;
                else return NIL_NODE;
            } else if (INT.equals(funcId)) {
                if (actualArgs.size() != 1) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node argNode = actualArgs.get(0);
                if (argNode.tokenInfo != null && argNode.tokenInfo.isNumeric()) return T_NODE;
                else return NIL_NODE;
            } else if (CAR.equals(funcId) || CDR.equals(funcId)) {
                if (actualArgs.size() != 1) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node argNode = actualArgs.get(0);
                //argument should be a list
                if (!argNode.isInner()) {
                    error(ARGS_TYPE_ERROR, fullExp);
                    return ERROR_NODE;
                } else {
                    if (CAR.equals(funcId)) return argNode.leftChild;
                    else return argNode.rightChild;
                }

            } else if (CONS.equals(funcId)) {
                if (actualArgs.size() != 2) {
                    error(ARG_SIZE_ERROR, fullExp);
                    return ERROR_NODE;
                }
                Node argNode0 = actualArgs.get(0);
                Node argNode1 = actualArgs.get(1);
//                Node rootNode = new Node(argNode0, argNode1);
                return cons(argNode0, argNode1);
            } else if (dMap.containsKey(funcId)) {
                //self defined functions
                Node body = dMap.get(funcId).rightChild;
                ArrayList<Node> formalArgs = getArgs(dMap.get(funcId).leftChild);
                if (formalArgs.size() != actualArgs.size()) {
                    error(funcId + FUNC_ARGS_MISMATCH, fullExp);
                    return ERROR_NODE;
                }
                HashMap<String, Node> aMapCopy = new HashMap<>(aMap);
                for (int i = 0; i < formalArgs.size(); i++) {
                    String key = formalArgs.get(i).tokenInfo.literal_value;
                    aMapCopy.put(key, actualArgs.get(i));
                }
                return eval(body, aMapCopy, dMap);

            }
        }
        error(INVALID_EXPRESSION, fullExp);
        return ERROR_NODE;
    }

    private Node evalList(Node root, HashMap<String, Node> aMap, HashMap<String, Node> dMap) throws ParseException {
        if (isNil(root)) return NIL_NODE;
        else {
            return cons(eval(root.leftChild, aMap, dMap), evalList(root.rightChild, aMap, dMap));
        }
    }

    private Node cons(Node left, Node right) {
        return new Node(left, right);
    }

    private boolean isAtom(Node root) {
        return !root.isInner();
    }

    private boolean isList(Node root) {
        return !isAtom(root);
    }

//    private Node eval(Node root) throws ParseException {
//        if (!root.isInner()) {
//            if (isNil(root) || (root.tokenInfo.equals(T_TOKEN)) || root.tokenInfo.isNumeric()) {
//                return root;
//            } else {
//                error("invalid isAtom " + root.tokenInfo);
//                return ERROR_NODE;
//            }
//        } else {
//            //list
//            TokenInfo op = root.leftChild.tokenInfo;
//            if (op.isLiteral) {
//                ArrayList<Node> args = getArgs(root.rightChild);
////                int argNum = height(root.rightChild);
//
//                if (BIN_NUM_OP.contains(op.literal_value)) {
//                    if (args.size() != 2) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
////                    Node arg1 = eval(root.rightChild.leftChild);
////                    Node arg2 = eval(root.rightChild.rightChild.leftChild);
//                    Node arg1 = eval(args.get(0));
//                    Node arg2 = eval(args.get(1));
//                    if (arg1.tokenInfo != null && arg1.tokenInfo.isNumeric() && arg2.tokenInfo != null &&
//                            arg2.tokenInfo.isNumeric()) {
//                        int argN1 = arg1.tokenInfo.numValue;
//                        int argN2 = arg2.tokenInfo.numValue;
//                        if (op.literal_value.equals("PLUS")) {
//                            return new Node(new TokenInfo(Token.ATOM, argN1 + argN2));
//                        } else if ("MINUS".equals(op.literal_value)) {
//                            return new Node(new TokenInfo(Token.ATOM, argN1 - argN2));
//                        } else if ("TIMES".equals(op.literal_value)) {
//                            return new Node(new TokenInfo(Token.ATOM, argN1 * argN2));
//
//                        } else if ("LESS".equals(op.literal_value)) {
//                            if (argN1 < argN2) return T_NODE;
//                            else return NIL_NODE;
//                        } else if ("GREATER".equals(op.literal_value)) {
//                            if (argN1 > argN2) return T_NODE;
//                            else return NIL_NODE;
//                        } else {
//                            return ERROR_NODE;
//                        }
//
//
//                    } else {
//                        error(op.literal_value + " wrong type of args");
//                        return ERROR_NODE;
//                    }
//                } else if ("EQ".equals(op.literal_value)) {
//                    if (args.size() != 2) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node arg1 = eval(args.get(0));
//                    Node arg2 = eval(args.get(1));
//                    if (arg1.tokenInfo != null && arg2.tokenInfo != null) {
//                        if (arg1.tokenInfo.equals(arg2.tokenInfo)) return T_NODE;
//                        else return NIL_NODE;
//
//                    } else {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                } else if ("ATOM".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//
//                    if (!eval(args.get(0)).isInner()) return T_NODE;
//                    else return NIL_NODE;
//                } else if ("NULL".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node argNode = eval(args.get(0));
//                    if (isNil(argNode)) return T_NODE;
//                    else return NIL_NODE;
//                } else if ("INT".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node argNode = eval(args.get(0));
//                    if (argNode.tokenInfo != null && argNode.tokenInfo.isNumeric()) return T_NODE;
//                    else return NIL_NODE;
//                } else if ("CAR".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node argNode = eval(args.get(0));
//                    //argument should be a list
//                    if (!argNode.isInner()) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    } else {
//                        return argNode.leftChild;
//                    }
//
//                } else if ("CDR".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node argNode = eval(args.get(0));
//                    if (!argNode.isInner()) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    } else {
//                        return argNode.rightChild;
//                    }
//                } else if ("CONS".equals(op.literal_value)) {
//                    if (args.size() != 2) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    Node argNode0 = eval(args.get(0));
//                    Node argNode1 = eval(args.get(1));
//                    Node rootNode = new Node(argNode0, argNode1);
//                    return rootNode;
//                } else if ("QUOTE".equals(op.literal_value)) {
//                    if (args.size() != 1) {
//                        error(op.literal_value + ":invalid arguements");
//                        return ERROR_NODE;
//                    }
//                    return args.get(0);
//                } else if ("COND".equals(op.literal_value)) {
////                    if (args.size() != 1) {
////                        error(op.literal_value + ":invalid arguements");
////                        return ERROR_NODE;
////                    }
//                    ArrayList<ArrayList<Node>> condargs = new ArrayList<>(args.size());
//                    for (Node arg : args) {
//                        if (!arg.isInner()) {
//                            error(op.literal_value + ":invalid arguements");
//                            return ERROR_NODE;
//                        }
//                        ArrayList<Node> temp = getArgs(arg);
//                        if (temp.size() != 2) {
//                            error(op.literal_value + ":invalid arguements");
//                            return ERROR_NODE;
//                        } else {
//                            condargs.add(getArgs(arg));
//                        }
//
//                    }
//                    for (ArrayList<Node> be : condargs) {
//                        Node cond = be.get(0);
//                        if (!isNil(eval(cond))) {
//                            return eval(be.get(1));
//                        }
//                    }
//                    error(op.literal_value + "all conditions in COND statement holds false");
//                    return ERROR_NODE;
//
////                    Node argNode = eval(args.get(0));
//                } else {
//                    error(op.literal_value + "invalid function");
//                    return ERROR_NODE;
//                }
//
//            }
//            error("invalid list start");
//            return ERROR_NODE;
//
//        }
//    }

    private boolean isNil(Node node) {
        return (NIL_TOKEN.equals(node.tokenInfo));
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
    //turn from binary tree to list of left node (the right most node should be nil)
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

    public static final Set<String> BIN_NUM_OP = new HashSet<>(Arrays.asList(PLUS, MINUS, TIMES, LESS, GREATER));
    public static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(PLUS, MINUS, TIMES, LESS, GREATER, "T",
            NIL, CAR, CDR, CONS, ATOM, EQ, NULL, INT, COND, QUOTE, DEFUN));

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


    public void error() throws ParseException {
        throw new ParseException();
//        System.exit(0);
    }

    //    public void error(String msg) throws ParseException {
////        println(msg);
//        throw new ParseException(msg);
////        error();
//    }
    public void error(String msg, Node node) throws ParseException {
//        println(msg);
        throw new ParseException(msg, node);
//        error();
    }


//        static final String ARG_SIZE_ERROR="too many or too few arguments";


    public static final TokenInfo T_TOKEN = new TokenInfo(Token.ATOM, "T");
    public static final Node T_NODE = new Node(T_TOKEN);
    public static final TokenInfo NIL_TOKEN = new TokenInfo(Token.ATOM, NIL);
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

    public void printNode(Node node) {
        if (node == null) return;
        if (!node.isInner()) {
            print(node.tokenInfo.toString());
        } else {
            print("(");
            Node cur = node;
            printNode(cur.leftChild);
            while (cur.rightChild != null) {
                cur = cur.rightChild;
                if (cur.leftChild != null) {
                    print(" ");
                    printNode(cur.leftChild);
                } else if (!isNil(cur)) {
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
