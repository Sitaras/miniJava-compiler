import syntaxtree.*;
import java.util.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws Exception {

        if(args.length < 1){ // arguments should be more than one
            System.err.println("Program execution template: java [MainClassName] [file1] [file2] ... [fileN]");
            System.exit(1);
        }
        
        
        for(String fileName:args){

            System.out.println();
            for (int i = 0; i < fileName.length() + 6; i++) {
                System.out.print("-");
            }
            System.out.println();
            System.out.println(">> " + fileName +" <<");
            for(int i=0; i<fileName.length() + 6; i++){
                System.out.print("-");
            }
            System.out.println();
            

            FileInputStream fis = null;
            FirstVisitor.symbolTable.table.clear();
            try{
                fis = new FileInputStream(fileName);
                MiniJavaParser parser = new MiniJavaParser(fis);
                
                Goal root = parser.Goal();
                
                System.err.println("Program parsed successfully.");
                System.out.println("-----------------------------");
                
                /*****************************************************************/
                FirstVisitor first = new FirstVisitor();
                root.accept(first, null);
                
                SecondVisitor second = new SecondVisitor();
                root.accept(second, null);
                /*****************************************************************/
                System.err.println("Program is semantically valid.");
                System.out.println("-----------------------------");
                
                // offsets printing
                SymbolTable symbolTable = FirstVisitor.symbolTable;
                HashMap<String, String> classVarCounter = new HashMap<String, String>(); // to store the corresponding counter for the inherited classes
                HashMap<String, String> classFunCounter = new HashMap<String, String>(); // to store the corresponding counter for the inherited classes
                
                for (String className : symbolTable.table.keySet()) {

                    if(symbolTable.table.get(className).methods.containsKey("main")){ // skip the class that contains the main method
                        continue;
                    }
                    int varCounter=0; // offset counter for the variables
                    int methodCounter=0; // offset counter for the methods


                    if(symbolTable.table.get(className).parentName!=null){ // check if this class is inherited / has a parent class
                        if(classVarCounter.get(symbolTable.table.get(className).parentName)!=null) // check if the corresponding class has already a variables counter saved
                            varCounter=Integer.parseInt(classVarCounter.get(symbolTable.table.get(className).parentName)); // take this counter
                    }

                    for(String id:symbolTable.table.get(className).vars.keySet()){ // count and print the offsets for the variables
                        System.out.println(className + "." + id + ": " + varCounter);
                        
                        symbolTable.table.get(className).varsOffset.put(id, Integer.toString(varCounter)); // store the variabe name and the corresponding offset at the Symbol Table

                        if(symbolTable.table.get(className).vars.get(id) == "int"){
                            varCounter += 4;
                        }
                        else if(symbolTable.table.get(className).vars.get(id) == "boolean"){
                            varCounter += 1;
                        }
                        else{
                            varCounter += 8;
                        }
                    }


                    if (symbolTable.table.get(className).parentName != null) { // check if this class is inherited / has a parent class
                        if(classFunCounter.get(symbolTable.table.get(className).parentName)!=null) // check if the corresponding class has already a functions counter saved
                            methodCounter = Integer.parseInt(classFunCounter.get(symbolTable.table.get(className).parentName)); // take this counter
                    }

                    for(String method : symbolTable.table.get(className).methods.keySet()){ // count and print the offsets for the functions
                        String parent;
                        if((parent=symbolTable.getDeclarationClassOfMethod(className,method))!=null){ // check for method overriding
                            String tempCounter=symbolTable.table.get(parent).methodsOffset.get(method);
                            symbolTable.table.get(className).methodsOffset.put(method,tempCounter);
                            continue;
                        }
                        
                        System.out.println(className + "." + method + ": " + methodCounter);
                        
                        symbolTable.table.get(className).methodsOffset.put(method, Integer.toString(methodCounter)); // store the method name and the corresponding offset at the Symbol Table

                        methodCounter+=8;
                    }

                    classVarCounter.put(className,String.valueOf(varCounter)); // store the class and the corresponding variable counter at hash map
                    classFunCounter.put(className,String.valueOf(methodCounter)); // store the class and the corresponding function counter at hash map

                }

                /*****************************************************************/
                FirstVisitor.symbolTable = symbolTable;
                String[] fileNameArray;

                fileNameArray = fileName.split("\\.");
                String newFile = fileNameArray[0]+".ll";
                File file = new File(newFile);

                // Create the .ll file
                if (file.createNewFile()) {
                    System.out.println("File created!");
                } 
                else {
                    System.out.println("File already exists.");
                }

                // generate the intermediate code of the given Minijava source file (MiniJava -> LLVM)
                GeneratorVisitor gen = new GeneratorVisitor(file);
                root.accept(gen, null);
                gen.fileClose();
                /*****************************************************************/
            }
            catch(ParseException ex){
                System.out.println(ex.getMessage());
            }
            catch(FileNotFoundException ex){
                System.err.println(ex.getMessage());
            }
            finally{
                try{
                    if(fis != null) fis.close();
                }
                catch(IOException ex){
                    System.err.println(ex.getMessage());
                }
            }
        }
    }
}