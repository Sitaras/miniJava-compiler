@.Main_vtable = global [0 x i8*] []

@.ArrayTest_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*,i32)* @ArrayTest.test to i8*)
]

@.B_vtable = global [1 x i8*] [
i8* bitcast (i32 (i8*,i32)* @B.test to i8*)
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
  %ab = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 20)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [1 x i8*], [1 x i8*]* @.ArrayTest_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %ab


  %_3 = load i8*, i8** %ab
  %_4 = bitcast i8* %_3 to i8***
  %_5 = load i8**, i8*** %_4
  %_6 = getelementptr i8*, i8** %_5, i32 0
  %_7 = load i8*, i8** %_6
  %_8 = bitcast i8* %_7 to i32 (i8*,i32)* 
  %_9 = call i32 %_8(i8* %_3, i32 3)

  call void (i32) @print_int(i32 %_9)

  ret i32 0
}

define i32 @ArrayTest.test(i8* %this, i32 %.num) {
  %num = alloca i32
  store i32 %.num, i32* %num
  %i = alloca i32
  store i32 0, i32* %i


  %intArray = alloca i32*


  %_0 = load i32, i32* %num
  %_1 = add i32 1, %_0
  %_2 = icmp sge i32 %_1, 1
  br i1 %_2, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_3 = call i8* @calloc( i32 4, i32 %_1)
  %_4 = bitcast i8* %_3 to i32*
  store i32 %_0, i32* %_4

  store i32* %_4, i32** %intArray

  %_5 = getelementptr i8, i8* %this, i32 16
  %_6 = bitcast i8* %_5 to i32*
  store i32 0, i32* %_6

  %_7 = getelementptr i8, i8* %this, i32 16
  %_8 = bitcast i8* %_7 to i32*
  %_9= load i32, i32* %_8
  call void (i32) @print_int(i32 %_9)

  %_10= load i32*, i32** %intArray
  %_11= getelementptr i32, i32* %_10, i32 0
  %_12= load i32, i32* %_11
  call void (i32) @print_int(i32 %_12)

  store i32 0, i32* %i

  call void (i32) @print_int(i32 111)

  br label %label_1

  label_1:
  %_13= load i32*, i32** %intArray
  %_14= getelementptr i32, i32* %_13, i32 0
  %_15= load i32, i32* %_14
  %_16 = load i32, i32* %i
  %_17 = icmp slt i32 %_16, %_15
  br i1 %_17, label %while_1, label %end_1

  while_1:
  %_18 = load i32, i32* %i
  %_20 = add i32 %_18, 1
  call void (i32) @print_int(i32 %_20)

  %_21 = load i32, i32* %i
  %_22 = load i32, i32* %i
  %_24 = add i32 %_22, 1
  %_25 = load i32*, i32** %intArray
  %_26 = load i32, i32* %_25
  %_27 = icmp sge i32 %_21, 0
  %_28 = icmp slt i32 %_21, %_26
  %_29 = and i1 %_27, %_28
  br i1 %_29, label %oob_ok_2, label %oob_err_2

  oob_err_2:
  call void @throw_oob()
  br label %oob_ok_2

  oob_ok_2:
  %_30 = add i32 1, %_21
  %_31 = getelementptr i32, i32* %_25, i32 %_30
  store i32 %_24, i32* %_31
  %_32 = load i32, i32* %i
  %_34 = add i32 %_32, 1
  store i32 %_34, i32* %i

  br label %label_1

  end_1:
  call void (i32) @print_int(i32 222)

  store i32 0, i32* %i

  br label %label_3

  label_3:
  %_35= load i32*, i32** %intArray
  %_36= getelementptr i32, i32* %_35, i32 0
  %_37= load i32, i32* %_36
  %_38 = load i32, i32* %i
  %_39 = icmp slt i32 %_38, %_37
  br i1 %_39, label %while_3, label %end_3

  while_3:
  %_40 = load i32*, i32** %intArray
  %_41 = load i32, i32* %_40
  %_42 = load i32, i32* %i
  %_43 = icmp sge i32 %_42, 0
  %_44 = icmp slt i32 %_42, %_41
  %_45 = and i1 %_43, %_44
  br i1 %_45, label %oob_ok_4, label %oob_err_4

  oob_err_4:
  call void @throw_oob()
  br label %oob_ok_4

  oob_ok_4:
  %_46 = add i32 1, %_42
  %_47 = getelementptr i32, i32* %_40, i32 %_46
  %_48 = load i32, i32* %_47

  call void (i32) @print_int(i32 %_48)

  %_49 = load i32, i32* %i
  %_51 = add i32 %_49, 1
  store i32 %_51, i32* %i

  br label %label_3

  end_3:
  call void (i32) @print_int(i32 333)

  %_52= load i32*, i32** %intArray
  %_53= getelementptr i32, i32* %_52, i32 0
  %_54= load i32, i32* %_53
  ret i32 %_54
}

