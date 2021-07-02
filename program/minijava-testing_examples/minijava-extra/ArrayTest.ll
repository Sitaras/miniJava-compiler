@.ArrayTest_vtable = global [0 x i8*] []

@.Test_vtable = global [1 x i8*] [
i8* bitcast (i1 (i8*,i32)* @Test.start to i8*)
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
  %n = alloca i1
  store i1 0, i1* %n


  %i = alloca i32
  store i32 0, i32* %i


  store i32 0, i32* %i


  %_0 = call i8* @calloc(i32 1,i32 8)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.Test_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1

  %_3 = bitcast i8* %_0 to i8***
  %_4 = load i8**, i8*** %_3
  %_5 = getelementptr i8*, i8** %_4, i32 0
  %_6 = load i8*, i8** %_5
  %_7 = bitcast i8* %_6 to i1 (i8*,i32)* 
  %_8 = load i32, i32* %i
  %_9 = call i1 %_7(i8* %_0, i32 %_8)

  store i1 %_9, i1* %n

  ret i32 0
}

define i1 @Test.start(i8* %this, i32 %.sz) {
  %sz = alloca i32
  store i32 %.sz, i32* %sz
  %b = alloca i32*


  %l = alloca i32
  store i32 0, i32* %l


  %i = alloca i32
  store i32 0, i32* %i


  %_0 = add i32 1, 5
  %_1 = icmp sge i32 %_0, 1
  br i1 %_1, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_2 = call i8* @calloc( i32 4, i32 %_0)
  %_3 = bitcast i8* %_2 to i32*
  store i32 5, i32* %_3

  store i32* %_3, i32** %b

  %_4= load i32*, i32** %b
  %_5= getelementptr i32, i32* %_4, i32 0
  %_6= load i32, i32* %_5
  store i32 %_6, i32* %l

  store i32 0, i32* %i

  br label %label_1

  label_1:
  %_7 = load i32, i32* %i
  %_8 = load i32, i32* %l
  %_9 = icmp slt i32 %_7, %_8
  br i1 %_9, label %while_1, label %end_1

  while_1:
  %_10 = load i32, i32* %i
  %_11 = load i32, i32* %i
  %_12 = load i32*, i32** %b
  %_13 = load i32, i32* %_12
  %_14 = icmp sge i32 %_10, 0
  %_15 = icmp slt i32 %_10, %_13
  %_16 = and i1 %_14, %_15
  br i1 %_16, label %oob_ok_2, label %oob_err_2

  oob_err_2:
  call void @throw_oob()
  br label %oob_ok_2

  oob_ok_2:
  %_17 = add i32 1, %_10
  %_18 = getelementptr i32, i32* %_12, i32 %_17
  store i32 %_11, i32* %_18
  %_19 = load i32*, i32** %b
  %_20 = load i32, i32* %_19
  %_21 = load i32, i32* %i
  %_22 = icmp sge i32 %_21, 0
  %_23 = icmp slt i32 %_21, %_20
  %_24 = and i1 %_22, %_23
  br i1 %_24, label %oob_ok_3, label %oob_err_3

  oob_err_3:
  call void @throw_oob()
  br label %oob_ok_3

  oob_ok_3:
  %_25 = add i32 1, %_21
  %_26 = getelementptr i32, i32* %_19, i32 %_25
  %_27 = load i32, i32* %_26

  call void (i32) @print_int(i32 %_27)

  %_28 = load i32, i32* %i
  %_30 = add i32 %_28, 1
  store i32 %_30, i32* %i

  br label %label_1

  end_1:
  ret i1 1
}

