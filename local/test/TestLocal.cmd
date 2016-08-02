@echo off

set JDIR=c:\java\local\classes

pushd %JDIR%

cd

java -cp %JDIR% TestLocal %1 %2 %3 %4 %5 %6

popd
