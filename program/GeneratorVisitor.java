import java.io.*;
import java.util.*;

import syntaxtree.*;
import visitor.*;

public class GeneratorVisitor extends GJDepthFirst<String, Void>{

    private SymbolTable symbolTable = FirstVisitor.symbolTable;

    private FileWriter writer;

    // to know at what class and what method I am in
    private String curClass;
    private String curMethod;

    private int counter = 0; // LLVM variables counter
    private int gotoCounter = 0; // LLVM labels counter
    private int varTmp = -1; // to store the LLVM variable that contains the final result of an expression (which is computed in a visitor) to transfer this from visitor to visitor
    private int booleanReturn = -1; // to store boolean literal
    private int intReturn = -1; // to store integer literal
    
    private String allocationClassName = null; // to store the class name for the "MessageSend visitor"
    private String savePreviousClassName = null; // to store the previous class name for the "MessageSend visitor"
    
    private boolean thisFlag = false; // flag turns to true only at "ThisExpression" visitor
    
    private Stack<Integer> varsStack = new Stack<Integer>(); // to store the LLVM variables for the continuous method calls (MessageSend)
    private Stack<Integer> varTmpStack = new Stack<Integer>(); // to store the varTmp ( that refers to object allocation expression for this case ) for the continuous method calls (MessageSend)
    private Queue<Integer> queueBool = new LinkedList<Integer>(); // to store the boolean literals that used for the argument list at method calls


    public boolean isStringInt(String s){
        // check if a string is integer literal
        try{
            Integer.parseInt(s);
            return true;
        } 
        catch (NumberFormatException ex){
            return false;
        }
    }

    public String getLLVMType(String type) {
        // return the LLVM type
        if (type.equals("int")) {
            return "i32";
        } 
        else if (type.equals("int[]")) {
            return "i32*";
        } 
        else if (type.equals("boolean")) {
            return "i1";
        } 
        else {
            return "i8*"; 
        }
    }

    public String toGetClassVar(String var) throws IOException{
        // check if var is a class variable
        if (!curMethod.equals("main") && symbolTable.table.get(curClass).allClassVars.containsKey(var) && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(var)) {
            // Yes, it is a class variable, then find the declaration class and the corresponding offset of this variable
            
            String className = symbolTable.getDeclarationClassOfVariable(curClass, var);
            if (className == null) {
                className = curClass;
            }
            String idType = symbolTable.getType(curClass, curMethod, var);
            int var1 = counter++;
            int var2 = counter++;

            int offset;
            if (symbolTable.table.get(curClass).vars.containsKey(var)) // check if the class variable belongs to the class variables (not the inherited ones)
                offset = Integer.parseInt(symbolTable.table.get(curClass).varsOffset.get(var)) + 8;
            else // the class variable has been inherited from a parent class
                offset = Integer.parseInt(symbolTable.table.get(className).varsOffset.get(var)) + 8;

            // finally, get a pointer to the data field of this variable and perform the necessary bitcasts
            writer.write("  %_" + var1 + " = getelementptr i8, i8* %this, i32 " + offset + "\n");
            writer.write("  %_" + var2 + " = bitcast i8* %_" + var1 + " to " + getLLVMType(idType) + "*" + "\n");
            var = "_" + Integer.toString(var2);
        }
        return var;
    }



