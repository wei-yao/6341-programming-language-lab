public class ParseException extends Exception{

    public ParseException(String msg){
        super(msg);
    }
    public Node node;
    public ParseException(String msg,Node node){
        super(msg);
        this.node=node;
    }
    public ParseException(){
        super();
    }
}