define i32 @B.test(i8* %this, i32 %.num) {
  %num = alloca i32
  store i32 %.num, i32* %num
  %i = alloca i32
  store i32 0, i32* %i


  %intArray = alloca i32*


  %_0 = load i32, i32* %num
  %_1 = add i32 1, %_0
  %_2 = icmp sge i32 %_1, 1
  br i1 %_2, label %nsz_ok_0, label %nsz_err_0

  nsz_err_0:
  call void @throw_nsz()
  br label %nsz_ok_0

  nsz_ok_0:
  %_3 = call i8* @calloc( i32 4, i32 %_1)
  %_4 = bitcast i8* %_3 to i32*
  store i32 %_0, i32* %_4

  store i32* %_4, i32** %intArray

  %_5 = getelementptr i8, i8* %this, i32 20
  %_6 = bitcast i8* %_5 to i32*
  store i32 12, i32* %_6

  %_7 = getelementptr i8, i8* %this, i32 20
  %_8 = bitcast i8* %_7 to i32*
  %_9= load i32, i32* %_8
  call void (i32) @print_int(i32 %_9)

  %_10= load i32*, i32** %intArray
  %_11= getelementptr i32, i32* %_10, i32 0
  %_12= load i32, i32* %_11
  call void (i32) @print_int(i32 %_12)

  store i32 0, i32* %i

  call void (i32) @print_int(i32 111)

  br label %label_1

  label_1:
  %_13= load i32*, i32** %intArray
  %_14= getelementptr i32, i32* %_13, i32 0
  %_15= load i32, i32* %_14
  %_16 = load i32, i32* %i
  %_17 = icmp slt i32 %_16, %_15
  br i1 %_17, label %while_1, label %end_1

  while_1:
  %_18 = load i32, i32* %i
  %_20 = add i32 %_18, 1
  call void (i32) @print_int(i32 %_20)

  %_21 = load i32, i32* %i
  %_22 = load i32, i32* %i
  %_24 = add i32 %_22, 1
  %_25 = load i32*, i32** %intArray
  %_26 = load i32, i32* %_25
  %_27 = icmp sge i32 %_21, 0
  %_28 = icmp slt i32 %_21, %_26
  %_29 = and i1 %_27, %_28
  br i1 %_29, label %oob_ok_2, label %oob_err_2

  oob_err_2:
  call void @throw_oob()
  br label %oob_ok_2

  oob_ok_2:
  %_30 = add i32 1, %_21
  %_31 = getelementptr i32, i32* %_25, i32 %_30
  store i32 %_24, i32* %_31
  %_32 = load i32, i32* %i
  %_34 = add i32 %_32, 1
  store i32 %_34, i32* %i

  br label %label_1

  end_1:
  call void (i32) @print_int(i32 222)

  store i32 0, i32* %i

  br label %label_3

  label_3:
  %_35= load i32*, i32** %intArray
  %_36= getelementptr i32, i32* %_35, i32 0
  %_37= load i32, i32* %_36
  %_38 = load i32, i32* %i
  %_39 = icmp slt i32 %_38, %_37
  br i1 %_39, label %while_3, label %end_3

  while_3:
  %_40 = load i32*, i32** %intArray
  %_41 = load i32, i32* %_40
  %_42 = load i32, i32* %i
  %_43 = icmp sge i32 %_42, 0
  %_44 = icmp slt i32 %_42, %_41
  %_45 = and i1 %_43, %_44
  br i1 %_45, label %oob_ok_4, label %oob_err_4

  oob_err_4:
  call void @throw_oob()
  br label %oob_ok_4

  oob_ok_4:
  %_46 = add i32 1, %_42
  %_47 = getelementptr i32, i32* %_40, i32 %_46
  %_48 = load i32, i32* %_47

  call void (i32) @print_int(i32 %_48)

  %_49 = load i32, i32* %i
  %_51 = add i32 %_49, 1
  store i32 %_51, i32* %i

  br label %label_3

  end_3:
  call void (i32) @print_int(i32 333)

  %_52= load i32*, i32** %intArray
  %_53= getelementptr i32, i32* %_52, i32 0
  %_54= load i32, i32* %_53
  ret i32 %_54
}