    public GeneratorVisitor(File file) throws IOException{
        counter=0;

        writer = new FileWriter(file);

        // LLVM virtual tables for every class 
        for (String className : symbolTable.table.keySet()) {
            if (symbolTable.table.get(className).methodsVtable.containsKey("main")) { // check if the class contains the main method
                writer.write("@." + className + "_vtable = global [0 x i8*] []\n\n");
                continue;
            }

            symbolTable.getAllMethodsForTheVtable(className,className); 
            symbolTable.getAllVariables(className, className);

            if (symbolTable.table.get(className).methodsVtable.size() == 0) { // check if the class has methods
                writer.write("@." + className + "_vtable = global [0 x i8*] []\n");
                continue;
            }

            int methodsNum = symbolTable.table.get(className).methodsVtable.size(); // get the number of the methods that the current class has
            writer.write("@." + className + "_vtable = global [" + methodsNum + " x i8*] [\n"); // initialize the virtual table
            int methodsCounter = 0;
            for (String methodName : symbolTable.table.get(className).methodsVtable.keySet()) { // now write at the V-table the "prototypes" for every method of this class
                methodsCounter++;
                writer.write("i8* bitcast ");
                writer.write("(");
                String returnType = symbolTable.table.get(className).methodsVtable.get(methodName).methodType; // get the return type of this method
                writer.write(getLLVMType(returnType));
                writer.write(" (");
                writer.write("i8*");
                if (symbolTable.table.get(className).methodsVtable.get(methodName).args.size() != 0) { // check if this method has arguments
                    // yes, it has
                    int argsCounter = 0;
                    writer.write(",");
                    for (String argType : symbolTable.table.get(className).methodsVtable.get(methodName).args) { // now write the size/type of every argument at the corresponding method of V-table 
                        argsCounter++;
                        writer.write(getLLVMType(argType));
                        if (argsCounter != symbolTable.table.get(className).methodsVtable.get(methodName).args.size())
                            writer.write(",");
                    }
                }
                writer.write(")* ");
                String parentName;
                // find the declaration class of this method
                // check for method inheritance
                if(!symbolTable.classMethod(className,methodName) && (parentName=symbolTable.getDeclarationClassOfMethod(className,methodName))!=null){
                    writer.write("@" + parentName+"."+ methodName + " to i8*");
                }
                else{
                    writer.write("@" + className + "."+ methodName + " to i8*");
                }
                writer.write(")");
                if (methodsCounter != methodsNum){
                    writer.write(",\n");
                }

            }
            writer.write("\n]\n\n");


        }

        /*************************************************************************************/ 
        // helper LLVM methods 
        writer.write("\ndeclare i8* @calloc(i32, i32)\n");
        writer.write("declare i32 @printf(i8*, ...)\n");
        writer.write("declare void @exit(i32)\n\n");
        writer.write("@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n");
        writer.write("@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n");
        writer.write("@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n");
        writer.write("define void @print_int(i32 %i) {\n");
        writer.write("  %_str = bitcast [4 x i8]* @_cint to i8*\n");
        writer.write("  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n");
        writer.write("  ret void\n");
        writer.write("}\n\n");
        writer.write("define void @throw_oob() {\n");
        writer.write("  %_str = bitcast [15 x i8]* @_cOOB to i8*\n");
        writer.write("  call i32 (i8*, ...) @printf(i8* %_str)\n");
        writer.write("  call void @exit(i32 1)\n");
        writer.write("  ret void\n");
        writer.write("}\n\n");
        writer.write("define void @throw_nsz() {\n");
        writer.write("  %_str = bitcast [15 x i8]* @_cNSZ to i8*\n");
        writer.write("  call i32 (i8*, ...) @printf(i8* %_str)\n");
        writer.write("  call void @exit(i32 1)\n");
        writer.write("  ret void\n");
        writer.write("}\n\n");
    }

    
    public void fileClose() throws IOException{
        writer.close();
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> "public"
     * f4 -> "static"
     * f5 -> "void"
     * f6 -> "main"
     * f7 -> "("
     * f8 -> "String"
     * f9 -> "["
     * f10 -> "]"
     * f11 -> Identifier()
     * f12 -> ")"
     * f13 -> "{"
     * f14 -> ( VarDeclaration() )*
     * f15 -> ( Statement() )*
     * f16 -> "}"
     * f17 -> "}"
     */
    @Override
    public String visit(MainClass n, Void argu) throws Exception {

        String className = n.f1.accept(this, null);

        curClass = className;
        curMethod = "main";

        writer.write("define i32 @main() {\n");
  
        super.visit(n, argu);

        writer.write("  ret i32 0\n");
        writer.write("}\n\n");


        curClass = null;
        curMethod = null;

        return null;
    }


    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    @Override
    public String visit(ClassDeclaration n, Void argu) throws Exception {

        String className = n.f1.accept(this, null);

        curClass = className;      
        super.visit(n, argu);

        curClass = null;

        return null;
    }

    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "extends"
     * f3 -> Identifier()
     * f4 -> "{"
     * f5 -> ( VarDeclaration() )*
     * f6 -> ( MethodDeclaration() )*
     * f7 -> "}"
     */
    @Override
    public String visit(ClassExtendsDeclaration n, Void argu) throws Exception {
        String className = n.f1.accept(this, null);

        curClass = className;

        super.visit(n, argu);

        curClass = null;

        return null;
    }

    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    @Override
    public String visit(MethodDeclaration n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        String methodType = n.f1.accept(this, null);
        String methodName = n.f2.accept(this, null);

        curMethod = methodName;

        String args="i8* %this"; // to save and write at .ll file the LLVM method's arguments
        if (symbolTable.table.get(curClass).methods.get(methodName).argsNameType.size() != 0) { // check if this method has arguments
            args += ", ";
            int argsCounter = 0;
            for (String argName : symbolTable.table.get(curClass).methods.get(methodName).argsNameType.keySet()) { // for every argument of the method
                argsCounter++;
                args += getLLVMType(symbolTable.table.get(curClass).methods.get(methodName).argsNameType.get(argName)); // "add" the type of the current argument
                args +=" "+"%."+ argName; // "add" the name of the current argument
                if (argsCounter != symbolTable.table.get(curClass).methods.get(methodName).argsNameType.size())
                    args+=", ";
            }
        }
        // finally write/define the LLVM method
        writer.write("define "+ getLLVMType(methodType) +" @" + curClass+"."+methodName+ "("+args+") {\n");
        
        
        counter = 0;
        n.f4.accept(this, null);
        n.f7.accept(this, null);
        n.f8.accept(this, null);

        String expr = n.f10.accept(this, null);
        String declClass;

        // find the declaration class
        declClass=symbolTable.getDeclarationClassOfVariable(curClass,expr);
        if (declClass==null){
            declClass=curClass;
        }


        // check if the return variable of this method is a class variable
        if (expr!=null && symbolTable.table.get(declClass).vars.containsKey(expr) && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)){
 
            String exprType=symbolTable.getType(curClass, curMethod, expr);
            expr=toGetClassVar(expr);
            int var3=counter++;
            
            writer.write("  %_" + var3 + " = load " + getLLVMType(exprType) + ", " + getLLVMType(exprType) + "* %" + expr + "\n");
    
            writer.write("  ret " + getLLVMType(methodType) + " %_" + var3 + "\n");
            writer.write("}\n\n");
        }
        else{ 
            if(booleanReturn!=-1) // check if the return value is a boolean literal
                writer.write("  ret " + getLLVMType(methodType) + " " + booleanReturn + "\n");
            else if (intReturn!=-1) // check if the return value is an int literal
                writer.write("  ret " + getLLVMType(methodType) + " " + intReturn + "\n");
            else if (varTmp!=-1) // check if the return value is an expression
                writer.write("  ret " + getLLVMType(methodType) + " %_" + varTmp + "\n");
            else{ // check if the return variable is a method's local variable
                String exprType = symbolTable.getType(curClass, curMethod, expr); 
                int var1 = counter++;
                writer.write("  %_" + var1 + " = load " + getLLVMType(exprType) + ", " + getLLVMType(exprType) + "* %" + expr + "\n");
                writer.write("  ret " + getLLVMType(methodType) + " %_" + var1 + "\n");
            }

            writer.write("}\n\n");
        }

        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        curMethod = null;

        return null;
    }

    /**
     * f0 -> Type() 
     * f1 -> Identifier()
     */
    @Override
    public String visit(FormalParameter n, Void argu) throws Exception {
        String type = n.f0.accept(this, null);
        String id = n.f1.accept(this, null);

        writer.write("  %" + id + " = alloca " + getLLVMType(type)+"\n");
        writer.write("  store " + getLLVMType(type) + " %." + id + ", " + getLLVMType(type) + "* %" + id + ""+"\n");
        symbolTable.table.get(curClass).methods.get(curMethod).varNum.put(id, "%" + id);

        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        
        return null;
    }

