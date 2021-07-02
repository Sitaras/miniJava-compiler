@.test93_vtable = global [0 x i8*] []

@.Test_vtable = global [2 x i8*] [
i8* bitcast (i32 (i8*)* @Test.start to i8*),
i8* bitcast (i8* (i8*)* @Test.next to i8*)
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

  %_0 = call i8* @calloc(i32 1,i32 24)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [2 x i8*], [2 x i8*]* @.Test_vtable, i32 0, i32 0
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
  %_0 = add i32 1, 10
  %_1 = icmp sge i32 %_0, 1
  br i1 %_1, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_2 = call i8* @calloc( i32 4, i32 %_0)
  %_3 = bitcast i8* %_2 to i32*
  store i32 10, i32* %_3

  %_4 = getelementptr i8, i8* %this, i32 16
  %_5 = bitcast i8* %_4 to i32**
  store i32* %_3, i32** %_5


  %_6 = call i8* @calloc(i32 1,i32 24)
  %_7 = bitcast i8* %_6 to i8***
  %_8 = getelementptr [2 x i8*], [2 x i8*]* @.Test_vtable, i32 0, i32 0
  store i8** %_8, i8*** %_7
  %_9 = getelementptr i8, i8* %this, i32 8
  %_10 = bitcast i8* %_9 to i8**
  store i8* %_6, i8** %_10


  %_12 = getelementptr i8, i8* %this, i32 8
  %_13 = bitcast i8* %_12 to i8**
  %_14 = load i8*, i8** %_13
  %_15 = bitcast i8* %_14 to i8***
  %_16 = load i8**, i8*** %_15
  %_17 = getelementptr i8*, i8** %_16, i32 1
  %_18 = load i8*, i8** %_17
  %_19 = bitcast i8* %_18 to i8* (i8*)* 
  %_20 = call i8* %_19(i8* %_14)


  %_21 = bitcast i8* %_20 to i8***
  %_22 = load i8**, i8*** %_21
  %_23 = getelementptr i8*, i8** %_22, i32 1
  %_24 = load i8*, i8** %_23
  %_25 = bitcast i8* %_24 to i8* (i8*)* 
  %_26 = call i8* %_25(i8* %_20)

  %_27 = getelementptr i8, i8* %this, i32 8
  %_28 = bitcast i8* %_27 to i8**
  store i8* %_26, i8** %_28

  ret i32 0
}

define i8* @Test.next(i8* %this) {

  %_0 = call i8* @calloc(i32 1,i32 24)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [2 x i8*], [2 x i8*]* @.Test_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  %_3 = getelementptr i8, i8* %this, i32 8
  %_4 = bitcast i8* %_3 to i8**
  store i8* %_0, i8** %_4

  %_5 = getelementptr i8, i8* %this, i32 8
  %_6 = bitcast i8* %_5 to i8**
  %_7 = load i8*, i8** %_6
  ret i8* %_7
}

