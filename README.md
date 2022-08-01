## Mini Java Compiler

This is a MiniJava-to-LLVM-IR compiler project, written in Java (with the help of JFlex, JavaCUP and JTB), originally introduced as an assignment for the compilers course (NKUA). Details about the project can be found here (sections homework 2 & 3). I thank Stefanos Baziotis for his MiniJava testsuite contribution!
<hr/>

#### Parts 

Part 1: MiniJava Static Checking (Semantic Analysis)<br />
Part 2: Generating intermediate code (MiniJava -> LLVM)

### Getting Started

In order to see that the output of the produced LLVM IR files is the same as compiling the input java file with javac and executing it with java, you will need Clang with version >=4.0.0.

#### In Ubuntu Trusty
```
sudo apt update && sudo apt install clang-4.0
```

#### Compilation command
```
make
```

### Execution

To perform semantic analysis on all files given as arguments:

```
java [MainClassName] [file1] [file2] ... [fileN]
```
To execute .ll files:

``` 
clang-4.0 -o out1 ex.ll
./out1
```

