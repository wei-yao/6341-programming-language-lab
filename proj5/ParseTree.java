import javax.print.attribute.standard.MediaSize;
import java.io.IOException;
import java.util.*;

public class ParseTree {
    public static final String TYPE_ERROR = "TYPE ERROR";

    public static final String INVALID_FUNCTION_NAME = TYPE_ERROR + " undefined function";
    public static final String INVALID_EXPRESSION = TYPE_ERROR + " invalid expression";
    static final String ARG_SIZE_ERROR = TYPE_ERROR + " too many or too few arguments";
    public static  final String EMPTY_LIST_ERROR="EMPTY LIST ERROR:";
    public static final String ALL_CONDITIONS_FALSE =TYPE_ERROR+ " all conditions in COND statement holds false";
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
        while (pos<tokenInfos.size()&& tokenInfos.get(pos).token != Token.EOF) {
            topExpStart = pos;
            Node root = null;
            try {
                root = ParseExp();

                Type type = eval(root);
                CheckerType checkerType=evalList(root);
                printNode(root);
//                    print(":   ");

                if (Main.DEBUG) {
                    System.out.println(": " + type);
                    System.out.println(": " + checkerType);

                }
//                if (evalTree == null) {
//                    println("");
//                    continue;
//                }
//                printNode(evalTree);
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
                if (!Main.DEBUG)
                    System.exit(0);


//                e.printStackTrace();


            }
        }


    }

    private boolean isBoolType(Node root) {
        return T_TOKEN.equals(root.tokenInfo) ||
                F_TOKEN.equals(root.tokenInfo);
    }

    /**
     * @param root
     * @return
     * @throws ParseException
     */
    private Type eval(Node root) throws ParseException {
        if (isAtom(root)) {
            if (NIL_TOKEN.equals(root.tokenInfo)) {
                return Type.LIST_NAT_TYPE;
            } else if (isBoolType(root)) {
                return Type.BOOL_TYPE;
            } else if (root.tokenInfo.isNumeric()) {
                return Type.NAT_TYPE;
            } else {
                error("TYPE ERROR invalid Atom " + root.tokenInfo, root);
                return Type.ERROR_TYPE;
            }
        } else { //exp is a list
            TokenInfo op = root.leftChild.tokenInfo;//car(root)
            if (op.isLiteral) {
                ArrayList<Node> args = getArgs(root.rightChild);
                String funcId = op.literal_value;
                if (COND.equals(op.literal_value)) {
//
                    ArrayList<ArrayList<Node>> condargs = new ArrayList<>(args.size());
                    for (Node arg : args) {
                        if (!arg.isInner()) {
                            error(ARG_SIZE_ERROR, root);
                            return Type.ERROR_TYPE;
                        }
                        ArrayList<Node> temp = getArgs(arg);
                        if (temp.size() != 2) {
                            error(ARG_SIZE_ERROR, root);
                            return Type.ERROR_TYPE;
                        } else {
                            condargs.add(temp);
                        }

                    }
                    Type type = null;
                    for (ArrayList<Node> be : condargs) {
                        Node cond = be.get(0);
                        if (eval(cond) == Type.BOOL_TYPE) {
                            //一个可能的bug是eval返回 type error时 如果补获exception cond依然会eval 正确？
                            //应该不会，因为这个expression的eval 会abort
                            Type t = eval(be.get(1));
                            if (type == null) {
                                type = t;
                            } else if (t != type) {
                                error(TYPE_ERROR + " cond statement argument type error", root);
                                return Type.ERROR_TYPE;

                            }
                        } else {
                            error(TYPE_ERROR + " cond statement argument type error", root);
                            return Type.ERROR_TYPE;
                        }
                    }
                    //type == null 对应cond没有参数
                    if (type != null)
                        return type;


//                    Node argNode = eval(args.get(0));
                } else if (PLUS.equals(funcId)) {
                    if (args.size() != 2) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    Type arg0 = eval(args.get(0));
                    Type arg1 = eval(args.get(1));
                    if (arg0 == Type.NAT_TYPE && arg1 == Type.NAT_TYPE) {
                        return Type.NAT_TYPE;
                    } else {
                        error(TYPE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                } else if (LESS.equals(funcId) || EQ.equals(funcId)) {
                    if (args.size() != 2) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    Type arg0 = eval(args.get(0));
                    Type arg1 = eval(args.get(1));
                    if (arg0 == Type.NAT_TYPE && arg1 == Type.NAT_TYPE) {
                        return Type.BOOL_TYPE;
                    }
                } else if (INT.equals(funcId) || ATOM.equals(funcId)) {
                    if (args.size() != 1) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    //即使不用，还是要eval一下，防止自表达式中出现type error
                    Type arg0 = eval(args.get(0));

                    return Type.BOOL_TYPE;
                } else if (NULL.equals(funcId)) {
                    if (args.size() != 1) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    if (eval(args.get(0)) == Type.LIST_NAT_TYPE) {
                        return Type.BOOL_TYPE;
                    }
                } else if (CAR.equals(funcId)) {
                    if (args.size() != 1) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    if (eval(args.get(0)) == Type.LIST_NAT_TYPE) {
                        return Type.NAT_TYPE;
                    }
                } else if (CDR.equals(funcId)) {
                    if (args.size() != 1) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    if (eval(args.get(0)) == Type.LIST_NAT_TYPE) {
                        return Type.LIST_NAT_TYPE;
                    }
                } else if (CONS.equals(funcId)) {
                    if (args.size() != 2) {
                        error(ARG_SIZE_ERROR, root);
                        return Type.ERROR_TYPE;
                    }
                    if (eval(args.get(0)) == Type.NAT_TYPE && eval(args.get(1)) ==
                            Type.LIST_NAT_TYPE) {
                        return Type.LIST_NAT_TYPE;
                    }
                } else {
                    error(INVALID_FUNCTION_NAME, root);
                }

            }
            error(TYPE_ERROR, root);
            return Type.ERROR_TYPE;
        }

    }
    public  final CheckerType ANY_BOOL=new CheckerType(Type.BOOL_TYPE);
    public  final CheckerType TRUE=new CheckerType(Type.TRUE_TYPE);
    public  final CheckerType FALSE=new CheckerType(Type.FALSE_TYPE);
    public final CheckerType ANY_NAT=new CheckerType(Type.NAT_TYPE);
    public final CheckerType ERROR=new CheckerType(Type.ERROR_TYPE);


    private class CheckerType{
        int value=-1;
        Type type;
        @Override
        public String toString() {
            switch (type) {
                case BOOL_TYPE:
                    return "ANY_BOOL";
                case LIST_NAT_TYPE:
                    return "List[>="+value+"]";
                case NAT_TYPE:
                    return "ANY_NAT";
                case ERROR_TYPE:
                    return "ERROR";
                case TRUE_TYPE:
                    return "TRUE";
                case FALSE_TYPE:
                    return "FALSE";
                default:
                    return "NULL";
            }
        }


        public CheckerType(Type type){
            this.type=type;
        }
        public CheckerType(int listNum){
            value=listNum;
            type=Type.LIST_NAT_TYPE;
        }
    }
    /**
     * @param root

     * @return
     * @throws ParseException
     * do not need to check arg size again, because it is already checked
     */
    private CheckerType evalList(Node root) throws ParseException {
        if (isAtom(root)) {
            if (NIL_TOKEN.equals(root.tokenInfo)){
                return new CheckerType(0);
            }else if(T_TOKEN.equals(root.tokenInfo) ){
                return TRUE;
            }else if(F_TOKEN.equals(root.tokenInfo)){
                return FALSE;
            }if(root.tokenInfo.isNumeric()){
                return    ANY_NAT;
            }
        } else { //exp is a list
            TokenInfo op = root.leftChild.tokenInfo;//car(root)
            if (op.isLiteral) {
                ArrayList<Node> args = getArgs(root.rightChild);
                String funcId=op.literal_value;
                if (COND.equals(op.literal_value)) {
//
                    ArrayList<ArrayList<Node>> condargs = new ArrayList<>(args.size());
                    for (Node arg : args) {

                        ArrayList<Node> temp = getArgs(arg);

                        condargs.add(temp);


                    }
                    //already checked before in the first checker
//                    if(condargs.size()==0){
//                        error(ARG_SIZE_ERROR, root);
//                        return ERROR;
//                    }
                    Type type;
                    boolean hasAnyBool=false;
                    int i=0;
                    int minIndex=Integer.MAX_VALUE;
                    ArrayList<CheckerType> checkerTypeArrayList=new ArrayList<>(condargs.size());
                    for (ArrayList<Node> be : condargs) {
                        Node cond = be.get(0);
                        CheckerType condType=evalList(cond);
                        checkerTypeArrayList.add(condType);
                        if (condType==ANY_BOOL) {
                            //一个可能的bug是eval返回 type error时 如果补获exception cond依然会eval 正确？
                            //应该不会，因为这个expression的eval 会abort
                                hasAnyBool=true;
                               // break;
                        }else if(condType==TRUE){
                            if(minIndex>i)
                                minIndex=i;
                        }else  if(condType!=FALSE){
                            error(TYPE_ERROR+" cond statement argument type error",root);
                            return  ERROR;
                        }
                        i++;

                    }
                    if(hasAnyBool){
                        ArrayList<CheckerType> eList=new ArrayList<>(checkerTypeArrayList.size());
                        //eval all expressions
                        for(ArrayList<Node> be : condargs) {
                            eList.add(evalList(be.get(1)));
                        }
                        if(allEqual(eList)){
                            return eList.get(0);
                        }else{
                            CheckerType firstType=eList.get(0);
                            if(firstType==TRUE||firstType==FALSE||firstType==ANY_BOOL){
                                return ANY_BOOL;
                            }else{
                                CheckerType min=firstType;
                                //find list with the lowest bound
                                for(CheckerType checkerType:eList){
                                    if(checkerType.value<min.value){
                                        min=checkerType;
                                    }
                                }
                                return min;
                            }
                        }

                    }else{
                        if(minIndex==Integer.MAX_VALUE){
                            //all condition false
                            error(ALL_CONDITIONS_FALSE,root);
                            return  ERROR;
                        }else{
                            return evalList(condargs.get(minIndex).get(1));
                        }
                    }
                    //type == null 对应cond没有参数



//                    Node argNode = eval(args.get(0));
                } else if(PLUS.equals(funcId)){

                    if(evalList(args.get(0))==ANY_NAT&&evalList(args.get(1))==ANY_NAT){
                        return ANY_NAT;
                    }
                }else if(LESS.equals(funcId)||EQ.equals(funcId)){

                    if(evalList(args.get(0))==ANY_NAT&&evalList(args.get(1))==ANY_NAT){
                        return ANY_BOOL ;
                    }
                }else  if(INT.equals(funcId)||ATOM.equals(funcId)){

                    //即使不用，还是要eval一下，防止自表达式中出现type error
                    CheckerType ct = evalList(args.get(0));
                    if(INT.equals(funcId)){
                        if(ct==ANY_NAT)
                            return TRUE;
                        else
                            return FALSE;
                    }else{
                        //ATOM
                        if(ct.type!=Type.LIST_NAT_TYPE){
                            return TRUE;
                        }
                        return  FALSE;
                    }

                }else if(NULL.equals(funcId)){

                    if(evalList(args.get(0)).value>0){
                        return FALSE;
                    }else{
                        return TRUE;
                    }
                }else if(CAR.equals(funcId)){

                    int listSize=evalList(args.get(0)).value;
                    if(listSize>0){
                        return ANY_NAT;
                    }else{
                        error(EMPTY_LIST_ERROR, root);
                        return ERROR;
                    }
                }else if(CDR.equals(funcId)){
                    int listSize=evalList(args.get(0)).value;
                    if(listSize>0){
                        return new CheckerType(listSize-1);
                    }else{
                        error(EMPTY_LIST_ERROR, root);
                        return ERROR;
                    }
                }else if(CONS.equals(funcId)) {
                    int listSize=evalList(args.get(1)).value;
                    return new CheckerType(listSize+1);

                }

            }

        }
        error(EMPTY_LIST_ERROR, root);
        return ERROR;
        }

    private boolean allEqual(ArrayList<CheckerType> checkerTypeArrayList) {
        if(checkerTypeArrayList.size()==0)
            return true;
        CheckerType ct=checkerTypeArrayList.get(0);
        for(CheckerType checkerType:checkerTypeArrayList){
            if(checkerType.type!=ct.type||checkerType.value!=ct.value)
                return false;
        }
        return true;
    }

    private enum  Type{
        BOOL_TYPE,NAT_TYPE,LIST_NAT_TYPE,ERROR_TYPE,TRUE_TYPE,FALSE_TYPE
    }

//    private Node evalList(Node root, HashMap<String, Node> aMap, HashMap<String, Node> dMap) throws ParseException {
//        if (isNil(root)) return NIL_NODE;
//        else {
//            return cons(eval(root.leftChild, aMap, dMap), evalList(root.rightChild, aMap, dMap));
//        }
//    }

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

    public static final Set<String> BIN_NUM_OP = new HashSet<>(Arrays.asList(PLUS, LESS));
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
                //pos--;//so that if the main program will meet eof
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
    public static final TokenInfo F_TOKEN=new TokenInfo(Token.ATOM, "F");
    public static final TokenInfo NAT_TOKEN=new TokenInfo(Token.ATOM, "NAT");
    public static final TokenInfo LIST_TOKEN=new TokenInfo(Token.ATOM, "LIST(NAT)");
    public static final Node NAT_NODE=new Node(NAT_TOKEN);
    public  static  final TokenInfo BOOL_TOKEN=new TokenInfo(Token.ATOM, "BOOL");
    public  static  final Node BOOL_NODE=new Node(BOOL_TOKEN);

    public static final Node LIST_NODE=new Node(LIST_TOKEN);
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
