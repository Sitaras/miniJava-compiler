@.test17_vtable = global [0 x i8*] []

@.Test_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*)* @Test.start to i8*),
i8* bitcast (i8* (i8*,i8*)* @Test.first to i8*),
i8* bitcast (i32 (i8*)* @Test.second to i8*)
]


declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)

@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"
@_cNSZ = constant [15 x i8] c"Negative size\0a\00"
define void @print_int(i32 %i) {
  %_str = bitcast [4 x i8]* @_cint to i8*
  call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
  ret void
}

define void @throw_oob() {
  %_str = bitcast [15 x i8]* @_cOOB to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}

define void @throw_nsz() {
  %_str = bitcast [15 x i8]* @_cNSZ to i8*
  call i32 (i8*, ...) @printf(i8* %_str)
  call void @exit(i32 1)
  ret void
}

define i32 @main() {

  %_0 = call i8* @calloc(i32 1,i32 12)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [3 x i8*], [3 x i8*]* @.Test_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1

  %_3 = bitcast i8* %_0 to i8***
  %_4 = load i8**, i8*** %_3
  %_5 = getelementptr i8*, i8** %_4, i32 0
  %_6 = load i8*, i8** %_5
  %_7 = bitcast i8* %_6 to i32 (i8*)* 
  %_8 = call i32 %_7(i8* %_0)

  call void (i32) @print_int(i32 %_8)

  ret i32 0
}

define i32 @Test.start(i8* %this) {
  %test = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 12)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [3 x i8*], [3 x i8*]* @.Test_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %test

  %_3 = getelementptr i8, i8* %this, i32 8
  %_4 = bitcast i8* %_3 to i32*
  store i32 10, i32* %_4

  %_5 = getelementptr i8, i8* %this, i32 8
  %_6 = bitcast i8* %_5 to i32*
  %_7 = load i32, i32* %_6

  %_8 = load i8*, i8** %test
  %_9 = bitcast i8* %_8 to i8***
  %_10 = load i8**, i8*** %_9
  %_11 = getelementptr i8*, i8** %_10, i32 1
  %_12 = load i8*, i8** %_11
  %_13 = bitcast i8* %_12 to i8* (i8*,i8*)* 
  %_14 = call i8* %_13(i8* %_8, i8* %this)


  %_15 = bitcast i8* %_14 to i8***
  %_16 = load i8**, i8*** %_15
  %_17 = getelementptr i8*, i8** %_16, i32 2
  %_18 = load i8*, i8** %_17
  %_19 = bitcast i8* %_18 to i32 (i8*)* 
  %_20 = call i32 %_19(i8* %_14)

  %_22 = add i32 %_7, %_20
  %_23 = getelementptr i8, i8* %this, i32 8
  %_24 = bitcast i8* %_23 to i32*
  store i32 %_22, i32* %_24

  %_25 = getelementptr i8, i8* %this, i32 8
  %_26 = bitcast i8* %_25 to i32*
  %_27 = load i32, i32* %_26
  ret i32 %_27
}

define i8* @Test.first(i8* %this, i8* %.test2) {
  %test2 = alloca i8*
  store i8* %.test2, i8** %test2
  %test3 = alloca i8*


  %_0 = load i8*, i8** %test2
  store i8* %_0, i8** %test3

  %_1 = load i8*, i8** %test3
  ret i8* %_1
}

define i32 @Test.second(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 8
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  %_4 = add i32 %_2, 10
  %_5 = getelementptr i8, i8* %this, i32 8
  %_6 = bitcast i8* %_5 to i32*
  store i32 %_4, i32* %_6

  %_7 = getelementptr i8, i8* %this, i32 8
  %_8 = bitcast i8* %_7 to i32*
  %_9 = load i32, i32* %_8
  ret i32 %_9
}

