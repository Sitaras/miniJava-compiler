@.test20_vtable = global [0 x i8*] []

@.A23_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*,i8*)* @A23.init to i8*),
i8* bitcast (i32 (i8*)* @A23.getI1 to i8*),
i8* bitcast (i32 (i8*,i32)* @A23.setI1 to i8*)
]

@.B23_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*,i8*)* @B23.init to i8*),
i8* bitcast (i32 (i8*)* @B23.getI1 to i8*),
i8* bitcast (i32 (i8*,i32)* @B23.setI1 to i8*)
]

@.C23_vtable = global [3 x i8*] [
i8* bitcast (i32 (i8*,i8*)* @C23.init to i8*),
i8* bitcast (i32 (i8*)* @C23.getI1 to i8*),
i8* bitcast (i32 (i8*,i32)* @C23.setI1 to i8*)
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

  %_0 = call i8* @calloc(i32 1,i32 36)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [3 x i8*], [3 x i8*]* @.C23_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1

  %_3 = bitcast i8* %_0 to i8***
  %_4 = load i8**, i8*** %_3
  %_5 = getelementptr i8*, i8** %_4, i32 0
  %_6 = load i8*, i8** %_5
  %_7 = bitcast i8* %_6 to i32 (i8*,i8*)* 

  %_8 = call i8* @calloc(i32 1,i32 28)
  %_9 = bitcast i8* %_8 to i8***
  %_10 = getelementptr [3 x i8*], [3 x i8*]* @.B23_vtable, i32 0, i32 0
  store i8** %_10, i8*** %_9
  %_11 = call i32 %_7(i8* %_0, i8* %_8)

  call void (i32) @print_int(i32 %_11)

  ret i32 0
}

define i32 @A23.init(i8* %this, i8* %.a) {
  %a = alloca i8*
  store i8* %.a, i8** %a

  %_0 = load i8*, i8** %a
  %_1 = bitcast i8* %_0 to i8***
  %_2 = load i8**, i8*** %_1
  %_3 = getelementptr i8*, i8** %_2, i32 1
  %_4 = load i8*, i8** %_3
  %_5 = bitcast i8* %_4 to i32 (i8*)* 
  %_6 = call i32 %_5(i8* %_0)

  %_7 = getelementptr i8, i8* %this, i32 12
  %_8 = bitcast i8* %_7 to i32*
  store i32 %_6, i32* %_8

  %_9 = getelementptr i8, i8* %this, i32 16
  %_10 = bitcast i8* %_9 to i32*
  store i32 222, i32* %_10


  %_11 = bitcast i8* %this to i8***
  %_12 = load i8**, i8*** %_11
  %_13 = getelementptr i8*, i8** %_12, i32 2
  %_14 = load i8*, i8** %_13
  %_15 = bitcast i8* %_14 to i32 (i8*,i32)* 
  %_16 = getelementptr i8, i8* %this, i32 12
  %_17 = bitcast i8* %_16 to i32*
  %_18 = load i32, i32* %_17
  %_19 = getelementptr i8, i8* %this, i32 16
  %_20 = bitcast i8* %_19 to i32*
  %_22 = load i32, i32* %_20
  %_23 = add i32 %_18, %_22
  %_24 = call i32 %_15(i8* %this, i32 %_23)

  %_25 = getelementptr i8, i8* %this, i32 8
  %_26 = bitcast i8* %_25 to i32*
  store i32 %_24, i32* %_26

  %_27 = getelementptr i8, i8* %this, i32 8
  %_28 = bitcast i8* %_27 to i32*
  %_29 = load i32, i32* %_28
  ret i32 %_29
}

define i32 @A23.getI1(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 8
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  ret i32 %_2
}

define i32 @A23.setI1(i8* %this, i32 %.i) {
  %i = alloca i32
  store i32 %.i, i32* %i
  %_0 = load i32, i32* %i
  ret i32 %_0
}