    /**
     * f0 -> Type() 
     * f1 -> Identifier() 
     * f2 -> ";"
     */
    @Override
    public String visit(VarDeclaration n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        String type = n.f0.accept(this, argu);
        String id = n.f1.accept(this, argu);

        if(curClass != null && curMethod!=null ){
            // local variable
            writer.write("  %"+id+" = alloca "+ getLLVMType(type) + "\n");
            if (getLLVMType(type).equals("i1") || getLLVMType(type).equals("i32"))
                writer.write("  store "+ getLLVMType(type)+" 0, "+ getLLVMType(type)+"* %"+id+"\n"); // initialize the local variable with 0 

            symbolTable.table.get(curClass).methods.get(curMethod).varNum.put(id,"%"+id);
            writer.write("\n\n");
        }
        

        n.f2.accept(this, argu);

        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        return null;

    }

    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "." 
     * f2 -> Identifier() 
     * f3 -> "(" 
     * f4 -> (ExpressionList() )? 
     * f5 -> ")"
     */
    @Override
    public String visit(MessageSend n, Void argu) throws Exception {

        varTmp = -1;
        booleanReturn = -1;
        intReturn = -1;
        
        savePreviousClassName=null;
        allocationClassName=null;
        thisFlag = false;

        String className;
        String expr = n.f0.accept(this, argu);

        thisFlag = false;

        // find the declaration class of the method
        if(allocationClassName!=null)
            className = symbolTable.getType(curClass, curMethod, allocationClassName);
        else 
            className = symbolTable.getType(curClass, curMethod, expr);

        if (className==null)
            className=savePreviousClassName;
        
        String methodName = n.f2.accept(this, argu);
        thisFlag = false;
        boolean thisUse=false;

        String type = getLLVMType(className);
        int var1 = counter++;

        writer.write("\n");
        if (symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)) { // check if the primary expression is local variable (object)
            int var2 = counter++;
            writer.write("  %_" + var1 + " = load " + type + ", " + type + "* %" + expr + "\n");
            writer.write("  %_" + var2 + " = bitcast i8* %_" + var1 + " to i8***\n");
            varsStack.add(var1);
        }
        else if (!curMethod.equals("main") && symbolTable.table.get(curClass).allClassVars.containsKey(expr)
                 && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)) { // check if the primary expression is class variable (object)
            
            String idType = symbolTable.getType(curClass, curMethod, expr);
            
            expr=toGetClassVar(expr);
            int var3 = counter++;
            int var4 = counter++;

            writer.write("  %_" + var3 + " = load " + getLLVMType(idType) + ", " + getLLVMType(idType) + "* %" + expr + "\n");
            var1=var3;
            writer.write("  %_" + var4 + " = bitcast i8* %_" + var3 + " to i8***\n");
            varsStack.add(var1);
        }
        else if(varTmp==-1 && symbolTable.table.containsKey(expr)){ // check if the primary expression is "this"
            thisUse=true;
            writer.write("  %_" + var1 + " = bitcast i8* %this to i8***\n");
        }
        else if (varTmp != -1) { // check if the primary expression is an object allocation expression ( "new ...()" )
            varTmpStack.add(varTmp);
            writer.write("  %_" + var1 + " = bitcast i8* %_" + varTmp + " to i8***\n");
        }

        int var3 = counter++;
        int var3prev = var3-1;
        writer.write("  %_" + var3 + " = load i8**, i8*** %_" + var3prev + "\n");
        
        String parentClassName;
        List<String> methodArgsList;
        int offset;
        String methodType;
        // get the method's type, the corresponding offset and the arguments list
        if ((parentClassName = symbolTable.getDeclarationClassOfMethod(className, methodName)) != null) {  // check for method overriding
            // yes, the method in child class is overriding method
            methodType = symbolTable.table.get(parentClassName).methods.get(methodName).methodType;
            offset = Integer.parseInt(symbolTable.table.get(parentClassName).methodsOffset.get(methodName))/8;
            methodArgsList = symbolTable.table.get(parentClassName).methods.get(methodName).args;
        } 
        else{
            // no
            methodType = symbolTable.table.get(className).methods.get(methodName).methodType;
            offset = Integer.parseInt(symbolTable.table.get(className).methodsOffset.get(methodName))/8;
            methodArgsList = symbolTable.table.get(className).methods.get(methodName).args;
        }

        type = getLLVMType(methodType);
        int var4=counter++;
        writer.write("  %_"+ var4 + " = getelementptr i8*, i8** %_" + var3 + ", i32 "+ offset+"\n");

        int var5=counter++;
        writer.write("  %_" + var5 + " = load i8*, i8** %_" + var4 + "\n");

        int var6 = counter++;
        writer.write("  %_" + var6 + " = bitcast i8* %_" + var5 + " to " + type + " (i8*");

        if (symbolTable.table.get(className).methodsVtable.get(methodName).args.size() != 0) { // check if this method call has arguments
            // yes, it has
            int argsCounter = 0;
            writer.write(",");
            for (String argType : symbolTable.table.get(className).methodsVtable.get(methodName).args) { // write the LLVM type of every argument
                argsCounter++;
                writer.write(getLLVMType(argType));
                if (argsCounter != symbolTable.table.get(className).methodsVtable.get(methodName).args.size())
                    writer.write(",");
            }
        }
        writer.write(")* \n");
        

        String[] args;
        String str=""; // to store the LLVM arguments of the method call
        args = null;

        queueBool.clear();

        String argsStrList = n.f4.accept(this, argu); // take the arguments from the method call
        if (argsStrList != null)
            args = argsStrList.split(",");

        if (argsStrList != null)
            for (int i = 0; i < args.length; i++) { // for every argument
                if(symbolTable.table.get(curClass).methods.get(curMethod).varNum.containsKey(args[i])){ // check if the argument is local variable
                    int var=counter++;
                    writer.write("  %_" + var + " = load " + getLLVMType(methodArgsList.get(i)) + ", " + getLLVMType(methodArgsList.get(i)) + "* %" + args[i] + "\n");
                    str += ", " + getLLVMType(methodArgsList.get(i)) + " %_" + var; // add the local variable      
                    continue;     
                }
                else if(args[i].equals("boolean")){ // check if the argument is boolean literal
                    str += ", " + getLLVMType(methodArgsList.get(i)) + " " + queueBool.remove(); // add the boolean literal
                    continue;
                }
                else if (!curMethod.equals("main") && symbolTable.table.get(curClass).allClassVars.containsKey(args[i]) 
                        && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(args[i])) { // check if the argument is class variable

                    String idType = symbolTable.getType(curClass, curMethod, args[i]);
       
                    args[i]=toGetClassVar(args[i]);
                    int var3Temp = counter++;

                    writer.write("  %_" + var3Temp + " = load " + getLLVMType(idType) + ", " + getLLVMType(idType) + "* %" + args[i] + "\n");
                    str += ", " + getLLVMType(methodArgsList.get(i)) + " %_" + var3Temp; // add the class variable
                    continue;
                }
                else if (thisFlag){ // check if the argument is "this"
                    str += ", " + getLLVMType(methodArgsList.get(i)) + " %this"; // add "this"
                    continue;
                }

                str+= ", " + getLLVMType(methodArgsList.get(i)) + " " + args[i]; // add the corresponding LLVM argument
            }


        int var7 = counter++;
        if(thisUse){ // check if the "this" used for a class method call
            writer.write("  %_" + var7 + " = call " + type + " %_" + var6 + "(i8* %this" + str + ")\n");
            if (!varTmpStack.isEmpty())
                varTmpStack.pop();
        }
        else if(!varTmpStack.isEmpty()){ // check for object allocation method call
            writer.write("  %_" + var7 + " = call " + type + " %_" + var6 + "(i8* %_" + varTmpStack.peek() + str + ")\n");
            if (!varTmpStack.isEmpty())
                varTmpStack.pop();
        }
        else { // "normal" method call
            writer.write("  %_" + var7 + " = call " + type + " %_" + var6 + "(i8* %_" + varsStack.peek() + str + ")\n");
            if (!varsStack.isEmpty())
                varsStack.pop();
        }
        writer.write("\n");

        // save class name
        if (className != null)
            savePreviousClassName=className;

        varTmp=var7; // store the LLVM variable, can be used in another visitor
        thisFlag=false;
        return "%_"+Integer.toString(var7); 
    }

    /**
     * f0 -> IntegerLiteral() | TrueLiteral() | FalseLiteral() | Identifier() |
     * ThisExpression() | ArrayAllocationExpression() | AllocationExpression() |
     * BracketExpression()
     */
    @Override
    public String visit(PrimaryExpression n, Void argu) throws Exception {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> "new" 
     * f1 -> "int" 
     * f2 -> "[" 
     * f3 -> Expression() 
     * f4 -> "]"
     */
    @Override
    public String visit(ArrayAllocationExpression n, Void argu) throws Exception {
        varTmp=-1;

        String expr = n.f3.accept(this, argu);
        expr=toGetClassVar(expr);

        int var1=counter++;
        int varSize=-1;
        int arraySize=0;

        if(varTmp!=-1){ // expression
            writer.write("  %_"+var1+" = add i32 1, %_"+ varTmp +"\n");
        }
        else if (isStringInt(expr)){ // integer literal
            writer.write("  %_"+var1+" = add i32 1, "+expr+"\n");
            arraySize=Integer.parseInt(expr);
        }
        else{ // class variable
            int var2=counter++;
            writer.write("  %_" + var1 + " = load i32, i32* %" + expr + "\n");
            writer.write("  %_" + var2 + " = add i32 1, %_" + var1 + "\n");
            varSize=var1; // variable that holds the array size
            var1=var2;
        }
        
        int var2=counter++;
        writer.write("  %_"+var2+" = icmp sge i32 %_"+var1+", 1\n");
        int saveGoto=gotoCounter++;

        writer.write("  br i1 %_" + var2 + ", label %nsz_ok_" + saveGoto + ", label %nsz_err_" + saveGoto + ""+"\n\n");

        writer.write("  nsz_err_" + saveGoto + ":"+"\n");
        writer.write("  call void @throw_nsz()"+"\n");

        writer.write("  br label %nsz_ok_" + saveGoto + ""+"\n\n");

        writer.write("  nsz_ok_" + saveGoto + ":"+"\n");
        int var3=counter++;
        writer.write("  %_" + var3 + " = call i8* @calloc( i32 4, i32 %_" + var1 + ")"+"\n");
        int var4=counter++;
        writer.write("  %_" + var4 + " = bitcast i8* %_" + var3 + " to i32*"+"\n");

        if(varTmp!=-1){ // expression
            writer.write("  store i32 %_" + varTmp + ", i32* %_" + var4 + "\n\n");
        }
        else if (isStringInt(expr)) { // integer literal
            writer.write("  store i32 " + arraySize + ", i32* %_" + var4 + "\n\n");
        }
        else{ // class variable
            writer.write("  store i32 %_" + varSize + ", i32* %_" + var4 + "\n\n");
        }

        intReturn = -1;
        booleanReturn = -1;
        thisFlag = false;
        varTmp=var4; // store the LLVM variable, can be used in another visitor
        
        return "%_"+Integer.toString(var4);
    }




    /**
     * f0 -> AndExpression() | CompareExpression() | PlusExpression() |
     * MinusExpression() | TimesExpression() | ArrayLookup() | ArrayLength() |
     * MessageSend() | Clause()
     */

    @Override
    public String visit(Expression n, Void argu) throws Exception {
        booleanReturn=-1;
        String expr=n.f0.accept(this, null);
        if (expr!=null && expr.equals("boolean") && booleanReturn!=-1) {
            queueBool.add(booleanReturn); // store boolean literals for the argument list
        }
        return expr;
    }


    /**
     * f0 -> Expression() f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, Void argu) throws Exception {

        String parList = n.f0.accept(this, null);

        if (n.f1 != null) {
            parList += n.f1.accept(this, null);
        }
        return parList;
    }


    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, Void argu) throws Exception {
        String toReturn = "";
        for (Node node : n.f0.nodes) {
            String id = node.accept(this, null);
            
            toReturn += "," + id;
        }

        return toReturn;
    }


    /**
     * f0 -> "," f1 -> Expression()
     */
    public String visit(ExpressionTerm n, Void argu) throws Exception {
        return n.f1.accept(this, argu);
    }


    /**
     * f0 -> "new" 
     * f1 -> Identifier() 
     * f2 -> "(" 
     * f3 -> ")"
     */
    @Override
    public String visit(AllocationExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        String className = n.f1.accept(this, argu);
        // let's find the field size of the class
        int fieldSize = 8 ; // "this" --> 8
        int methodsNum = symbolTable.table.get(className).methodsVtable.size();

        String lastVar=null;
        for (String varName : symbolTable.table.get(className).vars.keySet()) { // find the last class variable (if it exists)
            lastVar=varName;
        }

        if(lastVar!=null){
            fieldSize+=Integer.parseInt(symbolTable.table.get(className).varsOffset.get(lastVar)); // add the offset of the last class variable
            // and finally add the size of the last class variable
            if (symbolTable.table.get(className).vars.get(lastVar).equals("int")) {
                fieldSize += 4;
            } 
            else if (symbolTable.table.get(className).vars.get(lastVar).equals("boolean")) {
                fieldSize += 1;
            } 
            else {
                fieldSize += 8;
            }
        }


        writer.write("\n");
        writer.write("  %_" + counter++ + " = " + "call" + " i8* " + "@calloc(i32 1,i32 " + fieldSize + ")\n");
        int var1=counter-1;
        writer.write("  %_" + counter++ + " = " + "bitcast" + " i8* %_" + var1+" to i8***\n");
        int var2=counter-1;
        writer.write("  %_" + counter++ + " = getelementptr [" + methodsNum + " x i8*], [" + methodsNum + " x i8*]* @." + className + "_vtable, i32 0, i32 0\n");
        int var3=counter-1;
        writer.write("  store i8** %_" + var3 + ", i8*** %_" + var2 + "\n");
        

        varTmp=var1; // store the LLVM variable, can be used in another visitor
        allocationClassName=className;
        return "%_"+Integer.toString(varTmp);
    }

       
    /**
     * f0 -> Identifier() 
     * f1 -> "=" 
     * f2 -> Expression() 
     * f3 -> ";"
     */
    @Override
    public String visit(AssignmentStatement n, Void argu) throws Exception {
        varTmp=-1;
        intReturn = -1;
        booleanReturn = -1;
        thisFlag=false;

        String id = n.f0.accept(this, argu);
        String expr = n.f2.accept(this, argu);
        String idType = null;

        if(id!=null){
            idType = symbolTable.getType(curClass, curMethod, id);
            id=toGetClassVar(id);
        }
        
        if(expr!=null)
            expr=toGetClassVar(expr);
        
        if(thisFlag)
            expr="%this";
            


        if (varTmp!=-1 && expr!=null && (!isStringInt(expr) || intReturn==-1) && ( !expr.equals("boolean") || booleanReturn==-1 ) && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)){
            // class variable
            id=toGetClassVar(id);
            writer.write("  store "+ getLLVMType(idType) +" %_" + varTmp + ", "+ getLLVMType(idType) +"* %"+ id + "\n\n");
        }
        else {
            if (expr!=null){
                if (thisFlag){ // "this"
                    writer.write("  store i8* %this, i8** %" + id +"\n");
                }
                else {
                    if (isStringInt(expr) || (expr.equals("boolean") && booleanReturn!=-1 )) {

                        if(isStringInt(expr)) // integer literal
                            writer.write("  store "+ getLLVMType(idType) +" " + expr + ", "+ getLLVMType(idType) +"* %"+ id + "\n\n");
                        else // boolean literal
                            writer.write("  store "+ getLLVMType(idType) +" " + booleanReturn + ", "+ getLLVMType(idType) +"* %"+ id + "\n\n");
                    
                    }
                    else{ // variable 
                        int var=counter++;
                        
                        writer.write("  %_" + var + " = load " + getLLVMType(idType) + ", " + getLLVMType(idType) + "* %" + expr + "\n");
                        writer.write("  store "+ getLLVMType(idType) +" %_" + var + ", "+ getLLVMType(idType) +"* %"+ id + "\n\n");
                    }
                }
            }
        }
        
        varTmp = -1;
        thisFlag = false;
        intReturn=-1;
        booleanReturn=-1;
        
        return null;
    }
    
    /**
     * f0 -> "System.out.println" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> ";"
     */
    @Override
    public String visit(PrintStatement n, Void argu) throws Exception {
        varTmp=-1;
        thisFlag = false;
        intReturn = -1;
        booleanReturn = -1;
        String expr = n.f2.accept(this, argu);
        expr=toGetClassVar(expr);

        if(varTmp!=-1 && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)){ // expression
            writer.write("  call void (i32) @print_int(i32 %_" + varTmp + ")\n\n");     
        }
        else{
            if (isStringInt(expr)) { // integer literal
                writer.write("  call void (i32) @print_int(i32 " + expr + ")\n\n");
            }
            else{ //variable
                int var1=counter++;
                
                writer.write("  %_"+var1+ "= load i32, i32* %" + expr + "\n");
                writer.write("  call void (i32) @print_int(i32 %_" + var1 + ")\n\n");
            }
            
        }

        varTmp=-1;
        thisFlag = false;
        intReturn = -1;
        booleanReturn = -1;

        return null;
    }
    

    
    /**
     * f0 -> "(" f1 -> Expression() f2 -> ")"
     */
    @Override
    public String visit(BracketExpression n, Void argu) throws Exception {
        varTmp=-1;
        String expr = n.f1.accept(this, argu);
        return expr; // just return the expression between the brackets
    }


    /**
     * f0 -> Identifier() 
     * f1 -> "[" 
     * f2 -> Expression() 
     * f3 -> "]" 
     * f4 -> "=" 
     * f5 -> Expression() 
     * f6 -> ";"
     */
    @Override
    public String visit(ArrayAssignmentStatement n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        String id = n.f0.accept(this, argu);
        id=toGetClassVar(id);

        String index = n.f2.accept(this, argu);

        if (varTmp != -1) // expression 
            index = "%_"+Integer.toString(varTmp);

        if (symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(index)) { // local variable
            int var = counter++;
            writer.write("  %_" + var + " = load i32, i32* %" + index + "\n");
            index = "%_" + Integer.toString(var);
        }

        if (!curMethod.equals("main") && symbolTable.table.get(curClass).allClassVars.containsKey(index) && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(index)) {
            // class variable

            String idType = symbolTable.getType(curClass, curMethod, index); 

            index=toGetClassVar(index);
            int var3 = counter++;

            writer.write("  %_" + var3 + " = load " + getLLVMType(idType) + ", " + getLLVMType(idType) + "* %" + index + "\n");
            index = "%_" + Integer.toString(var3);            
        }

        varTmp = -1;
        thisFlag = false;

        String expr = n.f5.accept(this, argu);

        if (varTmp != -1) // expression
            expr = "%_" + Integer.toString(varTmp);

        if (symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)) { // local variable
            int var = counter++;
            writer.write("  %_" + var + " = load i32, i32* %" + expr + "\n");
            expr = "%_" + Integer.toString(var);
        }

        if (!curMethod.equals("main") && symbolTable.table.get(curClass).allClassVars.containsKey(expr) && !symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(expr)) {
            // class variable
            String idType = symbolTable.getType(curClass, curMethod, expr);
            
            expr = toGetClassVar(expr);
            int var3 = counter++;

            writer.write("  %_" + var3 + " = load " + getLLVMType(idType) + ", " + getLLVMType(idType) + "* %" + expr + "\n");
            expr = "%_" + Integer.toString(var3);
        }
                

        int var1=counter++;
        int var2=counter++;
        writer.write("  %_"+ var1 +" = load i32*, i32** %" + id + "\n");
        writer.write("  %_" + var2 + " = load i32, i32* %_"+ var1 +"\n");
        int var3=counter++;
        writer.write("  %_" + var3 + " = icmp sge i32 " + index + ", 0\n");
        int var4=counter++;
        writer.write("  %_" + var4 + " = icmp slt i32 " + index + ", %_"+ var2 +"\n");
        int var5=counter++;
        writer.write("  %_" + var5 + " = and i1 %_" + var3 + ", %_" + var4 +"\n");
        int var6=counter++;
        int saveGoto=gotoCounter++;

        writer.write("  br i1 %_" + var5 + ", label %oob_ok_" + saveGoto + ", label %oob_err_" + saveGoto +"\n\n");
        writer.write("  oob_err_" + saveGoto + ":" + "\n");
        writer.write("  call void @throw_oob()"+"\n");
        writer.write("  br label %oob_ok_" + saveGoto + ""+"\n\n");
        writer.write("  oob_ok_" + saveGoto + ":" + "\n");        
        int var7=counter++;
        writer.write("  %_" + var6 + " = add i32 1, " + index + "\n");
        writer.write("  %_" + var7 + " = getelementptr i32, i32* %_" + var1 + ", i32 %_" + var6 + "\n");
        writer.write("  store i32 " + expr + ", i32* %_" + var7 + "\n");
        
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        return null;
    }

    /**
     * f0 -> PrimaryExpression() 
     * f1 -> "[" 
     * f2 -> PrimaryExpression() 
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        String array = n.f0.accept(this, argu);
        if (varTmp != -1) // expression
            array = "_" + Integer.toString(varTmp);

        int temp = varTmp;
        array = toGetClassVar(array);

        int var1 = counter++;
        int var2 = counter++;

        varTmp = -1;
        thisFlag = false;
        String index = n.f2.accept(this, argu);

        if (varTmp != -1) // expression
            index = "%_" + Integer.toString(varTmp);

        index = toGetClassVar(index);

        if (temp == -1) { // variable
            writer.write("  %_" + var1 + " = load i32*, i32** %" + array + "\n");
            writer.write("  %_" + var2 + " = load i32, i32* %_" + var1 + "\n");
        } else { // expression
            writer.write("  %_" + var2 + " = load i32, i32* %" + array + "\n");
        }

        if (symbolTable.table.get(curClass).methods.get(curMethod).vars.containsKey(index)) { // local variable
            int var = counter++;
            writer.write("  %_" + var + " = load i32, i32* %" + index + "\n");
            index = "%_" + Integer.toString(var);
        }

        int var4 = counter++;
        writer.write("  %_" + var4 + " = icmp sge i32 " + index + ", 0\n");
        int var5 = counter++;
        writer.write("  %_" + var5 + " = icmp slt i32 " + index + ", %_" + var2 + "\n");
        int var6 = counter++;
        writer.write("  %_" + var6 + " = and i1 %_" + var4 + ", %_" + var5 + "\n");
        int saveGoto = gotoCounter++;
        writer.write("  br i1 %_" + var6 + ", label %oob_ok_" + saveGoto + ", label %oob_err_" + saveGoto + "\n\n");
        writer.write("  oob_err_" + saveGoto + ":" + "\n");
        writer.write("  call void @throw_oob()\n");
        writer.write("  br label %oob_ok_" + saveGoto + "\n\n");
        writer.write("  oob_ok_" + saveGoto + ":" + "\n");
        int var7 = counter++;
        writer.write("  %_" + var7 + " = add i32 1, " + index + "\n");
        int var8 = counter++;

        if (temp == -1) { // variable
            writer.write("  %_" + var8 + " = getelementptr i32, i32* %_" + var1 + ", i32 %_" + var7 + "\n");
            int var9 = counter++;
            writer.write("  %_" + var9 + " = load i32, i32* %_" + var8 + "\n\n");
            varTmp = var9; // store the LLVM variable, can be used in another visitor
        } else { // expression
            writer.write("  %_" + var8 + " = getelementptr i32, i32* %_" + temp + ", i32 %_" + var7 + "\n");
            int var9 = counter++;
            writer.write("  %_" + var9 + " = load i32, i32* %_" + var8 + "\n\n");
            varTmp = var9; // store the LLVM variable, can be used in another visitor
        }

        intReturn = -1;
        booleanReturn = -1;

        return "%_" + Integer.toString(varTmp);
    }

    /**
     * f0 -> PrimaryExpression() f1 -> "." f2 -> "length"
     */
    @Override
    public String visit(ArrayLength n, Void argu) throws Exception {
        // array's length/size has been stored at his first position
        varTmp = -1;
        thisFlag = false;
        String array = n.f0.accept(this, argu);
        
        array=toGetClassVar(array);
        if(varTmp!=-1){ // expression
            int var1 = counter++;
            writer.write("  %_" + var1 + "= load i32, i32* %_" + varTmp + "\n");
            varTmp = var1; // store the LLVM variable, can be used in another visitor
        }
        else{ // variable
            int var1 = counter++;
            int var2 = counter++;
            int var3 = counter++;
            writer.write("  %_" + var1 + "= load i32*, i32** %"+array+"\n");
            writer.write("  %_" + var2 + "= getelementptr i32, i32* %_" + var1 + ", i32 0\n");
            writer.write("  %_" + var3 + "= load i32, i32* %_" + var2 + "\n");
            varTmp = var3; // store the LLVM variable, can be used in another visitor
        }

        booleanReturn = -1;
        intReturn = -1;
        return "%_" + Integer.toString(varTmp);
    }

    /**
     * f0 -> "!" f1 -> Clause()
     */
    @Override
    public String visit(NotExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn=-1;
        String clause = n.f1.accept(this, argu);
        clause=toGetClassVar(clause);
        
        int var1=counter++;
        if(varTmp!=-1){ // expression 
            writer.write("  %_" + var1 + " = add i1 1, %_" + varTmp + "\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
        }
        else if (clause.equals("boolean")){ // boolean literal
            writer.write("  %_" + var1 + " = add i1 1, " + booleanReturn + "\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
        }
        else{ // variable
            int var2=counter++;
            writer.write("  %_" + var1 + " = load i1, i1* %" + clause + "\n");
            writer.write("  %_" + var2 + " = add i1 1, %_" + var1 + "\n");
            varTmp=var2; // store the LLVM variable, can be used in another visitor
        }


        booleanReturn = -1;
        return "boolean";
    }

    /**
     * f0 -> Clause() f1 -> "&&" f2 -> Clause()
     */
    @Override
    public String visit(AndExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        
        String leftExpr = n.f0.accept(this, argu);
        leftExpr=toGetClassVar(leftExpr);
        
        int save1=gotoCounter++;
        int save2=gotoCounter++;
        int save3=gotoCounter++;
        int save4=gotoCounter++;

        if(varTmp!=-1){ // expression
            writer.write("  br i1 %_" + varTmp + ", label %exp_res_" + save2 + ", label %exp_res_" + save1 + "\n");
        }
        else if (leftExpr != null && leftExpr.equals("boolean")){ // boolean literal
            writer.write("  br i1 " + booleanReturn + ", label %exp_res_" + save2 + ", label %exp_res_" + save1 + "\n");
        }
        else{ // variable
            int var1 = counter++;
            writer.write("  %_" + var1 + " = load i1, i1* %" + leftExpr + "\n");
            writer.write("  br i1 %_" + var1 + ", label %exp_res_" + save2 + ", label %exp_res_" + save1 + "\n\n");
        }

        writer.write("  exp_res_" + save1 + ":\n");
        writer.write("  br label %exp_res_" + save4 + "\n\n");
                
        
        n.f1.accept(this, argu);
        
        
        writer.write("  exp_res_" + save2 + ":\n");
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        String rightExpr = n.f2.accept(this, argu);
        rightExpr = toGetClassVar(rightExpr);
        
        int var = counter++;
        if (varTmp == -1 && rightExpr != null && !rightExpr.equals("boolean")){ // variable
            writer.write("  %_" + var + " = load i1, i1* %" + rightExpr + "\n");
        }

        writer.write("  br label %exp_res_" + save3 + "\n\n");
        
        
        writer.write("  exp_res_" + save3 + ":\n");
        writer.write("  br label %exp_res_" + save4 + "\n\n");
        
        
        writer.write("  exp_res_" + save4 + ":\n");

        if(varTmp!=-1){ // expression 
            int var1 = counter++;
            writer.write("  %_" + var1 + " = phi i1  [ 0, %exp_res_" + save1 + " ], [ %_" + varTmp + ", %exp_res_" + save3 + " ]\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
            
        }
        else if (rightExpr!=null && rightExpr.equals("boolean")){ // boolean literal
            int var1 = counter++;
            writer.write("  %_" + var1 + " = phi i1  [ 0, %exp_res_" + save1 + " ], [ "+ booleanReturn + ", %exp_res_" + save3 + " ]\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
        }
        else{ // variable
            int var1 = counter++;
            writer.write("  %_" + var1 + " = phi i1  [ 0, %exp_res_" + save1 + " ], [ %_" + var + ", %exp_res_" + save3 + " ]\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
        }

        booleanReturn = -1;
        intReturn = -1;


        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression() f1 -> "<" f2 -> PrimaryExpression()
     */
    @Override
    public String visit(CompareExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        String leftExpr = n.f0.accept(this, argu);
        leftExpr = toGetClassVar(leftExpr);
        int varSaveTmp1 = varTmp;

        n.f1.accept(this, argu);
        varTmp = -1;
        thisFlag = false;
        String rightExpr = n.f2.accept(this, argu);
        rightExpr = toGetClassVar(rightExpr);
        int varSaveTmp2 = varTmp;

        if (varSaveTmp1 != -1 && varSaveTmp2 != -1) { // left and right are expressions
            int var1 = counter++;
            writer.write("  %_" + var1 + " = icmp slt i32 %_" + varSaveTmp1 + ", %_" + varSaveTmp2 + "\n");
            varTmp = var1; // store the LLVM variable, can be used in another visitor
        } else if (varSaveTmp1 != -1 && varSaveTmp2 == -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(rightExpr)) { // integer literal
                writer.write("  %_" + var1 + " = icmp slt i32 %_" + varSaveTmp1 + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = icmp slt i32 %_" + varSaveTmp1 + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } else if (varSaveTmp1 == -1 && varSaveTmp2 != -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(leftExpr)) { // integer literal
                writer.write("  %_" + var1 + " = icmp slt i32 " + leftExpr + ", %_" + varSaveTmp2 + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
                writer.write("  %_" + var2 + " = icmp slt i32 %_" + var1 + ", %_" + varSaveTmp2 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } else {
            int var1 = counter++;
            if (isStringInt(rightExpr) && isStringInt(leftExpr)) { // left and right are integer literals
                writer.write("  %_" + var1 + " = icmp slt i32 " + leftExpr + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else if (isStringInt(rightExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
                writer.write("  %_" + var2 + " = icmp slt i32 %_" + var1 + ", " + rightExpr + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } else if (isStringInt(leftExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = icmp slt i32 " + leftExpr + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } else { // left and right are variables
                int var2 = counter++;
                int var3 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
                writer.write("  %_" + var2 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var3 + " = icmp slt i32 %_" + var1 + ", %_" + var2 + "\n");
                varTmp = var3; // store the LLVM variable, can be used in another visitor
            }
        }
        booleanReturn = -1;
        intReturn = -1;

        return "boolean";
    }

    /**
     * f0 -> PrimaryExpression() f1 -> "+" f2 -> PrimaryExpression()
     */
    @Override
    public String visit(PlusExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        String leftExpr = n.f0.accept(this, argu);
        leftExpr = toGetClassVar(leftExpr);

        int varSaveTmp1 = varTmp;
        int hold=-1;
        if (varSaveTmp1 == -1 && !isStringInt(leftExpr)){ // variable
            int var1 = counter++;
            writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
            hold=var1;
        }
            

        n.f1.accept(this, argu);
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        String rightExpr = n.f2.accept(this, argu);
        rightExpr = toGetClassVar(rightExpr);
        int varSaveTmp2 = varTmp;

        if (varSaveTmp1 != -1 && varSaveTmp2 != -1) { // left and right are expressions
            int var1 = counter++;
            writer.write("  %_" + var1 + " = add i32 %_" + varSaveTmp1 + ", %_" + varSaveTmp2 + "\n");
            varTmp = var1; // store the LLVM variable, can be used in another visitor
        } 
        else if (varSaveTmp1 != -1 && varSaveTmp2 == -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(rightExpr)) { // integer literal
                writer.write("  %_" + var1 + " = add i32 %_" + varSaveTmp1 + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = add i32 %_" + varSaveTmp1 + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } 
        else if (varSaveTmp1 == -1 && varSaveTmp2 != -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(leftExpr)) { // integer literal
                writer.write("  %_" + var1 + " = add i32 " + leftExpr + ", %_" + varSaveTmp2 + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var2 + " = add i32 %_" + hold + ", %_" + varSaveTmp2 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } 
        else {
            int var1 = counter++;
            if (isStringInt(rightExpr) && isStringInt(leftExpr)) { // left and right are integer literals
                writer.write("  %_" + var1 + " = add i32 " + leftExpr + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } 
            else if (isStringInt(rightExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var2 + " = add i32 %_" + hold + ", " + rightExpr + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } 
            else if (isStringInt(leftExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = add i32 " + leftExpr + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } 
            else { // left and right are variables
                int var2 = counter++;
                int var3 = counter++;
                writer.write("  %_" + var2 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var3 + " = add i32 %_" + hold + ", %_" + var2 + "\n");
                varTmp = var3; // store the LLVM variable, can be used in another visitor
            }
        }
        booleanReturn = -1;
        intReturn = -1;
        thisFlag = false;

        return "%_" +Integer.toString(varTmp);
    }

    /**
     * f0 -> PrimaryExpression() f1 -> "-" f2 -> PrimaryExpression()
     */
    @Override
    public String visit(MinusExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;
        String leftExpr = n.f0.accept(this, argu);
        leftExpr=toGetClassVar(leftExpr);
        
        int varSaveTmp1 = varTmp;
        int hold = -1;
        if (varSaveTmp1 == -1 && !isStringInt(leftExpr)) {
            int var1 = counter++;
            writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
            hold = var1;
        }
        

        
        n.f1.accept(this, argu);
        varTmp = -1;
        thisFlag = false;
        String rightExpr = n.f2.accept(this, argu);
        rightExpr=toGetClassVar(rightExpr);
        int varSaveTmp2 = varTmp;

        if (varSaveTmp1 != -1 && varSaveTmp2 != -1) { // left and right are expressions
            int var1 = counter++;
            writer.write("  %_" + var1 + " = sub i32 %_" + varSaveTmp1 + ", %_" + varSaveTmp2 + "\n");
            varTmp = var1; // store the LLVM variable, can be used in another visitor
        } else if (varSaveTmp1 != -1 && varSaveTmp2 == -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(rightExpr)) { // integer literal
                writer.write("  %_" + var1 + " = sub i32 %_" + varSaveTmp1 + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = sub i32 %_" + varSaveTmp1 + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } else if (varSaveTmp1 == -1 && varSaveTmp2 != -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(leftExpr)) { // integer literal
                writer.write("  %_" + var1 + " = sub i32 " + leftExpr + ", %_" + varSaveTmp2 + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var2 + " = sub i32 %_" + hold + ", %_" + varSaveTmp2 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        } else {
            int var1 = counter++;
            if (isStringInt(rightExpr) && isStringInt(leftExpr)) { // left and right are integer literals
                writer.write("  %_" + var1 + " = sub i32 " + leftExpr + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else if (isStringInt(rightExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var2 + " = sub i32 %_" + hold + ", " + rightExpr + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } else if (isStringInt(leftExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = sub i32 " + leftExpr + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } else { // left and right are variables
                int var2 = counter++;
                int var3 = counter++;
                writer.write("  %_" + var2 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var3 + " = sub i32 %_" + hold + ", %_" + var2 + "\n");
                varTmp = var3; // store the LLVM variable, can be used in another visitor
            }
        }

        booleanReturn = -1;
        intReturn = -1;
        thisFlag = false;

        return "%_" + Integer.toString(varTmp);
    }

     /**
     * f0 -> PrimaryExpression() 
     * f1 -> "*" 
     * f2 -> PrimaryExpression()
     */
    @Override
    public String visit(TimesExpression n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        intReturn = -1;

        String leftExpr = n.f0.accept(this, argu);
        leftExpr=toGetClassVar(leftExpr);
        int varSaveTmp1=varTmp;
        int hold = -1;
        
        if (varSaveTmp1 == -1 && !isStringInt(leftExpr)) { // class variable
            int var1 = counter++;
            writer.write("  %_" + var1 + " = load i32, i32* %" + leftExpr + "\n");
            hold = var1;
        }
        
        n.f1.accept(this, argu);
        varTmp = -1;
        thisFlag = false;

        String rightExpr = n.f2.accept(this, argu);
        rightExpr=toGetClassVar(rightExpr);
        int varSaveTmp2=varTmp;

        if(varSaveTmp1!=-1 && varSaveTmp2!=-1){ // left and right are expressions
            int var1 =  counter++;
            writer.write("  %_" + var1 + " = mul i32 %_"+varSaveTmp1+", %_"+varSaveTmp2 + "\n");
            varTmp=var1; // store the LLVM variable, can be used in another visitor
        }
        else if(varSaveTmp1 != -1 && varSaveTmp2==-1){
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(rightExpr)) { // integer literal
                writer.write("  %_" + var1 + " = mul i32 %_" + varSaveTmp1 + ", " + rightExpr + "\n");
                varTmp = var1;  // store the LLVM variable, can be used in another visitor
            }
            else{ // variable
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = mul i32 %_" + varSaveTmp1 + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        }
        else if (varSaveTmp1 == -1 && varSaveTmp2 != -1) {
            int var1 = counter++;
            int var2 = counter++;
            if (isStringInt(leftExpr)) { // integer literal
                writer.write("  %_" + var1 + " = mul i32 " + leftExpr + ", %_" + varSaveTmp2 + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } else { // variable
                writer.write("  %_" + var2 + " = mul i32 %_" + hold + ", %_" + varSaveTmp2 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
        }
        else{
            int var1 = counter++;
            if (isStringInt(rightExpr) && isStringInt(leftExpr)) { // left and right are integer literals
                writer.write("  %_" + var1 + " = mul i32 " + leftExpr + ", " + rightExpr + "\n");
                varTmp = var1; // store the LLVM variable, can be used in another visitor
            } 
            else if (isStringInt(rightExpr)) { // integer literal
                int var2 = counter++;
                writer.write("  %_" + var2 + " = mul i32 %_" + hold + ", " + rightExpr + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            } 
            else if (isStringInt(leftExpr)){ // integer literal
                int var2 = counter++;
                writer.write("  %_" + var1 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var2 + " = mul i32 " + leftExpr + ", %_" + var1 + "\n");
                varTmp = var2; // store the LLVM variable, can be used in another visitor
            }
            else{ // left and right are variables
                int var2 = counter++;
                int var3 = counter++;
                writer.write("  %_" + var2 + " = load i32, i32* %" + rightExpr + "\n");
                writer.write("  %_" + var3 + " = mul i32 %_" + hold + ", %_" + var2 + "\n");
                varTmp = var3; // store the LLVM variable, can be used in another visitor
            }
        }

        booleanReturn = -1;
        intReturn = -1;
        thisFlag = false;
        
        return "%_"+Integer.toString(varTmp);
    }


    /**
     * f0 -> "if" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement() 
     * f5 ->"else" 
     * f6 -> Statement()
     */
    @Override
    public String visit(IfStatement n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn=-1;
        n.f1.accept(this, argu);
        String expr=n.f2.accept(this, argu);
        expr=toGetClassVar(expr);
        
        int saveGoto = gotoCounter++;
        if (varTmp!=-1) // expression
            writer.write("  br i1 %_" + varTmp + ", label %if_then_" + saveGoto + ", label %if_else_" + saveGoto + "\n\n");
        else if(booleanReturn!=-1) // boolean literal
            writer.write("  br i1 " + booleanReturn + ", label %if_then_" + saveGoto + ", label %if_else_" + saveGoto + "\n\n");
        else{ // variable
            int var=counter++;
            writer.write("  %_" + var + " = load i1, i1* %" + expr + "\n\n");
            writer.write("  br i1 %_" + var + ", label %if_then_" + saveGoto + ", label %if_else_" + saveGoto + "\n\n");

        }

        
        writer.write("  if_then_" + saveGoto + ":\n");
        n.f4.accept(this, argu);
        writer.write("  br label %if_end_" + saveGoto + "\n\n");

        writer.write("  if_else_" + saveGoto + ":\n");
        n.f5.accept(this, argu);
        n.f6.accept(this, argu);
        writer.write("  br label %if_end_" + saveGoto + "\n\n");
        
        
        writer.write("  if_end_" + saveGoto + ":\n");

        varTmp = -1;
        thisFlag = false;
        booleanReturn=-1;

        return null;
    }

    /**
     * f0 -> "while" 
     * f1 -> "(" 
     * f2 -> Expression() 
     * f3 -> ")" 
     * f4 -> Statement()
     */
    @Override
    public String visit(WhileStatement n, Void argu) throws Exception {
        varTmp = -1;
        thisFlag = false;
        booleanReturn = -1;
        int saveGoto = gotoCounter++;
        writer.write("  br label %label_" + saveGoto +"\n\n");
        writer.write("  label_" + saveGoto + ":\n");
        String expr = n.f2.accept(this, argu);
        expr = toGetClassVar(expr);
        
        if (varTmp != -1) // expression
            writer.write("  br i1 %_" + varTmp + ", label %while_" + saveGoto + ", label %end_" + saveGoto + "\n\n");
        else if (booleanReturn != -1) // boolean literal
            writer.write("  br i1 " + booleanReturn + ", label %while_" + saveGoto + ", label %end_" + saveGoto + "\n\n");
        else { // variable
            int var = counter++;
            writer.write("  %_" + var + " = load i1, i1* %" + expr + "\n\n");
            writer.write("  br i1 %_" + var + ", label %while_" + saveGoto + ", label %end_" + saveGoto + "\n\n");
        }
        writer.write("  while_" + saveGoto + ":\n");
        
        n.f4.accept(this, argu);

        writer.write("  br label %label_" + saveGoto +"\n\n");
        writer.write("  end_" + saveGoto + ":\n");

        varTmp = -1;
        thisFlag = false;    
        booleanReturn = -1;

        return null;
    }


    /**
     * f0 -> "true"
     */
    @Override
    public String visit(TrueLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        varTmp = -1;
        thisFlag = false;
        booleanReturn = 1;
        return "boolean";
    }

    /**
     * f0 -> "false"
     */
    @Override
    public String visit(FalseLiteral n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        varTmp = -1;
        thisFlag = false;
        booleanReturn = 0;
        return "boolean";
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    @Override
    public String visit(IntegerLiteral n, Void argu) throws Exception {
        intReturn = Integer.parseInt(n.f0.toString());
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
     */
    @Override
    public String visit(ThisExpression n, Void argu) throws Exception {
        n.f0.accept(this, argu);
        thisFlag = true;
        return curClass;
    }

    @Override
    public String visit(Identifier n, Void argu) throws Exception {
        return n.f0.toString();
    }

    @Override
    public String visit(ArrayType n, Void argu) {
        return "int[]";
    }

    @Override
    public String visit(BooleanType n, Void argu) {
        return "boolean";
    }

    @Override
    public String visit(IntegerType n, Void argu) {
        return "int";
    }


}