define i32 @B23.init(i8* %this, i8* %.a) {
  %a = alloca i8*
  store i8* %.a, i8** %a
  %a_local = alloca i8*



  %_0 = call i8* @calloc(i32 1,i32 20)
  %_1 = bitcast i8* %_0 to i8***
  %_2 = getelementptr [3 x i8*], [3 x i8*]* @.A23_vtable, i32 0, i32 0
  store i8** %_2, i8*** %_1
  store i8* %_0, i8** %a_local


  %_3 = load i8*, i8** %a
  %_4 = bitcast i8* %_3 to i8***
  %_5 = load i8**, i8*** %_4
  %_6 = getelementptr i8*, i8** %_5, i32 1
  %_7 = load i8*, i8** %_6
  %_8 = bitcast i8* %_7 to i32 (i8*)* 
  %_9 = call i32 %_8(i8* %_3)

  %_10 = getelementptr i8, i8* %this, i32 24
  %_11 = bitcast i8* %_10 to i32*
  store i32 %_9, i32* %_11


  %_12 = bitcast i8* %this to i8***
  %_13 = load i8**, i8*** %_12
  %_14 = getelementptr i8*, i8** %_13, i32 2
  %_15 = load i8*, i8** %_14
  %_16 = bitcast i8* %_15 to i32 (i8*,i32)* 
  %_17 = getelementptr i8, i8* %this, i32 24
  %_18 = bitcast i8* %_17 to i32*
  %_19 = load i32, i32* %_18
  %_20 = call i32 %_16(i8* %this, i32 %_19)

  %_21 = getelementptr i8, i8* %this, i32 20
  %_22 = bitcast i8* %_21 to i32*
  store i32 %_20, i32* %_22


  %_23 = load i8*, i8** %a_local
  %_24 = bitcast i8* %_23 to i8***
  %_25 = load i8**, i8*** %_24
  %_26 = getelementptr i8*, i8** %_25, i32 0
  %_27 = load i8*, i8** %_26
  %_28 = bitcast i8* %_27 to i32 (i8*,i8*)* 
  %_29 = call i32 %_28(i8* %_23, i8* %this)

  ret i32 %_29
}

define i32 @B23.getI1(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 20
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  ret i32 %_2
}

define i32 @B23.setI1(i8* %this, i32 %.i) {
  %i = alloca i32
  store i32 %.i, i32* %i
  %_0 = load i32, i32* %i
  %_2 = add i32 %_0, 111
  ret i32 %_2
}

define i32 @C23.init(i8* %this, i8* %.a) {
  %a = alloca i8*
  store i8* %.a, i8** %a
  %_0 = getelementptr i8, i8* %this, i32 32
  %_1 = bitcast i8* %_0 to i32*
  store i32 333, i32* %_1


  %_2 = bitcast i8* %this to i8***
  %_3 = load i8**, i8*** %_2
  %_4 = getelementptr i8*, i8** %_3, i32 2
  %_5 = load i8*, i8** %_4
  %_6 = bitcast i8* %_5 to i32 (i8*,i32)* 
  %_7 = getelementptr i8, i8* %this, i32 32
  %_8 = bitcast i8* %_7 to i32*
  %_9 = load i32, i32* %_8
  %_10 = call i32 %_6(i8* %this, i32 %_9)

  %_11 = getelementptr i8, i8* %this, i32 28
  %_12 = bitcast i8* %_11 to i32*
  store i32 %_10, i32* %_12


  %_13 = load i8*, i8** %a
  %_14 = bitcast i8* %_13 to i8***
  %_15 = load i8**, i8*** %_14
  %_16 = getelementptr i8*, i8** %_15, i32 0
  %_17 = load i8*, i8** %_16
  %_18 = bitcast i8* %_17 to i32 (i8*,i8*)* 
  %_19 = call i32 %_18(i8* %_13, i8* %this)

  ret i32 %_19
}

define i32 @C23.getI1(i8* %this) {
  %_0 = getelementptr i8, i8* %this, i32 28
  %_1 = bitcast i8* %_0 to i32*
  %_2 = load i32, i32* %_1
  ret i32 %_2
}

define i32 @C23.setI1(i8* %this, i32 %.i) {
  %i = alloca i32
  store i32 %.i, i32* %i
  %_0 = load i32, i32* %i
  %_2 = mul i32 %_0, 2
  ret i32 %_2
}

